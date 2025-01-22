package com.aionlinecourses.ui.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.aionlinecourses.service.PaymentResult
import com.aionlinecourses.service.XenditPaymentService
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class XenditPaymentViewModel(application: Application) : AndroidViewModel(application) {
    private val xenditPaymentService = XenditPaymentService(
        context = application,
        publishableKey = "xnd_public_development_your_key" // TODO: Move to BuildConfig
    )
    
    private val _paymentState = MutableStateFlow<PaymentState>(PaymentState.Initial)
    val paymentState = _paymentState.asStateFlow()
    
    fun processPayment(
        courseId: Int,
        amount: Float,
        cardNumber: String,
        expiryMonth: String,
        expiryYear: String,
        cvv: String
    ) {
        viewModelScope.launch {
            _paymentState.value = PaymentState.Processing
            
            try {
                val result = xenditPaymentService.processPayment(
                    userId = 1, // TODO: Get from AuthRepository
                    courseId = courseId,
                    amount = amount,
                    cardNumber = cardNumber,
                    expiryMonth = expiryMonth,
                    expiryYear = expiryYear,
                    cvv = cvv
                )
                
                _paymentState.value = when (result) {
                    is PaymentResult.Success -> PaymentState.Success(result.id)
                    is PaymentResult.Error -> PaymentState.Error(result.message)
                }
            } catch (e: Exception) {
                _paymentState.value = PaymentState.Error(
                    e.message ?: "Failed to process payment"
                )
            }
        }
    }
    
    fun clearState() {
        _paymentState.value = PaymentState.Initial
    }
}

sealed class PaymentState {
    object Initial : PaymentState()
    object Processing : PaymentState()
    data class Success(val transactionId: Int) : PaymentState()
    data class Error(val message: String) : PaymentState()
}
