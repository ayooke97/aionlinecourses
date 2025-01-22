package com.aionlinecourses.data.entity

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "subscriptions",
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
data class Subscription(
    @PrimaryKey(autoGenerate = true)
    val id: Int = 0,
    val userId: Int,
    val courseId: Int,
    val paymentMethodId: Int?,
    val planType: SubscriptionPlanType,
    val status: SubscriptionStatus,
    val amount: Float,
    val currency: String = "USD",
    val startDate: Long = System.currentTimeMillis(),
    val endDate: Long? = null,
    val nextBillingDate: Long,
    val lastBillingDate: Long? = null,
    val canceledAt: Long? = null,
    val trialEndDate: Long? = null
)

enum class SubscriptionPlanType {
    MONTHLY,
    QUARTERLY,
    YEARLY,
    LIFETIME
}

enum class SubscriptionStatus {
    ACTIVE,
    CANCELED,
    EXPIRED,
    PAST_DUE,
    TRIALING
}
