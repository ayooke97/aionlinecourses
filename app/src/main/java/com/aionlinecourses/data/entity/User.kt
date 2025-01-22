package com.aionlinecourses.data.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "users")
data class User(
    @PrimaryKey(autoGenerate = true)
    val id: Int = 0,
    val email: String,
    val name: String,
    val profilePicture: String?,
    val enrolledCourses: List<Int> // List of course IDs
)
