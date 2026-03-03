package com.example.bricklyfrontend

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.runtime.*
import androidx.compose.ui.platform.LocalContext
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.example.bricklyfrontend.data.UserPreferences
import com.example.bricklyfrontend.screens.*
import com.example.bricklyfrontend.ui.theme.BricklyFrontendTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            BricklyFrontendTheme {
                BricklyApp()
            }
        }
    }
}

@Composable
fun BricklyApp() {
    val context = LocalContext.current
    val navController = rememberNavController()

    val startDest = if (UserPreferences.isLoggedIn(context)) "profile" else "login"

    NavHost(navController = navController, startDestination = startDest) {

        composable("login") {
            LoginScreen(
                onNavigateToRegister = { navController.navigate("register") },
                onLoggedIn = {
                    navController.navigate("profile") {
                        popUpTo("login") { inclusive = true }
                    }
                }
            )
        }

        composable("register") {
            RegisterScreen(
                onNavigateToLogin = { navController.popBackStack() },
                onRegistered = {
                    navController.navigate("profile") {
                        popUpTo("register") { inclusive = true }
                    }
                }
            )
        }

        composable("profile") {
            ProfileScreen(
                onNavigateToMeetings = {},
                onNavigateToOrders = {},
                onNavigateToShop = {},
                onNavigateToEditProfile = {},
                onLogout = {
                    navController.navigate("login") {
                        popUpTo("profile") { inclusive = true }
                    }
                }
            )
        }
    }
}
