package net.perfectdreams.perfectpayments.common.data

import kotlinx.serialization.Serializable
import kotlin.jvm.JvmInline

@Serializable
@JvmInline
value class PhoneNumber(val phoneNumber: String) {
    init {
        require(phoneNumber.isNotBlank()) { "Phone Number must not be blank!" }
    }
}