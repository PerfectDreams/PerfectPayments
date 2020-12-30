package net.perfectdreams.perfectpayments.routes.api.v1.callbacks

import io.ktor.application.ApplicationCall
import io.ktor.client.request.get
import io.ktor.client.request.header
import io.ktor.client.statement.HttpResponse
import io.ktor.client.statement.readText
import io.ktor.http.HttpStatusCode
import io.ktor.request.header
import io.ktor.request.receiveText
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.contentOrNull
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import mu.KotlinLogging
import net.perfectdreams.perfectpayments.PerfectPayments
import net.perfectdreams.perfectpayments.dao.Payment
import net.perfectdreams.perfectpayments.payments.PaymentStatus
import net.perfectdreams.perfectpayments.utils.PaymentQuery
import net.perfectdreams.perfectpayments.utils.extensions.respondEmptyJson
import net.perfectdreams.sequins.ktor.BaseRoute
import org.jetbrains.exposed.sql.transactions.experimental.newSuspendedTransaction

class PostPicPayCallbackRoute(val m: PerfectPayments) : BaseRoute("/api/v1/callbacks/picpay") {
    companion object {
        private val logger = KotlinLogging.logger {}
    }

    override suspend fun onRequest(call: ApplicationCall) {
        val sellerTokenHeader = call.request.header("x-seller-token")

        if (sellerTokenHeader == null || m.gateway.picPay.seller != sellerTokenHeader) {
            logger.warn { "Request Seller Token is different than what it is expected or null! Received Seller Token: $sellerTokenHeader"}
            call.respondEmptyJson(HttpStatusCode.Forbidden)
            return
        }

        val body = call.receiveText()
        val json = Json.parseToJsonElement(body)
                .jsonObject

        val referenceId = json["referenceId"]!!.jsonPrimitive.content
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

        val internalPayment = newSuspendedTransaction {
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

            newSuspendedTransaction {
                internalPayment.status = PaymentStatus.CHARGED_BACK
            }
        } else if (status == "paid" || status == "complete") {
            if (internalPayment.paidAt != null) {
                logger.warn { "PicPay Payment with Reference ID: $referenceId ($internalTransactionId) is already paid! Ignoring..." }
                call.respondEmptyJson()
                return
            }

            logger.info { "Setting Payment $internalTransactionId as paid! (via PicPay payment $referenceId)" }

            newSuspendedTransaction {
                // Pagamento aprovado!
                internalPayment.paidAt = System.currentTimeMillis()
                internalPayment.status = PaymentStatus.APPROVED
            }
        }

        // Send a update to the callback URL
        PaymentQuery.sendPaymentNotification(m, internalPayment)

        call.respondEmptyJson()
    }
}