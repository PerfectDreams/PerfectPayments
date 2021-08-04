package net.perfectdreams.perfectpayments.backend.utils

import club.minnced.discord.webhook.send.WebhookEmbed
import club.minnced.discord.webhook.send.WebhookEmbedBuilder
import io.ktor.client.request.*
import io.ktor.client.statement.*
import io.ktor.http.*
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.put
import mu.KotlinLogging
import net.perfectdreams.perfectpayments.backend.PerfectPayments
import net.perfectdreams.perfectpayments.common.data.PersonalData
import net.perfectdreams.perfectpayments.common.payments.PaymentGateway
import net.perfectdreams.perfectpayments.backend.dao.Payment
import net.perfectdreams.perfectpayments.backend.dao.PaymentPersonalInfo
import net.perfectdreams.perfectpayments.backend.payments.PaymentStatus
import java.awt.Color
import java.time.Instant
import java.util.*
import kotlin.math.pow

object PaymentQuery {
    private val logger = KotlinLogging.logger {}

    suspend fun startPayment(
        m: PerfectPayments,
        partialPaymentId: UUID,
        partialPayment: PartialPayment,
        gateway: PaymentGateway,
        personalData: PersonalData?,
        data: JsonObject
    ): String {
        val paymentCreator = m.paymentCreators[gateway] ?: error("Missing payment creator")

        val payment = m.newSuspendedTransaction {
            Payment.new {
                this.gateway = gateway
                this.status = PaymentStatus.CREATED
                this.referenceId = partialPaymentId
                this.title = partialPayment.title
                this.amount = partialPayment.amount
                this.currencyId = partialPayment.currencyId
                this.callbackUrl = partialPayment.callbackUrl
                this.createdAt = System.currentTimeMillis()
            }
        }
        val paymentId = payment.id.value

        if (personalData != null) {
            m.newSuspendedTransaction {
                PaymentPersonalInfo.new {
                    this.payment = payment
                    this.socialNumber = personalData.socialNumber.cleanDocument
                    this.name = personalData.name.name
                    this.email = personalData.email.buildEmailAddress()
                }
            }
        }

        val url = paymentCreator.createPayment(paymentId, partialPayment, data)

        // Remove the partial payment
        m.partialPayments.remove(partialPaymentId)

        val paymentObject = m.newSuspendedTransaction { Payment.findById(paymentId) }
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