package net.perfectdreams.perfectpayments.backend.routes.api.v1.callbacks

import com.mercadopago.client.payment.PaymentClient
import io.ktor.http.*
import io.ktor.server.application.*
import io.ktor.server.request.*
import mu.KotlinLogging
import net.perfectdreams.perfectpayments.backend.PerfectPayments
import net.perfectdreams.perfectpayments.backend.dao.Payment
import net.perfectdreams.perfectpayments.backend.payments.PaymentStatus
import net.perfectdreams.perfectpayments.backend.utils.PaymentUtils
import net.perfectdreams.perfectpayments.backend.utils.extensions.respondEmptyJson
import net.perfectdreams.sequins.ktor.BaseRoute
import javax.crypto.Mac
import javax.crypto.spec.SecretKeySpec


class PostMercadoPagoCallbackRoute(val m: PerfectPayments) : BaseRoute("/api/v1/callbacks/mercadopago") {
    companion object {
        private val logger = KotlinLogging.logger {}
    }

    private val paymentClient = PaymentClient()

    override suspend fun onRequest(call: ApplicationCall) {
        logger.info { "Received MercadoPago Webhook Request" }

        val parameters = call.request.queryParameters
        val body = call.receiveText()
        val type = parameters["type"]

        logger.info { "MercadoPago type: $type, params: ${parameters.entries()}; body: $body" }

        val xSignature = call.request.header("x-signature")
        val xRequestId = call.request.header("x-request-id")

        logger.info { "MercadoPago x-signature header: $xSignature" }
        logger.info { "MercadoPago x-request-id header: $xRequestId" }

        if (xSignature == null) {
            logger.warn { "MercadoPago request is missing the x-signature header!" }
            call.respondEmptyJson(HttpStatusCode.Forbidden)
            return
        }

        if (xRequestId == null) {
            logger.warn { "MercadoPago request is missing the x-request-id header!" }
            call.respondEmptyJson(HttpStatusCode.Forbidden)
            return
        }

        if (!validate(parameters["data.id"], xSignature, xRequestId)) {
            logger.warn { "MercadoPago request didn't match our signature!" }
            call.respondEmptyJson(HttpStatusCode.Forbidden)
            return
        }

        when (type) {
            "payment" -> {
                // Get payment info
                val dataId = parameters["data.id"]?.toLongOrNull() ?: error("Missing data.id!")
                val payment = paymentClient.get(dataId)

                val reference = payment.externalReference
                val internalTransactionId = reference.split("-").last()

                val internalPayment = m.newSuspendedTransaction {
                    Payment.findById(internalTransactionId.toLong())
                }

                if (internalPayment == null) {
                    logger.warn { "MercadoPago Payment with Reference ID: $reference ($internalTransactionId) doesn't have a matching internal ID! Bug?" }
                    call.respondEmptyJson()
                    return
                }

                when (payment.status) {
                    "approved" -> {
                        PaymentUtils.updatePaymentStatus(
                            m,
                            internalPayment,
                            PaymentStatus.APPROVED
                        )
                    }
                    "in_mediation" -> {
                        PaymentUtils.updatePaymentStatus(
                            m,
                            internalPayment,
                            PaymentStatus.CHARGED_BACK
                        )
                    }
                    "charged_back" -> {
                        PaymentUtils.updatePaymentStatus(
                            m,
                            internalPayment,
                            PaymentStatus.CHARGED_BACK
                        )
                    }
                }
            }
        }

        call.respondEmptyJson()
    }

    private fun validate(dataID: String?, xSignature: String, xRequestId: String): Boolean {
        // Separating the x-signature into parts
        val parts = xSignature.split(",".toRegex()).dropLastWhile { it.isEmpty() }.toTypedArray()

        // Initializing variables to store ts and hash
        var ts: String? = null
        var hash: String? = null

        // Iterate over the values to obtain ts and v1
        for (part in parts) {
            val keyValue = part.trim { it <= ' ' }.split("=".toRegex()).dropLastWhile { it.isEmpty() }
                .toTypedArray()
            if (keyValue.size == 2) {
                val key = keyValue[0].trim { it <= ' ' }
                val value = keyValue[1].trim { it <= ' ' }
                if ("ts" == key) {
                    ts = value
                } else if ("v1" == key) {
                    hash = value
                }
            }
        }

        // Generate the manifest string
        val manifest = buildString {
            // "Caso algum dos valores apresentados no template acima não esteja presente em sua notificação, você deverá removê-los do template."
            // https://www.mercadopago.com.br/developers/pt/docs/your-integrations/notifications/webhooks
            if (dataID != null) {
                append("id:$dataID;")
            }

            append("request-id:$xRequestId;")
            append("ts:$ts;")
        }

        logger.info { "MercadoPago request manifest: $manifest; TS: $ts; Hash: $hash" }
        val mac = Mac.getInstance("HmacSHA256")

        val signingKey = SecretKeySpec(m.gateway.mercadoPago.webhookSecretSignature.toByteArray(Charsets.UTF_8), "HmacSHA256")
        mac.init(signingKey)
        val doneFinal = mac.doFinal(manifest.toByteArray(Charsets.UTF_8))
        val doneFinalAsHex = doneFinal.bytesToHex()

        logger.info { "MercadoPago's request signature hash: $hash" }
        logger.info { "Our generated request signature hash: $doneFinalAsHex" }

        return hash == doneFinalAsHex
    }

    /**
     * Converts a ByteArray to a hexadecimal string
     *
     * @return the byte array in hexadecimal format
     */
    private fun ByteArray.bytesToHex(): String {
        val hexString = StringBuffer()
        for (i in this.indices) {
            val hex = Integer.toHexString(0xff and this[i].toInt())
            if (hex.length == 1) {
                hexString.append('0')
            }
            hexString.append(hex)
        }
        return hexString.toString()
    }
}