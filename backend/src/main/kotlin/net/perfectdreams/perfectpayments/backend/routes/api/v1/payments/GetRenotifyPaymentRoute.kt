package net.perfectdreams.perfectpayments.backend.routes.api.v1.payments

import io.ktor.server.application.*
import io.ktor.http.*
import net.perfectdreams.perfectpayments.backend.PerfectPayments
import net.perfectdreams.perfectpayments.backend.dao.Payment
import net.perfectdreams.perfectpayments.backend.routes.api.v1.RequiresAPIAuthenticationRoute
import net.perfectdreams.perfectpayments.backend.utils.PaymentQuery
import net.perfectdreams.perfectpayments.backend.utils.extensions.respondEmptyJson

class GetRenotifyPaymentRoute(m: PerfectPayments) : RequiresAPIAuthenticationRoute(m, "/api/v1/payments/{internalTransactionId}/webhooks/renotify") {
    override suspend fun onAuthenticatedRequest(call: ApplicationCall) {
        val internalTransactionId = call.parameters["internalTransactionId"]!!.toLong()

        val internalPayment = m.newSuspendedTransaction {
            Payment.findById(internalTransactionId)
        } ?: run {
            call.respondEmptyJson(HttpStatusCode.NotFound)
            return
        }

        // Send a update to the callback URL
        PaymentQuery.sendPaymentNotification(m, internalPayment)

        call.respondEmptyJson(HttpStatusCode.OK)
    }
}