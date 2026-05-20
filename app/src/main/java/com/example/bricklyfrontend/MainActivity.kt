package com.example.bricklyfrontend

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.SystemBarStyle
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.animation.*
import androidx.compose.animation.core.tween
import androidx.compose.runtime.*
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalContext
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.example.bricklyfrontend.data.RetrofitClient
import com.example.bricklyfrontend.data.UserPreferences
import com.example.bricklyfrontend.screens.*
import com.example.bricklyfrontend.ui.theme.Accent
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

    fun navToTab(route: String) {
        navController.navigate(route) {
            popUpTo(navController.graph.findStartDestination().id) {
                saveState = true
            }
            launchSingleTop = true
            restoreState = true
        }
    }

    NavHost(
        navController = navController,
        startDestination = startDest,
        enterTransition = {
            fadeIn(animationSpec = tween(NAV_ANIM_DURATION)) +
                    slideInHorizontally(animationSpec = tween(NAV_ANIM_DURATION), initialOffsetX = { it / 6 })
        },
        exitTransition = {
            fadeOut(animationSpec = tween(NAV_ANIM_DURATION)) +
                    slideOutHorizontally(animationSpec = tween(NAV_ANIM_DURATION), targetOffsetX = { -it / 6 })
        },
        popEnterTransition = {
            fadeIn(animationSpec = tween(NAV_ANIM_DURATION)) +
                    slideInHorizontally(animationSpec = tween(NAV_ANIM_DURATION), initialOffsetX = { -it / 6 })
        },
        popExitTransition = {
            fadeOut(animationSpec = tween(NAV_ANIM_DURATION)) +
                    slideOutHorizontally(animationSpec = tween(NAV_ANIM_DURATION), targetOffsetX = { it / 6 })
        }
    ) {
        composable("login") {
            LoginScreen(
                onNavigateToRegister = { navController.navigate("register") { launchSingleTop = true } },
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
                onNavigateToProfile = { navToTab("profile") },
                onNavigateToMeetingDetail = { meetingId ->
                    navController.navigate("meeting_detail/$meetingId") { launchSingleTop = true }
                },
                onNavigateToCart = { navToTab("cart") },
                onNavigateToHome = { navToTab("home") },
                onNavigateToBrickognize = { navToTab("brickognize") }
            )
        }

        composable("home") {
            CatalogScreen(
                onNavigateToMeetings = { navToTab("meetings") },
                onNavigateToProfile = { navToTab("profile") },
                onNavigateToCart = { navToTab("cart") },
                onNavigateToMeetingDetail = { meetingId ->
                    navController.navigate("meeting_detail/$meetingId") { launchSingleTop = true }
                },
                onNavigateToBrickognize = { navToTab("brickognize") }
            )
        }

        composable("cart") {
            CartScreen(
                onNavigateToMeetings = { navToTab("meetings") },
                onNavigateToProfile = { navToTab("profile") },
                onNavigateToHome = { navToTab("home") },
                onNavigateToBrickognize = { navToTab("brickognize") }
            )
        }

        composable("profile") {
            // Читаем toast-аргумент из savedStateHandle (кладём туда из edit_profile)
            val savedState = navController.currentBackStackEntry?.savedStateHandle
            val toast = savedState?.get<String>("toast")
            // После показа сбрасываем чтобы не показывался повторно
            LaunchedEffect(toast) {
                if (!toast.isNullOrBlank()) {
                    savedState?.remove<String>("toast")
                }
            }
            ProfileScreen(
                onNavigateToMeetings = { navToTab("meetings") },
                onNavigateToOrders = {},
                onNavigateToShop = {},
                onNavigateToEditProfile = {
                    navController.navigate("edit_profile") { launchSingleTop = true }
                },
                onNavigateToFeedbacks = {
                    val userId = UserPreferences.getUserId(context)
                    navController.navigate("feedbacks/$userId") { launchSingleTop = true }
                },
                onNavigateToCreateMeeting = {
                    navController.navigate("create_meeting") { launchSingleTop = true }
                },
                onNavigateToCart = { navToTab("cart") },
                onNavigateToHome = { navToTab("home") },
                onNavigateToBrickognize = { navToTab("brickognize") },
                onLogout = {
                    navController.navigate("login") {
                        popUpTo(0) { inclusive = true }
                    }
                },
                toastMessage = toast
            )
        }

        composable("edit_profile") {
            EditProfileScreen(
                onBack = { navController.popBackStack() },
                onSaved = {
                    // Кладём toast в предыдущий экран (profile) и возвращаемся
                    navController.previousBackStackEntry
                        ?.savedStateHandle
                        ?.set("toast", "Профиль обновлён")
                    navController.popBackStack()
                }
            )
        }

        composable("create_meeting") {
            CreateMeetingScreen(onBack = { navController.popBackStack() })
        }

        composable("meeting_detail/{meetingId}") { backStackEntry ->
            val meetingId = backStackEntry.arguments?.getString("meetingId")?.toLongOrNull() ?: -1L
            MeetingDetailScreen(
                meetingId = meetingId,
                onBack = { navController.popBackStack() },
                onNavigateToCart = { navToTab("cart") }
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
                onNavigateToMeetings = { navToTab("meetings") },
                onNavigateToHome = { navToTab("home") },
                onNavigateToCart = { navToTab("cart") },
                onNavigateToProfile = { navToTab("profile") }
            )
        }
    }
}
