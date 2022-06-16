package net.perfectdreams.perfectpayments.backend.routes

import io.ktor.server.application.*
import io.ktor.server.response.*
import net.perfectdreams.sequins.ktor.BaseRoute

class HomeRoute : BaseRoute("/") {
    override suspend fun onRequest(call: ApplicationCall) {
        call.respondText("PerfectPayments\n\nhttps://perfectdreams.net/\nhttps://loritta.website/\nhttps://sparklypower.net/")
    }
}