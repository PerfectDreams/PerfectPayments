package net.perfectdreams.perfectpayments.processors.creators

import kotlinx.serialization.json.JsonObject
import net.perfectdreams.perfectpayments.utils.PartialPayment

interface PaymentCreator {
    suspend fun createPayment(paymentId: Long, partialPayment: PartialPayment, data: JsonObject): String
}