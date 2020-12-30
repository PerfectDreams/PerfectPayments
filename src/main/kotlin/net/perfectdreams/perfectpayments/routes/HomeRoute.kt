package net.perfectdreams.perfectpayments.routes

import io.ktor.application.ApplicationCall
import io.ktor.response.respondText
import net.perfectdreams.sequins.ktor.BaseRoute

class HomeRoute : BaseRoute("/") {
    override suspend fun onRequest(call: ApplicationCall) {
        call.respondText("PerfectPayments\n\nhttps://perfectdreams.net/\nhttps://loritta.website/\nhttps://sparklypower.net/")
    }
}