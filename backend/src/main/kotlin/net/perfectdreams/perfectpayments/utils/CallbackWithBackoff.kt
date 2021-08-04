package net.perfectdreams.perfectpayments.utils

import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlin.math.pow

suspend fun callbackWithBackoff(callback: suspend () -> (Boolean), failure: suspend (Long) -> (Unit)) {
    GlobalScope.launch {
        var requestsMade = 0

        while (true) {
            val success = callback.invoke()

            if (success)
                break

            requestsMade++
            val waitTime = requestsMade.toDouble()
                .pow(2)
                .toLong() * 1000

            failure.invoke(waitTime)

            delay(waitTime)
        }
    }
}