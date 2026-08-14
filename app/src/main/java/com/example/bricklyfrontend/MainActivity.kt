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

    fun navToProfile() {
        val popped = navController.popBackStack("profile", inclusive = false)
        if (!popped) {
            navController.navigate("profile") {
                popUpTo("meetings") { saveState = false }
                launchSingleTop = true
                restoreState = false
            }
        }
    }

    fun navToTab(route: String) {
        val tabRoot = if (UserPreferences.isLoggedIn(context)) "meetings" else startDest
        navController.navigate(route) {
            popUpTo(tabRoot) {
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
                onNavigateToChats = { navToTab("chats") },
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
                onNavigateToChats = { navToTab("chats") },
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

        composable("home_search/{query}") { backStackEntry ->
            val query = backStackEntry.arguments?.getString("query") ?: ""
            CatalogScreen(
                onNavigateToMeetings = { navToTab("meetings") },
                onNavigateToProfile = { navToTab("profile") },
                onNavigateToChats = { navToTab("chats") },
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
                },
                initialSearchQuery = query
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
                onNavigateToShop = {},
                onNavigateToEditProfile = {
                    navController.navigate("edit_profile") { launchSingleTop = true }
                },
                onNavigateToFeedbacks = {
                    val userId = UserPreferences.getUserId(context)
                    navController.navigate("feedbacks/$userId") { launchSingleTop = true }
                },
                onNavigateToMyMeetings = {
                    navController.navigate("my_meetings") { launchSingleTop = true }
                },
                onNavigateToCreateMeeting = {
                    navController.navigate("create_meeting") { launchSingleTop = true }
                },
                onNavigateToMyListings = {
                    navController.navigate("my_listings") { launchSingleTop = true }
                },
                onNavigateToMyTickets = {
                    navController.navigate("my_tickets") { launchSingleTop = true }
                },
                onNavigateToTopUp = {
                    navController.navigate("top_up") { launchSingleTop = true }
                },
                onNavigateToCreateListing = {
                    navController.navigate("create_listing") { launchSingleTop = true }
                },
                onNavigateToChats = { navToTab("chats") },
                onNavigateToHome = { navToTab("home") },
                onNavigateToBrickognize = { navToTab("brickognize") },
                onNavigateToUserPermissions = {
                    navController.navigate("user_permissions") { launchSingleTop = true }
                },
                onLogout = {
                    navController.navigate("login") {
                        popUpTo(0) { inclusive = true }
                    }
                },
                toastMessage = toast
            )
        }

        composable("my_listings") {
            MyListingsScreen(onBack = { navController.popBackStack() }, onNavigate = { if (it == "profile") navToProfile() else navToTab(it) })
        }

        composable("user_permissions") {
            UserPermissionsScreen(onBack = { navController.popBackStack() }, onNavigate = { if (it == "profile") navToProfile() else navToTab(it) })
        }

        composable("edit_profile") {
            EditProfileScreen(
                onBack = { navController.popBackStack() },
                onSaved = {
                    navController.previousBackStackEntry
                        ?.savedStateHandle
                        ?.set("toast", "Профиль обновлён")
                    navController.popBackStack()
                },
                onNavigateToMeetings = { navToTab("meetings") },
                onNavigateToChats = { navToTab("chats") },
                onNavigateToHome = { navToTab("home") },
                onNavigateToBrickognize = { navToTab("brickognize") }
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
                },
                onNavigate = { navToTab(it) }
            )
        }

        composable("create_listing") {
            CreateListingScreen(
                onNavigateBack = { navController.popBackStack() },
                onListingCreated = {
                    navController.popBackStack()
                    navToTab("home")
                },
                onNavigateToBrickognize = {
                    navController.navigate("brickognize") { launchSingleTop = true }
                },
                onNavigate = { navToTab(it) }
            )
        }

        composable("meeting_detail/{meetingId}") { backStackEntry ->
            val meetingId = backStackEntry.arguments?.getString("meetingId")?.toLongOrNull() ?: -1L
            MeetingDetailScreen(
                meetingId = meetingId,
                onBack = { navController.popBackStack() },
                onNavigateToEditProfile = {
                    navController.navigate("edit_profile") { launchSingleTop = true }
                },
                onNavigateToMyTickets = {
                    val popped = navController.popBackStack("my_tickets", inclusive = false)
                    if (!popped) navController.navigate("my_tickets") { launchSingleTop = true }
                }
            )
        }

        composable("my_tickets") {
            MyTicketsScreen(
                onBack = { navController.popBackStack() },
                onNavigateToMeetingDetail = { meetingId ->
                    navController.navigate("meeting_detail/$meetingId") { launchSingleTop = true }
                }
            )
        }

        composable("feedbacks/{userId}") { backStackEntry ->
            val userId = backStackEntry.arguments?.getString("userId")?.toLongOrNull() ?: -1L
            FeedbacksScreen(
                targetUserId = userId,
                onBack = { navController.popBackStack() },
                onNavigateToMeetings = { navToTab("meetings") },
                onNavigateToChats = { navToTab("chats") },
                onNavigateToHome = { navToTab("home") },
                onNavigateToBrickognize = { navToTab("brickognize") }
            )
        }

        composable("brickognize") {
            BrickognizeScreen(
                onNavigateToMeetings = { navToTab("meetings") },
                onNavigateToHome = { navToTab("home") },
                onNavigateToChats = { navToTab("chats") },
                onNavigateToProfile = { navToTab("profile") },
                onNavigateToListingsByItem = { itemId ->
                    navToTab("home")
                    navController.navigate("home_search/$itemId") { launchSingleTop = true }
                }
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
                onNavigateToPartDetail = { blId ->
                    navController.navigate("part_detail/$blId") { launchSingleTop = true }
                },
                onNavigateToMeetings = { navToTab("meetings") },
                onNavigateToProfile = { navToTab("profile") },
                onNavigateToChats = { navToTab("chats") },
                onOpenChat = { sellerId ->
                    navController.navigate("chat_detail/$sellerId") { launchSingleTop = true }
                },
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
                onNavigateToChats = { navToTab("chats") },
                onNavigateToBrickognize = { navToTab("brickognize") },
                onNavigateToListings = { itemId ->
                    navController.navigate("home_search/$itemId") { launchSingleTop = true }
                },
                onNavigateToSetDetail = { setId ->
                    navController.navigate("set_detail/$setId") { launchSingleTop = true }
                },
                onNavigateToPartDetail = { partBlId ->
                    navController.navigate("part_detail/$partBlId") { launchSingleTop = true }
                }
            )
        }

        composable("part_detail/{blId}") { backStackEntry ->
            val blId = backStackEntry.arguments?.getString("blId") ?: ""
            PartDetailScreen(
                blId = blId,
                onBack = { navController.popBackStack() },
                onNavigateToMeetings = { navToTab("meetings") },
                onNavigateToProfile = { navToTab("profile") },
                onNavigateToChats = { navToTab("chats") },
                onNavigateToBrickognize = { navToTab("brickognize") },
                onNavigateToListings = { itemId ->
                    navController.navigate("home_search/$itemId") { launchSingleTop = true }
                },
                onNavigateToSetDetail = { setId ->
                    navController.navigate("set_detail/$setId") { launchSingleTop = true }
                },
                onNavigateToMinifigDetail = { minifigBlId ->
                    navController.navigate("minifig_detail/$minifigBlId") { launchSingleTop = true }
                }
            )
        }

        composable("top_up") {
            TopUpScreen(
                onBack = { navController.popBackStack() },
                onSuccess = { amount ->
                    navController.navigate("top_up_success/$amount") {
                        popUpTo("top_up") { inclusive = true }
                        launchSingleTop = true
                    }
                },
                onNavigate = { navToTab(it) }
            )
        }

        composable("top_up_with_amount/{amount}") { backStackEntry ->
            val amount = backStackEntry.arguments?.getString("amount")?.toIntOrNull()
            TopUpScreen(
                onBack = { navController.popBackStack() },
                onSuccess = { finalAmount ->
                    navController.navigate("top_up_success/$finalAmount") {
                        popUpTo("top_up_with_amount/{amount}") { inclusive = true }
                        launchSingleTop = true
                    }
                },
                initialAmount = amount,
                onNavigate = { navToTab(it) }
            )
        }

        composable("top_up_success/{amount}") { backStackEntry ->
            val amount = backStackEntry.arguments?.getString("amount")?.toIntOrNull() ?: 0
            PaymentSuccessScreen(
                totalPrice = amount,
                title = "Баланс пополнен!",
                subtitle = "Средства зачислены на ваш счёт",
                buttonLabel = "В профиль",
                onGoHome = {
                    navController.navigate("profile") {
                        popUpTo(0) { inclusive = true }
                        launchSingleTop = true
                    }
                }
            )
        }

        composable("my_meetings") {
            MyMeetingsScreen(
                onBack = { navController.popBackStack() },
                onNavigateToEditMeeting = { meetingId ->
                    navController.navigate("edit_meeting/$meetingId") { launchSingleTop = true }
                },
                onNavigate = { if (it == "profile") navToProfile() else navToTab(it) }
            )
        }

        composable("edit_meeting/{meetingId}") { backStackEntry ->
            val meetingId = backStackEntry.arguments?.getString("meetingId")?.toLongOrNull() ?: -1L
            EditMeetingScreen(
                meetingId = meetingId,
                onBack = { navController.popBackStack() },
                onSaved = { navController.popBackStack() },
                onNavigate = { navToTab(it) }
            )
        }

        composable("set_detail/{setId}") { backStackEntry ->
            val setId = backStackEntry.arguments?.getString("setId") ?: ""
            SetDetailScreen(
                setId = setId,
                onBack = { navController.popBackStack() },
                onNavigateToMeetings = { navToTab("meetings") },
                onNavigateToProfile = { navToTab("profile") },
                onNavigateToChats = { navToTab("chats") },
                onNavigateToBrickognize = { navToTab("brickognize") },
                onNavigateToPartDetail = { partBlId ->
                    navController.navigate("part_detail/$partBlId") { launchSingleTop = true }
                },
                onNavigateToMinifigDetail = { minifigBlId ->
                    navController.navigate("minifig_detail/$minifigBlId") { launchSingleTop = true }
                }
            )
        }

        composable("chats") {
            ChatsScreen(
                onNavigateToMeetings = { navToTab("meetings") },
                onNavigateToHome = { navToTab("home") },
                onNavigateToProfile = { navToTab("profile") },
                onNavigateToBrickognize = { navToTab("brickognize") },
                onOpenChat = { otherUserId ->
                    navController.navigate("chat_detail/$otherUserId") { launchSingleTop = true }
                }
            )
        }

        composable("chat_detail/{userId}") { backStackEntry ->
            val otherUserId = backStackEntry.arguments?.getString("userId")?.toLongOrNull() ?: -1L
            ChatDetailScreen(
                otherUserId = otherUserId,
                onBack = { navController.popBackStack() }
            )
        }
    }
}
