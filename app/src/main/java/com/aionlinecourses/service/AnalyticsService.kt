package com.aionlinecourses.service

import android.content.Context
import android.os.Bundle
import com.google.firebase.analytics.FirebaseAnalytics
import com.google.firebase.analytics.ktx.analytics
import com.google.firebase.ktx.Firebase
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class AnalyticsService(context: Context) {
    private val firebaseAnalytics = Firebase.analytics

    companion object {
        // Screen Views
        private const val SCREEN_VIEW = "screen_view"
        private const val SCREEN_NAME = "screen_name"
        
        // Course Events
        private const val COURSE_VIEW = "course_view"
        private const val COURSE_START = "course_start"
        private const val COURSE_COMPLETE = "course_complete"
        private const val COURSE_DOWNLOAD = "course_download"
        private const val COURSE_SEARCH = "course_search"
        private const val COURSE_FILTER = "course_filter"
        
        // User Events
        private const val USER_LOGIN = "user_login"
        private const val USER_REGISTER = "user_register"
        private const val USER_LOGOUT = "user_logout"
        
        // Video Events
        private const val VIDEO_START = "video_start"
        private const val VIDEO_COMPLETE = "video_complete"
        private const val VIDEO_PROGRESS = "video_progress"
    }
    
    suspend fun logScreenView(screenName: String) = withContext(Dispatchers.IO) {
        val bundle = Bundle().apply {
            putString(SCREEN_NAME, screenName)
        }
        firebaseAnalytics.logEvent(SCREEN_VIEW, bundle)
    }
    
    suspend fun logCourseView(courseId: Int, courseTitle: String) = withContext(Dispatchers.IO) {
        val bundle = Bundle().apply {
            putInt(FirebaseAnalytics.Param.ITEM_ID, courseId)
            putString(FirebaseAnalytics.Param.ITEM_NAME, courseTitle)
            putString(FirebaseAnalytics.Param.CONTENT_TYPE, "course")
        }
        firebaseAnalytics.logEvent(COURSE_VIEW, bundle)
    }
    
    suspend fun logCourseStart(courseId: Int, courseTitle: String) = withContext(Dispatchers.IO) {
        val bundle = Bundle().apply {
            putInt(FirebaseAnalytics.Param.ITEM_ID, courseId)
            putString(FirebaseAnalytics.Param.ITEM_NAME, courseTitle)
        }
        firebaseAnalytics.logEvent(COURSE_START, bundle)
    }
    
    suspend fun logCourseComplete(courseId: Int, courseTitle: String) = withContext(Dispatchers.IO) {
        val bundle = Bundle().apply {
            putInt(FirebaseAnalytics.Param.ITEM_ID, courseId)
            putString(FirebaseAnalytics.Param.ITEM_NAME, courseTitle)
        }
        firebaseAnalytics.logEvent(COURSE_COMPLETE, bundle)
    }
    
    suspend fun logCourseDownload(courseId: Int, courseTitle: String) = withContext(Dispatchers.IO) {
        val bundle = Bundle().apply {
            putInt(FirebaseAnalytics.Param.ITEM_ID, courseId)
            putString(FirebaseAnalytics.Param.ITEM_NAME, courseTitle)
        }
        firebaseAnalytics.logEvent(COURSE_DOWNLOAD, bundle)
    }
    
    suspend fun logCourseSearch(query: String, resultCount: Int) = withContext(Dispatchers.IO) {
        val bundle = Bundle().apply {
            putString("search_query", query)
            putInt("result_count", resultCount)
        }
        firebaseAnalytics.logEvent(COURSE_SEARCH, bundle)
    }
    
    suspend fun logCourseFilter(
        category: String?,
        difficulty: String?,
        duration: String?,
        resultCount: Int
    ) = withContext(Dispatchers.IO) {
        val bundle = Bundle().apply {
            category?.let { putString("filter_category", it) }
            difficulty?.let { putString("filter_difficulty", it) }
            duration?.let { putString("filter_duration", it) }
            putInt("result_count", resultCount)
        }
        firebaseAnalytics.logEvent(COURSE_FILTER, bundle)
    }
    
    suspend fun logUserLogin(userId: Int, method: String) = withContext(Dispatchers.IO) {
        val bundle = Bundle().apply {
            putInt("user_id", userId)
            putString("login_method", method)
        }
        firebaseAnalytics.logEvent(USER_LOGIN, bundle)
    }
    
    suspend fun logUserRegister(userId: Int, method: String) = withContext(Dispatchers.IO) {
        val bundle = Bundle().apply {
            putInt("user_id", userId)
            putString("register_method", method)
        }
        firebaseAnalytics.logEvent(USER_REGISTER, bundle)
    }
    
    suspend fun logUserLogout(userId: Int) = withContext(Dispatchers.IO) {
        val bundle = Bundle().apply {
            putInt("user_id", userId)
        }
        firebaseAnalytics.logEvent(USER_LOGOUT, bundle)
    }
    
    suspend fun logVideoProgress(
        courseId: Int,
        videoId: Int,
        progress: Float
    ) = withContext(Dispatchers.IO) {
        val bundle = Bundle().apply {
            putInt("course_id", courseId)
            putInt("video_id", videoId)
            putFloat("progress", progress)
        }
        firebaseAnalytics.logEvent(VIDEO_PROGRESS, bundle)
    }
    
    suspend fun logVideoStart(courseId: Int, videoId: Int) = withContext(Dispatchers.IO) {
        val bundle = Bundle().apply {
            putInt("course_id", courseId)
            putInt("video_id", videoId)
        }
        firebaseAnalytics.logEvent(VIDEO_START, bundle)
    }
    
    suspend fun logVideoComplete(courseId: Int, videoId: Int) = withContext(Dispatchers.IO) {
        val bundle = Bundle().apply {
            putInt("course_id", courseId)
            putInt("video_id", videoId)
        }
        firebaseAnalytics.logEvent(VIDEO_COMPLETE, bundle)
    }
}
