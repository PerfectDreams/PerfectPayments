package net.perfectdreams.perfectpayments.backend.html

import kotlinx.html.HTML
import kotlinx.html.body
import net.perfectdreams.perfectpayments.backend.utils.WebsiteAssetsHashManager

class CheckoutView(hashManager: WebsiteAssetsHashManager) : BaseView(hashManager) {
    override fun HTML.generateBody() {
        body {}
    }
}