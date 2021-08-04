package net.perfectdreams.perfectpayments.utils

import kotlinx.serialization.json.Json
import java.time.ZoneId

object Constants {
    val CPF_REGEX = Regex("^\\d{3}\\.\\d{3}\\.\\d{3}\\-\\d{2}\$")
    val CNPJ_REGEX = Regex("^\\d{2}\\.\\d{3}\\.\\d{3}\\/\\d{4}\\-\\d{2}\$")
    val ZONE_ID = ZoneId.of("America/Sao_Paulo")
    val jsonIgnoreUnknownKeys = Json {
        ignoreUnknownKeys = true
    }
}