package net.perfectdreams.perfectpayments.backend.processors.creators

import com.stripe.model.checkout.Session
import com.stripe.param.checkout.SessionCreateParams
import kotlinx.serialization.json.JsonObject
import net.perfectdreams.perfectpayments.backend.PerfectPayments
import net.perfectdreams.perfectpayments.backend.utils.PartialPayment

class StripePaymentCreator(val m: PerfectPayments) : PaymentCreator {
    override suspend fun createPayment(paymentId: Long, partialPayment: PartialPayment, data: JsonObject): CreatedPaymentInfo {
        // Watch out! Stripe Payment creator returns a session ID instead of a payment URL!
        // This is required due to the way Stripe payment works!!
        val params: SessionCreateParams = SessionCreateParams.builder()
            .addPaymentMethodType(SessionCreateParams.PaymentMethodType.CARD)
            .setMode(SessionCreateParams.Mode.PAYMENT)
            .setSuccessUrl(m.config.website.url + "/success")
            .setCancelUrl(m.config.website.url + "/cancelled")
            .setPaymentIntentData(
                SessionCreateParams.PaymentIntentData.builder()
                    .putMetadata("referenceId", partialPayment.externalReference.format(paymentId))
                    .build()
            )
            .addLineItem(
                SessionCreateParams.LineItem.builder()
                    .setQuantity(1L)
                    .setPriceData(
                        SessionCreateParams.LineItem.PriceData.builder()
                            .setCurrency(partialPayment.currencyId.toLowerCase())
                            .setUnitAmount(partialPayment.amount)
                            .setProductData(
                                SessionCreateParams.LineItem.PriceData.ProductData.builder()
                                    .setName(partialPayment.title)
                                    .build()
                            )
                            .build()
                    )
                    .build()
            )
            .build()

        val session = Session.create(params)

        return TODO()
    }
}