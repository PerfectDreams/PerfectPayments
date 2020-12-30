package net.perfectdreams.perfectpayments.utils

import club.minnced.discord.webhook.send.WebhookEmbed
import club.minnced.discord.webhook.send.WebhookEmbedBuilder
import club.minnced.discord.webhook.send.WebhookMessageBuilder
import io.ktor.client.request.get
import io.ktor.client.request.header
import io.ktor.client.request.post
import io.ktor.client.statement.HttpResponse
import io.ktor.http.ContentType
import io.ktor.http.userAgent
import io.ktor.request.receiveText
import io.ktor.response.respondText
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.serialization.json.*
import mu.KotlinLogging
import net.perfectdreams.perfectpayments.PerfectPayments
import net.perfectdreams.perfectpayments.dao.Payment
import net.perfectdreams.perfectpayments.payments.PaymentGateway
import net.perfectdreams.perfectpayments.payments.PaymentStatus
import net.perfectdreams.perfectpayments.tables.Payments
import org.jetbrains.exposed.sql.insertAndGetId
import org.jetbrains.exposed.sql.transactions.experimental.newSuspendedTransaction
import java.awt.Color
import java.time.Instant
import java.util.*
import kotlin.math.pow

object PaymentQuery {
    private val logger = KotlinLogging.logger {}

    suspend fun startPayment(m: PerfectPayments, partialPaymentId: UUID, partialPayment: PartialPayment, gateway: PaymentGateway, data: JsonObject): String {
        val paymentCreator = m.paymentCreators[gateway] ?: error("Missing payment creator")

        val paymentId = newSuspendedTransaction {
            Payments.insertAndGetId {
                it[Payments.gateway] = gateway
                it[Payments.status] = PaymentStatus.CREATED
                it[referenceId] = partialPaymentId
                it[title] = partialPayment.title
                it[amount] = partialPayment.amount
                it[currencyId] = partialPayment.currencyId
                it[callbackUrl] = partialPayment.callbackUrl
                it[createdAt] = System.currentTimeMillis()
            }
        }.value

        val url = paymentCreator.createPayment(paymentId, partialPayment, data)

        // Remove the partial payment
        m.partialPayments.remove(partialPaymentId)

        val paymentObject = newSuspendedTransaction { Payment.findById(paymentId) }
        if (paymentObject != null)
            sendPaymentNotification(m, paymentObject)

        return url
    }

    fun sendPaymentNotification(m: PerfectPayments, payment: Payment) {
        m.discordWebhook?.let {
            val emoji = when (payment.status) {
                PaymentStatus.UNKNOWN -> "<:lori_what:626942886361038868>"
                PaymentStatus.CREATED -> "<:lori_hm:741055999430885437>"
                PaymentStatus.APPROVED -> "<:lori_nice:726845783344939028>"
                PaymentStatus.CHARGED_BACK -> "<:lori_knife:727144421568544889>"
            }

            val color = when (payment.status) {
                PaymentStatus.UNKNOWN -> null
                PaymentStatus.CREATED -> Color(41, 62, 84)
                PaymentStatus.APPROVED -> Color(62, 156, 53)
                PaymentStatus.CHARGED_BACK -> Color(189, 61, 58)
            }

            val embed = WebhookEmbedBuilder()
                    .setTitle(WebhookEmbed.EmbedTitle("$emoji Payment ${payment.id.value}", null))
                    .addField(
                            WebhookEmbed.EmbedField(
                                    true,
                                    "\uD83C\uDFF7️ Title",
                                    "`${payment.title}`"
                            )
                    )
                    .addField(
                            WebhookEmbed.EmbedField(
                                    true,
                                    "\uD83D\uDCB8 Amount",
                                    "`${payment.amount} ${payment.currencyId}`"
                            )
                    )
                    .addField(
                            WebhookEmbed.EmbedField(
                                    true,
                                    "<:lori_rica:593979718919913474> Status",
                                    "`${payment.status.name}`"
                            )
                    )
                    .addField(
                            WebhookEmbed.EmbedField(
                                    true,
                                    "\uD83D\uDCB3 Gateway",
                                    "`${payment.gateway.name}`"
                            )
                    )
                    .setColor(color?.rgb)
                    .setFooter(WebhookEmbed.EmbedFooter("Reference ID: ${payment.referenceId}", null))
                    .setTimestamp(Instant.now())
                    .build()
            it.send(embed)
        }

        GlobalScope.launch {
            var requestsMade = 0

            while (true) {
                logger.info { "Trying to notify \"${payment.callbackUrl}\" for payment ${payment.id.value}..." }

                val response = try {
                    PerfectPayments.http.post<HttpResponse>(payment.callbackUrl) {
                        header("Authorization", m.config.notificationToken)
                        userAgent(PerfectPayments.USER_AGENT)

                        body = buildJsonObject {
                            put("referenceId", payment.referenceId.toString())
                            put("amount", payment.amount)
                            put("status", payment.status.toString())
                            put("gateway", payment.gateway.toString())
                            put("paidAt", payment.paidAt)
                            put("createdAt", payment.createdAt)
                        }.toString()
                    }
                } catch (e: Exception) {
                    logger.warn(e) { "Exception while trying to send a notification about payment ${payment.id.value} to \"${payment.callbackUrl}\"!" }
                    null
                }

                if (response != null)
                    if (response.status.value in 200..299) {
                        logger.info { "Notification for payment ${payment.id.value} was successfully sent to \"${payment.callbackUrl}\"! Status code: ${response.status}"}
                        return@launch
                    }

                requestsMade++
                val waitTime = requestsMade.toDouble()
                        .pow(2)
                        .toLong() * 1000

                logger.warn { "Something went wrong while trying to send a notification about payment ${payment.id.value} to \"${payment.callbackUrl}\"! Status code: ${response?.status}; Retrying again after ${waitTime}ms"}

                delay(waitTime)
            }
        }
    }
}