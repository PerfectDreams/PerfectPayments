package net.perfectdreams.perfectpayments.backend.tables

import net.perfectdreams.perfectpayments.backend.utils.exposed.jsonb
import org.jetbrains.exposed.dao.id.LongIdTable

object FocusNFeEvents : LongIdTable() {
    val event = jsonb("event")
}