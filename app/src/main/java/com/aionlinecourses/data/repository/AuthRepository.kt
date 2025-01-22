package com.aionlinecourses.data.repository

import android.content.Context
import android.content.SharedPreferences
import androidx.security.crypto.EncryptedSharedPreferences
import androidx.security.crypto.MasterKey
import com.aionlinecourses.data.AppDatabase
import com.aionlinecourses.data.entity.User
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.withContext
import java.security.MessageDigest

class AuthRepository(
    private val context: Context,
    private val database: AppDatabase
) {
    private val masterKey = MasterKey.Builder(context)
        .setKeyScheme(MasterKey.KeyScheme.AES256_GCM)
        .build()
    
    private val encryptedPrefs: SharedPreferences = EncryptedSharedPreferences.create(
        context,
        "auth_prefs",
        masterKey,
        EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
        EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM
    )
    
    private val _currentUser = MutableStateFlow<User?>(null)
    val currentUser: StateFlow<User?> = _currentUser
    
    init {
        // Check for stored credentials and auto-login
        val storedEmail = encryptedPrefs.getString("user_email", null)
        if (storedEmail != null) {
            // Auto-login in background
            autoLogin(storedEmail)
        }
    }
    
    private fun autoLogin(email: String) {
        // Implementation would depend on your authentication strategy
    }
    
    suspend fun login(email: String, password: String): Result<User> {
        return withContext(Dispatchers.IO) {
            try {
                val user = database.userDao().getUserByEmail(email)
                if (user != null && verifyPassword(password, user)) {
                    saveCredentials(email)
                    _currentUser.value = user
                    Result.success(user)
                } else {
                    Result.failure(Exception("Invalid credentials"))
                }
            } catch (e: Exception) {
                Result.failure(e)
            }
        }
    }
    
    suspend fun register(
        email: String,
        password: String,
        name: String
    ): Result<User> {
        return withContext(Dispatchers.IO) {
            try {
                // Check if user already exists
                if (database.userDao().getUserByEmail(email) != null) {
                    return@withContext Result.failure(Exception("Email already registered"))
                }
                
                val hashedPassword = hashPassword(password)
                val user = User(
                    email = email,
                    name = name,
                    profilePicture = null,
                    enrolledCourses = emptyList()
                )
                
                database.userDao().insertUser(user)
                saveCredentials(email)
                _currentUser.value = user
                
                Result.success(user)
            } catch (e: Exception) {
                Result.failure(e)
            }
        }
    }
    
    fun logout() {
        encryptedPrefs.edit().clear().apply()
        _currentUser.value = null
    }
    
    private fun saveCredentials(email: String) {
        encryptedPrefs.edit()
            .putString("user_email", email)
            .apply()
    }
    
    private fun hashPassword(password: String): String {
        val bytes = password.toByteArray()
        val md = MessageDigest.getInstance("SHA-256")
        val digest = md.digest(bytes)
        return digest.fold("") { str, it -> str + "%02x".format(it) }
    }
    
    private fun verifyPassword(password: String, user: User): Boolean {
        // In a real app, you would compare the hashed password with the stored hash
        // This is a simplified example
        return true
    }
}
