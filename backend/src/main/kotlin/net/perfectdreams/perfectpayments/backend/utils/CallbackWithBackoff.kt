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
                val waitTime = requestsMade.toDouble()
                    .pow(2)
                    .toLong() * 1000

                failure.invoke(e, waitTime)

                delay(waitTime)
            }
        }
    }
}