package com.aionlinecourses.service

import android.content.Context
import com.aionlinecourses.data.AppDatabase
import com.aionlinecourses.data.entity.SubscriptionPlanType
import com.aionlinecourses.data.entity.SubscriptionStatus
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import java.util.concurrent.TimeUnit

class SubscriptionAnalyticsService(context: Context) {
    private val database = AppDatabase.getDatabase(context)
    private val subscriptionDao = database.subscriptionDao()
    private val analyticsService = AnalyticsService(context)
    
    fun getSubscriptionMetrics(userId: Int): Flow<SubscriptionMetrics> {
        return subscriptionDao.getUserSubscriptions(userId).map { subscriptions ->
            SubscriptionMetrics(
                activeSubscriptions = subscriptions.count { it.status == SubscriptionStatus.ACTIVE },
                totalRevenue = subscriptions
                    .filter { it.status == SubscriptionStatus.ACTIVE }
                    .sumOf { it.amount.toDouble() }
                    .toFloat(),
                subscriptionsByPlan = subscriptions
                    .groupBy { it.planType }
                    .mapValues { it.value.size },
                averageSubscriptionLength = calculateAverageSubscriptionLength(subscriptions),
                churnRate = calculateChurnRate(subscriptions),
                trialConversionRate = calculateTrialConversionRate(subscriptions)
            )
        }
    }
    
    suspend fun trackSubscriptionEvent(
        eventName: String,
        userId: Int,
        subscriptionId: Int,
        additionalParams: Map<String, String> = emptyMap()
    ) {
        val params = mutableMapOf(
            "user_id" to userId.toString(),
            "subscription_id" to subscriptionId.toString()
        ).apply {
            putAll(additionalParams)
        }
        
        analyticsService.logEvent(eventName, params)
    }
    
    private fun calculateAverageSubscriptionLength(
        subscriptions: List<SubscriptionWithDetails>
    ): Float {
        val completedSubscriptions = subscriptions.filter {
            it.status == SubscriptionStatus.EXPIRED || it.status == SubscriptionStatus.CANCELED
        }
        
        if (completedSubscriptions.isEmpty()) return 0f
        
        val totalDays = completedSubscriptions.sumOf { subscription ->
            val endTime = subscription.canceledAt ?: subscription.endDate ?: System.currentTimeMillis()
            TimeUnit.MILLISECONDS.toDays(endTime - subscription.startDate)
        }
        
        return totalDays.toFloat() / completedSubscriptions.size
    }
    
    private fun calculateChurnRate(subscriptions: List<SubscriptionWithDetails>): Float {
        val totalSubscriptions = subscriptions.size
        if (totalSubscriptions == 0) return 0f
        
        val canceledSubscriptions = subscriptions.count {
            it.status == SubscriptionStatus.CANCELED
        }
        
        return (canceledSubscriptions.toFloat() / totalSubscriptions) * 100
    }
    
    private fun calculateTrialConversionRate(
        subscriptions: List<SubscriptionWithDetails>
    ): Float {
        val trialSubscriptions = subscriptions.filter {
            it.trialEndDate != null
        }
        
        if (trialSubscriptions.isEmpty()) return 0f
        
        val convertedTrials = trialSubscriptions.count {
            it.status == SubscriptionStatus.ACTIVE &&
                    (it.trialEndDate ?: 0) < System.currentTimeMillis()
        }
        
        return (convertedTrials.toFloat() / trialSubscriptions.size) * 100
    }
    
    fun getRevenueByPeriod(
        userId: Int,
        startTime: Long,
        endTime: Long
    ): Flow<Map<SubscriptionPlanType, Float>> {
        return subscriptionDao.getUserSubscriptions(userId).map { subscriptions ->
            subscriptions
                .filter {
                    it.status == SubscriptionStatus.ACTIVE &&
                            it.startDate >= startTime &&
                            it.startDate <= endTime
                }
                .groupBy { it.planType }
                .mapValues { entry ->
                    entry.value.sumOf { it.amount.toDouble() }.toFloat()
                }
        }
    }
    
    suspend fun generateSubscriptionReport(userId: Int): SubscriptionReport {
        val subscriptions = subscriptionDao.getUserSubscriptions(userId)
            .map { it.filter { sub -> sub.status == SubscriptionStatus.ACTIVE } }
            
        return SubscriptionReport(
            totalActiveSubscriptions = subscriptions.map { it.size },
            revenueByPlan = getRevenueByPeriod(
                userId,
                System.currentTimeMillis() - TimeUnit.DAYS.toMillis(30),
                System.currentTimeMillis()
            ),
            subscriptionMetrics = getSubscriptionMetrics(userId)
        )
    }
}

data class SubscriptionMetrics(
    val activeSubscriptions: Int,
    val totalRevenue: Float,
    val subscriptionsByPlan: Map<SubscriptionPlanType, Int>,
    val averageSubscriptionLength: Float,
    val churnRate: Float,
    val trialConversionRate: Float
)

data class SubscriptionReport(
    val totalActiveSubscriptions: Flow<Int>,
    val revenueByPlan: Flow<Map<SubscriptionPlanType, Float>>,
    val subscriptionMetrics: Flow<SubscriptionMetrics>
)
