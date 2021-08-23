package net.perfectdreams.perfectpayments.common.data

import kotlinx.serialization.Serializable
import kotlin.jvm.JvmInline

@Serializable
@JvmInline
value class FirstName(val name: String) {
    init {
        require(name.isNotBlank()) { "Name must not be blank!" }
    }
}