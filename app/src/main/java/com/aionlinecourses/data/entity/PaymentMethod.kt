package com.aionlinecourses.data.entity

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "payment_methods",
    foreignKeys = [
        ForeignKey(
            entity = User::class,
            parentColumns = ["id"],
            childColumns = ["userId"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [Index(value = ["userId"])]
)
data class PaymentMethod(
    @PrimaryKey(autoGenerate = true)
    val id: Int = 0,
    val userId: Int,
    val type: PaymentType,
    val lastFourDigits: String?,
    val expiryMonth: Int?,
    val expiryYear: Int?,
    val cardBrand: String?,
    val isDefault: Boolean = false,
    val createdAt: Long = System.currentTimeMillis(),
    // Encrypted payment details stored securely
    val encryptedData: String
)

enum class PaymentType {
    CREDIT_CARD,
    DEBIT_CARD,
    PAYPAL,
    BANK_TRANSFER,
    GOOGLE_PAY,
    APPLE_PAY
}
