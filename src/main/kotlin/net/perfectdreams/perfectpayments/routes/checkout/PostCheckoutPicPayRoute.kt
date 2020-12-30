package net.perfectdreams.perfectpayments.routes.checkout

import io.ktor.application.ApplicationCall
import io.ktor.application.call
import io.ktor.http.ContentType
import io.ktor.http.HttpStatusCode
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
import net.perfectdreams.perfectpayments.payments.PaymentGateway
import net.perfectdreams.perfectpayments.utils.PaymentQuery
import net.perfectdreams.perfectpayments.utils.extensions.getLocale
import net.perfectdreams.sequins.ktor.BaseRoute
import java.util.*

class PostCheckoutPicPayRoute(val m: PerfectPayments) : BaseRoute("/checkout/{partialPaymentId}/picpay") {
    override suspend fun onRequest(call: ApplicationCall) {
        val partialPaymentId = UUID.fromString(call.parameters["partialPaymentId"])

        val partialPayment = m.partialPayments[partialPaymentId]

        if (partialPayment == null) {
            call.respondText(MissingPartialPaymentView(call.getLocale(m)).render(), ContentType.Text.Html, HttpStatusCode.Forbidden)
            return
        }

        val parameters = call.receiveParameters()
        val firstName = parameters["firstName"]!!
        val lastName = parameters["lastName"]!!
        val document = parameters["document"]!!
        val email = parameters["email"]!!
        val phone = parameters["phone"]!!

        val paymentUrl = PaymentQuery.startPayment(
            m,
            partialPaymentId,
            partialPayment,
            PaymentGateway.PICPAY,
            buildJsonObject {
                putJsonObject("buyer") {
                    put("firstName", firstName)
                    put("lastName", lastName)
                    put("document", document)
                    put("email", email)
                    put("phone", phone)
                }
            }
        )

        call.respondRedirect(paymentUrl)
    }
}