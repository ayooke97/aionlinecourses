package com.aionlinecourses.service

import android.content.Context
import com.aionlinecourses.data.AppDatabase
import com.aionlinecourses.data.entity.Transaction
import com.aionlinecourses.data.entity.TransactionStatus
import com.aionlinecourses.data.entity.WebhookEvent
import com.google.gson.Gson
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.security.MessageDigest
import javax.crypto.Mac
import javax.crypto.spec.SecretKeySpec

class XenditWebhookService(
    private val context: Context,
    private val webhookSecret: String
) {
    private val database = AppDatabase.getDatabase(context)
    private val transactionDao = database.transactionDao()
    private val webhookDao = database.webhookDao()
    private val notificationService = PaymentNotificationService(context)
    private val analyticsService = AnalyticsService(context)
    
    suspend fun handleWebhook(
        payload: String,
        signature: String
    ) = withContext(Dispatchers.IO) {
        try {
            if (!verifySignature(payload, signature)) {
                throw Exception("Invalid webhook signature")
            }
            
            val event = Gson().fromJson(payload, WebhookEvent::class.java)
            
            // Store webhook event
            webhookDao.insertWebhookEvent(
                WebhookEvent(
                    eventId = event.id,
                    eventType = event.type,
                    payload = payload,
                    timestamp = System.currentTimeMillis()
                )
            )
            
            when (event.type) {
                "payment.succeeded" -> handlePaymentSuccess(event)
                "payment.failed" -> handlePaymentFailure(event)
                "payment.refunded" -> handlePaymentRefund(event)
                "payment.disputed" -> handlePaymentDispute(event)
                "subscription.created" -> handleSubscriptionCreated(event)
                "subscription.activated" -> handleSubscriptionActivated(event)
                "subscription.cancelled" -> handleSubscriptionCancelled(event)
                "subscription.expired" -> handleSubscriptionExpired(event)
            }
            
            // Track webhook event
            analyticsService.logEvent(
                "webhook_received",
                mapOf(
                    "event_type" to event.type,
                    "event_id" to event.id
                )
            )
            
            WebhookResult.Success
        } catch (e: Exception) {
            WebhookResult.Error(e.message ?: "Unknown error occurred")
        }
    }
    
    private suspend fun handlePaymentSuccess(event: WebhookEvent) {
        val transaction = transactionDao.getTransactionByReference(event.data.reference)
        
        transactionDao.updateTransactionStatus(
            transaction.id,
            TransactionStatus.COMPLETED
        )
        
        notificationService.sendPaymentSuccessNotification(
            transaction.userId,
            transaction.amount
        )
    }
    
    private suspend fun handlePaymentFailure(event: WebhookEvent) {
        val transaction = transactionDao.getTransactionByReference(event.data.reference)
        
        transactionDao.updateTransactionStatus(
            transaction.id,
            TransactionStatus.FAILED
        )
        
        notificationService.sendPaymentFailureNotification(
            transaction.userId,
            transaction.amount,
            event.data.failureReason
        )
    }
    
    private suspend fun handlePaymentRefund(event: WebhookEvent) {
        val transaction = transactionDao.getTransactionByReference(event.data.reference)
        
        transactionDao.updateTransactionStatus(
            transaction.id,
            TransactionStatus.REFUNDED
        )
        
        notificationService.sendRefundNotification(
            transaction.userId,
            transaction.amount
        )
    }
    
    private suspend fun handlePaymentDispute(event: WebhookEvent) {
        val transaction = transactionDao.getTransactionByReference(event.data.reference)
        
        transactionDao.updateTransactionStatus(
            transaction.id,
            TransactionStatus.DISPUTED
        )
        
        notificationService.sendDisputeNotification(
            transaction.userId,
            transaction.amount,
            event.data.disputeReason
        )
    }
    
    private suspend fun handleSubscriptionCreated(event: WebhookEvent) {
        // Handle subscription creation
    }
    
    private suspend fun handleSubscriptionActivated(event: WebhookEvent) {
        // Handle subscription activation
    }
    
    private suspend fun handleSubscriptionCancelled(event: WebhookEvent) {
        // Handle subscription cancellation
    }
    
    private suspend fun handleSubscriptionExpired(event: WebhookEvent) {
        // Handle subscription expiration
    }
    
    private fun verifySignature(payload: String, signature: String): Boolean {
        val hmac = Mac.getInstance("HmacSHA256")
        val secretKey = SecretKeySpec(webhookSecret.toByteArray(), "HmacSHA256")
        hmac.init(secretKey)
        
        val calculatedSignature = hmac.doFinal(payload.toByteArray())
            .joinToString("") { "%02x".format(it) }
        
        return calculatedSignature == signature
    }
}

sealed class WebhookResult {
    object Success : WebhookResult()
    data class Error(val message: String) : WebhookResult()
}
