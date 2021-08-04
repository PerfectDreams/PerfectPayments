package net.perfectdreams.perfectpayments.routes

import io.ktor.application.*
import io.ktor.http.*
import io.ktor.response.*
import net.perfectdreams.perfectpayments.PerfectPayments
import net.perfectdreams.perfectpayments.html.CancelledView
import net.perfectdreams.perfectpayments.utils.extensions.getI18nContext
import net.perfectdreams.sequins.ktor.BaseRoute

class CancelledRoute(val m: PerfectPayments) : BaseRoute("/cancelled") {
    override suspend fun onRequest(call: ApplicationCall) {
        val locale = call.getI18nContext(m)
        call.respondText(CancelledView(locale).render(), ContentType.Text.Html)
    }
}