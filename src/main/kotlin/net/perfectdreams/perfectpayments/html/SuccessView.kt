package net.perfectdreams.perfectpayments.html

import kotlinx.html.*
import net.perfectdreams.loritta.utils.locale.BaseLocale
import net.perfectdreams.perfectpayments.payments.PaymentGateway

class SuccessView(val locale: BaseLocale) : BaseView() {
    override fun HTML.generateBody() {
        body {
            div {
                id = "warning-screen"

                img(src = "/assets/img/lori_support.png")

                h1 {
                    + locale["paymentSuccess.title"]
                }

                for (str in locale.getList("paymentSuccess.thanksForThePurchase")) {
                    p {
                        + str
                    }
                }
            }
        }
    }
}