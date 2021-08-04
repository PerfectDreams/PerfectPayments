package net.perfectdreams.perfectpayments.html

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

abstract class BaseView {
    fun render(): String {
        val builder = StringBuilder()

        builder.appendHTML().html {
            head {
                title(getFullTitle())
                meta(name = "viewport", content = "width=device-width, initial-scale=1")
                styleLink("/assets/css/style.css?v=${System.currentTimeMillis()}") // TODO: REMOVE LATER
                script(src = "/assets/js/frontend.js?v=${System.currentTimeMillis()}") { // TODO: REMOVE LATER
                    defer = true // Only execute after the page has been parsed
                }

                // styleLink("https://use.fontawesome.com/releases/v5.8.1/css/all.css")
                /* unsafe {
                    raw("""<script async src="https://pagead2.googlesyndication.com/pagead/js/adsbygoogle.js"></script>""")
                } */
                /* unsafe {
                    raw("""<!-- Global site tag (gtag.js) - Google Analytics -->
<script async src="https://www.googletagmanager.com/gtag/js?id=UA-53518408-11"></script>
<script>
  window.dataLayer = window.dataLayer || [];
  function gtag(){dataLayer.push(arguments);}
  gtag('js', new Date());

  gtag('config', 'UA-53518408-11');
</script>
""")
                }
                script(src = "/assets/js/PowerCMSJS.js") {} */

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