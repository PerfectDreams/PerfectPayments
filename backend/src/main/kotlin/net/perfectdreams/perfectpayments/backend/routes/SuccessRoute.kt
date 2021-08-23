package net.perfectdreams.perfectpayments.backend.routes

import io.ktor.application.*
import io.ktor.http.*
import io.ktor.response.*
import net.perfectdreams.perfectpayments.backend.PerfectPayments
import net.perfectdreams.perfectpayments.backend.html.SuccessView
import net.perfectdreams.perfectpayments.backend.utils.extensions.getI18nContext
import net.perfectdreams.sequins.ktor.BaseRoute

class SuccessRoute(val m: PerfectPayments) : BaseRoute("/success") {
    override suspend fun onRequest(call: ApplicationCall) {
        val locale = call.getI18nContext(m)
        call.respondText(SuccessView(locale, m.hashManager).render(), ContentType.Text.Html)
    }
}