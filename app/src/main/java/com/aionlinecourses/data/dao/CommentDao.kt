package com.aionlinecourses.data.dao

import androidx.room.*
import com.aionlinecourses.data.entity.Comment
import kotlinx.coroutines.flow.Flow

@Dao
interface CommentDao {
    @Query("""
        SELECT c.*, u.username, u.profilePicture 
        FROM comments c 
        INNER JOIN users u ON c.userId = u.id 
        WHERE c.courseId = :courseId 
        AND c.parentCommentId IS NULL 
        ORDER BY c.timestamp DESC
    """)
    fun getCourseComments(courseId: Int): Flow<List<CommentWithUser>>
    
    @Query("""
        SELECT c.*, u.username, u.profilePicture 
        FROM comments c 
        INNER JOIN users u ON c.userId = u.id 
        WHERE c.parentCommentId = :parentId 
        ORDER BY c.timestamp ASC
    """)
    fun getReplies(parentId: Int): Flow<List<CommentWithUser>>
    
    @Query("""
        SELECT c.*, u.username, u.profilePicture 
        FROM comments c 
        INNER JOIN users u ON c.userId = u.id 
        WHERE c.userId = :userId 
        ORDER BY c.timestamp DESC
    """)
    fun getUserComments(userId: Int): Flow<List<CommentWithUser>>
    
    @Insert
    suspend fun insertComment(comment: Comment): Long
    
    @Update
    suspend fun updateComment(comment: Comment)
    
    @Delete
    suspend fun deleteComment(comment: Comment)
    
    @Query("UPDATE comments SET likes = likes + 1 WHERE id = :commentId")
    suspend fun incrementLikes(commentId: Int)
    
    @Query("UPDATE comments SET likes = likes - 1 WHERE id = :commentId")
    suspend fun decrementLikes(commentId: Int)
    
    @Query("SELECT COUNT(*) FROM comments WHERE courseId = :courseId")
    fun getCommentCount(courseId: Int): Flow<Int>
    
    @Query("""
        SELECT c.*, u.username, u.profilePicture 
        FROM comments c 
        INNER JOIN users u ON c.userId = u.id 
        WHERE c.courseId = :courseId 
        AND (
            LOWER(c.content) LIKE '%' || LOWER(:query) || '%' 
            OR LOWER(u.username) LIKE '%' || LOWER(:query) || '%'
        )
        ORDER BY c.timestamp DESC
    """)
    suspend fun searchComments(courseId: Int, query: String): List<CommentWithUser>
}

data class CommentWithUser(
    val id: Int,
    val userId: Int,
    val courseId: Int,
    val content: String,
    val timestamp: Long,
    val parentCommentId: Int?,
    val likes: Int,
    val isEdited: Boolean,
    val username: String,
    val profilePicture: String?
)
