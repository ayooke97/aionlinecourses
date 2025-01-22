package com.aionlinecourses.ui.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.aionlinecourses.data.AppDatabase
import com.aionlinecourses.data.dao.SubscriptionWithDetails
import com.aionlinecourses.data.entity.SubscriptionPlanType
import com.aionlinecourses.service.SubscriptionAnalyticsService
import com.aionlinecourses.service.SubscriptionResult
import com.aionlinecourses.service.SubscriptionService
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import java.util.concurrent.TimeUnit

class SubscriptionViewModel(application: Application) : AndroidViewModel(application) {
    private val database = AppDatabase.getDatabase(application)
    private val subscriptionDao = database.subscriptionDao()
    private val analyticsService = SubscriptionAnalyticsService(application)
    private val subscriptionService = SubscriptionService(
        context = application,
        paymentService = PaymentService(
            context = application,
            stripePublishableKey = "your_stripe_key"
        ),
        notificationService = PaymentNotificationService(application)
    )
    
    private val currentUserId = 1 // TODO: Get from AuthRepository
    
    private val _subscriptionState = MutableStateFlow<SubscriptionState>(SubscriptionState.Initial)
    val subscriptionState = _subscriptionState.asStateFlow()
    
    val subscriptions = subscriptionDao.getUserSubscriptions(currentUserId)
    val subscriptionMetrics = analyticsService.getSubscriptionMetrics(currentUserId)
    
    fun createSubscription(
        courseId: Int,
        planType: SubscriptionPlanType,
        paymentMethodId: Int?,
        amount: Float,
        withTrial: Boolean = false
    ) {
        viewModelScope.launch {
            _subscriptionState.value = SubscriptionState.Processing
            
            when (val result = subscriptionService.createSubscription(
                userId = currentUserId,
                courseId = courseId,
                planType = planType,
                paymentMethodId = paymentMethodId,
                amount = amount,
                withTrial = withTrial
            )) {
                is SubscriptionResult.Success -> {
                    _subscriptionState.value = SubscriptionState.Success(result.subscriptionId)
                    analyticsService.trackSubscriptionEvent(
                        "subscription_created",
                        currentUserId,
                        result.subscriptionId,
                        mapOf(
                            "plan_type" to planType.name,
                            "with_trial" to withTrial.toString()
                        )
                    )
                }
                is SubscriptionResult.Error -> {
                    _subscriptionState.value = SubscriptionState.Error(result.message)
                }
            }
        }
    }
    
    fun cancelSubscription(subscriptionId: Int) {
        viewModelScope.launch {
            _subscriptionState.value = SubscriptionState.Processing
            
            when (val result = subscriptionService.cancelSubscription(
                subscriptionId = subscriptionId,
                userId = currentUserId
            )) {
                is SubscriptionResult.Success -> {
                    _subscriptionState.value = SubscriptionState.Cancelled(subscriptionId)
                    analyticsService.trackSubscriptionEvent(
                        "subscription_cancelled",
                        currentUserId,
                        subscriptionId
                    )
                }
                is SubscriptionResult.Error -> {
                    _subscriptionState.value = SubscriptionState.Error(result.message)
                }
            }
        }
    }
    
    fun getRevenueAnalytics(period: AnalyticsPeriod) {
        viewModelScope.launch {
            val (startTime, endTime) = when (period) {
                AnalyticsPeriod.LAST_WEEK -> getTimeRange(TimeUnit.DAYS.toMillis(7))
                AnalyticsPeriod.LAST_MONTH -> getTimeRange(TimeUnit.DAYS.toMillis(30))
                AnalyticsPeriod.LAST_YEAR -> getTimeRange(TimeUnit.DAYS.toMillis(365))
            }
            
            analyticsService.getRevenueByPeriod(currentUserId, startTime, endTime)
                .collect { revenue ->
                    _subscriptionState.value = SubscriptionState.RevenueData(revenue)
                }
        }
    }
    
    fun generateReport() {
        viewModelScope.launch {
            _subscriptionState.value = SubscriptionState.Processing
            try {
                val report = analyticsService.generateSubscriptionReport(currentUserId)
                _subscriptionState.value = SubscriptionState.ReportGenerated(report)
            } catch (e: Exception) {
                _subscriptionState.value = SubscriptionState.Error(
                    e.message ?: "Failed to generate report"
                )
            }
        }
    }
    
    private fun getTimeRange(duration: Long): Pair<Long, Long> {
        val endTime = System.currentTimeMillis()
        val startTime = endTime - duration
        return startTime to endTime
    }
    
    fun clearState() {
        _subscriptionState.value = SubscriptionState.Initial
    }
}

sealed class SubscriptionState {
    object Initial : SubscriptionState()
    object Processing : SubscriptionState()
    data class Success(val subscriptionId: Int) : SubscriptionState()
    data class Cancelled(val subscriptionId: Int) : SubscriptionState()
    data class Error(val message: String) : SubscriptionState()
    data class RevenueData(val revenueByPlan: Map<SubscriptionPlanType, Float>) : SubscriptionState()
    data class ReportGenerated(val report: SubscriptionReport) : SubscriptionState()
}

enum class AnalyticsPeriod {
    LAST_WEEK,
    LAST_MONTH,
    LAST_YEAR
}
