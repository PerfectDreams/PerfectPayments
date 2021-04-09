package net.perfectdreams.perfectpayments.html

import kotlinx.html.*
import net.perfectdreams.loritta.utils.locale.BaseLocale
import net.perfectdreams.perfectpayments.payments.PaymentGateway
import net.perfectdreams.perfectpayments.payments.PaymentStatus
import net.perfectdreams.perfectpayments.utils.PartialPayment
import java.util.*

class SandboxCheckoutView(val locale: BaseLocale, val partialPaymentId: UUID, val partialPayment: PartialPayment) : BaseView() {
    override fun HTML.generateBody() {
        body {
            div {
                id = "payment-header"

                div {
                    h1 {
                        + "Sandbox Mode!!"
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
                postForm(action = "/checkout/${partialPaymentId}/sandbox") {
                    div {
                        + "Payment Status: "
                    }

                    select {
                        name = "status"

                        for (status in PaymentStatus.values()) {
                            option {
                                value = status.name

                                + status.name
                            }
                        }
                    }

                    button(classes = "button primary") {
                        + locale["continueToPayment"]
                    }
                }
            }
        }
    }
}