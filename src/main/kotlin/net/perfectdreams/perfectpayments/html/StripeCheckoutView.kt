package net.perfectdreams.perfectpayments.html

import com.stripe.Stripe
import kotlinx.html.*
import net.perfectdreams.loritta.utils.locale.BaseLocale
import net.perfectdreams.perfectpayments.payments.PaymentGateway
import net.perfectdreams.perfectpayments.utils.PartialPayment
import java.util.*

class StripeCheckoutView(val locale: BaseLocale, val partialPaymentId: UUID, val partialPayment: PartialPayment, val publishToken: String, val sessionId: String) : BaseView() {
    override fun HTML.generateBody() {
        body {
            div {
                id = "payment-header"

                div {
                    h1 {
                        + "Stripe"
                    }
                }

                div(classes = "divisor")

                div {
                    div {
                        b {
                            + "Produto: "
                        }

                        +partialPayment.title
                    }

                    div {
                        b {
                            + "Valor: "
                        }

                        +((partialPayment.amount.toDouble() / 100).toString() + " " + partialPayment.currencyId)
                    }
                }
            }

            div {
                id = "wrapper"

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
    }
}