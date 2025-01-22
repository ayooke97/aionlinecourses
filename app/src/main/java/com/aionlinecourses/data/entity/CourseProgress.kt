package com.aionlinecourses.data.entity

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index

@Entity(
    tableName = "course_progress",
    primaryKeys = ["userId", "courseId"],
    foreignKeys = [
        ForeignKey(
            entity = User::class,
            parentColumns = ["id"],
            childColumns = ["userId"],
            onDelete = ForeignKey.CASCADE
        ),
        ForeignKey(
            entity = Course::class,
            parentColumns = ["id"],
            childColumns = ["courseId"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [
        Index(value = ["userId"]),
        Index(value = ["courseId"])
    ]
)
data class CourseProgress(
    val userId: Int,
    val courseId: Int,
    val lastWatchedPosition: Long = 0, // Video position in milliseconds
    val completedLessons: List<Int> = emptyList(),
    val progress: Float = 0f, // Overall progress (0-1)
    val lastSyncTimestamp: Long = System.currentTimeMillis(),
    val downloadStatus: DownloadStatus = DownloadStatus.NOT_DOWNLOADED
)

enum class DownloadStatus {
    NOT_DOWNLOADED,
    DOWNLOADING,
    DOWNLOADED,
    ERROR
}
