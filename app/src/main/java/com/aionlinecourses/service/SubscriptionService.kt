package com.aionlinecourses.service

import android.content.Context
import com.aionlinecourses.data.AppDatabase
import com.aionlinecourses.data.entity.*
import kotlinx.coroutines.*
import java.util.*
import java.util.concurrent.TimeUnit

class SubscriptionService(
    private val context: Context,
    private val paymentService: PaymentService,
    private val notificationService: PaymentNotificationService
) {
    private val database = AppDatabase.getDatabase(context)
    private val subscriptionDao = database.subscriptionDao()
    private val analyticsService = AnalyticsService(context)
    
    private val subscriptionScope = CoroutineScope(
        Dispatchers.IO + SupervisorJob()
    )
    
    init {
        startSubscriptionRenewalJob()
    }
    
    private fun startSubscriptionRenewalJob() {
        subscriptionScope.launch {
            while (isActive) {
                try {
                    processSubscriptionRenewals()
                    processExpiredSubscriptions()
                    processOverdueSubscriptions()
                } catch (e: Exception) {
                    // Log error
                }
                delay(TimeUnit.HOURS.toMillis(1))
            }
        }
    }
    
    suspend fun createSubscription(
        userId: Int,
        courseId: Int,
        planType: SubscriptionPlanType,
        paymentMethodId: Int?,
        amount: Float,
        withTrial: Boolean = false
    ): SubscriptionResult = withContext(Dispatchers.IO) {
        try {
            // Check if user already has an active subscription
            val existingSubscription = subscriptionDao.getActiveSubscription(userId, courseId)
            if (existingSubscription != null) {
                return@withContext SubscriptionResult.Error("Already subscribed to this course")
            }
            
            val now = System.currentTimeMillis()
            val trialEndDate = if (withTrial) {
                now + TimeUnit.DAYS.toMillis(7)
            } else null
            
            val subscription = Subscription(
                userId = userId,
                courseId = courseId,
                paymentMethodId = paymentMethodId,
                planType = planType,
                status = if (withTrial) SubscriptionStatus.TRIALING else SubscriptionStatus.ACTIVE,
                amount = amount,
                startDate = now,
                nextBillingDate = trialEndDate ?: calculateNextBillingDate(now, planType),
                trialEndDate = trialEndDate
            )
            
            // Process initial payment if not in trial
            if (!withTrial) {
                val paymentResult = paymentService.processPayment(
                    userId = userId,
                    courseId = courseId,
                    amount = amount,
                    paymentMethodId = paymentMethodId
                )
                
                if (paymentResult is PaymentResult.Error) {
                    return@withContext SubscriptionResult.Error(paymentResult.message)
                }
            }
            
            val subscriptionId = subscriptionDao.insertSubscription(subscription)
            
            // Track analytics
            analyticsService.logEvent(
                "subscription_created",
                mapOf(
                    "subscription_id" to subscriptionId.toString(),
                    "plan_type" to planType.name,
                    "with_trial" to withTrial.toString()
                )
            )
            
            SubscriptionResult.Success(subscriptionId.toInt())
        } catch (e: Exception) {
            SubscriptionResult.Error(e.message ?: "Failed to create subscription")
        }
    }
    
    suspend fun cancelSubscription(
        subscriptionId: Int,
        userId: Int
    ): SubscriptionResult = withContext(Dispatchers.IO) {
        try {
            subscriptionDao.cancelSubscription(subscriptionId)
            
            // Track analytics
            analyticsService.logEvent(
                "subscription_canceled",
                mapOf(
                    "subscription_id" to subscriptionId.toString(),
                    "user_id" to userId.toString()
                )
            )
            
            SubscriptionResult.Success(subscriptionId)
        } catch (e: Exception) {
            SubscriptionResult.Error(e.message ?: "Failed to cancel subscription")
        }
    }
    
    private suspend fun processSubscriptionRenewals() {
        val subscriptionsDue = subscriptionDao.getSubscriptionsDueForRenewal(
            System.currentTimeMillis()
        )
        
        for (subscription in subscriptionsDue) {
            try {
                // Process payment
                val paymentResult = paymentService.processPayment(
                    userId = subscription.userId,
                    courseId = subscription.courseId,
                    amount = subscription.amount,
                    paymentMethodId = subscription.paymentMethodId
                )
                
                when (paymentResult) {
                    is PaymentResult.Success -> {
                        // Renew subscription
                        if (subscriptionDao.renewSubscription(subscription)) {
                            notificationService.showPaymentNotification(
                                Transaction(
                                    userId = subscription.userId,
                                    courseId = subscription.courseId,
                                    amount = subscription.amount,
                                    status = TransactionStatus.COMPLETED,
                                    paymentMethodId = subscription.paymentMethodId,
                                    transactionReference = "SUB-${subscription.id}-${UUID.randomUUID().toString().take(8)}"
                                )
                            )
                        }
                    }
                    is PaymentResult.Error -> {
                        // Mark subscription as past due
                        subscriptionDao.markOverdueSubscriptions(
                            currentTime = System.currentTimeMillis()
                        )
                    }
                }
            } catch (e: Exception) {
                // Log error
            }
        }
    }
    
    private suspend fun processExpiredSubscriptions() {
        subscriptionDao.expireSubscriptions(System.currentTimeMillis())
    }
    
    private suspend fun processOverdueSubscriptions() {
        val gracePeriod = System.currentTimeMillis() - TimeUnit.DAYS.toMillis(3)
        subscriptionDao.markOverdueSubscriptions(
            currentTime = System.currentTimeMillis(),
            gracePeriod = gracePeriod
        )
    }
}

sealed class SubscriptionResult {
    data class Success(val subscriptionId: Int) : SubscriptionResult()
    data class Error(val message: String) : SubscriptionResult()
}

fun calculateNextBillingDate(currentDate: Long, planType: SubscriptionPlanType): Long {
    val calendar = Calendar.getInstance().apply { timeInMillis = currentDate }
    
    when (planType) {
        SubscriptionPlanType.MONTHLY -> calendar.add(Calendar.MONTH, 1)
        SubscriptionPlanType.QUARTERLY -> calendar.add(Calendar.MONTH, 3)
        SubscriptionPlanType.YEARLY -> calendar.add(Calendar.YEAR, 1)
        SubscriptionPlanType.LIFETIME -> calendar.add(Calendar.YEAR, 100)
    }
    
    return calendar.timeInMillis
}
