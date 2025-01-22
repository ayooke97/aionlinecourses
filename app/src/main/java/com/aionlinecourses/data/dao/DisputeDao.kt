package com.aionlinecourses.data.dao

import androidx.room.*
import com.aionlinecourses.data.entity.Dispute
import com.aionlinecourses.data.entity.DisputeStatus
import kotlinx.coroutines.flow.Flow

@Dao
interface DisputeDao {
    @Insert
    suspend fun insertDispute(dispute: Dispute): Long
    
    @Query("SELECT * FROM disputes WHERE id = :disputeId")
    suspend fun getDispute(disputeId: Int): Dispute
    
    @Query("SELECT * FROM disputes WHERE id = :disputeId")
    fun getDisputeFlow(disputeId: Int): Flow<Dispute>
    
    @Query("SELECT * FROM disputes WHERE userId = :userId ORDER BY createdAt DESC")
    fun getDisputesByUser(userId: Int): Flow<List<Dispute>>
    
    @Query("SELECT * FROM disputes WHERE createdAt BETWEEN :startDate AND :endDate ORDER BY createdAt DESC")
    suspend fun getDisputesByDateRange(startDate: Long, endDate: Long): List<Dispute>
    
    @Query("""
        UPDATE disputes 
        SET status = :status, 
            resolution = :resolution,
            resolvedAt = :resolvedAt 
        WHERE id = :disputeId
    """)
    suspend fun updateDisputeStatus(
        disputeId: Int,
        status: DisputeStatus,
        resolution: String?,
        resolvedAt: Long?
    )
    
    @Query("UPDATE disputes SET evidence = :evidence WHERE id = :disputeId")
    suspend fun updateDisputeEvidence(disputeId: Int, evidence: String)
    
    @Query("SELECT COUNT(*) FROM disputes WHERE status = :status")
    suspend fun getDisputeCountByStatus(status: DisputeStatus): Int
    
    @Query("""
        SELECT COUNT(*) FROM disputes 
        WHERE status IN (:statuses) 
        AND createdAt BETWEEN :startDate AND :endDate
    """)
    suspend fun getDisputeCountByStatusAndDateRange(
        statuses: List<DisputeStatus>,
        startDate: Long,
        endDate: Long
    ): Int
    
    @Query("""
        SELECT AVG(CAST((resolvedAt - createdAt) AS FLOAT)) 
        FROM disputes 
        WHERE status IN (:resolvedStatuses) 
        AND resolvedAt IS NOT NULL
    """)
    suspend fun getAverageResolutionTime(
        resolvedStatuses: List<DisputeStatus> = listOf(
            DisputeStatus.RESOLVED_MERCHANT_WIN,
            DisputeStatus.RESOLVED_CUSTOMER_WIN
        )
    ): Float?
    
    @Transaction
    @Query("""
        SELECT d.*, t.amount, t.currency, u.name as userName
        FROM disputes d
        INNER JOIN transactions t ON d.transactionId = t.id
        INNER JOIN users u ON d.userId = u.id
        WHERE d.id = :disputeId
    """)
    suspend fun getDisputeWithDetails(disputeId: Int): DisputeWithDetails
}
