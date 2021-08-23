package net.perfectdreams.perfectpayments.common.data

import kotlinx.serialization.Serializable
import kotlin.jvm.JvmInline

@Serializable
@JvmInline
value class LegalName(val name: String) {
    constructor(firstName: FirstName, lastName: LastName) : this(firstName.name + " " + lastName.name)

    init {
        require(name.isNotBlank()) { "Name must not be blank!" }
    }
}