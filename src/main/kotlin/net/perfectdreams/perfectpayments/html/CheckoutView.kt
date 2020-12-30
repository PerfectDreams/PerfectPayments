package net.perfectdreams.perfectpayments.html

import kotlinx.html.*
import net.perfectdreams.loritta.utils.locale.BaseLocale
import net.perfectdreams.perfectpayments.payments.PaymentGateway
import net.perfectdreams.perfectpayments.utils.PartialPayment

class CheckoutView(val locale: BaseLocale, val partialPayment: PartialPayment, val gateways: List<PaymentGateway>) : BaseView() {
    override fun HTML.generateBody() {
        body {
            div {
                id = "payment-header"

                div {
                    h1 {
                        + locale["selectThePaymentMethod"]
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

                postForm {
                    id = "payment-methods"

                    for (gateway in gateways) {
                        button(classes = "payment-button") {
                            name = "paymentMethod"
                            value = gateway.name

                            img(
                                src = gateway.imageUrl,
                                alt = locale["gateways.${gateway.name.toLowerCase()}.name"]
                            ) {
                                style = "width: 100%; max-height: 80px;"
                            }

                            span {
                                style = "display: block;"

                                b {
                                    +"${locale["accepts"]}:"
                                }

                                val acceptsMethods = gateway.methods.map {
                                    it.name.toLowerCase()
                                        .replace("_", " ")
                                        .split(" ")
                                        .map {
                                            it.capitalize()
                                        }
                                        .joinToString("")
                                        .toCharArray().apply {
                                            this[0] = this[0].toLowerCase()
                                        }
                                        .joinToString("")
                                }.map { locale["methods.$it"]} .joinToString(", ")

                                + " $acceptsMethods"
                            }
                        }
                    }
                }

                div {
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
    }
}