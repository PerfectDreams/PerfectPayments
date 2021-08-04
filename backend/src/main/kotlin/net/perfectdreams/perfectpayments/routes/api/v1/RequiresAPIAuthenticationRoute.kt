package net.perfectdreams.perfectpayments.routes.api.v1

import io.ktor.application.*
import io.ktor.http.*
import io.ktor.request.*
import mu.KotlinLogging
import net.perfectdreams.perfectpayments.PerfectPayments
import net.perfectdreams.perfectpayments.utils.extensions.respondEmptyJson
import net.perfectdreams.sequins.ktor.BaseRoute

abstract class RequiresAPIAuthenticationRoute(val m: PerfectPayments, path: String) : BaseRoute(path) {
    companion object {
        private val logger = KotlinLogging.logger {}
    }

    abstract suspend fun onAuthenticatedRequest(call: ApplicationCall)

    override suspend fun onRequest(call: ApplicationCall) {
        val path = call.request.path()
        val auth = call.request.header("Authorization")
        val clazzName = this::class.simpleName

        if (auth == null) {
            logger.warn { "Someone tried to access $path (${clazzName}) but the Authorization header was missing!" }
            call.respondEmptyJson(HttpStatusCode.Forbidden)
            return
        }

        val validKey = m.config.tokens.firstOrNull {
            it.name == auth
        }

        logger.trace { "$auth is trying to access $path (${clazzName}), using key $validKey" }

        if (validKey != null)
            onAuthenticatedRequest(call)
        else {
            logger.warn { "Someone tried to access $path (${clazzName}) with a invalid key!" }
            call.respondEmptyJson(HttpStatusCode.Forbidden)
        }
    }
}