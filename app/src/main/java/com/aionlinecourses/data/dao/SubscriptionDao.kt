package com.aionlinecourses.data.dao

import androidx.room.*
import com.aionlinecourses.data.entity.Subscription
import com.aionlinecourses.data.entity.SubscriptionStatus
import kotlinx.coroutines.flow.Flow

@Dao
interface SubscriptionDao {
    @Query("""
        SELECT s.*, c.title as courseTitle, pm.type as paymentType, pm.lastFourDigits 
        FROM subscriptions s
        INNER JOIN courses c ON s.courseId = c.id
        LEFT JOIN payment_methods pm ON s.paymentMethodId = pm.id
        WHERE s.userId = :userId
    """)
    fun getUserSubscriptions(userId: Int): Flow<List<SubscriptionWithDetails>>
    
    @Query("""
        SELECT * FROM subscriptions 
        WHERE userId = :userId AND courseId = :courseId AND status = 'ACTIVE'
        LIMIT 1
    """)
    suspend fun getActiveSubscription(userId: Int, courseId: Int): Subscription?
    
    @Insert
    suspend fun insertSubscription(subscription: Subscription): Long
    
    @Update
    suspend fun updateSubscription(subscription: Subscription)
    
    @Query("""
        UPDATE subscriptions 
        SET status = :status, canceledAt = :timestamp 
        WHERE id = :subscriptionId
    """)
    suspend fun cancelSubscription(
        subscriptionId: Int,
        status: SubscriptionStatus = SubscriptionStatus.CANCELED,
        timestamp: Long = System.currentTimeMillis()
    )
    
    @Query("""
        SELECT * FROM subscriptions 
        WHERE status IN ('ACTIVE', 'TRIALING') 
        AND nextBillingDate <= :currentTime
    """)
    suspend fun getSubscriptionsDueForRenewal(currentTime: Long): List<Subscription>
    
    @Query("""
        UPDATE subscriptions 
        SET lastBillingDate = :timestamp,
            nextBillingDate = :nextBillingDate
        WHERE id = :subscriptionId
    """)
    suspend fun updateBillingDates(
        subscriptionId: Int,
        timestamp: Long = System.currentTimeMillis(),
        nextBillingDate: Long
    )
    
    @Query("""
        UPDATE subscriptions 
        SET status = 'EXPIRED'
        WHERE status = 'ACTIVE' 
        AND endDate IS NOT NULL 
        AND endDate <= :currentTime
    """)
    suspend fun expireSubscriptions(currentTime: Long = System.currentTimeMillis())
    
    @Query("""
        UPDATE subscriptions 
        SET status = 'PAST_DUE'
        WHERE status = 'ACTIVE' 
        AND nextBillingDate < :currentTime
        AND (:gracePeriod IS NULL OR nextBillingDate > :gracePeriod)
    """)
    suspend fun markOverdueSubscriptions(
        currentTime: Long = System.currentTimeMillis(),
        gracePeriod: Long? = null
    )
    
    @Query("""
        SELECT COUNT(*) FROM subscriptions 
        WHERE userId = :userId 
        AND status IN ('ACTIVE', 'TRIALING')
    """)
    suspend fun getActiveSubscriptionCount(userId: Int): Int
    
    @Transaction
    suspend fun renewSubscription(subscription: Subscription): Boolean {
        return try {
            // Update billing dates
            updateBillingDates(
                subscriptionId = subscription.id,
                nextBillingDate = calculateNextBillingDate(
                    subscription.nextBillingDate,
                    subscription.planType
                )
            )
            true
        } catch (e: Exception) {
            false
        }
    }
}

data class SubscriptionWithDetails(
    val id: Int,
    val userId: Int,
    val courseId: Int,
    val paymentMethodId: Int?,
    val planType: SubscriptionPlanType,
    val status: SubscriptionStatus,
    val amount: Float,
    val currency: String,
    val startDate: Long,
    val endDate: Long?,
    val nextBillingDate: Long,
    val lastBillingDate: Long?,
    val canceledAt: Long?,
    val trialEndDate: Long?,
    val courseTitle: String,
    val paymentType: String?,
    val lastFourDigits: String?
)
