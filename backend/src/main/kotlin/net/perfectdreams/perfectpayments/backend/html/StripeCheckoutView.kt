package net.perfectdreams.perfectpayments.backend.html

import kotlinx.html.DIV
import kotlinx.html.p
import kotlinx.html.script
import kotlinx.html.unsafe
import net.perfectdreams.i18nhelper.core.I18nContext
import net.perfectdreams.perfectpayments.backend.utils.PartialPayment
import net.perfectdreams.perfectpayments.backend.utils.WebsiteAssetsHashManager
import java.util.*

class StripeCheckoutView(
    locale: I18nContext,
    hashManager: WebsiteAssetsHashManager,
    partialPayment: PartialPayment,
    val partialPaymentId: UUID,
    val publishToken: String,
    val sessionId: String
) : CheckoutBaseView(
    locale,
    hashManager,
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