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

        composable("home_search/{query}") { backStackEntry ->
            val query = backStackEntry.arguments?.getString("query") ?: ""
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
                },
                initialSearchQuery = query
            )
        }

        composable("cart") {
            CartScreen(
                onNavigateToMeetings = { navToTab("meetings") },
                onNavigateToProfile = { navToTab("profile") },
                onNavigateToHome = { navToTab("home") },
                onNavigateToBrickognize = { navToTab("brickognize") },
                onNavigateToCheckout = { totalPrice ->
                    navController.navigate("checkout/$totalPrice") { launchSingleTop = true }
                },
                onNavigateToListingDetail = { listingId ->
                    navController.navigate("listing_detail/$listingId") { launchSingleTop = true }
                },
                onNavigateToMeetingDetail = { meetingId ->
                    navController.navigate("meeting_detail/$meetingId") { launchSingleTop = true }
                }
            )
        }

        composable("checkout/{totalPrice}") { backStackEntry ->
            val totalPrice = backStackEntry.arguments?.getString("totalPrice")?.toIntOrNull() ?: 0
            CheckoutScreen(
                onBack = { navController.popBackStack() },
                onPaymentSuccess = {
                    navController.navigate("payment_success/$totalPrice") {
                        popUpTo("cart") { inclusive = true }
                        launchSingleTop = true
                    }
                },
                onNavigateToTopUp = { deficit ->
                    navController.navigate("top_up_with_amount/$deficit") {
                        launchSingleTop = true
                    }
                }
            )
        }

        composable("payment_success/{totalPrice}") { backStackEntry ->
            val totalPrice = backStackEntry.arguments?.getString("totalPrice")?.toIntOrNull() ?: 0
            PaymentSuccessScreen(
                totalPrice = totalPrice,
                onGoHome = {
                    navController.navigate("home") {
                        popUpTo(0) { inclusive = true }
                        launchSingleTop = true
                    }
                }
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
                onNavigateToOrders = {
                    navController.navigate("my_orders") { launchSingleTop = true }
                },
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
                onNavigateToMySales = {
                    navController.navigate("my_sales") { launchSingleTop = true }
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

        composable("my_listings") {
            MyListingsScreen(onBack = { navController.popBackStack() })
        }

        composable("my_sales") {
            MySalesScreen(onBack = { navController.popBackStack() })
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
                onNavigateToCart = { navToTab("cart") },
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
                }
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
                }
            )
        }

        composable("meeting_detail/{meetingId}") { backStackEntry ->
            val meetingId = backStackEntry.arguments?.getString("meetingId")?.toLongOrNull() ?: -1L
            MeetingDetailScreen(
                meetingId = meetingId,
                onBack = { navController.popBackStack() },
                onNavigateToCart = { navToTab("cart") },
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

        composable("my_orders") {
            MyOrdersScreen(
                onBack = { navController.popBackStack() },
                onNavigateToOrderDetail = { orderId ->
                    navController.navigate("order_detail/$orderId") { launchSingleTop = true }
                }
            )
        }

        composable("order_detail/{orderId}") { backStackEntry ->
            val orderId = backStackEntry.arguments?.getString("orderId")?.toLongOrNull() ?: 0L
            OrderDetailScreen(
                orderId = orderId,
                onBack = { navController.popBackStack() }
            )
        }

        composable("feedbacks/{userId}") { backStackEntry ->
            val userId = backStackEntry.arguments?.getString("userId")?.toLongOrNull() ?: -1L
            FeedbacksScreen(
                targetUserId = userId,
                onBack = { navController.popBackStack() },
                onNavigateToMeetings = { navToTab("meetings") },
                onNavigateToCart = { navToTab("cart") },
                onNavigateToHome = { navToTab("home") },
                onNavigateToBrickognize = { navToTab("brickognize") }
            )
        }

        composable("brickognize") {
            BrickognizeScreen(
                onNavigateToMeetings = { navToTab("meetings") },
                onNavigateToHome = { navToTab("home") },
                onNavigateToCart = { navToTab("cart") },
                onNavigateToProfile = { navToTab("profile") },
                onNavigateToListingsByItem = { itemId ->
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
                onNavigateToCart = { navToTab("cart") },
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
                }
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
                initialAmount = amount
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

        composable("listing_cart") {
            ListingCartScreen(onBack = { navController.popBackStack() })
        }

        composable("my_meetings") {
            MyMeetingsScreen(
                onBack = { navController.popBackStack() },
                onNavigateToEditMeeting = { meetingId ->
                    navController.navigate("edit_meeting/$meetingId") { launchSingleTop = true }
                }
            )
        }

        composable("edit_meeting/{meetingId}") { backStackEntry ->
            val meetingId = backStackEntry.arguments?.getString("meetingId")?.toLongOrNull() ?: -1L
            EditMeetingScreen(
                meetingId = meetingId,
                onBack = { navController.popBackStack() },
                onSaved = { navController.popBackStack() }
            )
        }

        composable("set_detail/{setId}") { backStackEntry ->
            val setId = backStackEntry.arguments?.getString("setId") ?: ""
            SetDetailScreen(
                setId = setId,
                onBack = { navController.popBackStack() },
                onNavigateToMeetings = { navToTab("meetings") },
                onNavigateToProfile = { navToTab("profile") },
                onNavigateToCart = { navToTab("cart") },
                onNavigateToBrickognize = { navToTab("brickognize") },
                onNavigateToPartDetail = { partBlId ->
                    navController.navigate("part_detail/$partBlId") { launchSingleTop = true }
                },
                onNavigateToMinifigDetail = { minifigBlId ->
                    navController.navigate("minifig_detail/$minifigBlId") { launchSingleTop = true }
                }
            )
        }
    }
}
