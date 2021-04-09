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
import net.perfectdreams.perfectpayments.dao.Payment
import net.perfectdreams.perfectpayments.html.MissingPartialPaymentView
import net.perfectdreams.perfectpayments.payments.PaymentGateway
import net.perfectdreams.perfectpayments.payments.PaymentStatus
import net.perfectdreams.perfectpayments.utils.PaymentQuery
import net.perfectdreams.perfectpayments.utils.extensions.getLocale
import net.perfectdreams.sequins.ktor.BaseRoute
import org.jetbrains.exposed.sql.transactions.experimental.newSuspendedTransaction
import java.util.*

class PostCheckoutSandboxRoute(val m: PerfectPayments) : BaseRoute("/checkout/{partialPaymentId}/sandbox") {
    override suspend fun onRequest(call: ApplicationCall) {
        val partialPaymentId = UUID.fromString(call.parameters["partialPaymentId"])

        val partialPayment = m.partialPayments[partialPaymentId]

        if (partialPayment == null) {
            call.respondText(MissingPartialPaymentView(call.getLocale(m)).render(), ContentType.Text.Html, HttpStatusCode.Forbidden)
            return
        }

        val parameters = call.receiveParameters()
        val paymentStatus = PaymentStatus.valueOf(parameters["status"]!!)

        // This is sandbox mode, we are going to start a payment and then change the payment status
        val paymentUrl = PaymentQuery.startPayment(
            m,
            partialPaymentId,
            partialPayment,
            PaymentGateway.SANDBOX,
            buildJsonObject {}
        )

        val paymentId = paymentUrl.toLong()

        val internalPayment = newSuspendedTransaction {
            Payment.findById(paymentId)
        } ?: return // Should not be null but hey, maybe *it could* happen!

        if (internalPayment.status != paymentStatus) {
            newSuspendedTransaction {
                internalPayment.status = paymentStatus

                if (paymentStatus == PaymentStatus.APPROVED)
                    internalPayment.paidAt = System.currentTimeMillis()
            }
        }

        // Same status, just ignore it
        // Send a update to the callback URL
        PaymentQuery.sendPaymentNotification(m, internalPayment)

        call.respondText("Payment $paymentUrl created and set to status $paymentStatus")
    }
}