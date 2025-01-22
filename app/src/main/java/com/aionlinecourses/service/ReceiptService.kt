package com.aionlinecourses.service

import android.content.Context
import android.graphics.pdf.PdfDocument
import android.graphics.pdf.PdfDocument.PageInfo
import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.Typeface
import com.aionlinecourses.data.AppDatabase
import com.aionlinecourses.data.dao.TransactionWithDetails
import java.io.File
import java.io.FileOutputStream
import java.text.NumberFormat
import java.text.SimpleDateFormat
import java.util.*

class ReceiptService(private val context: Context) {
    private val database = AppDatabase.getDatabase(context)
    private val transactionDao = database.transactionDao()
    private val userDao = database.userDao()
    
    suspend fun generateReceipt(transactionId: Int): File? {
        val transaction = transactionDao.getTransactionById(transactionId) ?: return null
        val user = userDao.getUserById(transaction.userId)
        
        val document = PdfDocument()
        val pageInfo = PageInfo.Builder(595, 842, 1).create() // A4 size
        val page = document.startPage(pageInfo)
        val canvas = page.canvas
        
        val paint = Paint().apply {
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.NORMAL)
            textSize = 12f
        }
        
        // Header
        drawText(canvas, paint, "AI Online Courses", 40f, 50f, 16f, true)
        drawText(canvas, paint, "Receipt", 40f, 80f, 14f, true)
        
        // Transaction Details
        val dateFormat = SimpleDateFormat("dd/MM/yyyy HH:mm", Locale.getDefault())
        val currencyFormat = NumberFormat.getCurrencyInstance(Locale.US)
        
        drawText(canvas, paint, "Transaction ID: ${transaction.transactionReference}", 40f, 120f)
        drawText(canvas, paint, "Date: ${dateFormat.format(Date(transaction.timestamp))}", 40f, 140f)
        
        // Customer Details
        drawText(canvas, paint, "Customer Details", 40f, 180f, 14f, true)
        user?.let {
            drawText(canvas, paint, "Name: ${it.name}", 40f, 200f)
            drawText(canvas, paint, "Email: ${it.email}", 40f, 220f)
        }
        
        // Payment Details
        drawText(canvas, paint, "Payment Details", 40f, 260f, 14f, true)
        drawText(canvas, paint, "Amount: ${currencyFormat.format(transaction.amount)}", 40f, 280f)
        drawText(canvas, paint, "Status: ${transaction.status}", 40f, 300f)
        drawText(canvas, paint, "Payment Method: ${getPaymentMethodText(transaction)}", 40f, 320f)
        
        // Footer
        drawText(canvas, paint, "Thank you for your purchase!", 40f, 700f, 12f, true)
        drawText(canvas, paint, "For support, contact: support@aionlinecourses.com", 40f, 720f)
        
        document.finishPage(page)
        
        // Save PDF
        val receiptFile = File(context.filesDir, "receipt_${transaction.transactionReference}.pdf")
        FileOutputStream(receiptFile).use { out ->
            document.writeTo(out)
        }
        document.close()
        
        return receiptFile
    }
    
    private fun drawText(
        canvas: Canvas,
        paint: Paint,
        text: String,
        x: Float,
        y: Float,
        textSize: Float = 12f,
        isBold: Boolean = false
    ) {
        paint.apply {
            this.textSize = textSize
            typeface = if (isBold) {
                Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
            } else {
                Typeface.create(Typeface.DEFAULT, Typeface.NORMAL)
            }
        }
        canvas.drawText(text, x, y, paint)
    }
    
    private suspend fun getPaymentMethodText(transaction: TransactionWithDetails): String {
        return transaction.paymentType?.let { type ->
            when (type) {
                "CREDIT_CARD", "DEBIT_CARD" -> "**** ${transaction.lastFourDigits}"
                else -> type.replace("_", " ").capitalize()
            }
        } ?: "N/A"
    }
}
