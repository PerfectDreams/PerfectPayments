package net.perfectdreams.perfectpayments.backend.processors.creators

import kotlinx.serialization.json.JsonObject
import net.perfectdreams.perfectpayments.backend.utils.PartialPayment

interface PaymentCreator {
    suspend fun createPayment(paymentId: Long, partialPayment: PartialPayment, data: JsonObject): String
}