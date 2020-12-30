package net.perfectdreams.perfectpayments.html

import kotlinx.html.*
import kotlinx.html.stream.appendHTML

abstract class BaseView {
    val codePattern = Regex("\\{%(.*?)%}", RegexOption.DOT_MATCHES_ALL)

    fun render(): String {
        val builder = StringBuilder()

        builder.appendHTML().html {
            head {
                title(getFullTitle())
                meta(name = "viewport", content = "width=device-width, initial-scale=1")
                styleLink("/assets/css/style.css")
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