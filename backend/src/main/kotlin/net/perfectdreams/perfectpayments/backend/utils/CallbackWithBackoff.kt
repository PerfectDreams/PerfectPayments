package net.perfectdreams.perfectpayments.backend.utils

import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlin.math.pow

fun callbackWithBackoff(callback: suspend () -> (Boolean), failure: suspend (Throwable, Long) -> (Unit), afterSuccess: suspend () -> (Unit) = {}) {
    GlobalScope.launch {
        var requestsMade = 0

        while (true) {
            try {
                val success = callback.invoke()

                if (success) {
                    afterSuccess.invoke()
                    break
                }
            } catch (e: Throwable) {
                requestsMade++
                val waitTime = if (e is BackoffWithCustomTimeException)
                    e.waitTimeInMillis
                else (requestsMade.toDouble().pow(2).toLong() * 1000).coerceAtMost((60 * 5) * 1_000) // at most 5 minutes

                failure.invoke(e, waitTime)

                delay(waitTime)
            }
        }
    }
}

class BackoffWithCustomTimeException(val waitTimeInMillis: Long) : RuntimeException()