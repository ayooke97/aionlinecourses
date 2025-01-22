package com.aionlinecourses.ui.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.aionlinecourses.data.AppDatabase
import com.aionlinecourses.data.dao.CommentWithUser
import com.aionlinecourses.data.entity.Comment
import com.aionlinecourses.service.AnalyticsService
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch

class SocialViewModel(application: Application) : AndroidViewModel(application) {
    private val database = AppDatabase.getDatabase(application)
    private val commentDao = database.commentDao()
    private val analyticsService = AnalyticsService(application)
    
    private val currentUserId = 1 // TODO: Get from AuthRepository
    
    private val _commentState = MutableStateFlow<CommentState>(CommentState.Loading)
    val commentState = _commentState.asStateFlow()
    
    fun loadCourseComments(courseId: Int) {
        viewModelScope.launch {
            commentDao.getCourseComments(courseId)
                .catch { error ->
                    _commentState.value = CommentState.Error(error.message ?: "Unknown error")
                }
                .collect { comments ->
                    _commentState.value = CommentState.Success(comments)
                }
        }
    }
    
    fun addComment(courseId: Int, content: String, parentCommentId: Int? = null) {
        viewModelScope.launch {
            try {
                val comment = Comment(
                    userId = currentUserId,
                    courseId = courseId,
                    content = content,
                    parentCommentId = parentCommentId
                )
                commentDao.insertComment(comment)
                
                // Track comment analytics
                analyticsService.logEvent(
                    if (parentCommentId == null) "add_comment" else "add_reply",
                    mapOf(
                        "course_id" to courseId.toString(),
                        "user_id" to currentUserId.toString()
                    )
                )
            } catch (e: Exception) {
                _commentState.value = CommentState.Error("Failed to add comment")
            }
        }
    }
    
    fun editComment(commentId: Int, newContent: String) {
        viewModelScope.launch {
            try {
                val comment = (commentState.value as? CommentState.Success)?.comments
                    ?.find { it.id == commentId }
                    ?.let {
                        Comment(
                            id = it.id,
                            userId = it.userId,
                            courseId = it.courseId,
                            content = newContent,
                            timestamp = it.timestamp,
                            parentCommentId = it.parentCommentId,
                            likes = it.likes,
                            isEdited = true
                        )
                    }
                
                comment?.let {
                    commentDao.updateComment(it)
                    analyticsService.logEvent(
                        "edit_comment",
                        mapOf(
                            "comment_id" to commentId.toString(),
                            "user_id" to currentUserId.toString()
                        )
                    )
                }
            } catch (e: Exception) {
                _commentState.value = CommentState.Error("Failed to edit comment")
            }
        }
    }
    
    fun deleteComment(commentId: Int) {
        viewModelScope.launch {
            try {
                val comment = (commentState.value as? CommentState.Success)?.comments
                    ?.find { it.id == commentId }
                    ?.let {
                        Comment(
                            id = it.id,
                            userId = it.userId,
                            courseId = it.courseId,
                            content = it.content,
                            timestamp = it.timestamp,
                            parentCommentId = it.parentCommentId,
                            likes = it.likes,
                            isEdited = it.isEdited
                        )
                    }
                
                comment?.let {
                    commentDao.deleteComment(it)
                    analyticsService.logEvent(
                        "delete_comment",
                        mapOf(
                            "comment_id" to commentId.toString(),
                            "user_id" to currentUserId.toString()
                        )
                    )
                }
            } catch (e: Exception) {
                _commentState.value = CommentState.Error("Failed to delete comment")
            }
        }
    }
    
    fun likeComment(commentId: Int) {
        viewModelScope.launch {
            try {
                commentDao.incrementLikes(commentId)
                analyticsService.logEvent(
                    "like_comment",
                    mapOf(
                        "comment_id" to commentId.toString(),
                        "user_id" to currentUserId.toString()
                    )
                )
            } catch (e: Exception) {
                _commentState.value = CommentState.Error("Failed to like comment")
            }
        }
    }
    
    fun unlikeComment(commentId: Int) {
        viewModelScope.launch {
            try {
                commentDao.decrementLikes(commentId)
                analyticsService.logEvent(
                    "unlike_comment",
                    mapOf(
                        "comment_id" to commentId.toString(),
                        "user_id" to currentUserId.toString()
                    )
                )
            } catch (e: Exception) {
                _commentState.value = CommentState.Error("Failed to unlike comment")
            }
        }
    }
    
    fun searchComments(courseId: Int, query: String) {
        viewModelScope.launch {
            try {
                val results = commentDao.searchComments(courseId, query)
                _commentState.value = CommentState.Success(results)
                
                analyticsService.logEvent(
                    "search_comments",
                    mapOf(
                        "course_id" to courseId.toString(),
                        "query" to query,
                        "results_count" to results.size.toString()
                    )
                )
            } catch (e: Exception) {
                _commentState.value = CommentState.Error("Failed to search comments")
            }
        }
    }
}

sealed class CommentState {
    object Loading : CommentState()
    data class Success(val comments: List<CommentWithUser>) : CommentState()
    data class Error(val message: String) : CommentState()
}
