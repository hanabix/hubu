package hanabix.hudble.ble

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.channelFlow
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.onCompletion
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.take
import kotlinx.coroutines.flow.timeout
import kotlinx.coroutines.FlowPreview
import kotlin.time.Duration
import kotlin.time.Duration.Companion.seconds

@OptIn(FlowPreview::class)
internal class DefaultBleGather<T>(
    private val scope: CoroutineScope,
    private val scan: BleScan<T>,
    private val connect: BleConnect<T>,
    private val info: BleInfo<T>,
    private val timeout: Duration = 5.seconds,
) : BleGather {
    override fun invoke(metrics: List<BleMetric>): Flow<BleEvent> = channelFlow {
        val bus = Channel<Event<T>>(Channel.UNLIMITED)
        var state = State<T>(metrics = metrics)

        val fire: ToConnect<T> = ToConnect { device, requested ->
            connect(requested)(device)
                .onEach { event -> bus.trySend(Event.Reply(event)) }
                .launchIn(scope)
        }

        val handle = DefaultDispatch(fire, info) { event ->
            when (event) {
                BleEvent.Unavailable -> {
                    trySend(event)
                    close()
                }

                else -> trySend(event)
            }
        }

        val react = bus.receiveAsFlow()
            .onEach { event -> state = handle(state, event) }
            .launchIn(scope)

        val scanning = scan(metrics)
            .take(metrics.size)
            .timeout(timeout)
            .onEach { value -> bus.trySend(Event.Found(value)) }
            .onCompletion { bus.trySend(Event.NoMoreDevice) }
            .launchIn(scope)

        awaitClose {
            scanning.cancel()
            state.jobs.values.forEach { it.cancel() }
            react.cancel()
            bus.close()
        }
    }
}

private data class State<T>(
    val metrics: List<BleMetric>,
    val pending: List<T> = emptyList(),
    val solid: Boolean = false,
    val jobs: Map<String, Job> = emptyMap(),
)

private sealed interface Event<out T> {
    data class Found<T>(val value: T) : Event<T>
    data object NoMoreDevice : Event<Nothing>
    data class Reply<T>(
        val event: BleConnectEvent<T>,
    ) : Event<T>
}

private fun interface Dispatch<T> {
    operator fun invoke(state: State<T>, event: Event<T>): State<T>
}

private class DefaultDispatch<T>(
    private val fire: ToConnect<T>,
    private val info: BleInfo<T>,
    private val send: (BleEvent) -> Unit,
) : Dispatch<T> {
    override fun invoke(state: State<T>, event: Event<T>): State<T> {
        return when (event) {
            is Event.Found -> onFound(state, event.value)
            is Event.Reply -> onReply(state, event.event)
            Event.NoMoreDevice -> onNoMoreDevice(state)
        }
    }

    private fun onFound(
        state: State<T>,
        value: T,
    ): State<T> {
        val (metrics, pending, _, jobs) = state
        val next = pending + value

        return when (metrics.isEmpty()) {
            true -> state.copy(pending = next)
            false -> {
                val first = next.first()
                val id = info.id(first)

                state.copy(
                    metrics = emptyList(),
                    pending = next.drop(1),
                    jobs = jobs + (id to fire(first, metrics)),
                )
            }
        }
    }

    private fun onReply(
        state: State<T>,
        reply: BleConnectEvent<T>,
    ): State<T> = when (reply) {
        is BleConnectEvent.Unsupported -> {
            val (device, part, metrics) = reply
            onUnsupported(state, device, part, metrics)
        }

        is BleConnectEvent.Notify -> {
            val (device, meter) = reply
            onNotify(state, device, meter)
        }

        is BleConnectEvent.Fatal -> {
            val (device, _) = reply
            onFatal(state, device)
        }
    }

    private fun onUnsupported(
        state: State<T>,
        value: T,
        part: Boolean,
        metrics: List<BleMetric>,
    ): State<T> {
        val (_, pending, solid, jobs) = state
        val id = info.id(value)
        val actives = when (part) {
            true -> jobs.filterValues(Job::isActive)
            false -> (jobs - id).filterValues(Job::isActive)
        }

        return when {
            pending.isNotEmpty() -> {
                val head = pending.first()

                state.copy(
                    metrics = emptyList(),
                    pending = pending.drop(1),
                    jobs = actives + (info.id(head) to fire(head, metrics)),
                )
            }

            solid && actives.isEmpty() -> {
                send(BleEvent.Unavailable)
                state.copy(
                    metrics = metrics,
                    jobs = actives,
                )
            }

            else -> state.copy(
                metrics = metrics,
                jobs = actives,
            )
        }
    }

    private fun onNotify(
        state: State<T>,
        value: T,
        meter: BleMeter,
    ): State<T> {
        send(BleEvent.Available(device = info.name(value), meter = meter))
        return state
    }

    private fun onFatal(
        state: State<T>,
        value: T,
    ): State<T> {
        val next = state.copy(
            jobs = (state.jobs - info.id(value)).filterValues(Job::isActive),
        )
        return when {
            next.solid && next.jobs.isEmpty() && next.pending.isEmpty() -> {
                send(BleEvent.Unavailable)
                next
            }

            else -> next
        }
    }

    private fun onNoMoreDevice(
        state: State<T>,
    ): State<T> {
        val next = state.copy(
            solid = true,
            jobs = state.jobs.filterValues(Job::isActive),
        )
        return when {
            next.jobs.isEmpty() && next.pending.isEmpty() -> {
                send(BleEvent.Unavailable)
                next
            }

            else -> next
        }
    }

}
