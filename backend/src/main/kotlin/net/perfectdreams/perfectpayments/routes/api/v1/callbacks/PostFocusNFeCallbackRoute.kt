package net.perfectdreams.perfectpayments.routes.api.v1.callbacks

import io.ktor.application.*
import io.ktor.http.*
import io.ktor.request.*
import kotlinx.serialization.decodeFromString
import mu.KotlinLogging
import net.perfectdreams.perfectpayments.PerfectPayments
import net.perfectdreams.perfectpayments.config.FocusNFeConfig
import net.perfectdreams.perfectpayments.dao.NotaFiscal
import net.perfectdreams.perfectpayments.notafiscais.NotaFiscalStatus
import net.perfectdreams.perfectpayments.utils.Constants
import net.perfectdreams.perfectpayments.utils.extensions.receiveTextUTF8
import net.perfectdreams.perfectpayments.utils.extensions.respondEmptyJson
import net.perfectdreams.perfectpayments.utils.focusnfe.NFSeCallbackResponse
import net.perfectdreams.sequins.ktor.BaseRoute

class PostFocusNFeCallbackRoute(val m: PerfectPayments, val focusNFeConfig: FocusNFeConfig) : BaseRoute("/api/v1/callbacks/focusnfe") {
    companion object {
        private val logger = KotlinLogging.logger {}
    }

    override suspend fun onRequest(call: ApplicationCall) {
        logger.info { "Received FocusNFe Webhook Request" }

        val authorization = call.request.header("Authorization")

        if (authorization == null || authorization != focusNFeConfig.callbackAuthorization) {
            logger.warn { "Request Authorization is different than what it is expected or null! Received Authorization Token: $authorization"}
            call.respondEmptyJson(HttpStatusCode.Forbidden)
            return
        }

        val response = call.receiveTextUTF8()

        val nfseCallbackResponse = Constants.jsonIgnoreUnknownKeys.decodeFromString<NFSeCallbackResponse>(response)

        logger.info { "Received Nota Fiscal Update for Reference ID ${nfseCallbackResponse.ref}" }

        val theRealId = nfseCallbackResponse.ref.substringAfterLast("-")
            .toLongOrNull() ?: error("I wasn't able to process the Nota Fiscal because the reference can't be converted to a Long value!")

        val notaFiscal = m.newSuspendedTransaction {
            NotaFiscal.findById(theRealId)
        } ?: error("I wasn't able to find a Nota Fiscal with ID $theRealId!")

        val newStatus = when (nfseCallbackResponse.status) {
            "processando_autorizacao" -> NotaFiscalStatus.PROCESSING_AUTHORIZATION
            "autorizado" -> NotaFiscalStatus.AUTHORIZED
            "cancelado" -> NotaFiscalStatus.CANCELLED
            "erro_autorizacao" -> NotaFiscalStatus.AUTHORIZATION_ERROR
            else -> {
                logger.warn { "I don't know how to handle a ${nfseCallbackResponse.status}! I will change the status to ${NotaFiscalStatus.UNKNOWN}..." }
                NotaFiscalStatus.UNKNOWN
            }
        }

        logger.info { "Updating Nota Fiscal $theRealId status from ${notaFiscal.status} to $newStatus..." }

        m.newSuspendedTransaction {
            notaFiscal.status = newStatus
        }

        call.respondEmptyJson()
    }
}