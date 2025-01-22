package com.aionlinecourses.ui.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.aionlinecourses.data.AppDatabase
import com.aionlinecourses.data.dao.TransactionWithDetails
import com.aionlinecourses.data.entity.PaymentMethod
import com.aionlinecourses.data.entity.PaymentType
import com.aionlinecourses.service.PaymentResult
import com.aionlinecourses.service.PaymentService
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch

class PaymentViewModel(application: Application) : AndroidViewModel(application) {
    private val database = AppDatabase.getDatabase(application)
    private val paymentMethodDao = database.paymentMethodDao()
    private val transactionDao = database.transactionDao()
    
    private val paymentService = PaymentService(
        context = application,
        stripePublishableKey = "your_stripe_publishable_key" // TODO: Move to BuildConfig
    )
    
    private val currentUserId = 1 // TODO: Get from AuthRepository
    
    private val _paymentState = MutableStateFlow<PaymentState>(PaymentState.Initial)
    val paymentState = _paymentState.asStateFlow()
    
    val paymentMethods = paymentMethodDao.getUserPaymentMethods(currentUserId)
    val transactions = transactionDao.getUserTransactions(currentUserId)
    
    fun processPayment(
        courseId: Int,
        amount: Float,
        paymentMethodId: Int?
    ) {
        viewModelScope.launch {
            _paymentState.value = PaymentState.Processing
            
            when (val result = paymentService.processPayment(
                userId = currentUserId,
                courseId = courseId,
                amount = amount,
                paymentMethodId = paymentMethodId
            )) {
                is PaymentResult.Success -> {
                    _paymentState.value = PaymentState.Success(result.id)
                }
                is PaymentResult.Error -> {
                    _paymentState.value = PaymentState.Error(result.message)
                }
            }
        }
    }
    
    fun addPaymentMethod(
        type: PaymentType,
        cardNumber: String,
        expiryMonth: Int,
        expiryYear: Int,
        cvc: String
    ) {
        viewModelScope.launch {
            _paymentState.value = PaymentState.Processing
            
            try {
                // Create Stripe payment method params
                val params = createPaymentMethodParams(
                    cardNumber = cardNumber,
                    expiryMonth = expiryMonth,
                    expiryYear = expiryYear,
                    cvc = cvc
                )
                
                when (val result = paymentService.addPaymentMethod(
                    userId = currentUserId,
                    type = type,
                    params = params
                )) {
                    is PaymentResult.Success -> {
                        _paymentState.value = PaymentState.PaymentMethodAdded(result.id)
                    }
                    is PaymentResult.Error -> {
                        _paymentState.value = PaymentState.Error(result.message)
                    }
                }
            } catch (e: Exception) {
                _paymentState.value = PaymentState.Error(e.message ?: "Failed to add payment method")
            }
        }
    }
    
    fun removePaymentMethod(paymentMethodId: Int) {
        viewModelScope.launch {
            when (val result = paymentService.removePaymentMethod(
                userId = currentUserId,
                paymentMethodId = paymentMethodId
            )) {
                is PaymentResult.Success -> {
                    _paymentState.value = PaymentState.PaymentMethodRemoved(paymentMethodId)
                }
                is PaymentResult.Error -> {
                    _paymentState.value = PaymentState.Error(result.message)
                }
            }
        }
    }
    
    fun setDefaultPaymentMethod(paymentMethodId: Int) {
        viewModelScope.launch {
            try {
                paymentMethodDao.setDefaultPaymentMethod(currentUserId, paymentMethodId)
                _paymentState.value = PaymentState.PaymentMethodUpdated(paymentMethodId)
            } catch (e: Exception) {
                _paymentState.value = PaymentState.Error(e.message ?: "Failed to set default payment method")
            }
        }
    }
    
    fun clearState() {
        _paymentState.value = PaymentState.Initial
    }
    
    private fun createPaymentMethodParams(
        cardNumber: String,
        expiryMonth: Int,
        expiryYear: Int,
        cvc: String
    ): com.stripe.android.model.PaymentMethodCreateParams {
        return com.stripe.android.model.PaymentMethodCreateParams.create(
            com.stripe.android.model.PaymentMethodCreateParams.Card.Builder()
                .setNumber(cardNumber)
                .setExpiryMonth(expiryMonth)
                .setExpiryYear(expiryYear)
                .setCvc(cvc)
                .build()
        )
    }
}

sealed class PaymentState {
    object Initial : PaymentState()
    object Processing : PaymentState()
    data class Success(val transactionId: Int) : PaymentState()
    data class Error(val message: String) : PaymentState()
    data class PaymentMethodAdded(val paymentMethodId: Int) : PaymentState()
    data class PaymentMethodRemoved(val paymentMethodId: Int) : PaymentState()
    data class PaymentMethodUpdated(val paymentMethodId: Int) : PaymentState()
}
