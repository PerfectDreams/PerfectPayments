package net.perfectdreams.perfectpayments.backend.routes.checkout

import io.ktor.application.*
import io.ktor.http.*
import io.ktor.response.*
import net.perfectdreams.perfectpayments.backend.PerfectPayments
import net.perfectdreams.perfectpayments.backend.html.CheckoutView
import net.perfectdreams.sequins.ktor.BaseRoute
import java.util.*

class CheckoutRoute(val m: PerfectPayments) : BaseRoute("/checkout/{partialPaymentId}") {
    override suspend fun onRequest(call: ApplicationCall) {
        val partialPaymentId = try { UUID.fromString(call.parameters["partialPaymentId"]) } catch (e: NumberFormatException) {
            call.respondRedirect("/missing")
            return
        }

        val partialPayment = m.partialPayments[partialPaymentId]

        if (partialPayment == null) {
            call.respondRedirect("/missing")
            return
        }

        call.respondText(CheckoutView(m.hashManager).render(), ContentType.Text.Html)
    }
}