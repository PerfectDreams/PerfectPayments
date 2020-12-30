package net.perfectdreams.perfectpayments.html

import kotlinx.html.*
import net.perfectdreams.loritta.utils.locale.BaseLocale
import net.perfectdreams.perfectpayments.payments.PaymentGateway
import net.perfectdreams.perfectpayments.utils.PartialPayment
import java.util.*

class PicPayCheckoutView(val locale: BaseLocale, val partialPaymentId: UUID, val partialPayment: PartialPayment) : BaseView() {
    override fun HTML.generateBody() {
        body {
            div {
                id = "payment-header"

                div {
                    h1 {
                        + locale["fillYourData"]
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
                postForm(action = "/checkout/${partialPaymentId}/picpay") {
                    div {
                        + "Nome"
                    }
                    input(InputType.text, classes = "first-name") {
                        name = "firstName"

                        placeholder = "Loritta"
                    }

                    div {
                        + "Sobrenome"
                    }
                    input(InputType.text, classes = "last-name") {
                        name = "lastName"
                        placeholder = "Morenitta"
                    }

                    div {
                        + "Email"
                    }
                    input(InputType.text, classes = "email") {
                        name = "email"
                        placeholder = "me@loritta.website"
                    }

                    div {
                        + "CPF"
                    }
                    input(InputType.text, classes = "document") {
                        name = "document"
                        placeholder = "111.222.333-45"
                    }

                    div {
                        + "Número de Telefone"
                    }
                    input(InputType.text, classes = "phone") {
                        name = "phone"
                        placeholder = "+11 40028922"
                    }

                    div {
                        b {
                            + "${locale["attention"]}: "
                        }
                        + locale["gateways.picpay.dataWarning"]
                    }

                    hr {}

                    button(classes = "button primary") {
                        + locale["continueToPayment"]
                    }
                }
            }
        }
    }
}