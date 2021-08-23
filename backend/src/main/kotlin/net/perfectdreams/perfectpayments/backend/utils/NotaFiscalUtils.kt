package net.perfectdreams.perfectpayments.backend.utils

import mu.KotlinLogging
import net.perfectdreams.perfectpayments.backend.PerfectPayments
import net.perfectdreams.perfectpayments.backend.dao.NotaFiscal
import net.perfectdreams.perfectpayments.backend.dao.Payment
import net.perfectdreams.perfectpayments.backend.dao.PaymentPersonalInfo
import net.perfectdreams.perfectpayments.backend.notafiscais.NotaFiscalStatus
import net.perfectdreams.perfectpayments.backend.tables.NotaFiscais
import net.perfectdreams.perfectpayments.backend.tables.PaymentPersonalInfos
import net.perfectdreams.perfectpayments.backend.utils.focusnfe.FocusNFe
import net.perfectdreams.perfectpayments.backend.utils.focusnfe.NFSeCreateRequest
import java.time.ZonedDateTime

class NotaFiscalUtils(val m: PerfectPayments, val focusNFe: FocusNFe, val referencePrefix: String) {
    companion object {
        private val logger = KotlinLogging.logger {}
    }

    suspend fun generateNotaFiscal(payment: Payment) {
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
        focusNFe.createNFSe(
            "${referencePrefix}${notaFiscal.id}",
            ZonedDateTime.now(Constants.ZONE_ID),
            (payment.amount.toDouble() / 100),
            "Liberação de serviço \"${payment.title}\" para o usuário",
            tomador
        )
    }

    suspend fun cancelNotaFiscais(payment: Payment) {
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
            val result = focusNFe.cancelNFSe("${referencePrefix}${it.id}")
            logger.info { "Nota Fiscal ${referencePrefix}${it.id} cancellation result: ${result.status} ${result.erros}" }
        }
    }
}