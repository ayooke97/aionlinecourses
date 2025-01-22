package com.aionlinecourses.ui.screens

import androidx.compose.animation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
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
import com.aionlinecourses.data.entity.PaymentMethod
import com.aionlinecourses.data.entity.PaymentType
import com.aionlinecourses.ui.viewmodel.PaymentState
import com.aionlinecourses.ui.viewmodel.PaymentViewModel
import java.text.NumberFormat
import java.util.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PaymentScreen(
    navController: NavController,
    courseId: Int,
    amount: Float,
    viewModel: PaymentViewModel = viewModel()
) {
    val paymentState by viewModel.paymentState.collectAsState()
    val paymentMethods by viewModel.paymentMethods.collectAsState(initial = emptyList())
    
    var showAddCard by remember { mutableStateOf(false) }
    
    LaunchedEffect(paymentState) {
        when (paymentState) {
            is PaymentState.Success -> {
                navController.navigate("payment_success/${(paymentState as PaymentState.Success).transactionId}")
            }
            is PaymentState.Error -> {
                // Show error snackbar
            }
            else -> {}
        }
    }
    
    Scaffold(
        topBar = {
            SmallTopAppBar(
                title = { Text("Payment") },
                navigationIcon = {
                    IconButton(onClick = { navController.navigateUp() }) {
                        Icon(Icons.Default.ArrowBack, "Back")
                    }
                }
            )
        }
    ) { padding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
        ) {
            if (showAddCard) {
                AddCardForm(
                    onSubmit = { cardNumber, expiryMonth, expiryYear, cvc ->
                        viewModel.addPaymentMethod(
                            type = PaymentType.CREDIT_CARD,
                            cardNumber = cardNumber,
                            expiryMonth = expiryMonth,
                            expiryYear = expiryYear,
                            cvc = cvc
                        )
                        showAddCard = false
                    },
                    onCancel = { showAddCard = false }
                )
            } else {
                PaymentContent(
                    amount = amount,
                    paymentMethods = paymentMethods,
                    paymentState = paymentState,
                    onPaymentMethodSelect = { paymentMethodId ->
                        viewModel.processPayment(courseId, amount, paymentMethodId)
                    },
                    onAddCard = { showAddCard = true },
                    onRemoveCard = viewModel::removePaymentMethod,
                    onSetDefaultCard = viewModel::setDefaultPaymentMethod
                )
            }
            
            if (paymentState is PaymentState.Processing) {
                CircularProgressIndicator(
                    modifier = Modifier.align(Alignment.Center)
                )
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PaymentContent(
    amount: Float,
    paymentMethods: List<PaymentMethod>,
    paymentState: PaymentState,
    onPaymentMethodSelect: (Int) -> Unit,
    onAddCard: () -> Unit,
    onRemoveCard: (Int) -> Unit,
    onSetDefaultCard: (Int) -> Unit
) {
    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        item {
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
        }
        
        item {
            Text(
                text = "Payment Methods",
                style = MaterialTheme.typography.titleLarge
            )
        }
        
        items(paymentMethods) { paymentMethod ->
            PaymentMethodCard(
                paymentMethod = paymentMethod,
                onSelect = { onPaymentMethodSelect(paymentMethod.id) },
                onRemove = { onRemoveCard(paymentMethod.id) },
                onSetDefault = { onSetDefaultCard(paymentMethod.id) }
            )
        }
        
        item {
            OutlinedButton(
                onClick = onAddCard,
                modifier = Modifier.fillMaxWidth()
            ) {
                Icon(Icons.Default.Add, contentDescription = null)
                Spacer(modifier = Modifier.width(8.dp))
                Text("Add New Card")
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PaymentMethodCard(
    paymentMethod: PaymentMethod,
    onSelect: () -> Unit,
    onRemove: () -> Unit,
    onSetDefault: () -> Unit
) {
    Card(
        onClick = onSelect,
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = when (paymentMethod.type) {
                    PaymentType.CREDIT_CARD, PaymentType.DEBIT_CARD ->
                        Icons.Default.CreditCard
                    PaymentType.PAYPAL -> Icons.Default.Payment
                    else -> Icons.Default.AccountBalance
                },
                contentDescription = null,
                modifier = Modifier.size(24.dp)
            )
            
            Spacer(modifier = Modifier.width(16.dp))
            
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = "${paymentMethod.cardBrand} ****${paymentMethod.lastFourDigits}",
                    style = MaterialTheme.typography.titleMedium
                )
                Text(
                    text = "Expires ${paymentMethod.expiryMonth}/${paymentMethod.expiryYear}",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                if (paymentMethod.isDefault) {
                    Text(
                        text = "Default",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.primary
                    )
                }
            }
            
            if (!paymentMethod.isDefault) {
                IconButton(onClick = onSetDefault) {
                    Icon(Icons.Default.Star, "Set as default")
                }
            }
            
            IconButton(onClick = onRemove) {
                Icon(Icons.Default.Delete, "Remove card")
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddCardForm(
    onSubmit: (String, Int, Int, String) -> Unit,
    onCancel: () -> Unit
) {
    var cardNumber by remember { mutableStateOf("") }
    var expiryMonth by remember { mutableStateOf("") }
    var expiryYear by remember { mutableStateOf("") }
    var cvc by remember { mutableStateOf("") }
    
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(16.dp)
    ) {
        Text(
            text = "Add New Card",
            style = MaterialTheme.typography.titleLarge,
            modifier = Modifier.padding(bottom = 16.dp)
        )
        
        OutlinedTextField(
            value = cardNumber,
            onValueChange = { if (it.length <= 16) cardNumber = it },
            label = { Text("Card Number") },
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 8.dp)
        )
        
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 8.dp)
        ) {
            OutlinedTextField(
                value = expiryMonth,
                onValueChange = { if (it.length <= 2) expiryMonth = it },
                label = { Text("MM") },
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                modifier = Modifier
                    .weight(1f)
                    .padding(end = 8.dp)
            )
            
            OutlinedTextField(
                value = expiryYear,
                onValueChange = { if (it.length <= 2) expiryYear = it },
                label = { Text("YY") },
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                modifier = Modifier
                    .weight(1f)
                    .padding(start = 8.dp)
            )
        }
        
        OutlinedTextField(
            value = cvc,
            onValueChange = { if (it.length <= 4) cvc = it },
            label = { Text("CVC") },
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 8.dp)
        )
        
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 16.dp),
            horizontalArrangement = Arrangement.End
        ) {
            TextButton(onClick = onCancel) {
                Text("Cancel")
            }
            
            Button(
                onClick = {
                    if (isValidCardInput(cardNumber, expiryMonth, expiryYear, cvc)) {
                        onSubmit(
                            cardNumber,
                            expiryMonth.toInt(),
                            expiryYear.toInt(),
                            cvc
                        )
                    }
                },
                enabled = isValidCardInput(cardNumber, expiryMonth, expiryYear, cvc),
                modifier = Modifier.padding(start = 8.dp)
            ) {
                Text("Add Card")
            }
        }
    }
}

private fun isValidCardInput(
    cardNumber: String,
    expiryMonth: String,
    expiryYear: String,
    cvc: String
): Boolean {
    return cardNumber.length == 16 &&
            expiryMonth.length == 2 &&
            expiryYear.length == 2 &&
            cvc.length in 3..4 &&
            expiryMonth.toIntOrNull() in 1..12
}

private fun formatCurrency(amount: Float): String {
    return NumberFormat.getCurrencyInstance(Locale.US).format(amount)
}
