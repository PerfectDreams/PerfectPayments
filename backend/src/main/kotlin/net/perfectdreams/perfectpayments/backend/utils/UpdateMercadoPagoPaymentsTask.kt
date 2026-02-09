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
import java.time.Instant
import java.time.LocalDateTime
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.time.temporal.ChronoUnit

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

        val now = Instant.now()
        // This is so weird because the API that this SDK calls does not match the API reference
        val result = paymentClient.search(
            MPSearchRequest.builder()
                .offset(0)
                .limit(1000)
                // By default, the search API searches from the beginning of the account
                // So we need to filter it ourselves!
                .filters(
                    mapOf(
                        "range" to "date_created",
                        // 30 days
                        // Yes, the .toString() is a ISO8601 representation of the Instant
                        // And yes, we need to truncate it to SECONDS, if not MercadoPago will complain that the date is invalid
                        // {"message":"Params Error: Invalid date parameter","error":"bad_request","status":400,"cause":[{"code":1,"description":"Params Error","data":"12-12-2024T02:15:25UTC;71d0f6f7-0a81-42f8-ae93-e789d31e4986"}]}
                        "begin_date" to now.minusSeconds(86_400 * 30).truncatedTo(ChronoUnit.SECONDS).toString(),
                        "end_date" to now.truncatedTo(ChronoUnit.SECONDS).toString(),
                        "sort" to "date_created",
                        "criteria" to "desc"
                    )
                )
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

            logger.info { "MercadoPago payment $reference status is $status (mapped to PerfectPayments status: $paymentStatus)" }

            if (paymentStatus != null) {
                PaymentUtils.updatePaymentStatus(
                    m,
                    internalPayment,
                    paymentStatus,
                    nfsePaymentValue = it.transactionDetails?.totalPaidAmount,
                    netReceivedAmount = it.transactionDetails?.netReceivedAmount
                        ?.multiply(java.math.BigDecimal(100))?.toLong()
                )
            }
        }
    }
}