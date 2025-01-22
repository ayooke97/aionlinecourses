package com.aionlinecourses.data.repository

import com.aionlinecourses.data.AppDatabase
import com.aionlinecourses.data.entity.Course
import com.aionlinecourses.data.entity.User
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.withContext

class CourseRepository(private val database: AppDatabase) {
    fun getAllCourses(): Flow<List<Course>> {
        return database.courseDao().getAllCourses()
    }
    
    fun getCoursesByCategory(category: String): Flow<List<Course>> {
        return database.courseDao().getCoursesByCategory(category)
    }
    
    fun searchCourses(query: String): Flow<List<Course>> {
        return database.courseDao().searchCourses("%$query%")
    }
    
    suspend fun getCourseById(id: Int): Course? {
        return withContext(Dispatchers.IO) {
            database.courseDao().getCourseById(id)
        }
    }
    
    suspend fun insertCourse(course: Course) {
        withContext(Dispatchers.IO) {
            database.courseDao().insertCourse(course)
        }
    }
    
    suspend fun updateCourse(course: Course) {
        withContext(Dispatchers.IO) {
            database.courseDao().updateCourse(course)
        }
    }
    
    suspend fun enrollInCourse(userId: Int, courseId: Int): Result<Unit> {
        return withContext(Dispatchers.IO) {
            try {
                val user = database.userDao().getUserById(userId)
                val course = database.courseDao().getCourseById(courseId)
                
                if (user == null || course == null) {
                    return@withContext Result.failure(Exception("User or course not found"))
                }
                
                if (user.enrolledCourses.contains(courseId)) {
                    return@withContext Result.failure(Exception("Already enrolled in this course"))
                }
                
                val updatedUser = user.copy(
                    enrolledCourses = user.enrolledCourses + courseId
                )
                database.userDao().updateUser(updatedUser)
                
                Result.success(Unit)
            } catch (e: Exception) {
                Result.failure(e)
            }
        }
    }
    
    suspend fun unenrollFromCourse(userId: Int, courseId: Int): Result<Unit> {
        return withContext(Dispatchers.IO) {
            try {
                val user = database.userDao().getUserById(userId)
                
                if (user == null) {
                    return@withContext Result.failure(Exception("User not found"))
                }
                
                if (!user.enrolledCourses.contains(courseId)) {
                    return@withContext Result.failure(Exception("Not enrolled in this course"))
                }
                
                val updatedUser = user.copy(
                    enrolledCourses = user.enrolledCourses - courseId
                )
                database.userDao().updateUser(updatedUser)
                
                Result.success(Unit)
            } catch (e: Exception) {
                Result.failure(e)
            }
        }
    }
    
    fun getEnrolledCourses(userId: Int): Flow<List<Course>> {
        return database.userDao().getUserByIdFlow(userId)
            .filterNotNull()
            .map { user -> user.enrolledCourses }
            .flatMapLatest { courseIds ->
                database.courseDao().getCoursesByIds(courseIds)
            }
    }
}
