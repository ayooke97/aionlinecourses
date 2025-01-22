package com.aionlinecourses.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import com.aionlinecourses.ui.viewmodel.XenditPaymentViewModel
import java.text.NumberFormat
import java.util.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun XenditPaymentScreen(
    navController: NavController,
    courseId: Int,
    amount: Float,
    viewModel: XenditPaymentViewModel = viewModel()
) {
    var cardNumber by remember { mutableStateOf("") }
    var expiryMonth by remember { mutableStateOf("") }
    var expiryYear by remember { mutableStateOf("") }
    var cvv by remember { mutableStateOf("") }
    
    val paymentState by viewModel.paymentState.collectAsState()
    
    LaunchedEffect(paymentState) {
        when (paymentState) {
            is PaymentState.Success -> {
                navController.navigate(
                    "payment_success/${(paymentState as PaymentState.Success).transactionId}"
                )
            }
            else -> {}
        }
    }
    
    Scaffold(
        topBar = {
            SmallTopAppBar(
                title = { Text("Xendit Payment") },
                navigationIcon = {
                    IconButton(onClick = { navController.navigateUp() }) {
                        Icon(Icons.Default.ArrowBack, "Back")
                    }
                }
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Amount Card
            Card(
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp)
                ) {
                    Text(
                        text = "Amount to Pay",
                        style = MaterialTheme.typography.titleMedium
                    )
                    Text(
                        text = formatCurrency(amount),
                        style = MaterialTheme.typography.headlineMedium,
                        color = MaterialTheme.colorScheme.primary
                    )
                }
            }
            
            // Card Details Form
            Card(
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    Text(
                        text = "Card Details",
                        style = MaterialTheme.typography.titleMedium
                    )
                    
                    OutlinedTextField(
                        value = cardNumber,
                        onValueChange = { if (it.length <= 16) cardNumber = it },
                        label = { Text("Card Number") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        modifier = Modifier.fillMaxWidth(),
                        leadingIcon = {
                            Icon(Icons.Default.CreditCard, contentDescription = null)
                        }
                    )
                    
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        OutlinedTextField(
                            value = expiryMonth,
                            onValueChange = { if (it.length <= 2) expiryMonth = it },
                            label = { Text("MM") },
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                            modifier = Modifier.weight(1f)
                        )
                        
                        OutlinedTextField(
                            value = expiryYear,
                            onValueChange = { if (it.length <= 2) expiryYear = it },
                            label = { Text("YY") },
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                            modifier = Modifier.weight(1f)
                        )
                        
                        OutlinedTextField(
                            value = cvv,
                            onValueChange = { if (it.length <= 4) cvv = it },
                            label = { Text("CVV") },
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                            modifier = Modifier.weight(1f)
                        )
                    }
                }
            }
            
            // Payment Button
            Button(
                onClick = {
                    viewModel.processPayment(
                        courseId = courseId,
                        amount = amount,
                        cardNumber = cardNumber,
                        expiryMonth = expiryMonth,
                        expiryYear = expiryYear,
                        cvv = cvv
                    )
                },
                modifier = Modifier.fillMaxWidth(),
                enabled = isValidCardInput(cardNumber, expiryMonth, expiryYear, cvv)
            ) {
                if (paymentState is PaymentState.Processing) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(24.dp),
                        color = MaterialTheme.colorScheme.onPrimary
                    )
                } else {
                    Text("Pay ${formatCurrency(amount)}")
                }
            }
            
            // Error Message
            if (paymentState is PaymentState.Error) {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.errorContainer
                    )
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Icon(
                            Icons.Default.Error,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.error
                        )
                        Text(
                            text = (paymentState as PaymentState.Error).message,
                            color = MaterialTheme.colorScheme.error
                        )
                    }
                }
            }
            
            // Secure Payment Notice
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 16.dp),
                horizontalArrangement = Arrangement.Center,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    Icons.Default.Lock,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.size(16.dp)
                )
                Spacer(modifier = Modifier.width(4.dp))
                Text(
                    text = "Secure payment powered by Xendit",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

private fun isValidCardInput(
    cardNumber: String,
    expiryMonth: String,
    expiryYear: String,
    cvv: String
): Boolean {
    return cardNumber.length == 16 &&
            expiryMonth.length == 2 &&
            expiryMonth.toIntOrNull() in 1..12 &&
            expiryYear.length == 2 &&
            cvv.length in 3..4
}

private fun formatCurrency(amount: Float): String {
    return NumberFormat.getCurrencyInstance(Locale.US).format(amount)
}
