package com.aionlinecourses.data.dao

import androidx.room.*
import com.aionlinecourses.data.entity.Transaction
import com.aionlinecourses.data.entity.TransactionStatus
import kotlinx.coroutines.flow.Flow

@Dao
interface TransactionDao {
    @Query("""
        SELECT t.*, c.title as courseTitle, pm.type as paymentType 
        FROM transactions t
        INNER JOIN courses c ON t.courseId = c.id
        LEFT JOIN payment_methods pm ON t.paymentMethodId = pm.id
        WHERE t.userId = :userId
        ORDER BY t.timestamp DESC
    """)
    fun getUserTransactions(userId: Int): Flow<List<TransactionWithDetails>>
    
    @Query("""
        SELECT * FROM transactions 
        WHERE userId = :userId AND courseId = :courseId
        ORDER BY timestamp DESC LIMIT 1
    """)
    suspend fun getLatestTransactionForCourse(userId: Int, courseId: Int): Transaction?
    
    @Query("""
        SELECT COUNT(*) FROM transactions 
        WHERE userId = :userId AND courseId = :courseId AND status = 'COMPLETED'
    """)
    suspend fun hasUserPurchasedCourse(userId: Int, courseId: Int): Int
    
    @Insert
    suspend fun insertTransaction(transaction: Transaction): Long
    
    @Update
    suspend fun updateTransaction(transaction: Transaction)
    
    @Query("UPDATE transactions SET status = :status WHERE id = :transactionId")
    suspend fun updateTransactionStatus(transactionId: Int, status: TransactionStatus)
    
    @Query("""
        SELECT SUM(amount) FROM transactions 
        WHERE userId = :userId AND status = 'COMPLETED'
        AND timestamp BETWEEN :startTime AND :endTime
    """)
    suspend fun getTotalSpentInPeriod(userId: Int, startTime: Long, endTime: Long): Float?
    
    @Query("""
        SELECT * FROM transactions 
        WHERE status = 'PENDING' 
        AND timestamp < :expiryTime
    """)
    suspend fun getExpiredPendingTransactions(expiryTime: Long): List<Transaction>
    
    @Transaction
    suspend fun processRefund(transactionId: Int): Boolean {
        return try {
            // Create refund record
            val originalTransaction = getTransactionById(transactionId)
            originalTransaction?.let {
                val refundTransaction = it.copy(
                    id = 0,
                    status = TransactionStatus.REFUNDED,
                    amount = -it.amount,
                    transactionReference = "REFUND-${it.transactionReference}",
                    timestamp = System.currentTimeMillis()
                )
                insertTransaction(refundTransaction)
                // Update original transaction status
                updateTransactionStatus(transactionId, TransactionStatus.REFUNDED)
            }
            true
        } catch (e: Exception) {
            false
        }
    }
    
    @Query("SELECT * FROM transactions WHERE id = :id")
    suspend fun getTransactionById(id: Int): Transaction?
}

data class TransactionWithDetails(
    val id: Int,
    val userId: Int,
    val courseId: Int,
    val amount: Float,
    val currency: String,
    val status: TransactionStatus,
    val paymentMethodId: Int?,
    val timestamp: Long,
    val transactionReference: String,
    val courseTitle: String,
    val paymentType: String?
)
