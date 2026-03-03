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
import com.example.bricklyfrontend.data.AppConfig
import com.example.bricklyfrontend.data.RetrofitClient
import com.example.bricklyfrontend.data.UserPreferences
import com.example.bricklyfrontend.ui.theme.*

@Composable
fun ProfileScreen(
    onNavigateToMeetings: () -> Unit = {},
    onNavigateToOrders: () -> Unit = {},
    onNavigateToShop: () -> Unit = {},
    onNavigateToEditProfile: () -> Unit = {},
    onLogout: () -> Unit = {}
) {
    val context = LocalContext.current

    val userId = remember { UserPreferences.getUserId(context) }
    val savedUsername = remember { UserPreferences.getUsername(context) }

    var username by remember { mutableStateOf(savedUsername) }
    var name by remember { mutableStateOf<String?>(null) }
    var isLoading by remember { mutableStateOf(false) }

    LaunchedEffect(userId) {
        if (!AppConfig.debugMode && userId != -1L) {
            isLoading = true
            try {
                val response = RetrofitClient.api.getUserById(userId)
                if (response.isSuccessful) {
                    val user = response.body()
                    username = user?.username ?: savedUsername
                    name = user?.name
                }
            } catch (_: Exception) {}
            isLoading = false
        }
    }

    Scaffold(
        containerColor = Background,
        bottomBar = { BricklyBottomBar(currentRoute = "profile") }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .verticalScroll(rememberScrollState()),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Spacer(Modifier.height(48.dp))

            Box(
                modifier = Modifier
                    .size(90.dp)
                    .clip(CircleShape)
                    .background(Color(0xFFD9D9D9)),
                contentAlignment = Alignment.Center
            ) {
                if (isLoading) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(32.dp),
                        color = Accent,
                        strokeWidth = 2.dp
                    )
                }
            }

            Spacer(Modifier.height(14.dp))

            Text(
                text = if (!name.isNullOrBlank()) name!! else username,
                style = MaterialTheme.typography.headlineMedium.copy(
                    fontWeight = FontWeight.ExtraBold,
                    fontSize = 26.sp
                ),
                color = TextPrimary
            )

            Spacer(Modifier.height(28.dp))

            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp),
                shape = RoundedCornerShape(20.dp),
                colors = CardDefaults.cardColors(containerColor = CardBackground),
                elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
                border = androidx.compose.foundation.BorderStroke(1.5.dp, Accent)
            ) {
                Column {
                    ProfileMenuItem(
                        icon = Icons.Outlined.CalendarMonth,
                        text = "Записи на мероприятия",
                        onClick = onNavigateToMeetings,
                        showDivider = true
                    )
                    ProfileMenuItem(
                        icon = Icons.Outlined.ShoppingBag,
                        text = "Заказы",
                        onClick = onNavigateToOrders,
                        showDivider = true
                    )
                    ProfileMenuItem(
                        icon = Icons.Outlined.Storefront,
                        text = "Мой магазин",
                        onClick = onNavigateToShop,
                        showDivider = true
                    )
                    ProfileMenuItem(
                        icon = Icons.Outlined.Edit,
                        text = "Изменить профиль",
                        onClick = onNavigateToEditProfile,
                        showDivider = true
                    )
                    ProfileMenuItem(
                        icon = Icons.Outlined.Logout,
                        text = "Выйти из аккаунта",
                        onClick = {
                            UserPreferences.clear(context)
                            onLogout()
                        },
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
            Box(
                modifier = Modifier
                    .size(36.dp)
                    .clip(RoundedCornerShape(10.dp))
                    .background(
                        if (isDestructive) ErrorColor.copy(alpha = 0.08f)
                        else Accent.copy(alpha = 0.15f)
                    ),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = contentColor,
                    modifier = Modifier.size(18.dp)
                )
            }

            Spacer(Modifier.width(14.dp))

            Text(
                text = text,
                style = MaterialTheme.typography.bodyLarge.copy(fontWeight = FontWeight.Medium),
                color = contentColor,
                modifier = Modifier.weight(1f)
            )

            Icon(
                imageVector = Icons.Outlined.ChevronRight,
                contentDescription = null,
                tint = if (isDestructive) ErrorColor.copy(alpha = 0.4f) else TextSecondary,
                modifier = Modifier.size(20.dp)
            )
        }

        if (showDivider) {
            HorizontalDivider(
                modifier = Modifier.padding(start = 70.dp, end = 20.dp),
                color = Divider,
                thickness = 1.dp
            )
        }
    }
}
