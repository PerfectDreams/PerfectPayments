package net.perfectdreams.perfectpayments.html

import kotlinx.html.*
import net.perfectdreams.loritta.utils.locale.BaseLocale
import net.perfectdreams.perfectpayments.payments.PaymentGateway

class MissingPartialPaymentView(val locale: BaseLocale) : BaseView() {
    override fun HTML.generateBody() {
        body {
            div {
                id = "warning-screen"

                img(src = "/assets/img/lori_donate.png")

                h1 {
                    + locale["missingPartialPayment.expiredPaymentLink"]
                }

                for (str in locale.getList("missingPartialPayment.pleaseGenerateANewOne")) {
                    p {
                        + str
                    }
                }
            }
        }
    }
}