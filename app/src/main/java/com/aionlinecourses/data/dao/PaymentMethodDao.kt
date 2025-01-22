package com.aionlinecourses.data.dao

import androidx.room.*
import com.aionlinecourses.data.entity.PaymentMethod
import com.aionlinecourses.data.entity.PaymentType
import kotlinx.coroutines.flow.Flow

@Dao
interface PaymentMethodDao {
    @Query("SELECT * FROM payment_methods WHERE userId = :userId ORDER BY isDefault DESC, createdAt DESC")
    fun getUserPaymentMethods(userId: Int): Flow<List<PaymentMethod>>
    
    @Query("SELECT * FROM payment_methods WHERE userId = :userId AND isDefault = 1 LIMIT 1")
    suspend fun getDefaultPaymentMethod(userId: Int): PaymentMethod?
    
    @Query("SELECT * FROM payment_methods WHERE id = :id AND userId = :userId")
    suspend fun getPaymentMethodById(id: Int, userId: Int): PaymentMethod?
    
    @Insert
    suspend fun insertPaymentMethod(paymentMethod: PaymentMethod): Long
    
    @Update
    suspend fun updatePaymentMethod(paymentMethod: PaymentMethod)
    
    @Delete
    suspend fun deletePaymentMethod(paymentMethod: PaymentMethod)
    
    @Query("SELECT COUNT(*) FROM payment_methods WHERE userId = :userId AND type = :type")
    suspend fun getPaymentMethodCountByType(userId: Int, type: PaymentType): Int
    
    @Transaction
    suspend fun setDefaultPaymentMethod(userId: Int, paymentMethodId: Int) {
        // Remove default status from all payment methods
        clearDefaultPaymentMethods(userId)
        // Set the new default payment method
        setPaymentMethodAsDefault(paymentMethodId)
    }
    
    @Query("UPDATE payment_methods SET isDefault = 0 WHERE userId = :userId")
    suspend fun clearDefaultPaymentMethods(userId: Int)
    
    @Query("UPDATE payment_methods SET isDefault = 1 WHERE id = :paymentMethodId")
    suspend fun setPaymentMethodAsDefault(paymentMethodId: Int)
    
    @Query("""
        SELECT * FROM payment_methods 
        WHERE userId = :userId 
        AND (expiryYear < :currentYear 
        OR (expiryYear = :currentYear AND expiryMonth < :currentMonth))
    """)
    suspend fun getExpiredPaymentMethods(
        userId: Int,
        currentYear: Int,
        currentMonth: Int
    ): List<PaymentMethod>
    
    @Query("""
        DELETE FROM payment_methods 
        WHERE userId = :userId 
        AND type = :type 
        AND lastFourDigits = :lastFourDigits
    """)
    suspend fun removePaymentMethodByCard(
        userId: Int,
        type: PaymentType,
        lastFourDigits: String
    )
}
