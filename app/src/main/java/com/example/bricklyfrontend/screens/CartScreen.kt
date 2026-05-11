package com.example.bricklyfrontend.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.bricklyfrontend.ui.theme.*
import java.time.OffsetDateTime
import java.time.format.DateTimeFormatter
import java.util.Locale

@Composable
fun CartScreen(
    onNavigateToMeetings: () -> Unit = {},
    onNavigateToProfile: () -> Unit = {},
    onNavigateToHome: () -> Unit = {},
    onNavigateToBrickognize: () -> Unit = {}
) {
    val cartItems = CartState.items

    Scaffold(
        containerColor = Background,
        topBar = {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(bottomStart = 28.dp, bottomEnd = 28.dp))
                    .background(Accent)
                    .statusBarsPadding()
                    .padding(horizontal = 20.dp, vertical = 18.dp)
            ) {
                Text(
                    text = "Корзина",
                    color = TextPrimary,
                    fontWeight = FontWeight.ExtraBold,
                    fontSize = 26.sp
                )
            }
        },
        bottomBar = {
            BricklyBottomBar(currentRoute = "cart", onNavigate = { route ->
                when (route) {
                    "meetings" -> onNavigateToMeetings()
                    "profile" -> onNavigateToProfile()
                    "home" -> onNavigateToHome()
                    "brickognize" -> onNavigateToBrickognize()
                }
            }, onScanClick = onNavigateToBrickognize)
        }
    ) { padding ->
        if (cartItems.isEmpty()) {
            Box(
                Modifier.fillMaxSize().padding(padding).padding(32.dp),
                contentAlignment = Alignment.Center
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Box(
                        modifier = Modifier
                            .size(80.dp)
                            .clip(RoundedCornerShape(24.dp))
                            .background(Accent.copy(alpha = 0.15f)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(Icons.Outlined.ShoppingCart, null, tint = Accent, modifier = Modifier.size(36.dp))
                    }
                    Spacer(Modifier.height(16.dp))
                    Text("Корзина пуста", style = MaterialTheme.typography.bodyLarge.copy(fontWeight = FontWeight.Bold), color = TextPrimary)
                    Spacer(Modifier.height(6.dp))
                    Text("Добавьте билеты на мероприятия", style = MaterialTheme.typography.bodyMedium, color = TextSecondary, textAlign = TextAlign.Center)
                }
            }
        } else {
            Column(modifier = Modifier.fillMaxSize().padding(padding)) {
                LazyColumn(
                    modifier = Modifier.weight(1f),
                    contentPadding = PaddingValues(vertical = 12.dp)
                ) {
                    items(cartItems, key = { it.meetingId }) { item ->
                        CartItemCard(item = item)
                    }
                }

                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(topStart = 28.dp, topEnd = 28.dp),
                    colors = CardDefaults.cardColors(containerColor = CardBackground),
                    elevation = CardDefaults.cardElevation(8.dp)
                ) {
                    Column(modifier = Modifier.padding(20.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column {
                                Text("Итого", style = MaterialTheme.typography.bodySmall, color = TextSecondary)
                                Text(
                                    "${CartState.totalPrice()} \u20BD",
                                    style = MaterialTheme.typography.headlineSmall.copy(fontWeight = FontWeight.ExtraBold),
                                    color = TextPrimary
                                )
                            }
                            Text(
                                "${CartState.totalTickets()} ${pluralTickets(CartState.totalTickets())}",
                                style = MaterialTheme.typography.bodyMedium,
                                color = TextSecondary
                            )
                        }

                        Spacer(Modifier.height(16.dp))

                        Button(
                            onClick = {},
                            modifier = Modifier.fillMaxWidth().height(56.dp),
                            shape = RoundedCornerShape(16.dp),
                            colors = ButtonDefaults.buttonColors(containerColor = Accent, contentColor = TextPrimary)
                        ) {
                            Text("Оплатить", style = MaterialTheme.typography.bodyLarge.copy(fontWeight = FontWeight.Bold))
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun CartItemCard(item: CartItem) {
    val dateFormatted = item.meetingDate?.let {
        try {
            val dt = OffsetDateTime.parse(it)
            dt.format(DateTimeFormatter.ofPattern("d MMM, HH:mm", Locale("ru")))
        } catch (e: Exception) {
            try {
                java.time.LocalDateTime.parse(it)
                    .format(DateTimeFormatter.ofPattern("d MMM, HH:mm", Locale("ru")))
            } catch (e2: Exception) { it }
        }
    }

    Card(
        modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 6.dp),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = CardBackground),
        elevation = CardDefaults.cardElevation(0.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(52.dp)
                    .clip(RoundedCornerShape(14.dp))
                    .background(Accent.copy(alpha = 0.15f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(Icons.Outlined.ConfirmationNumber, null, tint = Accent, modifier = Modifier.size(26.dp))
            }

            Spacer(Modifier.width(14.dp))

            Column(modifier = Modifier.weight(1f)) {
                Text(item.meetingTitle, style = MaterialTheme.typography.bodyLarge.copy(fontWeight = FontWeight.SemiBold), color = TextPrimary, maxLines = 1)
                if (dateFormatted != null) {
                    Spacer(Modifier.height(2.dp))
                    Text(dateFormatted, style = MaterialTheme.typography.bodySmall, color = TextSecondary)
                }
                Spacer(Modifier.height(2.dp))
                Text(
                    "${item.quantity} ${pluralTickets(item.quantity)} · ${item.ticketPrice * item.quantity} \u20BD",
                    style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.Medium),
                    color = TextSecondary
                )
            }

            IconButton(onClick = { CartState.removeItem(item.meetingId) }, modifier = Modifier.size(36.dp)) {
                Icon(Icons.Outlined.Close, null, tint = ErrorColor, modifier = Modifier.size(18.dp))
            }
        }
    }
}

private fun pluralTickets(n: Int): String = when {
    n % 100 in 11..19 -> "билетов"
    n % 10 == 1 -> "билет"
    n % 10 in 2..4 -> "билета"
    else -> "билетов"
}
