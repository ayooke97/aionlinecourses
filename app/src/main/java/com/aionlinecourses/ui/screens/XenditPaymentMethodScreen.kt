package com.aionlinecourses.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import com.aionlinecourses.service.EWalletType
import com.aionlinecourses.service.RetailOutletType
import com.aionlinecourses.ui.viewmodel.XenditPaymentMethodViewModel
import java.text.NumberFormat
import java.util.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun XenditPaymentMethodScreen(
    navController: NavController,
    courseId: Int,
    amount: Float,
    viewModel: XenditPaymentMethodViewModel = viewModel()
) {
    var selectedPaymentMethod by remember { mutableStateOf<PaymentMethod?>(null) }
    var showPhoneDialog by remember { mutableStateOf(false) }
    var phoneNumber by remember { mutableStateOf("") }
    
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
                title = { Text("Choose Payment Method") },
                navigationIcon = {
                    IconButton(onClick = { navController.navigateUp() }) {
                        Icon(Icons.Default.ArrowBack, "Back")
                    }
                }
            )
        }
    ) { padding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            item {
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
            }
            
            // Virtual Account Section
            item {
                Text(
                    text = "Virtual Account",
                    style = MaterialTheme.typography.titleMedium,
                    modifier = Modifier.padding(vertical = 8.dp)
                )
            }
            
            items(listOf("BCA", "BNI", "Mandiri", "BRI")) { bank ->
                PaymentMethodCard(
                    title = "$bank Virtual Account",
                    icon = Icons.Default.AccountBalance,
                    selected = selectedPaymentMethod == PaymentMethod.VirtualAccount(bank),
                    onClick = {
                        selectedPaymentMethod = PaymentMethod.VirtualAccount(bank)
                        viewModel.createVirtualAccount(bank, amount)
                    }
                )
            }
            
            // E-Wallet Section
            item {
                Text(
                    text = "E-Wallet",
                    style = MaterialTheme.typography.titleMedium,
                    modifier = Modifier.padding(vertical = 8.dp)
                )
            }
            
            items(EWalletType.values()) { wallet ->
                PaymentMethodCard(
                    title = wallet.name,
                    icon = Icons.Default.AccountBalanceWallet,
                    selected = selectedPaymentMethod == PaymentMethod.EWallet(wallet),
                    onClick = {
                        selectedPaymentMethod = PaymentMethod.EWallet(wallet)
                        showPhoneDialog = true
                    }
                )
            }
            
            // Retail Outlet Section
            item {
                Text(
                    text = "Retail Outlet",
                    style = MaterialTheme.typography.titleMedium,
                    modifier = Modifier.padding(vertical = 8.dp)
                )
            }
            
            items(RetailOutletType.values()) { outlet ->
                PaymentMethodCard(
                    title = outlet.name,
                    icon = Icons.Default.Store,
                    selected = selectedPaymentMethod == PaymentMethod.RetailOutlet(outlet),
                    onClick = {
                        selectedPaymentMethod = PaymentMethod.RetailOutlet(outlet)
                        viewModel.createRetailOutletPayment(outlet, amount)
                    }
                )
            }
            
            // QR Code Section
            item {
                PaymentMethodCard(
                    title = "QR Code Payment",
                    icon = Icons.Default.QrCode,
                    selected = selectedPaymentMethod == PaymentMethod.QRCode,
                    onClick = {
                        selectedPaymentMethod = PaymentMethod.QRCode
                        viewModel.createQRCode(amount)
                    }
                )
            }
        }
        
        // Phone Number Dialog
        if (showPhoneDialog) {
            AlertDialog(
                onDismissRequest = { showPhoneDialog = false },
                title = { Text("Enter Phone Number") },
                text = {
                    OutlinedTextField(
                        value = phoneNumber,
                        onValueChange = { phoneNumber = it },
                        label = { Text("Phone Number") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone)
                    )
                },
                confirmButton = {
                    TextButton(
                        onClick = {
                            showPhoneDialog = false
                            selectedPaymentMethod?.let {
                                if (it is PaymentMethod.EWallet) {
                                    viewModel.createEWalletPayment(it.type, amount, phoneNumber)
                                }
                            }
                        }
                    ) {
                        Text("Confirm")
                    }
                },
                dismissButton = {
                    TextButton(onClick = { showPhoneDialog = false }) {
                        Text("Cancel")
                    }
                }
            )
        }
        
        // Loading Indicator
        if (paymentState is PaymentState.Processing) {
            Box(
                modifier = Modifier.fillMaxSize(),
                contentAlignment = Alignment.Center
            ) {
                CircularProgressIndicator()
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PaymentMethodCard(
    title: String,
    icon: ImageVector,
    selected: Boolean,
    onClick: () -> Unit
) {
    Card(
        onClick = onClick,
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = if (selected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface
            )
            
            Text(
                text = title,
                style = MaterialTheme.typography.titleMedium,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.weight(1f)
            )
            
            if (selected) {
                Icon(
                    imageVector = Icons.Default.Check,
                    contentDescription = "Selected",
                    tint = MaterialTheme.colorScheme.primary
                )
            }
        }
    }
}

sealed class PaymentMethod {
    data class VirtualAccount(val bankCode: String) : PaymentMethod()
    data class EWallet(val type: EWalletType) : PaymentMethod()
    data class RetailOutlet(val type: RetailOutletType) : PaymentMethod()
    object QRCode : PaymentMethod()
}

private fun formatCurrency(amount: Float): String {
    return NumberFormat.getCurrencyInstance(Locale.US).format(amount)
}
