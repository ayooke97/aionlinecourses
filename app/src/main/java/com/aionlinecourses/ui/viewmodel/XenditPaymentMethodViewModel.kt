package com.aionlinecourses.ui.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.aionlinecourses.service.*
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class XenditPaymentMethodViewModel(application: Application) : AndroidViewModel(application) {
    private val xenditPaymentMethodService = XenditPaymentMethodService(
        context = application,
        publishableKey = "xnd_public_development_your_key" // TODO: Move to BuildConfig
    )
    
    private val _paymentState = MutableStateFlow<PaymentState>(PaymentState.Initial)
    val paymentState = _paymentState.asStateFlow()
    
    fun createVirtualAccount(bankCode: String, amount: Float) {
        viewModelScope.launch {
            _paymentState.value = PaymentState.Processing
            
            try {
                val result = xenditPaymentMethodService.createVirtualAccount(
                    bankCode = bankCode,
                    name = "User Name", // TODO: Get from AuthRepository
                    amount = amount
                )
                
                _paymentState.value = when (result) {
                    is VirtualAccountResult.Success -> {
                        PaymentState.VirtualAccountCreated(
                            accountNumber = result.accountNumber,
                            bankCode = result.bankCode,
                            amount = result.amount,
                            expirationDate = result.expirationDate
                        )
                    }
                    is VirtualAccountResult.Error -> {
                        PaymentState.Error(result.message)
                    }
                }
            } catch (e: Exception) {
                _paymentState.value = PaymentState.Error(
                    e.message ?: "Failed to create virtual account"
                )
            }
        }
    }
    
    fun createEWalletPayment(type: EWalletType, amount: Float, phone: String) {
        viewModelScope.launch {
            _paymentState.value = PaymentState.Processing
            
            try {
                val result = xenditPaymentMethodService.createEWalletCharge(
                    eWalletType = type,
                    amount = amount,
                    phone = phone
                )
                
                _paymentState.value = when (result) {
                    is EWalletResult.Success -> {
                        PaymentState.EWalletCreated(
                            chargeId = result.chargeId,
                            status = result.status,
                            checkoutUrl = result.checkoutUrl
                        )
                    }
                    is EWalletResult.Error -> {
                        PaymentState.Error(result.message)
                    }
                }
            } catch (e: Exception) {
                _paymentState.value = PaymentState.Error(
                    e.message ?: "Failed to create e-wallet payment"
                )
            }
        }
    }
    
    fun createQRCode(amount: Float) {
        viewModelScope.launch {
            _paymentState.value = PaymentState.Processing
            
            try {
                val result = xenditPaymentMethodService.createQRCode(amount)
                
                _paymentState.value = when (result) {
                    is QRCodeResult.Success -> {
                        PaymentState.QRCodeCreated(
                            qrString = result.qrString,
                            amount = result.amount,
                            expiresAt = result.expiresAt
                        )
                    }
                    is QRCodeResult.Error -> {
                        PaymentState.Error(result.message)
                    }
                }
            } catch (e: Exception) {
                _paymentState.value = PaymentState.Error(
                    e.message ?: "Failed to create QR code"
                )
            }
        }
    }
    
    fun createRetailOutletPayment(type: RetailOutletType, amount: Float) {
        viewModelScope.launch {
            _paymentState.value = PaymentState.Processing
            
            try {
                val result = xenditPaymentMethodService.createRetailOutlet(
                    retailOutletType = type,
                    name = "User Name", // TODO: Get from AuthRepository
                    amount = amount
                )
                
                _paymentState.value = when (result) {
                    is RetailOutletResult.Success -> {
                        PaymentState.RetailOutletCreated(
                            paymentCode = result.paymentCode,
                            amount = result.amount,
                            expiresAt = result.expiresAt
                        )
                    }
                    is RetailOutletResult.Error -> {
                        PaymentState.Error(result.message)
                    }
                }
            } catch (e: Exception) {
                _paymentState.value = PaymentState.Error(
                    e.message ?: "Failed to create retail outlet payment"
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
    
    data class VirtualAccountCreated(
        val accountNumber: String,
        val bankCode: String,
        val amount: Float,
        val expirationDate: String
    ) : PaymentState()
    
    data class EWalletCreated(
        val chargeId: String,
        val status: String,
        val checkoutUrl: String
    ) : PaymentState()
    
    data class QRCodeCreated(
        val qrString: String,
        val amount: Float,
        val expiresAt: String
    ) : PaymentState()
    
    data class RetailOutletCreated(
        val paymentCode: String,
        val amount: Float,
        val expiresAt: String
    ) : PaymentState()
}
