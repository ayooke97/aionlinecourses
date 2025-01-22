package com.aionlinecourses.service

import android.content.Context
import com.xendit.Models.*
import com.xendit.Xendit
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.withContext
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException

class XenditPaymentMethodService(
    private val context: Context,
    private val publishableKey: String
) {
    init {
        Xendit.init(publishableKey)
    }
    
    suspend fun createVirtualAccount(
        bankCode: String,
        name: String,
        amount: Float
    ): VirtualAccountResult = withContext(Dispatchers.IO) {
        try {
            val vaRequest = VirtualAccountRequest().apply {
                this.bankCode = bankCode
                this.name = name
                this.amount = amount
            }
            
            val va = Xendit.createVirtualAccount(vaRequest)
            
            VirtualAccountResult.Success(
                accountNumber = va.accountNumber,
                bankCode = va.bankCode,
                amount = va.amount,
                expirationDate = va.expirationDate
            )
        } catch (e: Exception) {
            VirtualAccountResult.Error(e.message ?: "Failed to create virtual account")
        }
    }
    
    suspend fun createEWalletCharge(
        eWalletType: EWalletType,
        amount: Float,
        phone: String
    ): EWalletResult = withContext(Dispatchers.IO) {
        try {
            val eWalletRequest = EWalletChargeRequest().apply {
                this.eWalletType = eWalletType.name
                this.amount = amount
                this.mobileNumber = phone
            }
            
            val charge = Xendit.createEWalletCharge(eWalletRequest)
            
            EWalletResult.Success(
                chargeId = charge.id,
                status = charge.status,
                checkoutUrl = charge.checkoutUrl
            )
        } catch (e: Exception) {
            EWalletResult.Error(e.message ?: "Failed to create e-wallet charge")
        }
    }
    
    suspend fun createQRCode(
        amount: Float,
        expiresIn: Int = 3600
    ): QRCodeResult = withContext(Dispatchers.IO) {
        try {
            val qrRequest = QRCodeRequest().apply {
                this.amount = amount
                this.expiresIn = expiresIn
            }
            
            val qrCode = Xendit.createQRCode(qrRequest)
            
            QRCodeResult.Success(
                qrString = qrCode.qrString,
                amount = qrCode.amount,
                expiresAt = qrCode.expiresAt
            )
        } catch (e: Exception) {
            QRCodeResult.Error(e.message ?: "Failed to create QR code")
        }
    }
    
    suspend fun createRetailOutlet(
        retailOutletType: RetailOutletType,
        name: String,
        amount: Float
    ): RetailOutletResult = withContext(Dispatchers.IO) {
        try {
            val retailRequest = RetailOutletRequest().apply {
                this.retailOutletName = retailOutletType.name
                this.name = name
                this.amount = amount
            }
            
            val payment = Xendit.createRetailOutletCharge(retailRequest)
            
            RetailOutletResult.Success(
                paymentCode = payment.paymentCode,
                amount = payment.amount,
                expiresAt = payment.expiresAt
            )
        } catch (e: Exception) {
            RetailOutletResult.Error(e.message ?: "Failed to create retail outlet payment")
        }
    }
}

sealed class VirtualAccountResult {
    data class Success(
        val accountNumber: String,
        val bankCode: String,
        val amount: Float,
        val expirationDate: String
    ) : VirtualAccountResult()
    data class Error(val message: String) : VirtualAccountResult()
}

sealed class EWalletResult {
    data class Success(
        val chargeId: String,
        val status: String,
        val checkoutUrl: String
    ) : EWalletResult()
    data class Error(val message: String) : EWalletResult()
}

sealed class QRCodeResult {
    data class Success(
        val qrString: String,
        val amount: Float,
        val expiresAt: String
    ) : QRCodeResult()
    data class Error(val message: String) : QRCodeResult()
}

sealed class RetailOutletResult {
    data class Success(
        val paymentCode: String,
        val amount: Float,
        val expiresAt: String
    ) : RetailOutletResult()
    data class Error(val message: String) : RetailOutletResult()
}

enum class EWalletType {
    OVO,
    DANA,
    LINKAJA,
    SHOPEEPAY,
    GOPAY
}

enum class RetailOutletType {
    ALFAMART,
    INDOMARET
}
