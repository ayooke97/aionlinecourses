package com.aionlinecourses.workers

import android.content.Context
import android.util.Log
import androidx.work.*
import com.aionlinecourses.data.AppDatabase
import com.aionlinecourses.data.entity.DownloadStatus
import com.aionlinecourses.service.NotificationService
import java.io.File
import java.io.FileOutputStream
import java.net.URL
import java.util.concurrent.TimeUnit
import okhttp3.OkHttpClient
import okhttp3.Request
import okio.buffer
import okio.sink
import kotlin.math.roundToInt

class CourseDownloadWorker(
    private val context: Context,
    workerParams: WorkerParameters
) : CoroutineWorker(context, workerParams) {

    private val notificationService = NotificationService(context)

    companion object {
        private const val TAG = "CourseDownloadWorker"
        private const val WORK_NAME = "course_download_work"

        fun enqueue(
            context: Context,
            courseId: Int,
            videoUrl: String,
            courseTitle: String
        ) {
            val workManager = WorkManager.getInstance(context)
            
            val constraints = Constraints.Builder()
                .setRequiredNetworkType(NetworkType.UNMETERED)
                .setRequiresStorageNotLow(true)
                .setRequiresBatteryNotLow(true)
                .build()
            
            val inputData = workDataOf(
                "course_id" to courseId,
                "video_url" to videoUrl,
                "course_title" to courseTitle
            )
            
            val downloadRequest = OneTimeWorkRequestBuilder<CourseDownloadWorker>()
                .setConstraints(constraints)
                .setInputData(inputData)
                .setBackoffCriteria(
                    BackoffPolicy.LINEAR,
                    OneTimeWorkRequest.MIN_BACKOFF_MILLIS,
                    TimeUnit.MILLISECONDS
                )
                .build()
            
            workManager.enqueueUniqueWork(
                "${WORK_NAME}_$courseId",
                ExistingWorkPolicy.REPLACE,
                downloadRequest
            )
        }
    }

    override suspend fun doWork(): Result {
        val courseId = inputData.getInt("course_id", -1)
        val videoUrl = inputData.getString("video_url")
        val courseTitle = inputData.getString("course_title") ?: "Course $courseId"

        if (courseId == -1 || videoUrl == null) {
            return Result.failure()
        }

        return try {
            // Update progress status to downloading
            updateDownloadStatus(courseId, DownloadStatus.DOWNLOADING)
            notificationService.showDownloadProgressNotification(courseId, courseTitle, 0)

            // Create directory if it doesn't exist
            val courseDir = File(context.getExternalFilesDir(null), "courses/$courseId")
            courseDir.mkdirs()

            // Download the video file with progress tracking
            val videoFile = File(courseDir, "video.mp4")
            downloadFileWithProgress(videoUrl, videoFile) { progress ->
                setProgress(workDataOf("progress" to progress))
                notificationService.showDownloadProgressNotification(courseId, courseTitle, progress)
            }

            // Update progress status to downloaded
            updateDownloadStatus(courseId, DownloadStatus.DOWNLOADED)
            notificationService.showDownloadCompleteNotification(courseId, courseTitle)

            Result.success()
        } catch (e: Exception) {
            Log.e(TAG, "Error downloading course: ${e.message}")
            updateDownloadStatus(courseId, DownloadStatus.ERROR)
            notificationService.cancelDownloadNotification(courseId)
            Result.retry()
        }
    }

    private fun downloadFileWithProgress(url: String, outputFile: File, onProgress: (Int) -> Unit) {
        val client = OkHttpClient()
        val request = Request.Builder().url(url).build()
        
        client.newCall(request).execute().use { response ->
            val body = response.body
            val contentLength = body?.contentLength() ?: -1
            var bytesRead = 0L
            
            body?.source()?.use { source ->
                outputFile.sink().buffer().use { sink ->
                    val buffer = ByteArray(8192)
                    var bytes = source.read(buffer)
                    while (bytes >= 0) {
                        sink.write(buffer, 0, bytes)
                        bytesRead += bytes
                        if (contentLength > 0) {
                            val progress = ((bytesRead.toFloat() / contentLength) * 100).roundToInt()
                            onProgress(progress)
                        }
                        bytes = source.read(buffer)
                    }
                }
            }
        }
    }

    private suspend fun updateDownloadStatus(courseId: Int, status: DownloadStatus) {
        val database = AppDatabase.getDatabase(context)
        val progress = database.courseProgressDao().getProgress(
            userId = 1, // TODO: Get actual user ID
            courseId = courseId
        )
        
        progress?.let {
            database.courseProgressDao().updateProgress(
                it.copy(downloadStatus = status)
            )
        }
    }
}
