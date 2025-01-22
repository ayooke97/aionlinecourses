package com.aionlinecourses

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.aionlinecourses.ui.theme.AIOnlineCoursesTheme
import com.aionlinecourses.ui.screens.*
import com.aionlinecourses.ui.viewmodel.AuthViewModel

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            AIOnlineCoursesTheme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    AppNavigation()
                }
            }
        }
    }
}

@Composable
fun AppNavigation() {
    val navController = rememberNavController()
    val authViewModel: AuthViewModel = viewModel()
    val isLoggedIn by authViewModel.currentUser.collectAsState()
    
    NavHost(
        navController = navController,
        startDestination = if (isLoggedIn != null) "home" else "login"
    ) {
        composable("login") {
            LoginScreen(authViewModel, navController)
        }
        
        composable("register") {
            // TODO: Implement RegisterScreen
        }
        
        composable("home") {
            HomeScreen(navController)
        }
        
        composable("course/{courseId}") { backStackEntry ->
            val courseId = backStackEntry.arguments?.getString("courseId")
            CourseDetailScreen(
                courseId = courseId?.toIntOrNull() ?: 0,
                navController = navController
            )
        }
        
        composable("video/{courseId}") { backStackEntry ->
            val courseId = backStackEntry.arguments?.getString("courseId")
            val course = sampleCourses.find { it.id == courseId?.toIntOrNull() }
            if (course != null) {
                VideoPlayerScreen(
                    videoUrl = course.videoUrl,
                    title = course.title,
                    navController = navController
                )
            }
        }
        
        composable("profile") {
            ProfileScreen(navController)
        }
        
        composable("subscriptions") {
            SubscriptionManagementScreen(navController)
        }
        
        composable(
            route = "payment/{courseId}/{amount}",
            arguments = listOf(
                navArgument("courseId") { type = NavType.IntType },
                navArgument("amount") { type = NavType.FloatType }
            )
        ) { backStackEntry ->
            PaymentScreen(
                navController = navController,
                courseId = backStackEntry.arguments?.getInt("courseId") ?: 0,
                amount = backStackEntry.arguments?.getFloat("amount") ?: 0f
            )
        }
        
        composable(
            route = "xendit_payment/{courseId}/{amount}",
            arguments = listOf(
                navArgument("courseId") { type = NavType.IntType },
                navArgument("amount") { type = NavType.FloatType }
            )
        ) { backStackEntry ->
            XenditPaymentScreen(
                navController = navController,
                courseId = backStackEntry.arguments?.getInt("courseId") ?: 0,
                amount = backStackEntry.arguments?.getFloat("amount") ?: 0f
            )
        }
        
        composable(
            route = "xendit_payment_method/{courseId}/{amount}",
            arguments = listOf(
                navArgument("courseId") { type = NavType.IntType },
                navArgument("amount") { type = NavType.FloatType }
            )
        ) { backStackEntry ->
            XenditPaymentMethodScreen(
                navController = navController,
                courseId = backStackEntry.arguments?.getInt("courseId") ?: 0,
                amount = backStackEntry.arguments?.getFloat("amount") ?: 0f
            )
        }
        
        composable(
            route = "payment_success/{transactionId}",
            arguments = listOf(
                navArgument("transactionId") { type = NavType.IntType }
            )
        ) { backStackEntry ->
            PaymentSuccessScreen(
                navController = navController,
                transactionId = backStackEntry.arguments?.getInt("transactionId") ?: 0
            )
        }
    }
}
