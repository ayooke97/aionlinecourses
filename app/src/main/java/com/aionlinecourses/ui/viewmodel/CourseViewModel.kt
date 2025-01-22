package com.aionlinecourses.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.aionlinecourses.data.entity.Course
import com.aionlinecourses.data.repository.CourseRepository
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch

class CourseViewModel(
    private val repository: CourseRepository
) : ViewModel() {
    
    private val _searchQuery = MutableStateFlow("")
    val searchQuery = _searchQuery.asStateFlow()
    
    private val _courses = MutableStateFlow<List<Course>>(emptyList())
    val courses = searchQuery
        .debounce(300L)
        .combine(_courses) { query, courses ->
            if (query.isBlank()) {
                courses
            } else {
                courses.filter {
                    it.title.contains(query, ignoreCase = true) ||
                    it.description.contains(query, ignoreCase = true)
                }
            }
        }
        .stateIn(
            viewModelScope,
            SharingStarted.WhileSubscribed(5000),
            emptyList()
        )
    
    private val _selectedCourse = MutableStateFlow<Course?>(null)
    val selectedCourse = _selectedCourse.asStateFlow()
    
    init {
        loadCourses()
    }
    
    private fun loadCourses() {
        viewModelScope.launch {
            repository.getAllCourses()
                .collect { courseList ->
                    _courses.value = courseList
                }
        }
    }
    
    fun searchCourses(query: String) {
        _searchQuery.value = query
    }
    
    fun selectCourse(courseId: Int) {
        viewModelScope.launch {
            repository.getCourseById(courseId)?.let { course ->
                _selectedCourse.value = course
            }
        }
    }
    
    class Factory(private val repository: CourseRepository) : ViewModelProvider.Factory {
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            if (modelClass.isAssignableFrom(CourseViewModel::class.java)) {
                @Suppress("UNCHECKED_CAST")
                return CourseViewModel(repository) as T
            }
            throw IllegalArgumentException("Unknown ViewModel class")
        }
    }
}
