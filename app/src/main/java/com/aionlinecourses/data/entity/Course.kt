package com.aionlinecourses.data.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "courses")
data class Course(
    @PrimaryKey(autoGenerate = true)
    val id: Int = 0,
    val title: String,
    val description: String,
    val instructor: String,
    val thumbnailUrl: String,
    val videoUrl: String,
    val duration: Int, // in minutes
    val rating: Float,
    val price: Double,
    val category: String
)
