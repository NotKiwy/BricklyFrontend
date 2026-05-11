package com.example.bricklyfrontend.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Remove
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.bricklyfrontend.data.MeetingDefaultDTO
import com.example.bricklyfrontend.data.RetrofitClient
import com.example.bricklyfrontend.ui.theme.*
import java.time.OffsetDateTime
import java.time.format.DateTimeFormatter
import java.util.Locale

data class CartItem(
    val meetingId: Long,
    val meetingTitle: String,
    val meetingDate: String?,
    val meetingAddress: String?,
    val ticketPrice: Int,
    val quantity: Int
)

object CartState {
    var items by mutableStateOf<List<CartItem>>(emptyList())

    fun addItem(item: CartItem) {
        val existing = items.find { it.meetingId == item.meetingId }
        items = if (existing != null) {
            items.map { if (it.meetingId == item.meetingId) it.copy(quantity = it.quantity + item.quantity) else it }
        } else {
            items + item
        }
    }

    fun removeItem(meetingId: Long) { items = items.filter { it.meetingId != meetingId } }
    fun totalPrice(): Int = items.sumOf { it.ticketPrice * it.quantity }
    fun totalTickets(): Int = items.sumOf { it.quantity }
}

@Composable
fun MeetingDetailScreen(
    meetingId: Long,
    onBack: () -> Unit,
    onNavigateToCart: () -> Unit
) {
    var meeting by remember { mutableStateOf<MeetingDefaultDTO?>(null) }
    var isLoading by remember { mutableStateOf(true) }
    var errorMessage by remember { mutableStateOf<String?>(null) }
    var ticketCount by remember { mutableIntStateOf(1) }
    var addedToCart by remember { mutableStateOf(false) }

    LaunchedEffect(meetingId) {
        try {
            val response = RetrofitClient.api.getMeetingById(meetingId)
            if (response.isSuccessful) meeting = response.body()
            else errorMessage = "Ошибка загрузки (${response.code()})"
        } catch (e: Exception) {
            errorMessage = "Нет соединения с сервером"
        }
        isLoading = false
    }

    Scaffold(
        containerColor = Background,
        topBar = {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(bottomStart = 28.dp, bottomEnd = 28.dp))
                    .background(Accent)
                    .statusBarsPadding()
                    .padding(horizontal = 8.dp, vertical = 12.dp)
            ) {
                IconButton(onClick = onBack, modifier = Modifier.align(Alignment.CenterStart)) {
                    Icon(Icons.Outlined.ArrowBackIosNew, "Назад", tint = TextPrimary)
                }
                Text(
                    text = "Подробности",
                    color = TextPrimary,
                    fontWeight = FontWeight.Bold,
                    fontSize = 18.sp,
                    modifier = Modifier.align(Alignment.Center)
                )
            }
        }
    ) { padding ->
        when {
            isLoading -> Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) { CircularProgressIndicator(color = Accent) }

            errorMessage != null -> Box(Modifier.fillMaxSize().padding(32.dp), contentAlignment = Alignment.Center) {
                Text(errorMessage!!, color = ErrorColor, textAlign = TextAlign.Center)
            }

            meeting != null -> {
                val m = meeting!!
                val dateFormatted = m.date?.let { formatDetailDate(it) }

                Column(modifier = Modifier.fillMaxSize().padding(padding).verticalScroll(rememberScrollState())) {
                    Spacer(Modifier.height(16.dp))

                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(200.dp)
                            .padding(horizontal = 20.dp)
                            .clip(RoundedCornerShape(24.dp))
                            .background(Accent.copy(alpha = 0.12f)),
                        contentAlignment = Alignment.Center
                    ) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Icon(Icons.Outlined.Map, null, tint = Accent, modifier = Modifier.size(48.dp))
                            Spacer(Modifier.height(8.dp))
                            Text(m.address ?: "Адрес не указан", style = MaterialTheme.typography.bodyMedium, color = TextSecondary, textAlign = TextAlign.Center, modifier = Modifier.padding(horizontal = 16.dp))
                        }
                    }

                    Spacer(Modifier.height(16.dp))

                    Card(
                        modifier = Modifier.fillMaxWidth().padding(horizontal = 20.dp),
                        shape = RoundedCornerShape(24.dp),
                        colors = CardDefaults.cardColors(containerColor = CardBackground),
                        elevation = CardDefaults.cardElevation(0.dp)
                    ) {
                        Column(modifier = Modifier.padding(20.dp)) {
                            Text(
                                text = m.type?.description ?: m.description?.take(50) ?: "Мероприятие",
                                style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.ExtraBold),
                                color = TextPrimary
                            )
                            Spacer(Modifier.height(16.dp))

                            InfoRow(icon = Icons.Outlined.CalendarMonth, text = dateFormatted ?: "Дата не указана")
                            Spacer(Modifier.height(10.dp))
                            InfoRow(icon = Icons.Outlined.LocationOn, text = m.address ?: "Адрес не указан")
                            Spacer(Modifier.height(10.dp))
                            InfoRow(
                                icon = Icons.Outlined.ConfirmationNumber,
                                text = if (m.ticketPrice != null && m.ticketPrice > 0) "${m.ticketPrice} \u20BD" else "Бесплатно",
                                bold = true
                            )
                        }
                    }

                    if (!m.description.isNullOrBlank()) {
                        Spacer(Modifier.height(12.dp))
                        Card(
                            modifier = Modifier.fillMaxWidth().padding(horizontal = 20.dp),
                            shape = RoundedCornerShape(24.dp),
                            colors = CardDefaults.cardColors(containerColor = CardBackground),
                            elevation = CardDefaults.cardElevation(0.dp)
                        ) {
                            Column(modifier = Modifier.padding(20.dp)) {
                                Text("Описание", style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold), color = TextPrimary)
                                Spacer(Modifier.height(8.dp))
                                Text(m.description, style = MaterialTheme.typography.bodyMedium, color = TextSecondary, lineHeight = 22.sp)
                            }
                        }
                    }

                    Spacer(Modifier.height(12.dp))

                    Card(
                        modifier = Modifier.fillMaxWidth().padding(horizontal = 20.dp),
                        shape = RoundedCornerShape(24.dp),
                        colors = CardDefaults.cardColors(containerColor = CardBackground),
                        elevation = CardDefaults.cardElevation(0.dp)
                    ) {
                        Column(modifier = Modifier.padding(20.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                            Text("Количество билетов", style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold), color = TextPrimary)
                            Spacer(Modifier.height(16.dp))

                            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.Center) {
                                IconButton(
                                    onClick = { if (ticketCount > 1) ticketCount-- },
                                    modifier = Modifier.size(44.dp).clip(RoundedCornerShape(14.dp)).background(Color(0xFFF0F0F0))
                                ) {
                                    Icon(Icons.Filled.Remove, null, tint = TextPrimary)
                                }
                                Text("$ticketCount", style = MaterialTheme.typography.headlineMedium.copy(fontWeight = FontWeight.ExtraBold), color = TextPrimary, modifier = Modifier.padding(horizontal = 28.dp))
                                IconButton(
                                    onClick = { ticketCount++ },
                                    modifier = Modifier.size(44.dp).clip(RoundedCornerShape(14.dp)).background(Accent)
                                ) {
                                    Icon(Icons.Filled.Add, null, tint = TextPrimary)
                                }
                            }

                            if (m.ticketPrice != null && m.ticketPrice > 0) {
                                Spacer(Modifier.height(8.dp))
                                Text("Итого: ${m.ticketPrice * ticketCount} \u20BD", style = MaterialTheme.typography.bodyLarge.copy(fontWeight = FontWeight.SemiBold), color = TextSecondary)
                            }

                            Spacer(Modifier.height(16.dp))

                            if (addedToCart) {
                                Button(
                                    onClick = onNavigateToCart,
                                    modifier = Modifier.fillMaxWidth().height(52.dp),
                                    shape = RoundedCornerShape(14.dp),
                                    colors = ButtonDefaults.buttonColors(containerColor = TextPrimary, contentColor = Accent)
                                ) {
                                    Icon(Icons.Outlined.ShoppingBag, null, modifier = Modifier.size(18.dp))
                                    Spacer(Modifier.width(8.dp))
                                    Text("Перейти в корзину", fontWeight = FontWeight.SemiBold)
                                }
                            } else {
                                Button(
                                    onClick = {
                                        CartState.addItem(CartItem(meetingId = m.id, meetingTitle = m.type?.description ?: m.description?.take(30) ?: "Мероприятие", meetingDate = m.date, meetingAddress = m.address, ticketPrice = m.ticketPrice ?: 0, quantity = ticketCount))
                                        addedToCart = true
                                    },
                                    modifier = Modifier.fillMaxWidth().height(52.dp),
                                    shape = RoundedCornerShape(14.dp),
                                    colors = ButtonDefaults.buttonColors(containerColor = Accent, contentColor = TextPrimary)
                                ) {
                                    Icon(Icons.Outlined.ShoppingCart, null, modifier = Modifier.size(18.dp))
                                    Spacer(Modifier.width(8.dp))
                                    Text("В корзину", fontWeight = FontWeight.SemiBold)
                                }
                            }
                        }
                    }

                    Spacer(Modifier.height(32.dp))
                }
            }
        }
    }
}

@Composable
private fun InfoRow(icon: androidx.compose.ui.graphics.vector.ImageVector, text: String, bold: Boolean = false) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Box(
            modifier = Modifier.size(36.dp).clip(RoundedCornerShape(10.dp)).background(Accent.copy(alpha = 0.15f)),
            contentAlignment = Alignment.Center
        ) {
            Icon(icon, null, tint = Accent, modifier = Modifier.size(18.dp))
        }
        Spacer(Modifier.width(12.dp))
        Text(text, style = MaterialTheme.typography.bodyLarge.copy(fontWeight = if (bold) FontWeight.SemiBold else FontWeight.Normal), color = TextPrimary)
    }
}

private fun formatDetailDate(dateStr: String): String? {
    return try {
        val dt = OffsetDateTime.parse(dateStr)
        dt.format(DateTimeFormatter.ofPattern("d MMMM yyyy, HH:mm", Locale("ru")))
    } catch (e: Exception) {
        try {
            java.time.LocalDateTime.parse(dateStr)
                .format(DateTimeFormatter.ofPattern("d MMMM yyyy, HH:mm", Locale("ru")))
        } catch (e2: Exception) { dateStr }
    }
}
