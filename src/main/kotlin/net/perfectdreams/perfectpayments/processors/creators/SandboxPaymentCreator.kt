package net.perfectdreams.perfectpayments.processors.creators

import kotlinx.serialization.json.JsonObject
import net.perfectdreams.perfectpayments.PerfectPayments
import net.perfectdreams.perfectpayments.utils.PartialPayment

class SandboxPaymentCreator(val m: PerfectPayments) : PaymentCreator {
    override suspend fun createPayment(paymentId: Long, partialPayment: PartialPayment, data: JsonObject): String {
        // This is a hacky workaround: Sends the Payment ID back because we don't have a Payment URL
        return paymentId.toString()
    }
}