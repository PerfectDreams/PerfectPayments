package net.perfectdreams.perfectpayments.common.data

import kotlinx.serialization.Serializable

@Serializable
class Email private constructor(val accountName: String, val domainName: String) {
    companion object {
        private val EMAIL_REGEX = Regex("^(\\S+)@(\\S+)\$")

        operator fun invoke(input: String): Email {
            // The validation is done here because we don't need to validate it later, due to the data already being extracted
            val result = EMAIL_REGEX.matchEntire(input) ?: throw IllegalArgumentException("Email didn't match RegEx!")

            return Email(result.groupValues[1], result.groupValues[2])
        }
    }

    fun buildEmailAddress(): String = "$accountName@$domainName"
}