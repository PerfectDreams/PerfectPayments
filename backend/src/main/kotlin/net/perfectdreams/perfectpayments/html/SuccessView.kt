package net.perfectdreams.perfectpayments.html

import kotlinx.html.HTML
import kotlinx.html.body
import kotlinx.html.div
import kotlinx.html.h1
import kotlinx.html.id
import kotlinx.html.img
import kotlinx.html.p
import net.perfectdreams.i18nwrapper.I18nContext
import net.perfectdreams.perfectpayments.i18n.TranslationData

class SuccessView(val context: I18nContext) : BaseView() {
    override fun HTML.generateBody() {
        body {
            div {
                id = "warning-screen"

                img(src = "/assets/img/lori_support.png")

                h1 {
                    + context.get(TranslationData.PaymentSuccess.Title)
                }

                for (str in context.get(TranslationData.PaymentSuccess.Description)) {
                    p {
                        + str
                    }
                }
            }
        }
    }
}