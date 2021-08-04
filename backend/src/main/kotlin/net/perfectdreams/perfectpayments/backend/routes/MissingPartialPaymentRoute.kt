package net.perfectdreams.perfectpayments.backend.routes

import io.ktor.application.*
import io.ktor.http.*
import io.ktor.response.*
import net.perfectdreams.perfectpayments.backend.PerfectPayments
import net.perfectdreams.perfectpayments.backend.html.MissingPartialPaymentView
import net.perfectdreams.perfectpayments.backend.utils.extensions.getI18nContext
import net.perfectdreams.sequins.ktor.BaseRoute

class MissingPartialPaymentRoute(val m: PerfectPayments) : BaseRoute("/missing") {
    override suspend fun onRequest(call: ApplicationCall) {
        val locale = call.getI18nContext(m)
        call.respondText(MissingPartialPaymentView(locale, m.hashManager).render(), ContentType.Text.Html)
    }
}