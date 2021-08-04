package net.perfectdreams.perfectpayments.routes

import io.ktor.application.*
import io.ktor.http.*
import io.ktor.response.*
import net.perfectdreams.perfectpayments.PerfectPayments
import net.perfectdreams.perfectpayments.html.MissingPartialPaymentView
import net.perfectdreams.perfectpayments.utils.extensions.getI18nContext
import net.perfectdreams.sequins.ktor.BaseRoute

class MissingPartialPaymentRoute(val m: PerfectPayments) : BaseRoute("/missing") {
    override suspend fun onRequest(call: ApplicationCall) {
        val locale = call.getI18nContext(m)
        call.respondText(MissingPartialPaymentView(locale).render(), ContentType.Text.Html)
    }
}