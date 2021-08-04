package net.perfectdreams.perfectpayments.common.data

import kotlinx.serialization.Serializable

@Serializable
data class PersonalData(
    val socialNumber: CPF,
    val name: LegalName,
    val email: Email
)