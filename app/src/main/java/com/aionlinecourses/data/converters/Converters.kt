package com.aionlinecourses.data.converters

import androidx.room.TypeConverter
import com.aionlinecourses.data.entity.DownloadStatus
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken

class Converters {
    private val gson = Gson()
    
    @TypeConverter
    fun fromIntList(value: List<Int>): String {
        return gson.toJson(value)
    }
    
    @TypeConverter
    fun toIntList(value: String): List<Int> {
        val listType = object : TypeToken<List<Int>>() {}.type
        return gson.fromJson(value, listType)
    }
    
    @TypeConverter
    fun fromDownloadStatus(value: DownloadStatus): String {
        return value.name
    }
    
    @TypeConverter
    fun toDownloadStatus(value: String): DownloadStatus {
        return try {
            DownloadStatus.valueOf(value)
        } catch (e: IllegalArgumentException) {
            DownloadStatus.NOT_DOWNLOADED
        }
    }
}
