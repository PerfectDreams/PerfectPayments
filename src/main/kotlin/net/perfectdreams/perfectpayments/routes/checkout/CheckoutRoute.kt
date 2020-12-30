package net.perfectdreams.perfectpayments.routes.checkout

import io.ktor.application.ApplicationCall
import io.ktor.http.ContentType
import io.ktor.http.HttpStatusCode
import io.ktor.response.respondText
import net.perfectdreams.perfectpayments.PerfectPayments
import net.perfectdreams.perfectpayments.html.CheckoutView
import net.perfectdreams.perfectpayments.html.MissingPartialPaymentView
import net.perfectdreams.perfectpayments.payments.PaymentGateway
import net.perfectdreams.perfectpayments.utils.extensions.getLocale
import net.perfectdreams.sequins.ktor.BaseRoute
import java.util.*

class CheckoutRoute(val m: PerfectPayments) : BaseRoute("/checkout/{partialPaymentId}") {
    override suspend fun onRequest(call: ApplicationCall) {
        val locale = call.getLocale(m)

        val partialPaymentId = UUID.fromString(call.parameters["partialPaymentId"])

        val partialPayment = m.partialPayments[partialPaymentId]

        if (partialPayment == null) {
            call.respondText(MissingPartialPaymentView(call.getLocale(m)).render(), ContentType.Text.Html, HttpStatusCode.Forbidden)
            return
        }

        call.respondText(CheckoutView(locale, partialPayment, m.config.gateways).render(), ContentType.Text.Html)
    }
}