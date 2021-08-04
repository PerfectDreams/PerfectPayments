package net.perfectdreams.perfectpayments.backend.html

import kotlinx.html.HEAD
import kotlinx.html.HTML
import kotlinx.html.head
import kotlinx.html.html
import kotlinx.html.link
import kotlinx.html.meta
import kotlinx.html.script
import kotlinx.html.stream.appendHTML
import kotlinx.html.styleLink
import kotlinx.html.title
import net.perfectdreams.perfectpayments.backend.utils.WebsiteAssetsHashManager

abstract class BaseView(internal val hashManager: WebsiteAssetsHashManager) {
    fun render(): String {
        val builder = StringBuilder()

        builder.appendHTML().html {
            head {
                title(getFullTitle())
                meta(name = "viewport", content = "width=device-width, initial-scale=1")
                styleLink("/assets/css/style.css?hash=${hashManager.getAssetHash("/assets/css/style.css")}")
                script(src = "/assets/js/frontend.js?hash=${hashManager.getAssetHash("/assets/js/frontend.js")}") {
                    defer = true // Only execute after the page has been parsed
                }

                link(href = "/favicon.ico", rel = "icon", type = "image/x-icon")
                generateMeta()
            }
            generateBody()
        }

        return builder.toString()
    }

    open fun getTitle() = "Pagamento"
    open fun getFullTitle() = "${getTitle()} • PerfectPayments"
    open fun getDescription() = ""

    open fun HEAD.generateMeta() {
        /* meta("theme-color", "#00c0ff")
        meta(name = "twitter:card", content = "summary")
        meta(name = "twitter:site", content = "@MrPowerGamerBR")
        meta(name = "twitter:creator", content = "@MrPowerGamerBR")
        meta(content = "MrPowerGamerBR Website") { attributes["property"] = "og:site_name" }
        meta(content = getDescription()) { attributes["property"] = "og:description" }
        meta(content = getTitle()) { attributes["property"] = "og:title" }
        meta(content = "600") { attributes["property"] = "og:ttl" }
        meta(content = "/assets/img/avatar.png") { attributes["property"] = "og:image"} */
    }

    abstract fun HTML.generateBody()
}