package hanabix.hubu.util

import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import java.time.format.DateTimeFormatter
import java.time.LocalTime
import java.util.Locale

class Clock(
    private val formatter: DateTimeFormatter = DateTimeFormatter.ofPattern("HH:mm", Locale.ROOT),
    private val millisecondFrom: (LocalTime) -> Long = { now -> 60_000L - (now.second * 1000L + now.nano / 1_000_000L) },
) {
    fun now(): Flow<String> = flow {
        while (true) {
            val now = LocalTime.now()
            emit(now.format(formatter))
            delay(millisecondFrom(now))
        }
    }
}
