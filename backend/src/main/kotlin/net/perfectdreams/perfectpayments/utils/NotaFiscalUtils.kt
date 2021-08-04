package net.perfectdreams.perfectpayments.utils

import mu.KotlinLogging
import net.perfectdreams.perfectpayments.dao.NotaFiscal
import net.perfectdreams.perfectpayments.dao.Payment
import net.perfectdreams.perfectpayments.dao.PaymentPersonalInfo
import net.perfectdreams.perfectpayments.notafiscais.NotaFiscalStatus
import net.perfectdreams.perfectpayments.tables.PaymentPersonalInfos
import net.perfectdreams.perfectpayments.utils.focusnfe.FocusNFe
import net.perfectdreams.perfectpayments.utils.focusnfe.NFSeCreateRequest
import org.jetbrains.exposed.sql.transactions.experimental.newSuspendedTransaction
import java.time.ZonedDateTime

class NotaFiscalUtils(val focusNFe: FocusNFe, val referencePrefix: String) {
    companion object {
        private val logger = KotlinLogging.logger {}
    }

    suspend fun generateNotaFiscal(payment: Payment) {
        logger.info { "Getting Nota Fiscal information for ${payment.id}..." }

        val personalInfo = newSuspendedTransaction {
            PaymentPersonalInfo.find {
                PaymentPersonalInfos.payment eq payment.id
            }.firstOrNull()
        }

        if (personalInfo == null) {
            logger.warn { "Payment ${payment.id} does not have any personal information associated with it! We are going to generate a Nota Fiscal without them..." }
        }

        val notaFiscal = newSuspendedTransaction {
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
}