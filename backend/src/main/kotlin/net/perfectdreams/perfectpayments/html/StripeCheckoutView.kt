package net.perfectdreams.perfectpayments.html

import kotlinx.html.DIV
import kotlinx.html.p
import kotlinx.html.script
import kotlinx.html.unsafe
import net.perfectdreams.i18nwrapper.I18nContext
import net.perfectdreams.perfectpayments.utils.PartialPayment
import java.util.*

class StripeCheckoutView(
    locale: I18nContext,
    partialPayment: PartialPayment,
    val partialPaymentId: UUID,
    val publishToken: String,
    val sessionId: String
) : CheckoutBaseView(
    locale,
    "Stripe",
    partialPayment
) {
    override fun DIV.generateWrapper() {
        p {
            + "Redirecionando ao Stripe..."
        }
        script(src = "https://js.stripe.com/v3/") {}

        script {
            unsafe {
                raw(
                    """
var stripe = Stripe('${publishToken}');
  
stripe.redirectToCheckout({ sessionId: "$sessionId" })
                    """.trimIndent()
                )
            }
        }
    }
}