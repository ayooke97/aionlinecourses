package com.aionlinecourses.data.dao

import androidx.room.*
import com.aionlinecourses.data.entity.Course
import kotlinx.coroutines.flow.Flow

@Dao
interface CourseDao {
    @Query("SELECT * FROM courses")
    fun getAllCourses(): Flow<List<Course>>

    @Query("SELECT * FROM courses WHERE id = :id")
    suspend fun getCourseById(id: Int): Course?

    @Query("SELECT * FROM courses WHERE id IN (:ids)")
    fun getCoursesByIds(ids: List<Int>): Flow<List<Course>>

    @Query("""
        SELECT * FROM courses 
        WHERE title LIKE :query 
        AND category LIKE :category
        AND difficulty LIKE :difficulty
        AND duration BETWEEN :minDuration AND :maxDuration
        AND price BETWEEN :minPrice AND :maxPrice
        AND rating >= :minRating
        ORDER BY 
        CASE 
            WHEN :query = '%' THEN rating 
            ELSE (
                CASE 
                    WHEN title LIKE :query THEN 3
                    WHEN title LIKE '%' || :query || '%' THEN 2
                    ELSE 1
                END
            )
        END DESC,
        rating DESC
    """)
    suspend fun searchCourses(
        query: String,
        category: String,
        difficulty: String,
        minDuration: Int,
        maxDuration: Int,
        minPrice: Float,
        maxPrice: Float,
        minRating: Float
    ): List<Course>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertCourse(course: Course)

    @Update
    suspend fun updateCourse(course: Course)

    @Delete
    suspend fun deleteCourse(course: Course)
    
    @Query("""
        SELECT DISTINCT category 
        FROM courses 
        WHERE category IS NOT NULL 
        ORDER BY category
    """)
    fun getAllCategories(): Flow<List<String>>
    
    @Query("""
        SELECT DISTINCT difficulty 
        FROM courses 
        WHERE difficulty IS NOT NULL 
        ORDER BY difficulty
    """)
    fun getAllDifficulties(): Flow<List<String>>
    
    @Query("""
        SELECT * FROM courses 
        WHERE rating >= :minRating 
        ORDER BY rating DESC 
        LIMIT :limit
    """)
    fun getTopRatedCourses(minRating: Float = 4.0f, limit: Int = 10): Flow<List<Course>>
    
    @Query("""
        SELECT * FROM courses 
        ORDER BY enrollmentCount DESC 
        LIMIT :limit
    """)
    fun getPopularCourses(limit: Int = 10): Flow<List<Course>>
    
    @Query("""
        SELECT * FROM courses 
        WHERE releaseDate >= :startDate
        ORDER BY releaseDate DESC 
        LIMIT :limit
    """)
    fun getNewCourses(startDate: Long, limit: Int = 10): Flow<List<Course>>
}
