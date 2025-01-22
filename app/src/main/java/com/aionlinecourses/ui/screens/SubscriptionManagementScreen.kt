package com.aionlinecourses.ui.screens

import androidx.compose.animation.*
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
import com.aionlinecourses.data.dao.SubscriptionWithDetails
import com.aionlinecourses.data.entity.SubscriptionPlanType
import com.aionlinecourses.data.entity.SubscriptionStatus
import com.aionlinecourses.ui.viewmodel.AnalyticsPeriod
import com.aionlinecourses.ui.viewmodel.SubscriptionState
import com.aionlinecourses.ui.viewmodel.SubscriptionViewModel
import java.text.NumberFormat
import java.text.SimpleDateFormat
import java.util.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SubscriptionManagementScreen(
    navController: NavController,
    viewModel: SubscriptionViewModel = viewModel()
) {
    val subscriptionState by viewModel.subscriptionState.collectAsState()
    val subscriptions by viewModel.subscriptions.collectAsState(initial = emptyList())
    val metrics by viewModel.subscriptionMetrics.collectAsState(initial = null)
    
    var selectedPeriod by remember { mutableStateOf(AnalyticsPeriod.LAST_MONTH) }
    var showCancelDialog by remember { mutableStateOf<Int?>(null) }
    
    LaunchedEffect(selectedPeriod) {
        viewModel.getRevenueAnalytics(selectedPeriod)
    }
    
    Scaffold(
        topBar = {
            SmallTopAppBar(
                title = { Text("Subscriptions") },
                navigationIcon = {
                    IconButton(onClick = { navController.navigateUp() }) {
                        Icon(Icons.Default.ArrowBack, "Back")
                    }
                },
                actions = {
                    IconButton(onClick = { viewModel.generateReport() }) {
                        Icon(Icons.Default.Assessment, "Generate Report")
                    }
                }
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
        ) {
            // Metrics Cards
            metrics?.let { metrics ->
                MetricsSection(metrics = metrics)
            }
            
            // Analytics Period Selector
            AnalyticsPeriodSelector(
                selectedPeriod = selectedPeriod,
                onPeriodSelected = {
                    selectedPeriod = it
                    viewModel.getRevenueAnalytics(it)
                }
            )
            
            // Subscriptions List
            LazyColumn(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f),
                contentPadding = PaddingValues(16.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                items(subscriptions) { subscription ->
                    SubscriptionCard(
                        subscription = subscription,
                        onCancel = { showCancelDialog = subscription.id }
                    )
                }
            }
        }
        
        // Cancel Dialog
        showCancelDialog?.let { subscriptionId ->
            CancelSubscriptionDialog(
                onConfirm = {
                    viewModel.cancelSubscription(subscriptionId)
                    showCancelDialog = null
                },
                onDismiss = { showCancelDialog = null }
            )
        }
        
        // Loading Indicator
        if (subscriptionState is SubscriptionState.Processing) {
            Box(
                modifier = Modifier.fillMaxSize(),
                contentAlignment = Alignment.Center
            ) {
                CircularProgressIndicator()
            }
        }
    }
}

@Composable
fun MetricsSection(metrics: SubscriptionMetrics) {
    LazyColumn(
        modifier = Modifier
            .fillMaxWidth()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        item {
            MetricCard(
                title = "Active Subscriptions",
                value = metrics.activeSubscriptions.toString(),
                icon = Icons.Default.People
            )
        }
        item {
            MetricCard(
                title = "Total Revenue",
                value = formatCurrency(metrics.totalRevenue),
                icon = Icons.Default.AttachMoney
            )
        }
        item {
            MetricCard(
                title = "Churn Rate",
                value = "%.1f%%".format(metrics.churnRate),
                icon = Icons.Default.TrendingDown
            )
        }
        item {
            MetricCard(
                title = "Trial Conversion",
                value = "%.1f%%".format(metrics.trialConversionRate),
                icon = Icons.Default.TrendingUp
            )
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MetricCard(
    title: String,
    value: String,
    icon: ImageVector
) {
    Card(
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                modifier = Modifier.size(24.dp),
                tint = MaterialTheme.colorScheme.primary
            )
            Spacer(modifier = Modifier.width(16.dp))
            Column {
                Text(
                    text = title,
                    style = MaterialTheme.typography.titleSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Text(
                    text = value,
                    style = MaterialTheme.typography.headlineSmall
                )
            }
        }
    }
}

@Composable
fun AnalyticsPeriodSelector(
    selectedPeriod: AnalyticsPeriod,
    onPeriodSelected: (AnalyticsPeriod) -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        AnalyticsPeriod.values().forEach { period ->
            FilterChip(
                selected = period == selectedPeriod,
                onClick = { onPeriodSelected(period) },
                label = {
                    Text(
                        period.name.replace("_", " ")
                            .lowercase()
                            .capitalize()
                    )
                }
            )
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SubscriptionCard(
    subscription: SubscriptionWithDetails,
    onCancel: () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = subscription.courseTitle,
                        style = MaterialTheme.typography.titleMedium,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    Text(
                        text = "${subscription.planType.name.lowercase().capitalize()} Plan",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                
                SubscriptionStatusChip(status = subscription.status)
            }
            
            Spacer(modifier = Modifier.height(8.dp))
            
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Column {
                    Text(
                        text = "Next Billing",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Text(
                        text = formatDate(subscription.nextBillingDate),
                        style = MaterialTheme.typography.bodyMedium
                    )
                }
                
                Column(horizontalAlignment = Alignment.End) {
                    Text(
                        text = "Amount",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Text(
                        text = formatCurrency(subscription.amount),
                        style = MaterialTheme.typography.bodyMedium
                    )
                }
            }
            
            if (subscription.status == SubscriptionStatus.ACTIVE) {
                OutlinedButton(
                    onClick = onCancel,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 16.dp)
                ) {
                    Icon(Icons.Default.Cancel, contentDescription = null)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Cancel Subscription")
                }
            }
        }
    }
}

@Composable
fun SubscriptionStatusChip(status: SubscriptionStatus) {
    val (color, text) = when (status) {
        SubscriptionStatus.ACTIVE -> MaterialTheme.colorScheme.primary to "Active"
        SubscriptionStatus.CANCELED -> MaterialTheme.colorScheme.error to "Canceled"
        SubscriptionStatus.EXPIRED -> MaterialTheme.colorScheme.error to "Expired"
        SubscriptionStatus.PAST_DUE -> MaterialTheme.colorScheme.error to "Past Due"
        SubscriptionStatus.TRIALING -> MaterialTheme.colorScheme.tertiary to "Trial"
    }
    
    Surface(
        color = color.copy(alpha = 0.1f),
        shape = MaterialTheme.shapes.small
    ) {
        Text(
            text = text,
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
            style = MaterialTheme.typography.bodySmall,
            color = color
        )
    }
}

@Composable
fun CancelSubscriptionDialog(
    onConfirm: () -> Unit,
    onDismiss: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Cancel Subscription") },
        text = {
            Text("Are you sure you want to cancel this subscription? You'll continue to have access until the end of the current billing period.")
        },
        confirmButton = {
            TextButton(onClick = onConfirm) {
                Text("Cancel Subscription")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Keep Subscription")
            }
        }
    )
}

private fun formatDate(timestamp: Long): String {
    val dateFormat = SimpleDateFormat("MMM dd, yyyy", Locale.getDefault())
    return dateFormat.format(Date(timestamp))
}

private fun formatCurrency(amount: Float): String {
    return NumberFormat.getCurrencyInstance(Locale.US).format(amount)
}
