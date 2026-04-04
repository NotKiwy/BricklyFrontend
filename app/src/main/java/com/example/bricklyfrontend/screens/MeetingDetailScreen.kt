package com.example.bricklyfrontend.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import kotlinx.coroutines.launch
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
            items.map {
                if (it.meetingId == item.meetingId)
                    it.copy(quantity = it.quantity + item.quantity)
                else it
            }
        } else {
            items + item
        }
    }

    fun removeItem(meetingId: Long) {
        items = items.filter { it.meetingId != meetingId }
    }

    fun totalPrice(): Int = items.sumOf { it.ticketPrice * it.quantity }
    fun totalTickets(): Int = items.sumOf { it.quantity }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MeetingDetailScreen(
    meetingId: Long,
    onBack: () -> Unit,
    onNavigateToCart: () -> Unit
) {
    val scope = rememberCoroutineScope()

    var meeting by remember { mutableStateOf<MeetingDefaultDTO?>(null) }
    var isLoading by remember { mutableStateOf(true) }
    var errorMessage by remember { mutableStateOf<String?>(null) }
    var ticketCount by remember { mutableIntStateOf(1) }
    var addedToCart by remember { mutableStateOf(false) }

    LaunchedEffect(meetingId) {
        try {
            val response = RetrofitClient.api.getMeetingById(meetingId)
            if (response.isSuccessful) {
                meeting = response.body()
            } else {
                errorMessage = "Ошибка загрузки (${response.code()})"
            }
        } catch (e: Exception) {
            errorMessage = "Нет соединения с сервером"
        }
        isLoading = false
    }

    Scaffold(
        containerColor = Background,
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        "Подробности",
                        style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold),
                        color = TextPrimary
                    )
                },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.Outlined.ArrowBackIosNew, "Назад", tint = TextPrimary)
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = Background)
            )
        }
    ) { padding ->
        when {
            isLoading -> {
                Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator(color = Accent)
                }
            }

            errorMessage != null -> {
                Box(
                    Modifier
                        .fillMaxSize()
                        .padding(32.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(errorMessage!!, color = ErrorColor, textAlign = TextAlign.Center)
                }
            }

            meeting != null -> {
                val m = meeting!!
                val dateFormatted = m.date?.let { formatDetailDate(it) }

                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(padding)
                        .verticalScroll(rememberScrollState())
                ) {
                    // Map placeholder
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(200.dp)
                            .padding(horizontal = 20.dp)
                            .clip(RoundedCornerShape(20.dp))
                            .background(Color(0xFFE8E8E8)),
                        contentAlignment = Alignment.Center
                    ) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Icon(
                                Icons.Outlined.Map,
                                contentDescription = null,
                                tint = TextSecondary,
                                modifier = Modifier.size(48.dp)
                            )
                            Spacer(Modifier.height(8.dp))
                            Text(
                                m.address ?: "Адрес не указан",
                                style = MaterialTheme.typography.bodyMedium,
                                color = TextSecondary,
                                textAlign = TextAlign.Center,
                                modifier = Modifier.padding(horizontal = 16.dp)
                            )
                        }
                    }

                    Spacer(Modifier.height(20.dp))

                    // Info card
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 20.dp),
                        shape = RoundedCornerShape(20.dp),
                        colors = CardDefaults.cardColors(containerColor = CardBackground),
                        elevation = CardDefaults.cardElevation(0.dp)
                    ) {
                        Column(modifier = Modifier.padding(20.dp)) {
                            // Title
                            Text(
                                text = m.type?.description
                                    ?: m.description?.take(50)
                                    ?: "Мероприятие",
                                style = MaterialTheme.typography.titleLarge.copy(
                                    fontWeight = FontWeight.Bold
                                ),
                                color = TextPrimary
                            )

                            Spacer(Modifier.height(16.dp))

                            // Date
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(
                                    Icons.Outlined.CalendarMonth,
                                    null,
                                    tint = Accent,
                                    modifier = Modifier.size(20.dp)
                                )
                                Spacer(Modifier.width(10.dp))
                                Text(
                                    dateFormatted ?: "Дата не указана",
                                    style = MaterialTheme.typography.bodyLarge,
                                    color = TextPrimary
                                )
                            }

                            Spacer(Modifier.height(10.dp))

                            // Address
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(
                                    Icons.Outlined.LocationOn,
                                    null,
                                    tint = Accent,
                                    modifier = Modifier.size(20.dp)
                                )
                                Spacer(Modifier.width(10.dp))
                                Text(
                                    m.address ?: "Адрес не указан",
                                    style = MaterialTheme.typography.bodyLarge,
                                    color = TextPrimary
                                )
                            }

                            Spacer(Modifier.height(10.dp))

                            // Price
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(
                                    Icons.Outlined.ConfirmationNumber,
                                    null,
                                    tint = Accent,
                                    modifier = Modifier.size(20.dp)
                                )
                                Spacer(Modifier.width(10.dp))
                                Text(
                                    if (m.ticketPrice != null && m.ticketPrice > 0)
                                        "${m.ticketPrice} \u20BD"
                                    else "Бесплатно",
                                    style = MaterialTheme.typography.bodyLarge.copy(
                                        fontWeight = FontWeight.SemiBold
                                    ),
                                    color = TextPrimary
                                )
                            }
                        }
                    }

                    // Description
                    if (!m.description.isNullOrBlank()) {
                        Spacer(Modifier.height(16.dp))
                        Card(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 20.dp),
                            shape = RoundedCornerShape(20.dp),
                            colors = CardDefaults.cardColors(containerColor = CardBackground),
                            elevation = CardDefaults.cardElevation(0.dp)
                        ) {
                            Column(modifier = Modifier.padding(20.dp)) {
                                Text(
                                    "Описание",
                                    style = MaterialTheme.typography.titleMedium.copy(
                                        fontWeight = FontWeight.Bold
                                    ),
                                    color = TextPrimary
                                )
                                Spacer(Modifier.height(8.dp))
                                Text(
                                    m.description,
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = TextSecondary,
                                    lineHeight = 22.sp
                                )
                            }
                        }
                    }

                    Spacer(Modifier.height(20.dp))

                    // Ticket quantity selector + Add to cart
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 20.dp),
                        shape = RoundedCornerShape(20.dp),
                        colors = CardDefaults.cardColors(containerColor = CardBackground),
                        elevation = CardDefaults.cardElevation(0.dp)
                    ) {
                        Column(
                            modifier = Modifier.padding(20.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Text(
                                "Количество билетов",
                                style = MaterialTheme.typography.titleMedium.copy(
                                    fontWeight = FontWeight.Bold
                                ),
                                color = TextPrimary
                            )

                            Spacer(Modifier.height(16.dp))

                            // Counter
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.Center
                            ) {
                                IconButton(
                                    onClick = { if (ticketCount > 1) ticketCount-- },
                                    modifier = Modifier
                                        .size(40.dp)
                                        .clip(RoundedCornerShape(12.dp))
                                        .background(Accent.copy(alpha = 0.15f))
                                ) {
                                    Icon(Icons.Filled.Remove, "Уменьшить", tint = TextPrimary)
                                }

                                Text(
                                    text = "$ticketCount",
                                    style = MaterialTheme.typography.headlineMedium.copy(
                                        fontWeight = FontWeight.Bold
                                    ),
                                    color = TextPrimary,
                                    modifier = Modifier.padding(horizontal = 24.dp)
                                )

                                IconButton(
                                    onClick = { ticketCount++ },
                                    modifier = Modifier
                                        .size(40.dp)
                                        .clip(RoundedCornerShape(12.dp))
                                        .background(Accent.copy(alpha = 0.15f))
                                ) {
                                    Icon(Icons.Filled.Add, "Увеличить", tint = TextPrimary)
                                }
                            }

                            if (m.ticketPrice != null && m.ticketPrice > 0) {
                                Spacer(Modifier.height(8.dp))
                                Text(
                                    "Итого: ${m.ticketPrice * ticketCount} \u20BD",
                                    style = MaterialTheme.typography.bodyLarge.copy(
                                        fontWeight = FontWeight.SemiBold
                                    ),
                                    color = TextSecondary
                                )
                            }

                            Spacer(Modifier.height(16.dp))

                            if (addedToCart) {
                                Button(
                                    onClick = onNavigateToCart,
                                    modifier = Modifier.fillMaxWidth(),
                                    shape = RoundedCornerShape(14.dp),
                                    colors = ButtonDefaults.buttonColors(
                                        containerColor = AccentDark,
                                        contentColor = TextPrimary
                                    )
                                ) {
                                    Icon(
                                        Icons.Outlined.ShoppingBag,
                                        null,
                                        modifier = Modifier.size(18.dp)
                                    )
                                    Spacer(Modifier.width(8.dp))
                                    Text("Перейти в корзину", fontWeight = FontWeight.SemiBold)
                                }
                            } else {
                                Button(
                                    onClick = {
                                        CartState.addItem(
                                            CartItem(
                                                meetingId = m.id,
                                                meetingTitle = m.type?.description
                                                    ?: m.description?.take(30)
                                                    ?: "Мероприятие",
                                                meetingDate = m.date,
                                                meetingAddress = m.address,
                                                ticketPrice = m.ticketPrice ?: 0,
                                                quantity = ticketCount
                                            )
                                        )
                                        addedToCart = true
                                    },
                                    modifier = Modifier.fillMaxWidth(),
                                    shape = RoundedCornerShape(14.dp),
                                    colors = ButtonDefaults.buttonColors(
                                        containerColor = Accent,
                                        contentColor = TextPrimary
                                    )
                                ) {
                                    Icon(
                                        Icons.Outlined.ShoppingCart,
                                        null,
                                        modifier = Modifier.size(18.dp)
                                    )
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

private fun formatDetailDate(dateStr: String): String? {
    return try {
        val dt = OffsetDateTime.parse(dateStr)
        val fmt = DateTimeFormatter.ofPattern("d MMMM yyyy, HH:mm", Locale("ru"))
        dt.format(fmt)
    } catch (e: Exception) {
        try {
            val local = java.time.LocalDateTime.parse(dateStr)
            val fmt = DateTimeFormatter.ofPattern("d MMMM yyyy, HH:mm", Locale("ru"))
            local.format(fmt)
        } catch (e2: Exception) {
            dateStr
        }
    }
}
