package com.aionlinecourses.data.dao

import androidx.room.*
import com.aionlinecourses.data.entity.CourseProgress
import kotlinx.coroutines.flow.Flow

@Dao
interface CourseProgressDao {
    @Query("SELECT * FROM course_progress WHERE userId = :userId AND courseId = :courseId")
    suspend fun getProgress(userId: Int, courseId: Int): CourseProgress?
    
    @Query("SELECT * FROM course_progress WHERE userId = :userId AND courseId = :courseId")
    fun getProgressFlow(userId: Int, courseId: Int): Flow<CourseProgress?>
    
    @Query("SELECT * FROM course_progress WHERE userId = :userId")
    fun getAllProgress(userId: Int): Flow<List<CourseProgress>>
    
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertProgress(progress: CourseProgress)
    
    @Update
    suspend fun updateProgress(progress: CourseProgress)
    
    @Delete
    suspend fun deleteProgress(progress: CourseProgress)
    
    @Query("SELECT * FROM course_progress WHERE downloadStatus = 'DOWNLOADING'")
    fun getDownloadingCourses(): Flow<List<CourseProgress>>
    
    @Query("SELECT * FROM course_progress WHERE downloadStatus = 'DOWNLOADED'")
    fun getDownloadedCourses(): Flow<List<CourseProgress>>
    
    @Transaction
    suspend fun updateProgressAndTimestamp(
        userId: Int,
        courseId: Int,
        lastPosition: Long,
        completedLessons: List<Int>,
        progress: Float
    ) {
        val courseProgress = getProgress(userId, courseId)
        courseProgress?.let {
            updateProgress(
                it.copy(
                    lastWatchedPosition = lastPosition,
                    completedLessons = completedLessons,
                    progress = progress,
                    lastSyncTimestamp = System.currentTimeMillis()
                )
            )
        }
    }
}
