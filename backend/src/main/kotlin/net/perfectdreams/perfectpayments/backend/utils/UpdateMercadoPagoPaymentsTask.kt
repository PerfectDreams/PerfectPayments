package net.perfectdreams.perfectpayments.backend.utils

import com.mercadopago.client.payment.PaymentClient
import com.mercadopago.net.MPSearchRequest
import io.ktor.client.*
import io.ktor.client.request.*
import io.ktor.client.statement.*
import mu.KotlinLogging
import net.perfectdreams.perfectpayments.backend.PerfectPayments
import net.perfectdreams.perfectpayments.backend.dao.Payment
import net.perfectdreams.perfectpayments.backend.routes.api.v1.callbacks.PostPagSeguroCallbackRoute
import net.perfectdreams.perfectpayments.backend.utils.extensions.respondEmptyJson
import org.jsoup.Jsoup
import java.time.LocalDateTime
import java.time.ZoneId
import java.time.format.DateTimeFormatter

/**
 * Queries MercadoPago payments and updates their status if the payment is approved
 *
 * While not really needed, this is only as a fallback because "what if the webhooks fail"...
 */
class UpdateMercadoPagoPaymentsTask(val m: PerfectPayments) : RunnableCoroutine {
    companion object {
        private val logger = KotlinLogging.logger {}
    }

    private val paymentClient = PaymentClient()

    override suspend fun run() {
        logger.info { "Querying MercadoPago payments manually..." }

        // This is so weird because the API that this SDK calls does not match the API reference
        val result = paymentClient.search(
            MPSearchRequest.builder()
                .offset(0)
                .limit(1000)
                .filters(mapOf())
                .build()
        )

        result.results.forEach {
            val status = it.status
            val reference = it.externalReference

            // This code is from PostMercadoPagoCallbackRoute
            logger.info { "MercadoPago payment $reference status is $status" }

            val internalTransactionId = reference.split("-").last()

            val internalPayment = m.newSuspendedTransaction {
                Payment.findById(internalTransactionId.toLong())
            }

            if (internalPayment == null) {
                logger.warn { "MercadoPago Payment with Reference ID: $reference ($internalTransactionId) doesn't have a matching internal ID! Bug?" }
                return
            }

            val paymentStatus = MercadoPagoUtils.getPaymentStatusFromMercadoPagoPaymentStatus(status)

            logger.info { "MercadoPago payment $reference status is $paymentStatus" }

            if (paymentStatus != null) {
                PaymentUtils.updatePaymentStatus(
                    m,
                    internalPayment,
                    paymentStatus
                )
            }
        }
    }
}