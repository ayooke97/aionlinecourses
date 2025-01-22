package com.aionlinecourses.ui.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.aionlinecourses.data.AppDatabase
import com.aionlinecourses.data.entity.CourseProgress
import com.aionlinecourses.data.entity.DownloadStatus
import com.aionlinecourses.workers.CourseDownloadWorker
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch

class CourseProgressViewModel(application: Application) : AndroidViewModel(application) {
    private val database = AppDatabase.getDatabase(application)
    private val courseProgressDao = database.courseProgressDao()
    
    private val currentUserId = 1 // TODO: Get from AuthRepository
    
    val downloadingCourses = courseProgressDao.getDownloadingCourses()
    val downloadedCourses = courseProgressDao.getDownloadedCourses()
    
    fun getCourseProgress(courseId: Int): Flow<CourseProgress?> {
        return courseProgressDao.getProgressFlow(currentUserId, courseId)
    }
    
    fun getAllProgress(): Flow<List<CourseProgress>> {
        return courseProgressDao.getAllProgress(currentUserId)
    }
    
    fun downloadCourse(courseId: Int, videoUrl: String) {
        viewModelScope.launch {
            // Initialize progress entry if it doesn't exist
            val progress = courseProgressDao.getProgress(currentUserId, courseId)
            if (progress == null) {
                courseProgressDao.insertProgress(
                    CourseProgress(
                        userId = currentUserId,
                        courseId = courseId
                    )
                )
            }
            
            // Start download worker
            CourseDownloadWorker.enqueue(
                context = getApplication(),
                courseId = courseId,
                videoUrl = videoUrl
            )
        }
    }
    
    fun updateProgress(
        courseId: Int,
        lastPosition: Long,
        completedLessons: List<Int>,
        progress: Float
    ) {
        viewModelScope.launch {
            courseProgressDao.updateProgressAndTimestamp(
                userId = currentUserId,
                courseId = courseId,
                lastPosition = lastPosition,
                completedLessons = completedLessons,
                progress = progress
            )
        }
    }
    
    fun cancelDownload(courseId: Int) {
        viewModelScope.launch {
            val progress = courseProgressDao.getProgress(currentUserId, courseId)
            progress?.let {
                if (it.downloadStatus == DownloadStatus.DOWNLOADING) {
                    courseProgressDao.updateProgress(
                        it.copy(downloadStatus = DownloadStatus.NOT_DOWNLOADED)
                    )
                    // Cancel WorkManager job
                    WorkManager.getInstance(getApplication())
                        .cancelUniqueWork("course_download_work_$courseId")
                }
            }
        }
    }
    
    fun deleteDownloadedCourse(courseId: Int) {
        viewModelScope.launch {
            val progress = courseProgressDao.getProgress(currentUserId, courseId)
            progress?.let {
                if (it.downloadStatus == DownloadStatus.DOWNLOADED) {
                    // Delete the downloaded file
                    val courseDir = File(
                        getApplication<Application>().getExternalFilesDir(null),
                        "courses/$courseId"
                    )
                    courseDir.deleteRecursively()
                    
                    // Update database
                    courseProgressDao.updateProgress(
                        it.copy(downloadStatus = DownloadStatus.NOT_DOWNLOADED)
                    )
                }
            }
        }
    }
}
