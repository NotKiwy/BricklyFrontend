package com.example.bricklyfrontend.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.bricklyfrontend.data.RetrofitClient
import com.example.bricklyfrontend.data.UserPreferences
import com.example.bricklyfrontend.ui.theme.*
import kotlinx.coroutines.launch

@Composable
fun ProfileScreen(
    onNavigateToMeetings: () -> Unit = {},
    onNavigateToOrders: () -> Unit = {},
    onNavigateToShop: () -> Unit = {},
    onNavigateToEditProfile: () -> Unit = {},
    onNavigateToFeedbacks: () -> Unit = {},
    onNavigateToMyMeetings: () -> Unit = {},
    onNavigateToCreateMeeting: () -> Unit = {},
    onNavigateToCreateListing: () -> Unit = {},
    onNavigateToMyListings: () -> Unit = {},
    onNavigateToMySales: () -> Unit = {},
    onNavigateToMyTickets: () -> Unit = {},
    onNavigateToTopUp: () -> Unit = {},
    onNavigateToCart: () -> Unit = {},
    onNavigateToHome: () -> Unit = {},
    onNavigateToBrickognize: () -> Unit = {},
    onNavigateToUserPermissions: () -> Unit = {},
    onLogout: () -> Unit = {},
    toastMessage: String? = null
) {
    val context = LocalContext.current
    val snackbarHostState = remember { SnackbarHostState() }

    SetStatusBarColor(Accent)

    val userId = remember { UserPreferences.getUserId(context) }
    val savedUsername = remember { UserPreferences.getUsername(context) }
    var role by remember { mutableStateOf(UserPreferences.getRole(context)) }
    val canCreateMeeting by remember { derivedStateOf {
        val r = role.split(",")
        "ROLE_ADMIN" in r || "ROLE_MEETING_CREATOR" in r || "ROLE_SUPERADMIN" in r
    } }
    val isSeller by remember { derivedStateOf {
        val r = role.split(",")
        "ROLE_SELLER" in r || "ROLE_ADMIN" in r || "ROLE_SUPERADMIN" in r
    } }
    val isSuperAdmin by remember { derivedStateOf { "ROLE_SUPERADMIN" in role.split(",") } }

    var balance by remember { mutableStateOf<Int?>(null) }
    var showLogoutDialog by remember { mutableStateOf(false) }
    LaunchedEffect(userId) {
        try {
            val resp = RetrofitClient.api.getUserById(userId)
            if (resp.isSuccessful) {
                val user = resp.body()
                balance = user?.balance
                val serverRole = UserPreferences.extractRole(user?.authorities)
                if (serverRole != role) {
                    UserPreferences.saveUser(
                        context = context,
                        id = userId,
                        username = savedUsername,
                        password = UserPreferences.getPassword(context),
                        role = serverRole
                    )
                    role = serverRole
                }
            }
        } catch (_: Exception) {}
    }

    if (showLogoutDialog) {
        AlertDialog(
            onDismissRequest = { showLogoutDialog = false },
            containerColor = CardBackground,
            shape = RoundedCornerShape(24.dp),
            title = { Text("Выход из аккаунта", fontWeight = FontWeight.Bold, color = TextPrimary) },
            text = { Text("Вы действительно хотите выйти?", color = TextSecondary) },
            confirmButton = {
                Button(
                    onClick = {
                        showLogoutDialog = false
                        UserPreferences.clear(context)
                        onLogout()
                    },
                    shape = RoundedCornerShape(12.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = ErrorColor, contentColor = Color.White)
                ) { Text("Выйти", fontWeight = FontWeight.SemiBold) }
            },
            dismissButton = {
                TextButton(onClick = { showLogoutDialog = false }) {
                    Text("Отмена", color = TextSecondary)
                }
            }
        )
    }

    LaunchedEffect(toastMessage) {
        if (!toastMessage.isNullOrBlank()) {
            snackbarHostState.showSnackbar(toastMessage)
        }
    }


    Scaffold(
        containerColor = Background,
        snackbarHost = { SnackbarHost(snackbarHostState) },
        topBar = {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(bottomStart = 28.dp, bottomEnd = 28.dp))
                    .background(Accent)
                    .statusBarsPadding()
                    .padding(horizontal = 20.dp)
                    .padding(top = 18.dp, bottom = 24.dp),
                contentAlignment = Alignment.Center
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Box(
                        modifier = Modifier
                            .size(72.dp)
                            .clip(CircleShape)
                            .background(TextPrimary),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = savedUsername.take(1).uppercase(),
                            fontSize = 28.sp,
                            fontWeight = FontWeight.Bold,
                            color = Accent
                        )
                    }
                    Spacer(Modifier.height(10.dp))
                    Text(
                        text = savedUsername,
                        fontSize = 22.sp,
                        fontWeight = FontWeight.ExtraBold,
                        color = TextPrimary
                    )
                    Spacer(Modifier.height(12.dp))
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Surface(
                            shape = RoundedCornerShape(12.dp),
                            color = TextPrimary.copy(alpha = 0.15f)
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 14.dp, vertical = 7.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                Icon(Icons.Outlined.AccountBalanceWallet, null, tint = TextPrimary, modifier = Modifier.size(16.dp))
                                Text(
                                    text = if (balance != null) "${balance} ₽" else "—",
                                    fontSize = 15.sp,
                                    fontWeight = FontWeight.SemiBold,
                                    color = TextPrimary
                                )
                            }
                        }
                        Surface(
                            shape = RoundedCornerShape(12.dp),
                            color = TextPrimary,
                            onClick = onNavigateToTopUp
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 14.dp, vertical = 7.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                Icon(Icons.Outlined.Add, null, tint = Accent, modifier = Modifier.size(16.dp))
                                Text("Пополнить", fontSize = 14.sp, fontWeight = FontWeight.SemiBold, color = Accent)
                            }
                        }
                    }
                }
            }
        },
        bottomBar = {
            BricklyBottomBar(currentRoute = "profile", onNavigate = { route ->
                when (route) {
                    "meetings" -> onNavigateToMeetings()
                    "cart" -> onNavigateToCart()
                    "home" -> onNavigateToHome()
                    "brickognize" -> onNavigateToBrickognize()
                }
            }, onScanClick = onNavigateToBrickognize)
        }
    ) { padding ->
        Column(
            modifier = Modifier.fillMaxSize().padding(padding).verticalScroll(rememberScrollState()),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Spacer(Modifier.height(20.dp))

            Card(
                modifier = Modifier.fillMaxWidth().padding(horizontal = 20.dp),
                shape = RoundedCornerShape(20.dp),
                colors = CardDefaults.cardColors(containerColor = CardBackground),
                elevation = CardDefaults.cardElevation(0.dp)
            ) {
                Column {
                    ProfileMenuItem(icon = Icons.Outlined.ConfirmationNumber, text = "Мои билеты", onClick = onNavigateToMyTickets, showDivider = true)
                    if (canCreateMeeting) {
                        ProfileMenuItem(icon = Icons.Outlined.Event, text = "Мои мероприятия", onClick = onNavigateToMyMeetings, showDivider = true)
                    }
                    ProfileMenuItem(icon = Icons.Outlined.ShoppingBag, text = "Заказы", onClick = onNavigateToOrders, showDivider = true)
                    if (isSeller) {
                        ProfileMenuItem(icon = Icons.Outlined.Storefront, text = "Мой магазин", onClick = onNavigateToMyListings, showDivider = true)
                        ProfileMenuItem(icon = Icons.Outlined.LocalShipping, text = "Мои продажи", onClick = onNavigateToMySales, showDivider = true)
                    }
                    ProfileMenuItem(icon = Icons.Outlined.Star, text = "Отзывы", onClick = onNavigateToFeedbacks, showDivider = true)
                    ProfileMenuItem(icon = Icons.Outlined.Edit, text = "Изменить профиль", onClick = onNavigateToEditProfile, showDivider = true)
                    if (isSuperAdmin) {
                        ProfileMenuItem(icon = Icons.Outlined.AdminPanelSettings, text = "Управление ролями", onClick = onNavigateToUserPermissions, showDivider = true)
                    }
                    ProfileMenuItem(
                        icon = Icons.Outlined.Logout,
                        text = "Выйти из аккаунта",
                        onClick = { showLogoutDialog = true },
                        showDivider = false,
                        isDestructive = true
                    )
                }
            }

            Spacer(Modifier.height(24.dp))
        }
    }
}

@Composable
private fun ProfileMenuItem(
    icon: ImageVector,
    text: String,
    onClick: () -> Unit,
    showDivider: Boolean,
    isDestructive: Boolean = false
) {
    val contentColor = if (isDestructive) ErrorColor else TextPrimary

    Column {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clickable(onClick = onClick)
                .padding(horizontal = 20.dp, vertical = 16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = if (isDestructive) ErrorColor else IconInactive,
                modifier = Modifier.size(22.dp)
            )
            Spacer(Modifier.width(16.dp))
            Text(
                text = text,
                style = MaterialTheme.typography.bodyLarge.copy(fontWeight = FontWeight.Medium),
                color = contentColor,
                modifier = Modifier.weight(1f)
            )
            Icon(
                imageVector = Icons.Outlined.ChevronRight,
                contentDescription = null,
                tint = if (isDestructive) ErrorColor.copy(alpha = 0.4f) else IconInactive,
                modifier = Modifier.size(20.dp)
            )
        }
        if (showDivider) {
            HorizontalDivider(
                modifier = Modifier.padding(start = 58.dp, end = 20.dp),
                color = Divider,
                thickness = 1.dp
            )
        }
    }
}
