package com.aionlinecourses.service

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import com.aionlinecourses.MainActivity
import com.aionlinecourses.R
import com.aionlinecourses.data.entity.Transaction
import com.aionlinecourses.data.entity.TransactionStatus
import java.text.NumberFormat
import java.util.*

class PaymentNotificationService(private val context: Context) {
    private val notificationManager = NotificationManagerCompat.from(context)
    
    init {
        createNotificationChannels()
    }
    
    private fun createNotificationChannels() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channels = listOf(
                NotificationChannel(
                    CHANNEL_PAYMENT_SUCCESS,
                    "Payment Success",
                    NotificationManager.IMPORTANCE_DEFAULT
                ).apply {
                    description = "Notifications for successful payments"
                    enableLights(true)
                },
                NotificationChannel(
                    CHANNEL_PAYMENT_FAILED,
                    "Payment Failed",
                    NotificationManager.IMPORTANCE_HIGH
                ).apply {
                    description = "Notifications for failed payments"
                    enableLights(true)
                    setShowBadge(true)
                },
                NotificationChannel(
                    CHANNEL_PAYMENT_REFUND,
                    "Payment Refund",
                    NotificationManager.IMPORTANCE_DEFAULT
                ).apply {
                    description = "Notifications for payment refunds"
                    enableLights(true)
                }
            )
            
            notificationManager.createNotificationChannels(channels)
        }
    }
    
    fun showPaymentNotification(transaction: Transaction) {
        val intent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
            putExtra("transactionId", transaction.id)
        }
        
        val pendingIntent = PendingIntent.getActivity(
            context,
            0,
            intent,
            PendingIntent.FLAG_IMMUTABLE
        )
        
        val (channelId, title, message, priority) = when (transaction.status) {
            TransactionStatus.COMPLETED -> {
                Quadruple(
                    CHANNEL_PAYMENT_SUCCESS,
                    "Payment Successful",
                    "Your payment of ${formatAmount(transaction.amount)} was successful",
                    NotificationCompat.PRIORITY_DEFAULT
                )
            }
            TransactionStatus.FAILED -> {
                Quadruple(
                    CHANNEL_PAYMENT_FAILED,
                    "Payment Failed",
                    "Your payment of ${formatAmount(transaction.amount)} failed. Please try again",
                    NotificationCompat.PRIORITY_HIGH
                )
            }
            TransactionStatus.REFUNDED -> {
                Quadruple(
                    CHANNEL_PAYMENT_REFUND,
                    "Payment Refunded",
                    "Your payment of ${formatAmount(transaction.amount)} has been refunded",
                    NotificationCompat.PRIORITY_DEFAULT
                )
            }
            else -> return
        }
        
        val notification = NotificationCompat.Builder(context, channelId)
            .setSmallIcon(R.drawable.ic_notification)
            .setContentTitle(title)
            .setContentText(message)
            .setStyle(NotificationCompat.BigTextStyle().bigText(message))
            .setPriority(priority)
            .setAutoCancel(true)
            .setContentIntent(pendingIntent)
            .build()
        
        notificationManager.notify(transaction.id, notification)
    }
    
    fun showPaymentReminderNotification(transaction: Transaction) {
        val intent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
            putExtra("transactionId", transaction.id)
        }
        
        val pendingIntent = PendingIntent.getActivity(
            context,
            0,
            intent,
            PendingIntent.FLAG_IMMUTABLE
        )
        
        val notification = NotificationCompat.Builder(context, CHANNEL_PAYMENT_FAILED)
            .setSmallIcon(R.drawable.ic_notification)
            .setContentTitle("Complete Your Payment")
            .setContentText("Your payment of ${formatAmount(transaction.amount)} is pending")
            .setStyle(
                NotificationCompat.BigTextStyle()
                    .bigText("Your payment of ${formatAmount(transaction.amount)} for course purchase is pending. Complete your payment to access the course.")
            )
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setAutoCancel(true)
            .setContentIntent(pendingIntent)
            .addAction(
                R.drawable.ic_payment,
                "Complete Payment",
                pendingIntent
            )
            .build()
        
        notificationManager.notify(PAYMENT_REMINDER_ID + transaction.id, notification)
    }
    
    private fun formatAmount(amount: Float): String {
        return NumberFormat.getCurrencyInstance(Locale.US).format(amount)
    }
    
    companion object {
        private const val CHANNEL_PAYMENT_SUCCESS = "payment_success"
        private const val CHANNEL_PAYMENT_FAILED = "payment_failed"
        private const val CHANNEL_PAYMENT_REFUND = "payment_refund"
        private const val PAYMENT_REMINDER_ID = 1000
    }
}

data class Quadruple<A, B, C, D>(
    val first: A,
    val second: B,
    val third: C,
    val fourth: D
)
