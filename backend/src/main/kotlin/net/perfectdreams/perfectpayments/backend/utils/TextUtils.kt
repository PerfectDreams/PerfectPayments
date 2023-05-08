package net.perfectdreams.perfectpayments.backend.utils

object TextUtils {
    // Some APIs, like PagSeguro's API, fail if the payment title contains "$"
    // Due to this, we will clean up all titles to avoid these kinds of issues
    private val cleanTitleRegex = Regex("[^\\w0-9()\\-# ]")

    fun cleanTitle(title: String) = title.replace(cleanTitleRegex, "")
}