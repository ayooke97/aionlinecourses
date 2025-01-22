package com.aionlinecourses.service

import android.content.Context
import com.xendit.Models.*
import com.xendit.Xendit
import com.xendit.callback.TokenCallback
import com.aionlinecourses.data.AppDatabase
import com.aionlinecourses.data.entity.Transaction
import com.aionlinecourses.data.entity.TransactionStatus
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.withContext
import java.util.*
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException

class XenditPaymentService(
    private val context: Context,
    private val publishableKey: String
) {
    private val database = AppDatabase.getDatabase(context)
    private val transactionDao = database.transactionDao()
    private val analyticsService = AnalyticsService(context)
    
    init {
        Xendit.init(publishableKey)
    }
    
    suspend fun processPayment(
        userId: Int,
        courseId: Int,
        amount: Float,
        cardNumber: String,
        expiryMonth: String,
        expiryYear: String,
        cvv: String
    ): PaymentResult = withContext(Dispatchers.IO) {
        try {
            // Create transaction record
            val transactionReference = generateTransactionReference()
            val transaction = Transaction(
                userId = userId,
                courseId = courseId,
                amount = amount,
                status = TransactionStatus.PENDING,
                transactionReference = transactionReference
            )
            
            val transactionId = transactionDao.insertTransaction(transaction)
            
            // Create Xendit token
            val tokenResult = createToken(
                cardNumber = cardNumber,
                expiryMonth = expiryMonth,
                expiryYear = expiryYear,
                cvv = cvv
            )
            
            // Create charge
            val chargeResult = createCharge(
                tokenId = tokenResult.id,
                amount = amount,
                transactionReference = transactionReference
            )
            
            // Update transaction status
            when (chargeResult.status) {
                "CAPTURED" -> {
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
                            "payment_method" to "xendit_card"
                        )
                    )
                    
                    PaymentResult.Success(transactionId.toInt())
                }
                else -> {
                    transactionDao.updateTransactionStatus(
                        transactionId.toInt(),
                        TransactionStatus.FAILED
                    )
                    
                    // Track analytics
                    analyticsService.logEvent(
                        "payment_failed",
                        mapOf(
                            "transaction_id" to transactionId.toString(),
                            "error" to chargeResult.status
                        )
                    )
                    
                    PaymentResult.Error("Payment failed: ${chargeResult.status}")
                }
            }
        } catch (e: Exception) {
            PaymentResult.Error(e.message ?: "Unknown error occurred")
        }
    }
    
    private suspend fun createToken(
        cardNumber: String,
        expiryMonth: String,
        expiryYear: String,
        cvv: String
    ): Token = suspendCancellableCoroutine { continuation ->
        val card = Card(
            creditCardNumber = cardNumber,
            creditCardCVN = cvv,
            expirationMonth = expiryMonth,
            expirationYear = expiryYear
        )
        
        Xendit.createToken(card, object : TokenCallback {
            override fun onSuccess(token: Token) {
                continuation.resume(token)
            }
            
            override fun onError(xenditError: XenditError) {
                continuation.resumeWithException(
                    Exception("Failed to create token: ${xenditError.errorMessage}")
                )
            }
        })
    }
    
    private suspend fun createCharge(
        tokenId: String,
        amount: Float,
        transactionReference: String
    ): XenditCharge {
        // TODO: Implement server-side charge creation
        // This should be done on your backend server for security
        return XenditCharge(
            id = UUID.randomUUID().toString(),
            status = "CAPTURED",
            amount = amount,
            currency = "IDR",
            chargeType = "CREDIT_CARD",
            referenceId = transactionReference
        )
    }
    
    private fun generateTransactionReference(): String {
        return "XND-${UUID.randomUUID().toString().take(8)}-${System.currentTimeMillis()}"
    }
}

data class XenditCharge(
    val id: String,
    val status: String,
    val amount: Float,
    val currency: String,
    val chargeType: String,
    val referenceId: String
)
