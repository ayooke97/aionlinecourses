package com.aionlinecourses.service

import android.content.Context
import com.aionlinecourses.data.AppDatabase
import com.aionlinecourses.data.entity.Dispute
import com.aionlinecourses.data.entity.DisputeStatus
import com.aionlinecourses.data.entity.Transaction
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.withContext
import java.util.*

class XenditDisputeService(
    private val context: Context
) {
    private val database = AppDatabase.getDatabase(context)
    private val disputeDao = database.disputeDao()
    private val transactionDao = database.transactionDao()
    private val notificationService = PaymentNotificationService(context)
    private val analyticsService = AnalyticsService(context)
    
    suspend fun createDispute(
        transactionId: Int,
        reason: String,
        evidence: String
    ): DisputeResult = withContext(Dispatchers.IO) {
        try {
            val transaction = transactionDao.getTransaction(transactionId)
            
            // Create dispute record
            val dispute = Dispute(
                transactionId = transactionId,
                userId = transaction.userId,
                reason = reason,
                evidence = evidence,
                status = DisputeStatus.PENDING,
                createdAt = System.currentTimeMillis()
            )
            
            val disputeId = disputeDao.insertDispute(dispute)
            
            // Track dispute creation
            analyticsService.logEvent(
                "dispute_created",
                mapOf(
                    "dispute_id" to disputeId.toString(),
                    "transaction_id" to transactionId.toString(),
                    "reason" to reason
                )
            )
            
            // Notify user
            notificationService.sendDisputeCreatedNotification(
                transaction.userId,
                transaction.amount,
                reason
            )
            
            DisputeResult.Success(disputeId.toInt())
        } catch (e: Exception) {
            DisputeResult.Error(e.message ?: "Failed to create dispute")
        }
    }
    
    suspend fun updateDisputeStatus(
        disputeId: Int,
        status: DisputeStatus,
        resolution: String? = null
    ): DisputeResult = withContext(Dispatchers.IO) {
        try {
            val dispute = disputeDao.getDispute(disputeId)
            val transaction = transactionDao.getTransaction(dispute.transactionId)
            
            // Update dispute status
            disputeDao.updateDisputeStatus(
                disputeId = disputeId,
                status = status,
                resolution = resolution,
                resolvedAt = if (status.isResolved()) System.currentTimeMillis() else null
            )
            
            // Track dispute update
            analyticsService.logEvent(
                "dispute_updated",
                mapOf(
                    "dispute_id" to disputeId.toString(),
                    "status" to status.name,
                    "resolution" to (resolution ?: "")
                )
            )
            
            // Notify user
            notificationService.sendDisputeUpdatedNotification(
                transaction.userId,
                transaction.amount,
                status,
                resolution
            )
            
            DisputeResult.Success(disputeId)
        } catch (e: Exception) {
            DisputeResult.Error(e.message ?: "Failed to update dispute")
        }
    }
    
    suspend fun submitDisputeEvidence(
        disputeId: Int,
        evidence: String
    ): DisputeResult = withContext(Dispatchers.IO) {
        try {
            val dispute = disputeDao.getDispute(disputeId)
            
            // Update dispute evidence
            disputeDao.updateDisputeEvidence(
                disputeId = disputeId,
                evidence = evidence
            )
            
            // Track evidence submission
            analyticsService.logEvent(
                "dispute_evidence_submitted",
                mapOf(
                    "dispute_id" to disputeId.toString()
                )
            )
            
            DisputeResult.Success(disputeId)
        } catch (e: Exception) {
            DisputeResult.Error(e.message ?: "Failed to submit evidence")
        }
    }
    
    fun getDisputesByUser(userId: Int): Flow<List<Dispute>> {
        return disputeDao.getDisputesByUser(userId)
    }
    
    fun getDisputeDetails(disputeId: Int): Flow<Dispute> {
        return disputeDao.getDisputeFlow(disputeId)
    }
    
    private fun DisputeStatus.isResolved(): Boolean {
        return this == DisputeStatus.RESOLVED_MERCHANT_WIN ||
                this == DisputeStatus.RESOLVED_CUSTOMER_WIN
    }
}

sealed class DisputeResult {
    data class Success(val disputeId: Int) : DisputeResult()
    data class Error(val message: String) : DisputeResult()
}
