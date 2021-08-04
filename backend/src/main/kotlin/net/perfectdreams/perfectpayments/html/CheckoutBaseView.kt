package net.perfectdreams.perfectpayments.html

import kotlinx.html.DIV
import kotlinx.html.HTML
import kotlinx.html.a
import kotlinx.html.b
import kotlinx.html.body
import kotlinx.html.div
import kotlinx.html.footer
import kotlinx.html.h1
import kotlinx.html.id
import kotlinx.html.p
import kotlinx.html.style
import net.perfectdreams.i18nwrapper.I18nContext
import net.perfectdreams.perfectpayments.utils.PartialPayment

abstract class CheckoutBaseView(
    val locale: I18nContext,
    val checkoutStageTitle: String,
    val partialPayment: PartialPayment
) : BaseView() {
    override fun HTML.generateBody() {
        body {
            div {
                id = "payment-header"

                div {
                    h1 {
                        + checkoutStageTitle
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

            div("payment-methods-menu") {
                id = "wrapper"

                div {
                    generateWrapper()
                }
            }

            footer {
                style = "text-align: center;"

                p {
                    + "PerfectPayments é um serviço experimental de integração de plataforma de pagamentos utilizada pela Loritta, SparklyPower e muito mais. Caso você tenha problemas com o seu pagamento, "

                    a {
                        a("https://loritta.website/support") {
                            + "clique aqui"
                        }
                    }

                    + "."
                }
            }
        }
    }

    abstract fun DIV.generateWrapper()
}