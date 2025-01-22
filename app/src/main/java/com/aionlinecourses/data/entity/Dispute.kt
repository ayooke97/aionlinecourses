package com.aionlinecourses.data.entity

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "disputes",
    foreignKeys = [
        ForeignKey(
            entity = Transaction::class,
            parentColumns = ["id"],
            childColumns = ["transactionId"],
            onDelete = ForeignKey.CASCADE
        ),
        ForeignKey(
            entity = User::class,
            parentColumns = ["id"],
            childColumns = ["userId"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [
        Index(value = ["transactionId"]),
        Index(value = ["userId"])
    ]
)
data class Dispute(
    @PrimaryKey(autoGenerate = true)
    val id: Int = 0,
    val transactionId: Int,
    val userId: Int,
    val reason: String,
    val evidence: String,
    val status: DisputeStatus,
    val resolution: String? = null,
    val createdAt: Long = System.currentTimeMillis(),
    val resolvedAt: Long? = null
)

enum class DisputeStatus {
    PENDING,
    UNDER_REVIEW,
    RESOLVED_MERCHANT_WIN,
    RESOLVED_CUSTOMER_WIN,
    CANCELLED
}
