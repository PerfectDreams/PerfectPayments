package net.perfectdreams.perfectpayments.routes

import io.ktor.application.ApplicationCall
import io.ktor.http.ContentType
import io.ktor.http.HttpStatusCode
import io.ktor.response.respondText
import net.perfectdreams.perfectpayments.PerfectPayments
import net.perfectdreams.perfectpayments.html.CheckoutView
import net.perfectdreams.perfectpayments.html.MissingPartialPaymentView
import net.perfectdreams.perfectpayments.html.SuccessView
import net.perfectdreams.perfectpayments.utils.extensions.getLocale
import net.perfectdreams.sequins.ktor.BaseRoute
import java.util.*

class SuccessRoute(val m: PerfectPayments) : BaseRoute("/success") {
    override suspend fun onRequest(call: ApplicationCall) {
        val locale = call.getLocale(m)
        call.respondText(SuccessView(locale).render(), ContentType.Text.Html)
    }
}