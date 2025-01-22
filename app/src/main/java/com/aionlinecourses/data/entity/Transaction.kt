package com.aionlinecourses.data.entity

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "transactions",
    foreignKeys = [
        ForeignKey(
            entity = User::class,
            parentColumns = ["id"],
            childColumns = ["userId"],
            onDelete = ForeignKey.CASCADE
        ),
        ForeignKey(
            entity = Course::class,
            parentColumns = ["id"],
            childColumns = ["courseId"],
            onDelete = ForeignKey.CASCADE
        ),
        ForeignKey(
            entity = PaymentMethod::class,
            parentColumns = ["id"],
            childColumns = ["paymentMethodId"],
            onDelete = ForeignKey.SET_NULL
        )
    ],
    indices = [
        Index(value = ["userId"]),
        Index(value = ["courseId"]),
        Index(value = ["paymentMethodId"])
    ]
)
data class Transaction(
    @PrimaryKey(autoGenerate = true)
    val id: Int = 0,
    val userId: Int,
    val courseId: Int,
    val amount: Float,
    val currency: String = "USD",
    val status: TransactionStatus,
    val paymentMethodId: Int?,
    val timestamp: Long = System.currentTimeMillis(),
    val transactionReference: String,
    val metadata: Map<String, String> = emptyMap()
)

enum class TransactionStatus {
    PENDING,
    COMPLETED,
    FAILED,
    REFUNDED,
    CANCELLED,
    DISPUTED,
    EXPIRED,
    AUTHORIZED,
    CAPTURED
}

enum class PaymentProvider {
    STRIPE,
    XENDIT
}

enum class XenditPaymentType {
    CREDIT_CARD,
    VIRTUAL_ACCOUNT,
    EWALLET,
    RETAIL_OUTLET,
    QR_CODE
}
