package net.perfectdreams.perfectpayments.backend.utils

import club.minnced.discord.webhook.send.WebhookEmbed
import club.minnced.discord.webhook.send.WebhookEmbedBuilder
import club.minnced.discord.webhook.send.WebhookMessageBuilder
import mu.KotlinLogging
import net.perfectdreams.perfectpayments.backend.PerfectPayments
import net.perfectdreams.perfectpayments.backend.dao.NotaFiscal
import net.perfectdreams.perfectpayments.backend.dao.Payment
import net.perfectdreams.perfectpayments.backend.dao.PaymentPersonalInfo
import net.perfectdreams.perfectpayments.backend.notafiscais.NotaFiscalStatus
import net.perfectdreams.perfectpayments.backend.payments.PaymentStatus
import net.perfectdreams.perfectpayments.backend.tables.NotaFiscais
import net.perfectdreams.perfectpayments.backend.tables.PaymentPersonalInfos
import net.perfectdreams.perfectpayments.backend.utils.focusnfe.FocusNFe
import net.perfectdreams.perfectpayments.backend.utils.focusnfe.requests.NFSeCreateRequest
import net.perfectdreams.perfectpayments.backend.utils.focusnfe.responses.NFSeCancellationResponse
import net.perfectdreams.perfectpayments.backend.utils.focusnfe.responses.NFSeCreateResponse
import java.awt.Color
import java.math.BigDecimal
import java.time.Instant
import java.time.ZonedDateTime

class NotaFiscalUtils(val m: PerfectPayments, val focusNFe: FocusNFe, val referencePrefix: String) {
    companion object {
        private val logger = KotlinLogging.logger {}
    }

    suspend fun generateNotaFiscal(
        payment: Payment,
        nfsePaymentValue: BigDecimal?
    ) {
        if (!focusNFe.config.enabled) {
            logger.warn { "We won't generate a Nota Fiscal for ${payment.id} because it is disabled on the configuration!" }
            return
        }

        logger.info { "Getting Nota Fiscal information for ${payment.id}..." }

        val personalInfo = m.newSuspendedTransaction {
            PaymentPersonalInfo.find {
                PaymentPersonalInfos.payment eq payment.id
            }.firstOrNull()
        }

        if (personalInfo == null) {
            logger.warn { "Payment ${payment.id} does not have any personal information associated with it! We are going to generate a Nota Fiscal without them..." }
        }

        val notaFiscal = m.newSuspendedTransaction {
            NotaFiscal.new {
                this.payment = payment
                this.personalInfo = personalInfo
                this.status = NotaFiscalStatus.CREATED
            }
        }

        val tomador = if (personalInfo == null) {
            null
        } else {
            // val isCnpj = Constants.CNPJ_REGEX.matches(personalInfo.socialNumber)

            NFSeCreateRequest.Tomador(
                personalInfo.socialNumber.toString(), // if (!isCnpj) personalInfo.socialNumber else null,
                null, // if (isCnpj) personalInfo.socialNumber else null,
                personalInfo.name,
                personalInfo.email
            )
        }

        // Nota Fiscal criada!
        val ref = "${referencePrefix}${notaFiscal.id}"
        val result = focusNFe.createNFSe(
            ref,
            ZonedDateTime.now(Constants.ZONE_ID),
            nfsePaymentValue?.toDouble() ?: (payment.amount.toDouble() / 100),
            "Liberação de serviço \"${payment.title}\" para o usuário",
            tomador
        )

        m.discordWebhook?.let {
            val embedBuilder = WebhookEmbedBuilder()
                .setTitle(WebhookEmbed.EmbedTitle("\uD83D\uDCC4 Nota Fiscal for Payment ${payment.id.value}", null))
                .addField(
                    WebhookEmbed.EmbedField(
                        true,
                        "\uD83C\uDFF7 Title",
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

            if (nfsePaymentValue != null) {
                embedBuilder.addField(
                    WebhookEmbed.EmbedField(
                        true,
                        "\uD83D\uDCB8 Amount (Overriden!)",
                        "`${nfsePaymentValue}`"
                    )
                )
            }

            val embed = embedBuilder
                .setFooter(WebhookEmbed.EmbedFooter("Reference ID: ${payment.referenceId}", null))
                .setTimestamp(Instant.now())
                .build()

            it.send(
                WebhookMessageBuilder()
                    .addEmbeds(embed)
                    .build()
            )
        }

        when (result) {
            is NFSeCreateResponse.AlreadyBeingProcessed -> {
                logger.warn { "Tried to create a nota fiscal for $ref, but it is already being processed!" }
            }
            is NFSeCreateResponse.RateLimited -> {
                // This should never happen here, because we already check if it was rate limited beforehand
                error("This should never happen!")
            }
            is NFSeCreateResponse.Success -> {
                logger.info { "Successfully registered a nota fiscal for $ref!" }
            }
            is NFSeCreateResponse.UnknownError -> error("Unknown Error while trying to create a NFSe! $result")
        }
    }

    suspend fun cancelNotaFiscais(payment: Payment, reason: String) {
        logger.info { "Cancelling Nota Fiscal for ${payment.id}..." }

        val generatedNotasFiscais = m.newSuspendedTransaction {
            NotaFiscal.find {
                NotaFiscais.payment eq payment.id
            }.toList()
        }

        // A payment may have multiple notas fiscais
        logger.info { "There are ${generatedNotasFiscais.size} notas fiscais generated that we need to cancel!" }

        generatedNotasFiscais.forEach {
            logger.info { "Cancelling Nota Fiscal ${referencePrefix}${it.id}..." }
            when (val result = focusNFe.cancelNFSe("${referencePrefix}${it.id}", reason)) {
                is NFSeCancellationResponse.Success -> {
                    logger.info { "Nota Fiscal ${referencePrefix}${it.id} cancellation result: ${result.status}" }
                }
                is NFSeCancellationResponse.ErrorWhileCancelling -> {
                    logger.warn { "Something went wrong while trying to cancel Nota Fiscal ${referencePrefix}${it.id}! ${result.status} ${result.erros}" }
                }
                is NFSeCancellationResponse.GenericError -> {
                    logger.warn { "Something went wrong while trying to cancel Nota Fiscal ${referencePrefix}${it.id}! ${result.codigo} ${result.mensagem}" }
                }
            }
        }
    }
}