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
import com.example.bricklyfrontend.screens.ListingCartScreen
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
                onNavigateToBrickognize = { navToTab("brickognize") },
                onNavigateToCreateMeeting = {
                    navController.navigate("create_meeting") { launchSingleTop = true }
                }
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
                onNavigateToBrickognize = { navToTab("brickognize") },
                onNavigateToCreateListing = {
                    navController.navigate("create_listing") { launchSingleTop = true }
                },
                onNavigateToListingDetail = { listingId ->
                    navController.navigate("listing_detail/$listingId") { launchSingleTop = true }
                },
                onNavigateToSetDetail = { setId ->
                    navController.navigate("set_detail/$setId") { launchSingleTop = true }
                }
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
            val savedState = navController.currentBackStackEntry?.savedStateHandle
            val toast = savedState?.get<String>("toast")
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
                onNavigateToCreateListing = {
                    navController.navigate("create_listing") { launchSingleTop = true }
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
                    navController.previousBackStackEntry
                        ?.savedStateHandle
                        ?.set("toast", "Профиль обновлён")
                    navController.popBackStack()
                }
            )
        }

        composable("create_meeting") {
            val ctx = LocalContext.current
            CreateMeetingScreen(
                onBack = { navController.popBackStack() },
                onMeetingCreated = {
                    android.widget.Toast.makeText(ctx, "Сходка создана", android.widget.Toast.LENGTH_SHORT).show()
                    navController.navigate("meetings") {
                        popUpTo("create_meeting") { inclusive = true }
                        launchSingleTop = true
                    }
                }
            )
        }

        composable("create_listing") {
            CreateListingScreen(
                onNavigateBack = { navController.popBackStack() },
                onListingCreated = {
                    navController.popBackStack()
                    navToTab("home")
                }
            )
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

        composable("listing_detail/{listingId}") { backStackEntry ->
            val listingId = backStackEntry.arguments?.getString("listingId")?.toLongOrNull() ?: -1L
            ListingDetailScreen(
                listingId = listingId,
                onBack = { navController.popBackStack() },
                onNavigateToSetDetail = { setId ->
                    navController.navigate("set_detail/$setId") { launchSingleTop = true }
                },
                onNavigateToMinifigDetail = { blId ->
                    navController.navigate("minifig_detail/$blId") { launchSingleTop = true }
                },
                onNavigateToMeetings = { navToTab("meetings") },
                onNavigateToProfile = { navToTab("profile") },
                onNavigateToCart = { navToTab("cart") },
                onNavigateToBrickognize = { navToTab("brickognize") }
            )
        }

        composable("minifig_detail/{blId}") { backStackEntry ->
            val blId = backStackEntry.arguments?.getString("blId") ?: ""
            MinifigDetailScreen(
                blId = blId,
                onBack = { navController.popBackStack() },
                onNavigateToMeetings = { navToTab("meetings") },
                onNavigateToProfile = { navToTab("profile") },
                onNavigateToCart = { navToTab("cart") },
                onNavigateToBrickognize = { navToTab("brickognize") }
            )
        }

        composable("listing_cart") {
            ListingCartScreen(onBack = { navController.popBackStack() })
        }

        composable("set_detail/{setId}") { backStackEntry ->
            val setId = backStackEntry.arguments?.getString("setId") ?: ""
            SetDetailScreen(
                setId = setId,
                onBack = { navController.popBackStack() },
                onNavigateToMeetings = { navToTab("meetings") },
                onNavigateToProfile = { navToTab("profile") },
                onNavigateToCart = { navToTab("cart") },
                onNavigateToBrickognize = { navToTab("brickognize") }
            )
        }
    }
}
