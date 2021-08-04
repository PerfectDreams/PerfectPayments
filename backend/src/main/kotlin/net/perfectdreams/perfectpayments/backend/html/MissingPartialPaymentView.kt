package net.perfectdreams.perfectpayments.backend.html

import kotlinx.html.HTML
import kotlinx.html.body
import kotlinx.html.div
import kotlinx.html.h1
import kotlinx.html.id
import kotlinx.html.img
import kotlinx.html.p
import net.perfectdreams.i18nhelper.core.I18nContext
import net.perfectdreams.perfectpayments.backend.utils.WebsiteAssetsHashManager
import net.perfectdreams.perfectpayments.i18n.TranslationData

class MissingPartialPaymentView(val context: I18nContext, hashManager: WebsiteAssetsHashManager) : BaseView(hashManager) {
    override fun HTML.generateBody() {
        body {
            div {
                id = "warning-screen"

                img(src = "/assets/img/lori_donate.png")

                h1 {
                    + context.get(TranslationData.MissingPartialPayment.Title)
                }

                for (str in context.get(TranslationData.MissingPartialPayment.Description)) {
                    p {
                        + str
                    }
                }
            }
        }
    }
}