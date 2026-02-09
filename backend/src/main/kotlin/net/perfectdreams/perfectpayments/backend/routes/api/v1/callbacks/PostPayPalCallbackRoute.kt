package net.perfectdreams.perfectpayments.backend.routes.api.v1.callbacks

import io.ktor.client.request.*
import io.ktor.client.statement.*
import io.ktor.content.*
import io.ktor.http.*
import io.ktor.server.application.*
import io.ktor.server.request.*
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.contentOrNull
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import kotlinx.serialization.json.put
import mu.KotlinLogging
import net.perfectdreams.perfectpayments.backend.PerfectPayments
import net.perfectdreams.perfectpayments.backend.dao.Payment
import net.perfectdreams.perfectpayments.backend.payments.PaymentStatus
import net.perfectdreams.perfectpayments.backend.utils.PaymentUtils
import net.perfectdreams.perfectpayments.backend.utils.extensions.receiveTextUTF8
import net.perfectdreams.perfectpayments.backend.utils.extensions.respondEmptyJson
import net.perfectdreams.sequins.ktor.BaseRoute
import java.math.BigDecimal
import java.util.*

class PostPayPalCallbackRoute(val m: PerfectPayments) : BaseRoute("/api/v1/callbacks/paypal") {
    companion object {
        private val logger = KotlinLogging.logger {}
    }

    override suspend fun onRequest(call: ApplicationCall) {
        logger.info { "Received PayPal Webhook Request" }

        val paypalTransmissionId = call.request.header("PAYPAL-TRANSMISSION-ID")
        val paypalTransmissionTime = call.request.header("PAYPAL-TRANSMISSION-TIME")
        val paypalTransmissionSignature = call.request.header("PAYPAL-TRANSMISSION-SIG")
        val paypalCertUrl = call.request.header("PAYPAL-CERT-URL")
        val paypalAuthAlgo = call.request.header("PAYPAL-AUTH-ALGO")

        val response = call.receiveTextUTF8()
        logger.info { "PayPal Received Body: $response" }
        val webhookEvent = Json.parseToJsonElement(response)
            .jsonObject

        val webhookVerificationResponse = PerfectPayments.http.post(m.gateway.payPal.getBaseUrl() + "/v1/notifications/verify-webhook-signature") {
            // contentType(ContentType.Application.Json)
            header("Authorization", "Basic ${Base64.getEncoder().encodeToString("${m.gateway.payPal.clientId}:${m.gateway.payPal.clientSecret}".toByteArray())}")

            setBody(
                TextContent(
                    buildJsonObject {
                        put("auth_algo", paypalAuthAlgo)
                        put("cert_url", paypalCertUrl)
                        put("transmission_id", paypalTransmissionId)
                        put("transmission_sig", paypalTransmissionSignature)
                        put("transmission_time", paypalTransmissionTime)
                        put("webhook_id", m.gateway.payPal.webhookId)
                        put("webhook_event", webhookEvent)
                    }.toString().also { println(it) },
                    ContentType.Application.Json
                )
            )
        }

        val webhookVerificationResponsePayload = Json.parseToJsonElement(webhookVerificationResponse.bodyAsText())
            .jsonObject
        val verificationStatus = webhookVerificationResponsePayload["verification_status"]?.jsonPrimitive?.content

        if (verificationStatus != "SUCCESS") {
            logger.warn { "PayPal Webhook Event seems to have been tampered! Verification Status: $verificationStatus" }
            call.respondEmptyJson(HttpStatusCode.Forbidden)
            return
        }

        val eventType = webhookEvent["event_type"]!!.jsonPrimitive.content

        logger.info { "Received PayPal Webhook Event: $eventType" }

        if (eventType == "CUSTOMER.DISPUTE.CREATED") {
            logger.warn { "A PayPal Payment was charged back >:(" }

            val resource = webhookEvent["resource"]!!.jsonObject

            val captures = resource["disputed_transactions"]!!.jsonArray

            for (capture in captures) {
                val captureId = capture.jsonObject["buyer_transaction_id"]!!.jsonPrimitive.content

                val customId = capture.jsonObject["custom"]?.jsonPrimitive?.contentOrNull

                val paymentId = customId?.substringAfterLast("-")

                if (paymentId != null) {
                    val internalPayment = m.newSuspendedTransaction {
                        Payment.findById(paymentId.toLong())
                    }

                    if (internalPayment == null) {
                        logger.warn { "PayPal Payment with Reference ID: $paymentId ($captureId) doesn't have a matching internal ID! Bug?" }
                        continue
                    }

                    logger.warn { "PayPal Payment ${internalPayment.id.value} was charged back >:(" }

                    PaymentUtils.updatePaymentStatus(
                        m,
                        internalPayment,
                        PaymentStatus.CHARGED_BACK
                    )
                }
            }
        } else if (eventType == "CHECKOUT.ORDER.APPROVED") {
            logger.info { "Received PayPal Order Approved Event! We are now going to capture the payment..." }

            val links = webhookEvent["resource"]!!.jsonObject["links"]!!.jsonArray

            val link = links.firstOrNull { it.jsonObject["rel"]?.jsonPrimitive?.content == "capture" }
                ?.jsonObject?.get("href")?.jsonPrimitive?.content

            if (link == null) {
                logger.warn { "Missing PayPal capture URL from the link list..." }
                call.respondEmptyJson(HttpStatusCode.OK)
                return
            }

            val capturePaymentResponse = PerfectPayments.http.post(link) {
                header("Authorization", "Basic ${Base64.getEncoder().encodeToString("${m.gateway.payPal.clientId}:${m.gateway.payPal.clientSecret}".toByteArray())}")

                setBody(
                    TextContent("", ContentType.Application.Json)
                )
            }

            val capturePaymentPayload = capturePaymentResponse.bodyAsText()

            val capturePayment = Json.parseToJsonElement(capturePaymentPayload)
                .jsonObject

            val status = capturePayment["status"]?.jsonPrimitive?.content

            // {"name":"UNPROCESSABLE_ENTITY","details":[{"issue":"ORDER_ALREADY_CAPTURED","description":"Order already captured.If 'intent=CAPTURE' only one capture per order is allowed."}],"message":"The requested action could not be performed, semantically incorrect, or failed business validation.","debug_id":"f8574efcf7ad9","links":[{"href":"https://developer.paypal.com/docs/api/orders/v2/#error-ORDER_ALREADY_CAPTURED","rel":"information_link","method":"GET"}]}
            val orderAlreadyCaptured = capturePayment["details"]?.jsonArray
                ?.all { it.jsonObject["issue"]?.jsonPrimitive?.content == "ORDER_ALREADY_CAPTURED" }

            logger.info { "Tried capturing PayPal payment, status: $status; order already captured? $orderAlreadyCaptured" }

            // TODO: The "orderAlreadyCaptured" check doesn't work because it doesn't provide the info that we need, this needs to be fixed!
            if (status == "COMPLETED" || orderAlreadyCaptured == true) {
                val purchaseUnits = capturePayment["purchase_units"]!!.jsonArray

                for (purchaseUnit in purchaseUnits) {
                    val payments = purchaseUnit.jsonObject["payments"]!!.jsonObject

                    val captures = payments["captures"]!!.jsonArray

                    for (capture in captures) {
                        val captureId = capture.jsonObject["id"]!!.jsonPrimitive.content

                        val customId = capture.jsonObject["custom_id"]?.jsonPrimitive?.contentOrNull

                        val paymentId = customId?.substringAfterLast("-")

                        if (paymentId != null) {
                            val internalPayment = m.newSuspendedTransaction {
                                Payment.findById(paymentId.toLong())
                            }

                            if (internalPayment == null) {
                                logger.warn { "PayPal Payment with Reference ID: $paymentId ($captureId) doesn't have a matching internal ID! Bug?" }
                                continue
                            }

                            if (internalPayment.paidAt != null) {
                                logger.warn { "PayPal Payment with Reference ID: $paymentId ($captureId) is already paid! Ignoring..." }
                                continue
                            }

                            val netAmountCents = capture.jsonObject["seller_receivable_breakdown"]
                                ?.jsonObject?.get("net_amount")
                                ?.jsonObject?.get("value")
                                ?.jsonPrimitive?.contentOrNull
                                ?.let { BigDecimal(it).multiply(BigDecimal(100)).toLong() }

                            logger.info { "Setting Payment ${internalPayment.id} ($captureId) as paid! (via PayPal payment $paymentId)" }

                            PaymentUtils.updatePaymentStatus(
                                m,
                                internalPayment,
                                PaymentStatus.APPROVED,
                                netReceivedAmount = netAmountCents
                            )
                        }
                    }
                }
            } else {
                logger.warn { "Captured PayPal payment but status is $status, ignoring..." }
            }
        }

        call.respondEmptyJson()
    }
}