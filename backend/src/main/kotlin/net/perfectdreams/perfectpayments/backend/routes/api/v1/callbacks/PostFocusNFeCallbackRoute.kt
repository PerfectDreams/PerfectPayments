package net.perfectdreams.perfectpayments.backend.routes.api.v1.callbacks

import io.ktor.http.*
import io.ktor.server.application.*
import io.ktor.server.request.*
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.encodeToJsonElement
import mu.KotlinLogging
import net.perfectdreams.perfectpayments.backend.PerfectPayments
import net.perfectdreams.perfectpayments.backend.config.FocusNFeConfig
import net.perfectdreams.perfectpayments.backend.dao.NotaFiscal
import net.perfectdreams.perfectpayments.backend.notafiscais.NotaFiscalStatus
import net.perfectdreams.perfectpayments.backend.tables.FocusNFeEvents
import net.perfectdreams.perfectpayments.backend.utils.APIError
import net.perfectdreams.perfectpayments.backend.utils.Constants
import net.perfectdreams.perfectpayments.backend.utils.extensions.receiveTextUTF8
import net.perfectdreams.perfectpayments.backend.utils.extensions.respondEmptyJson
import net.perfectdreams.perfectpayments.backend.utils.extensions.respondJson
import net.perfectdreams.perfectpayments.backend.utils.focusnfe.NFSeCallbackResponse
import net.perfectdreams.sequins.ktor.BaseRoute
import org.jetbrains.exposed.sql.insert

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
        logger.info { "FocusNFe Received Body: $response" }
        val nfseCallbackResponse = Constants.jsonIgnoreUnknownKeys.decodeFromString<NFSeCallbackResponse>(response)

        logger.info { "Received Nota Fiscal Update for Reference ID ${nfseCallbackResponse.ref}" }

        val theRealId = nfseCallbackResponse.ref.substringAfterLast("-")
            .toLongOrNull() ?: respondAndThrow(call, HttpStatusCode.UnprocessableEntity, "I wasn't able to process the Nota Fiscal because the reference can't be converted to a Long value!")

        val notaFiscal = m.newSuspendedTransaction {
            NotaFiscal.findById(theRealId)
        } ?: respondAndThrow(call, HttpStatusCode.NotFound, "I wasn't able to find a Nota Fiscal with ID $theRealId!")

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

        val url = nfseCallbackResponse.url

        logger.info { "Updating Nota Fiscal $theRealId status from ${notaFiscal.status} to $newStatus..." }

        m.newSuspendedTransaction {
            // Store the received FocusNFe event in the database, useful for debugging
            FocusNFeEvents.insert {
                it[event] = response
            }

            notaFiscal.status = newStatus

            if (url != null)
                notaFiscal.url = url // Store the Nota Fiscal URL
        }

        call.respondEmptyJson()
    }

    private suspend fun respondAndThrow(call: ApplicationCall, statusCode: HttpStatusCode, message: String): Nothing {
        call.respondJson(Json.encodeToJsonElement(APIError(message)), status = statusCode)
        error(message)
    }
}