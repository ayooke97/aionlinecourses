package com.aionlinecourses.data.dao

import androidx.room.*
import com.aionlinecourses.data.entity.WebhookEvent
import kotlinx.coroutines.flow.Flow

@Dao
interface WebhookDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertWebhookEvent(event: WebhookEvent)
    
    @Query("SELECT * FROM webhook_events WHERE eventId = :eventId")
    suspend fun getWebhookEvent(eventId: String): WebhookEvent?
    
    @Query("SELECT * FROM webhook_events ORDER BY timestamp DESC")
    fun getAllWebhookEvents(): Flow<List<WebhookEvent>>
    
    @Query("SELECT * FROM webhook_events WHERE processed = :processed ORDER BY timestamp DESC")
    suspend fun getWebhookEventsByStatus(processed: Boolean): List<WebhookEvent>
    
    @Query("SELECT * FROM webhook_events WHERE timestamp BETWEEN :startDate AND :endDate ORDER BY timestamp DESC")
    suspend fun getWebhookEventsByDateRange(startDate: Long, endDate: Long): List<WebhookEvent>
    
    @Query("UPDATE webhook_events SET processed = :processed, error = :error WHERE eventId = :eventId")
    suspend fun updateWebhookEventStatus(eventId: String, processed: Boolean, error: String? = null)
    
    @Query("DELETE FROM webhook_events WHERE timestamp < :timestamp")
    suspend fun deleteOldEvents(timestamp: Long)
    
    @Query("""
        SELECT COUNT(*) FROM webhook_events 
        WHERE eventType = :eventType 
        AND timestamp BETWEEN :startDate AND :endDate
    """)
    suspend fun getEventCountByType(
        eventType: String,
        startDate: Long,
        endDate: Long
    ): Int
}
