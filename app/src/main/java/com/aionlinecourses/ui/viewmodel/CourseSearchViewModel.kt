package com.aionlinecourses.ui.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.aionlinecourses.data.AppDatabase
import com.aionlinecourses.data.entity.Course
import com.aionlinecourses.service.AnalyticsService
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch

data class CourseFilters(
    val category: String? = null,
    val difficulty: String? = null,
    val duration: String? = null,
    val priceRange: ClosedRange<Float>? = null,
    val rating: Float? = null
)

@OptIn(ExperimentalCoroutinesApi::class)
class CourseSearchViewModel(application: Application) : AndroidViewModel(application) {
    private val database = AppDatabase.getDatabase(application)
    private val courseDao = database.courseDao()
    private val analyticsService = AnalyticsService(application)
    
    private val _searchQuery = MutableStateFlow("")
    private val _filters = MutableStateFlow(CourseFilters())
    private val _isSearching = MutableStateFlow(false)
    
    val searchQuery = _searchQuery.asStateFlow()
    val filters = _filters.asStateFlow()
    val isSearching = _isSearching.asStateFlow()
    
    val searchResults = combine(
        _searchQuery,
        _filters
    ) { query, filters ->
        Pair(query, filters)
    }.flatMapLatest { (query, filters) ->
        _isSearching.value = true
        searchCourses(query, filters)
    }.onCompletion {
        _isSearching.value = false
    }
    
    private suspend fun searchCourses(
        query: String,
        filters: CourseFilters
    ): Flow<List<Course>> = flow {
        val results = courseDao.searchCourses(
            query = if (query.isBlank()) "%" else "%$query%",
            category = filters.category ?: "%",
            difficulty = filters.difficulty ?: "%",
            minDuration = if (filters.duration != null) 0 else Int.MIN_VALUE,
            maxDuration = if (filters.duration != null) Int.MAX_VALUE else Int.MAX_VALUE,
            minPrice = filters.priceRange?.start ?: Float.MIN_VALUE,
            maxPrice = filters.priceRange?.endInclusive ?: Float.MAX_VALUE,
            minRating = filters.rating ?: 0f
        )
        
        // Log analytics
        analyticsService.logCourseSearch(query, results.size)
        if (filters != CourseFilters()) {
            analyticsService.logCourseFilter(
                filters.category,
                filters.difficulty,
                filters.duration,
                results.size
            )
        }
        
        emit(results)
    }
    
    fun updateSearchQuery(query: String) {
        _searchQuery.value = query
    }
    
    fun updateFilters(filters: CourseFilters) {
        _filters.value = filters
    }
    
    fun clearFilters() {
        _filters.value = CourseFilters()
    }
    
    fun clearSearch() {
        _searchQuery.value = ""
    }
    
    fun clearAll() {
        clearSearch()
        clearFilters()
    }
}
