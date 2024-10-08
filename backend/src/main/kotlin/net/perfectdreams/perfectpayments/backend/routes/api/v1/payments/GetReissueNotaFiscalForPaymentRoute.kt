package net.perfectdreams.perfectpayments.backend.routes.api.v1.payments

import io.ktor.http.*
import io.ktor.server.application.*
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.put
import net.perfectdreams.perfectpayments.backend.PerfectPayments
import net.perfectdreams.perfectpayments.backend.dao.Payment
import net.perfectdreams.perfectpayments.backend.payments.PaymentStatus
import net.perfectdreams.perfectpayments.backend.routes.api.v1.RequiresAPIAuthenticationRoute
import net.perfectdreams.perfectpayments.backend.utils.extensions.respondEmptyJson
import net.perfectdreams.perfectpayments.backend.utils.extensions.respondJson

class GetReissueNotaFiscalForPaymentRoute(m: PerfectPayments) : RequiresAPIAuthenticationRoute(m, "/api/v1/payments/{internalTransactionId}/notas-fiscais/reissue") {
    override suspend fun onAuthenticatedRequest(call: ApplicationCall) {
        val internalTransactionId = call.parameters["internalTransactionId"]!!.toLong()

        val internalPayment = m.newSuspendedTransaction {
            Payment.findById(internalTransactionId)
        } ?: run {
            call.respondEmptyJson(HttpStatusCode.NotFound)
            return
        }

        if (internalPayment.status != PaymentStatus.APPROVED) {
            call.respondJson(
                buildJsonObject {
                    put("status", "You shouldn't issue a nota fiscal for a payment that isn't approved!")
                },
                HttpStatusCode.Forbidden
            )
            return
        }

        // First, we will cancel all notas fiscais issued for this payment, if they exist
        m.notaFiscais?.cancelNotaFiscais(internalPayment, "Criando nova Nota Fiscal")

        // And then we will reissue notas fiscais if the payment was approved
        m.notaFiscais?.generateNotaFiscal(internalPayment, null)

        call.respondEmptyJson(HttpStatusCode.OK)
    }
}