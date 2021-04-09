package net.perfectdreams.perfectpayments.routes.api.v1.payments

import io.ktor.application.*
import io.ktor.http.*
import io.ktor.request.*
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import mu.KotlinLogging
import net.perfectdreams.perfectpayments.PerfectPayments
import net.perfectdreams.perfectpayments.dao.Payment
import net.perfectdreams.perfectpayments.payments.PaymentStatus
import net.perfectdreams.perfectpayments.routes.api.v1.RequiresAPIAuthenticationRoute
import net.perfectdreams.perfectpayments.utils.PaymentQuery
import net.perfectdreams.perfectpayments.utils.extensions.receiveTextUTF8
import net.perfectdreams.perfectpayments.utils.extensions.respondEmptyJson
import org.jetbrains.exposed.sql.transactions.experimental.newSuspendedTransaction

class PatchChangePaymentStatusRoute(m: PerfectPayments) : RequiresAPIAuthenticationRoute(m, "/api/v1/payments/{internalTransactionId}") {
    companion object {
        private val logger = KotlinLogging.logger {}
    }

    override suspend fun onAuthenticatedRequest(call: ApplicationCall) {
        val internalTransactionId = call.parameters["internalTransactionId"]!!.toLong()

        val paymentPayload = call.receiveTextUTF8()

        val body = Json.parseToJsonElement(paymentPayload)
            .jsonObject

        val status = PaymentStatus.valueOf(body["status"]!!.jsonPrimitive.content.toUpperCase())

        logger.info { "Received request to change payment $internalTransactionId status to $status" }

        val internalPayment = newSuspendedTransaction {
            Payment.findById(internalTransactionId)
        }

        if (internalPayment == null) {
            call.respondEmptyJson(HttpStatusCode.NotFound)
            return
        }

        logger.info { "Setting payment $internalTransactionId status to $status" }

        if (internalPayment.status != status) {
            newSuspendedTransaction {
                internalPayment.status = status

                if (status == PaymentStatus.APPROVED)
                    internalPayment.paidAt = System.currentTimeMillis()
            }
        }

        // Same status, just ignore it
        // Send a update to the callback URL
        PaymentQuery.sendPaymentNotification(m, internalPayment)

        call.respondEmptyJson()
    }
}