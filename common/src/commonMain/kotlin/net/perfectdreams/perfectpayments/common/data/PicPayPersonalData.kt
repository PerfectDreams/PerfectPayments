package net.perfectdreams.perfectpayments.common.data

import kotlinx.serialization.Serializable

@Serializable
data class PicPayPersonalData(
    val socialNumber: CPF,
    val firstName: FirstName,
    val lastName: LastName,
    val email: Email,
    val phone: PhoneNumber
)