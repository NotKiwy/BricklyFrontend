package com.example.bricklyfrontend

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.animation.*
import androidx.compose.animation.core.tween
import androidx.compose.runtime.*
import androidx.compose.ui.platform.LocalContext
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.example.bricklyfrontend.data.RetrofitClient
import com.example.bricklyfrontend.data.UserPreferences
import com.example.bricklyfrontend.screens.*
import com.example.bricklyfrontend.ui.theme.BricklyFrontendTheme

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

private const val NAV_ANIM_DURATION = 220

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

    NavHost(
        navController = navController,
        startDestination = startDest,
        enterTransition = {
            fadeIn(animationSpec = tween(NAV_ANIM_DURATION)) +
                    slideInHorizontally(
                        animationSpec = tween(NAV_ANIM_DURATION),
                        initialOffsetX = { it / 6 }
                    )
        },
        exitTransition = {
            fadeOut(animationSpec = tween(NAV_ANIM_DURATION)) +
                    slideOutHorizontally(
                        animationSpec = tween(NAV_ANIM_DURATION),
                        targetOffsetX = { -it / 6 }
                    )
        },
        popEnterTransition = {
            fadeIn(animationSpec = tween(NAV_ANIM_DURATION)) +
                    slideInHorizontally(
                        animationSpec = tween(NAV_ANIM_DURATION),
                        initialOffsetX = { -it / 6 }
                    )
        },
        popExitTransition = {
            fadeOut(animationSpec = tween(NAV_ANIM_DURATION)) +
                    slideOutHorizontally(
                        animationSpec = tween(NAV_ANIM_DURATION),
                        targetOffsetX = { it / 6 }
                    )
        }
    ) {

        composable("login") {
            LoginScreen(
                onNavigateToRegister = { navController.navigate("register") },
                onLoggedIn = {
                    navController.navigate("meetings") {
                        popUpTo("login") { inclusive = true }
                    }
                }
            )
        }

        composable("register") {
            RegisterScreen(
                onNavigateToLogin = { navController.popBackStack() },
                onRegistered = {
                    navController.navigate("meetings") {
                        popUpTo("register") { inclusive = true }
                    }
                }
            )
        }

        composable("meetings") {
            MeetingsScreen(
                onNavigateToProfile = { navController.navigate("profile") },
                onNavigateToMeetingDetail = { meetingId ->
                    navController.navigate("meeting_detail/$meetingId")
                },
                onNavigateToCart = { navController.navigate("cart") },
                onNavigateToHome = { navController.navigate("home") },
                onNavigateToBrickognize = { navController.navigate("brickognize") }
            )
        }

        composable("home") {
            CatalogScreen(
                onNavigateToMeetings = { navController.navigate("meetings") },
                onNavigateToProfile = { navController.navigate("profile") },
                onNavigateToCart = { navController.navigate("cart") },
                onNavigateToMeetingDetail = { meetingId ->
                    navController.navigate("meeting_detail/$meetingId")
                },
                onNavigateToBrickognize = { navController.navigate("brickognize") }
            )
        }

        composable("cart") {
            CartScreen(
                onNavigateToMeetings = { navController.navigate("meetings") },
                onNavigateToProfile = { navController.navigate("profile") },
                onNavigateToHome = { navController.navigate("home") },
                onNavigateToBrickognize = { navController.navigate("brickognize") }
            )
        }

        composable("profile") {
            ProfileScreen(
                onNavigateToMeetings = { navController.navigate("meetings") },
                onNavigateToOrders = {},
                onNavigateToShop = {},
                onNavigateToEditProfile = { navController.navigate("edit_profile") },
                onNavigateToFeedbacks = {
                    val userId = UserPreferences.getUserId(context)
                    navController.navigate("feedbacks/$userId")
                },
                onNavigateToCreateMeeting = { navController.navigate("create_meeting") },
                onNavigateToCart = { navController.navigate("cart") },
                onNavigateToHome = { navController.navigate("home") },
                onNavigateToBrickognize = { navController.navigate("brickognize") },
                onLogout = {
                    navController.navigate("login") {
                        popUpTo("meetings") { inclusive = true }
                    }
                }
            )
        }

        composable("edit_profile") {
            EditProfileScreen(
                onBack = { navController.popBackStack() }
            )
        }

        composable("create_meeting") {
            CreateMeetingScreen(
                onBack = { navController.popBackStack() }
            )
        }

        composable("meeting_detail/{meetingId}") { backStackEntry ->
            val meetingId = backStackEntry.arguments?.getString("meetingId")?.toLongOrNull() ?: -1L
            MeetingDetailScreen(
                meetingId = meetingId,
                onBack = { navController.popBackStack() },
                onNavigateToCart = { navController.navigate("cart") }
            )
        }

        composable("feedbacks/{userId}") { backStackEntry ->
            val userId = backStackEntry.arguments?.getString("userId")?.toLongOrNull() ?: -1L
            FeedbacksScreen(
                targetUserId = userId,
                onBack = { navController.popBackStack() }
            )
        }

        composable("brickognize") {
            BrickognizeScreen(
                onBack = { navController.popBackStack() }
            )
        }
    }
}
