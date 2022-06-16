package net.perfectdreams.perfectpayments.backend.routes.api.v1.callbacks

import com.stripe.model.Dispute
import com.stripe.model.PaymentIntent
import com.stripe.net.Webhook
import io.ktor.server.application.*
import io.ktor.http.*
import io.ktor.server.request.*
import mu.KotlinLogging
import net.perfectdreams.perfectpayments.backend.PerfectPayments
import net.perfectdreams.perfectpayments.backend.dao.Payment
import net.perfectdreams.perfectpayments.backend.payments.PaymentStatus
import net.perfectdreams.perfectpayments.backend.utils.PaymentUtils
import net.perfectdreams.perfectpayments.backend.utils.extensions.receiveTextUTF8
import net.perfectdreams.perfectpayments.backend.utils.extensions.respondEmptyJson
import net.perfectdreams.sequins.ktor.BaseRoute

class PostStripeCallbackRoute(val m: PerfectPayments) : BaseRoute("/api/v1/callbacks/stripe") {
    companion object {
        private val logger = KotlinLogging.logger {}
    }

    override suspend fun onRequest(call: ApplicationCall) {
        logger.info { "Received Stripe Webhook Request" }

        val payload = call.receiveTextUTF8()
        logger.info { "Stripe Received Body: $payload" }
        val signatureHeader = call.request.header("Stripe-Signature")
        val endpointSecret = m.gateway.stripe.webhookSecret

        // println(payload)

        val event = try {
            Webhook.constructEvent(
                payload, signatureHeader, endpointSecret
            )
        } catch (e: Exception) {
            // Invalid payload
            logger.warn(e) { "Invalid Stripe Webhook event!" }
            call.respondEmptyJson(HttpStatusCode.BadRequest)
            return
        }

        logger.info { "Received Stripe Event: ${event.type}" }
        // println(event.type)
        // println(event.dataObjectDeserializer.deserializeUnsafe()::class)

        if (event.type == "charge.dispute.created") {
            val dispute = event.dataObjectDeserializer.deserializeUnsafe() as Dispute
            logger.info { "Received Stripe dispute event, oh no... Dispute ID: ${dispute.id}" }

            val paymentIntent = PaymentIntent.retrieve(dispute.paymentIntent)
            val referenceId = paymentIntent.metadata["referenceId"]

            logger.info { "Received Stripe dispute status for payment with reference ID $referenceId; Payment Intent ID: ${paymentIntent.id}" }
            if (referenceId != null) {
                val internalTransactionId = referenceId.split("-").last()

                val internalPayment = m.newSuspendedTransaction {
                    Payment.findById(internalTransactionId.toLong())
                }

                if (internalPayment == null) {
                    logger.warn { "Stripe Dispute with Reference ID: $referenceId ($internalTransactionId); Payment Intent ID: ${paymentIntent.id} doesn't have a matching internal ID! Bug?" }
                    call.respondEmptyJson()
                    return
                }

                PaymentUtils.updatePaymentStatus(
                    m,
                    internalPayment,
                    PaymentStatus.CHARGED_BACK
                )
            }
        }
        if (event.type == "payment_intent.succeeded") {
            val paymentIntent = event.dataObjectDeserializer.deserializeUnsafe() as PaymentIntent
            val referenceId = paymentIntent.metadata["referenceId"]

            logger.info { "Received Stripe paid payment status for payment with reference ID $referenceId; Payment Intent ID: ${paymentIntent.id}" }
            if (referenceId != null) {
                val internalTransactionId = referenceId.split("-").last()

                val internalPayment = m.newSuspendedTransaction {
                    Payment.findById(internalTransactionId.toLong())
                }

                if (internalPayment == null) {
                    logger.warn { "Stripe Payment with Reference ID: $referenceId ($internalTransactionId); Payment Intent ID: ${paymentIntent.id} doesn't have a matching internal ID! Bug?" }
                    call.respondEmptyJson()
                    return
                }

                if (internalPayment.paidAt != null) {
                    logger.warn { "Stripe Payment with Reference ID: $referenceId ($internalTransactionId); Payment Intent ID: ${paymentIntent.id} is already paid! Ignoring..." }
                    call.respondEmptyJson()
                    return
                }

                logger.info { "Setting Payment $internalTransactionId as paid! (via Stripe payment $referenceId; Payment Intent ID: ${paymentIntent.id})" }

                PaymentUtils.updatePaymentStatus(
                    m,
                    internalPayment,
                    PaymentStatus.APPROVED
                )
            }
        }

        call.respondEmptyJson()
    }
}