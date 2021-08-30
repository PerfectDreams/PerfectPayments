package net.perfectdreams.perfectpayments.frontend.utils

import org.jetbrains.compose.web.attributes.AttrsBuilder

// Issue: https://github.com/JetBrains/compose-jb/issues/1053
fun AttrsBuilder<*>.onClickSafariWorkaround(callback: () -> (Unit)) {
    addEventListener("click") {
        callback.invoke()
    }
}