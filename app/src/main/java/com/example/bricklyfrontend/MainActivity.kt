package com.example.bricklyfrontend

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.runtime.*
import androidx.compose.ui.platform.LocalContext
import androidx.navigation.compose.rememberNavController
import com.example.bricklyfrontend.data.RetrofitClient
import com.example.bricklyfrontend.data.UserPreferences
import com.example.bricklyfrontend.screens.*
import com.example.bricklyfrontend.ui.theme.BricklyFrontendTheme
import com.example.bricklyfrontend.ui.navigation.BricklyNavigation

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        RetrofitClient.init(this)
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

    var showSplash by remember { mutableStateOf(true) }

    if (showSplash) {
        SplashScreen(onFinished = { showSplash = false })
        return
    }

    val startDest = if (UserPreferences.isLoggedIn(context)) "meetings" else "login"

    BricklyNavigation(
        navController = navController,
        startDestination = startDest
    )
}