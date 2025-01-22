package com.aionlinecourses.data.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "webhook_events")
data class WebhookEvent(
    @PrimaryKey
    val eventId: String,
    val eventType: String,
    val payload: String,
    val timestamp: Long = System.currentTimeMillis(),
    val processed: Boolean = false,
    val error: String? = null
) {
    data class Data(
        val reference: String,
        val status: String,
        val failureReason: String? = null,
        val disputeReason: String? = null
    )
}
