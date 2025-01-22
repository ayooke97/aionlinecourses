package com.aionlinecourses.service

import android.content.Context
import com.aionlinecourses.data.AppDatabase
import com.aionlinecourses.data.entity.*
import com.stripe.android.PaymentConfiguration
import com.stripe.android.Stripe
import com.stripe.android.model.ConfirmPaymentIntentParams
import com.stripe.android.model.PaymentMethodCreateParams
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.util.*

class PaymentService(
    private val context: Context,
    private val stripePublishableKey: String
) {
    private val database = AppDatabase.getDatabase(context)
    private val transactionDao = database.transactionDao()
    private val paymentMethodDao = database.paymentMethodDao()
    private val analyticsService = AnalyticsService(context)
    
    init {
        PaymentConfiguration.init(context, stripePublishableKey)
    }
    
    suspend fun processPayment(
        userId: Int,
        courseId: Int,
        amount: Float,
        paymentMethodId: Int?,
        currency: String = "USD"
    ): PaymentResult = withContext(Dispatchers.IO) {
        try {
            // Create transaction record
            val transactionReference = generateTransactionReference()
            val transaction = Transaction(
                userId = userId,
                courseId = courseId,
                amount = amount,
                currency = currency,
                status = TransactionStatus.PENDING,
                paymentMethodId = paymentMethodId,
                transactionReference = transactionReference
            )
            
            val transactionId = transactionDao.insertTransaction(transaction)
            
            // Get payment method
            val paymentMethod = paymentMethodId?.let {
                paymentMethodDao.getPaymentMethodById(it, userId)
            }
            
            // Process payment with Stripe
            val stripe = Stripe(context, stripePublishableKey)
            val paymentIntent = createPaymentIntent(
                amount = amount,
                currency = currency,
                paymentMethod = paymentMethod
            )
            
            // Confirm payment
            val confirmParams = ConfirmPaymentIntentParams.createWithPaymentMethodId(
                paymentMethodId = paymentMethod?.encryptedData ?: "",
                clientSecret = paymentIntent.clientSecret
            )
            
            val result = stripe.confirmPayment(confirmParams)
            
            if (result.isSuccessful) {
                // Update transaction status
                transactionDao.updateTransactionStatus(
                    transactionId.toInt(),
                    TransactionStatus.COMPLETED
                )
                
                // Track analytics
                analyticsService.logEvent(
                    "payment_success",
                    mapOf(
                        "transaction_id" to transactionId.toString(),
                        "amount" to amount.toString(),
                        "currency" to currency,
                        "course_id" to courseId.toString()
                    )
                )
                
                PaymentResult.Success(transactionId.toInt())
            } else {
                // Update transaction status
                transactionDao.updateTransactionStatus(
                    transactionId.toInt(),
                    TransactionStatus.FAILED
                )
                
                // Track analytics
                analyticsService.logEvent(
                    "payment_failed",
                    mapOf(
                        "transaction_id" to transactionId.toString(),
                        "error" to (result.error?.message ?: "Unknown error")
                    )
                )
                
                PaymentResult.Error(result.error?.message ?: "Payment failed")
            }
        } catch (e: Exception) {
            PaymentResult.Error(e.message ?: "Unknown error occurred")
        }
    }
    
    suspend fun addPaymentMethod(
        userId: Int,
        type: PaymentType,
        params: PaymentMethodCreateParams
    ): PaymentResult = withContext(Dispatchers.IO) {
        try {
            val stripe = Stripe(context, stripePublishableKey)
            val paymentMethod = stripe.createPaymentMethod(params)
            
            // Store payment method
            val newPaymentMethod = PaymentMethod(
                userId = userId,
                type = type,
                lastFourDigits = paymentMethod.card?.last4,
                expiryMonth = paymentMethod.card?.expiryMonth,
                expiryYear = paymentMethod.card?.expiryYear,
                cardBrand = paymentMethod.card?.brand?.displayName,
                encryptedData = paymentMethod.id ?: ""
            )
            
            val id = paymentMethodDao.insertPaymentMethod(newPaymentMethod)
            
            // Track analytics
            analyticsService.logEvent(
                "payment_method_added",
                mapOf(
                    "type" to type.name,
                    "user_id" to userId.toString()
                )
            )
            
            PaymentResult.Success(id.toInt())
        } catch (e: Exception) {
            PaymentResult.Error(e.message ?: "Failed to add payment method")
        }
    }
    
    suspend fun removePaymentMethod(
        userId: Int,
        paymentMethodId: Int
    ): PaymentResult = withContext(Dispatchers.IO) {
        try {
            val paymentMethod = paymentMethodDao.getPaymentMethodById(paymentMethodId, userId)
            paymentMethod?.let {
                paymentMethodDao.deletePaymentMethod(it)
                
                // Track analytics
                analyticsService.logEvent(
                    "payment_method_removed",
                    mapOf(
                        "type" to it.type.name,
                        "user_id" to userId.toString()
                    )
                )
                
                PaymentResult.Success(paymentMethodId)
            } ?: PaymentResult.Error("Payment method not found")
        } catch (e: Exception) {
            PaymentResult.Error(e.message ?: "Failed to remove payment method")
        }
    }
    
    private fun generateTransactionReference(): String {
        return "TXN-${UUID.randomUUID().toString().take(8)}-${System.currentTimeMillis()}"
    }
    
    private suspend fun createPaymentIntent(
        amount: Float,
        currency: String,
        paymentMethod: PaymentMethod?
    ): PaymentIntent {
        // TODO: Implement actual Stripe API call to create PaymentIntent
        return PaymentIntent("dummy_client_secret")
    }
}

sealed class PaymentResult {
    data class Success(val id: Int) : PaymentResult()
    data class Error(val message: String) : PaymentResult()
}

data class PaymentIntent(
    val clientSecret: String
)
