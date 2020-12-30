package net.perfectdreams.perfectpayments.routes.checkout

import io.ktor.application.ApplicationCall
import io.ktor.application.call
import io.ktor.http.ContentType
import io.ktor.http.HttpStatusCode
import io.ktor.http.content.PartData
import io.ktor.http.content.readAllParts
import io.ktor.request.receiveMultipart
import io.ktor.request.receiveParameters
import io.ktor.response.respondRedirect
import io.ktor.response.respondText
import kotlinx.html.*
import kotlinx.html.stream.appendHTML
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.put
import kotlinx.serialization.json.putJsonObject
import net.perfectdreams.perfectpayments.PerfectPayments
import net.perfectdreams.perfectpayments.html.MissingPartialPaymentView
import net.perfectdreams.perfectpayments.html.PicPayCheckoutView
import net.perfectdreams.perfectpayments.html.StripeCheckoutView
import net.perfectdreams.perfectpayments.payments.PaymentGateway
import net.perfectdreams.perfectpayments.processors.creators.PagSeguroPaymentCreator
import net.perfectdreams.perfectpayments.utils.PaymentQuery
import net.perfectdreams.perfectpayments.utils.extensions.getLocale
import net.perfectdreams.sequins.ktor.BaseRoute
import java.util.*

class PostCheckoutRoute(val m: PerfectPayments) : BaseRoute("/checkout/{partialPaymentId}") {
    override suspend fun onRequest(call: ApplicationCall) {
        val locale = call.getLocale(m)
        val partialPaymentId = UUID.fromString(call.parameters["partialPaymentId"])

        val partialPayment = m.partialPayments[partialPaymentId]

        if (partialPayment == null) {
            call.respondText(MissingPartialPaymentView(call.getLocale(m)).render(), ContentType.Text.Html, HttpStatusCode.Forbidden)
            return
        }

        val paymentMethodAsString = call.receiveParameters()["paymentMethod"]!!
        val paymentMethod = PaymentGateway.valueOf(paymentMethodAsString)

        if (paymentMethod == PaymentGateway.PICPAY) {
            call.respondText(PicPayCheckoutView(locale, partialPaymentId, partialPayment).render(), ContentType.Text.Html)
        } else {
            val paymentUrl = PaymentQuery.startPayment(
                    m,
                    partialPaymentId,
                    partialPayment,
                    paymentMethod,
                    buildJsonObject {}
            )

            if (paymentMethod == PaymentGateway.STRIPE) {
                call.respondText(StripeCheckoutView(locale, partialPaymentId, partialPayment, m.gateway.stripe.publishToken, paymentUrl).render(), ContentType.Text.Html)
            } else {
                call.respondRedirect(paymentUrl)
            }
        }
    }
}