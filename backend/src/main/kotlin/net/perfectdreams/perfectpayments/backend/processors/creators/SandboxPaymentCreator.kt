package net.perfectdreams.perfectpayments.backend.processors.creators

import kotlinx.serialization.json.JsonObject
import net.perfectdreams.perfectpayments.backend.PerfectPayments
import net.perfectdreams.perfectpayments.backend.utils.PartialPayment

class SandboxPaymentCreator(val m: PerfectPayments) : PaymentCreator {
    override suspend fun createPayment(paymentId: Long, partialPayment: PartialPayment, data: JsonObject): CreatedPaymentInfo {
        // This is a hacky workaround: Sends the Payment ID back because we don't have a Payment URL
        return CreatedSandboxPaymentInfo(
            paymentId.toString()
        )
    }
}