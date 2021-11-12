package net.perfectdreams.perfectpayments.backend.routes.api.v1.callbacks

import io.ktor.application.*
import io.ktor.client.request.*
import io.ktor.client.statement.*
import io.ktor.http.*
import io.ktor.request.*
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.contentOrNull
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import mu.KotlinLogging
import net.perfectdreams.perfectpayments.backend.PerfectPayments
import net.perfectdreams.perfectpayments.backend.dao.Payment
import net.perfectdreams.perfectpayments.backend.payments.PaymentStatus
import net.perfectdreams.perfectpayments.backend.utils.PaymentUtils
import net.perfectdreams.perfectpayments.backend.utils.extensions.receiveTextUTF8
import net.perfectdreams.perfectpayments.backend.utils.extensions.respondEmptyJson
import net.perfectdreams.sequins.ktor.BaseRoute

class PostPicPayCallbackRoute(val m: PerfectPayments) : BaseRoute("/api/v1/callbacks/picpay") {
    companion object {
        private val logger = KotlinLogging.logger {}
    }

    override suspend fun onRequest(call: ApplicationCall) {
        logger.info { "Received PicPay Webhook Request" }

        val sellerTokenHeader = call.request.header("x-seller-token")

        if (sellerTokenHeader == null || m.gateway.picPay.seller != sellerTokenHeader) {
            logger.warn { "Request Seller Token is different than what it is expected or null! Received Seller Token: $sellerTokenHeader"}
            call.respondEmptyJson(HttpStatusCode.Forbidden)
            return
        }

        val body = call.receiveTextUTF8()
        logger.info { "PicPay Received Body: $body" }
        val json = Json.parseToJsonElement(body)
                .jsonObject

        val referenceId = json["referenceId"]!!.jsonPrimitive.content
        if (!json.containsKey("authorizationId")) {
            logger.info { "Received PicPay callback: Payment generated for $referenceId" }
            call.respondEmptyJson()
            return
        }

        val authorizationId = json["authorizationId"]!!.jsonPrimitive.contentOrNull

        logger.info { "Received PicPay callback: Reference ID: $referenceId; Authorization ID: $authorizationId" }

        val httpResponse = PerfectPayments.http.get<HttpResponse>("https://appws.picpay.com/ecommerce/public/payments/$referenceId/status") {
            header("x-picpay-token", m.gateway.picPay.token)
        }

        val payloadAsString = httpResponse.readText()

        if (httpResponse.status.value != 200) {
            logger.warn { "Weird status code while checking for PicPay's payment info: ${httpResponse.status.value}; Payload: $payloadAsString" }
            call.respondEmptyJson()
            return
        }

        val payload = Json.parseToJsonElement(payloadAsString)
                .jsonObject

        val status = payload["status"]!!.jsonPrimitive.content

        logger.info { "PicPay payment $referenceId status is $status" }

        val internalTransactionId = referenceId.split("-").last()

        val internalPayment = m.newSuspendedTransaction {
            Payment.findById(internalTransactionId.toLong())
        }

        if (internalPayment == null) {
            logger.warn { "PicPay Payment with Reference ID: $referenceId ($internalTransactionId) doesn't have a matching internal ID! Bug?" }
            call.respondEmptyJson()
            return
        }

        if (status == "chargeback") {
            // User charged back the payment, let's ban him!
            logger.warn { "Payment ${internalPayment.id.value} was charged back >:(" }

            PaymentUtils.updatePaymentStatus(
                m,
                internalPayment,
                PaymentStatus.CHARGED_BACK
            )
        } else if (status == "paid" || status == "complete") {
            if (internalPayment.paidAt != null) {
                logger.warn { "PicPay Payment with Reference ID: $referenceId ($internalTransactionId) is already paid! Ignoring..." }
                call.respondEmptyJson()
                return
            }

            logger.info { "Setting Payment $internalTransactionId as paid! (via PicPay payment $referenceId)" }

            PaymentUtils.updatePaymentStatus(
                m,
                internalPayment,
                PaymentStatus.APPROVED
            )
        }

        call.respondEmptyJson()
    }
}