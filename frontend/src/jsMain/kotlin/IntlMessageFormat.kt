@file:JsModule("intl-messageformat")
@file:JsNonModule
import org.jetbrains.compose.web.attributes.Options

@JsName("default")
external class IntlMessageFormat {
    constructor(message: String, locales: String = definedExternally, overrideFormats: Any = definedExternally, opts: Options = definedExternally)

    fun format(values: dynamic): String
}