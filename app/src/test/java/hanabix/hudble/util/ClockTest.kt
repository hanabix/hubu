package hanabix.hudble.util

import kotlinx.coroutines.flow.take
import kotlinx.coroutines.flow.toList
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.LocalTime
import java.time.format.DateTimeFormatter
import java.util.Locale
import java.util.regex.Pattern

class ClockTest {

    @Test
    fun emitsFormattedTime() = runBlocking {
        val subject = Clock(
            formatter = DateTimeFormatter.ofPattern("HH:mm:ss", Locale.ROOT),
            millisecondFrom = { 10 }
        )

        val result = subject.now().take(1).toList().first()

        assertTrue(Pattern.matches("\\d{2}:\\d{2}:\\d{2}", result))
    }

    @Test
    fun emitsMultipleValues() = runBlocking {
        val subject = Clock(
            millisecondFrom = { 10 }
        )

        val results = subject.now().take(3).toList()

        assertEquals(3, results.size)
    }

    @Test
    fun usesDefaultFormat() = runBlocking {
        val subject = Clock(
            millisecondFrom = { 10 }
        )

        val result = subject.now().take(1).toList().first()

        assertTrue(Pattern.matches("\\d{2}:\\d{2}", result))
    }

    @Test
    fun customFormatterWorks() = runBlocking {
        val customFormatter = DateTimeFormatter.ofPattern("mm", Locale.ROOT)
        val subject = Clock(
            formatter = customFormatter,
            millisecondFrom = { 10 }
        )

        val result = subject.now().take(1).toList().first()

        assertTrue(result.toInt() in 0..59)
    }

    @Test
    fun customDelayWorks() = runBlocking {
        var callCount = 0
        val subject = Clock(
            millisecondFrom = {
                callCount++
                10
            }
        )

        // take(3) cancels during the 3rd emit, so millisecondFrom is called only twice
        subject.now().take(3).toList()

        assertEquals(2, callCount)
    }
}
