package com.example.bricklyfrontend.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.bricklyfrontend.data.MeetingDefaultDTO
import com.example.bricklyfrontend.data.RetrofitClient
import com.example.bricklyfrontend.ui.theme.*
import kotlinx.coroutines.launch
import java.time.OffsetDateTime
import java.time.format.DateTimeFormatter
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MeetingsScreen(
    onNavigateToProfile: () -> Unit = {},
    onNavigateToMeetingDetail: (Long) -> Unit = {},
    onNavigateToCart: () -> Unit = {},
    onNavigateToHome: () -> Unit = {}
) {
    val scope = rememberCoroutineScope()

    var meetings by remember { mutableStateOf<List<MeetingDefaultDTO>>(emptyList()) }
    var isLoading by remember { mutableStateOf(true) }
    var errorMessage by remember { mutableStateOf<String?>(null) }
    var searchQuery by remember { mutableStateOf("") }

    fun loadMeetings() {
        scope.launch {
            isLoading = true
            errorMessage = null
            try {
                val response = RetrofitClient.api.getAllMeetings()
                if (response.isSuccessful) {
                    meetings = response.body() ?: emptyList()
                } else {
                    errorMessage = "Ошибка загрузки (${response.code()})"
                }
            } catch (e: Exception) {
                errorMessage = "Нет соединения с сервером"
            } finally {
                isLoading = false
            }
        }
    }

    LaunchedEffect(Unit) { loadMeetings() }

    val filteredMeetings = if (searchQuery.isBlank()) meetings
    else meetings.filter {
        it.address?.contains(searchQuery, ignoreCase = true) == true ||
                it.description?.contains(searchQuery, ignoreCase = true) == true ||
                it.type?.description?.contains(searchQuery, ignoreCase = true) == true
    }

    val upcoming = filteredMeetings
        .sortedWith(compareBy(nullsLast()) { parseDateSafe(it.date) })

    Scaffold(
        containerColor = Background,
        bottomBar = {
            BricklyBottomBar(currentRoute = "meetings", onNavigate = { route ->
                when (route) {
                    "profile" -> onNavigateToProfile()
                    "cart" -> onNavigateToCart()
                    "home" -> onNavigateToHome()
                }
            })
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .verticalScroll(rememberScrollState())
        ) {
            // Yellow header with search
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(bottomStart = 28.dp, bottomEnd = 28.dp))
                    .background(Accent)
                    .padding(horizontal = 20.dp)
                    .padding(top = 56.dp, bottom = 20.dp)
            ) {
                SearchBar(
                    value = searchQuery,
                    onValueChange = { searchQuery = it }
                )
            }

            Spacer(Modifier.height(20.dp))

            // Upcoming meetings - small cards
            Text(
                text = "Ближайшие мероприятия",
                style = MaterialTheme.typography.headlineSmall.copy(fontWeight = FontWeight.ExtraBold),
                color = TextPrimary,
                modifier = Modifier.padding(horizontal = 20.dp)
            )

            Spacer(Modifier.height(14.dp))

            when {
                isLoading -> {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(180.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        CircularProgressIndicator(color = Accent)
                    }
                }

                errorMessage != null -> {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 20.dp, vertical = 16.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text(errorMessage!!, color = ErrorColor)
                        Spacer(Modifier.height(8.dp))
                        Button(
                            onClick = { loadMeetings() },
                            colors = ButtonDefaults.buttonColors(containerColor = Accent)
                        ) {
                            Text("Повторить", color = TextPrimary)
                        }
                    }
                }

                upcoming.isEmpty() -> {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 20.dp, vertical = 16.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.Outlined.Block,
                            contentDescription = null,
                            tint = IconInactive,
                            modifier = Modifier.size(32.dp)
                        )
                        Spacer(Modifier.width(12.dp))
                        Text(
                            text = "Мероприятий пока не запланировано",
                            style = MaterialTheme.typography.bodyLarge,
                            color = IconInactive
                        )
                    }
                }

                else -> {
                    // Small cards row
                    LazyRow(
                        contentPadding = PaddingValues(horizontal = 20.dp),
                        horizontalArrangement = Arrangement.spacedBy(14.dp)
                    ) {
                        items(upcoming.take(10)) { meeting ->
                            SmallMeetingCard(
                                meeting = meeting,
                                onClick = { onNavigateToMeetingDetail(meeting.id) }
                            )
                        }
                    }

                    Spacer(Modifier.height(28.dp))

                    // Large cards section
                    Text(
                        text = "Все мероприятия",
                        style = MaterialTheme.typography.headlineSmall.copy(fontWeight = FontWeight.ExtraBold),
                        color = TextPrimary,
                        modifier = Modifier.padding(horizontal = 20.dp)
                    )

                    Spacer(Modifier.height(14.dp))

                    upcoming.forEach { meeting ->
                        LargeMeetingCard(
                            meeting = meeting,
                            onClick = { onNavigateToMeetingDetail(meeting.id) }
                        )
                        Spacer(Modifier.height(12.dp))
                    }
                }
            }

            Spacer(Modifier.height(24.dp))
        }
    }
}

@Composable
private fun SearchBar(value: String, onValueChange: (String) -> Unit) {
    OutlinedTextField(
        value = value,
        onValueChange = onValueChange,
        modifier = Modifier.fillMaxWidth(),
        placeholder = {
            Text(
                "Искать...",
                color = TextSecondary,
                style = MaterialTheme.typography.bodyLarge
            )
        },
        leadingIcon = {
            Icon(
                imageVector = Icons.Outlined.Search,
                contentDescription = null,
                tint = TextSecondary,
                modifier = Modifier.size(20.dp)
            )
        },
        shape = RoundedCornerShape(16.dp),
        colors = OutlinedTextFieldDefaults.colors(
            focusedBorderColor = Color.Transparent,
            unfocusedBorderColor = Color.Transparent,
            focusedContainerColor = Color.White,
            unfocusedContainerColor = Color.White.copy(alpha = 0.92f),
            cursorColor = TextPrimary
        ),
        singleLine = true,
        textStyle = MaterialTheme.typography.bodyLarge.copy(color = TextPrimary)
    )
}

@Composable
private fun SmallMeetingCard(meeting: MeetingDefaultDTO, onClick: () -> Unit) {
    val dateFormatted = formatMeetingDate(meeting.date)

    Box(
        modifier = Modifier
            .width(180.dp)
            .height(220.dp)
            .clip(RoundedCornerShape(20.dp))
            .clickable(onClick = onClick)
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Color.White)
        )

        // Yellow banner at bottom: Date, Address, Price
        Box(
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .fillMaxWidth()
                .clip(RoundedCornerShape(bottomStart = 20.dp, bottomEnd = 20.dp))
                .background(Accent)
                .padding(horizontal = 12.dp, vertical = 10.dp)
        ) {
            Column {
                // Date
                Text(
                    text = dateFormatted ?: "Дата не указана",
                    style = MaterialTheme.typography.bodyLarge.copy(fontWeight = FontWeight.Bold),
                    color = TextPrimary,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Spacer(Modifier.height(2.dp))

                // Address
                Text(
                    text = meeting.address?.let { addr ->
                        addr.split(",").firstOrNull()?.trim() ?: addr.take(30)
                    } ?: "Адрес не указан",
                    style = MaterialTheme.typography.bodySmall,
                    color = TextPrimary.copy(alpha = 0.7f),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )

                // Price
                if (meeting.ticketPrice != null && meeting.ticketPrice > 0) {
                    Spacer(Modifier.height(4.dp))
                    Text(
                        text = "${meeting.ticketPrice} \u20BD",
                        style = MaterialTheme.typography.labelMedium.copy(
                            fontWeight = FontWeight.SemiBold,
                            fontSize = 12.sp
                        ),
                        color = TextPrimary.copy(alpha = 0.8f)
                    )
                } else {
                    Spacer(Modifier.height(4.dp))
                    Text(
                        text = "Бесплатно",
                        style = MaterialTheme.typography.labelMedium.copy(
                            fontWeight = FontWeight.SemiBold,
                            fontSize = 12.sp
                        ),
                        color = TextPrimary.copy(alpha = 0.8f)
                    )
                }
            }
        }
    }
}

@Composable
private fun LargeMeetingCard(meeting: MeetingDefaultDTO, onClick: () -> Unit) {
    val dateFormatted = formatMeetingDate(meeting.date)

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 20.dp)
            .clickable(onClick = onClick),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = CardBackground),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
    ) {
        Column {
            // Photo placeholder
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(140.dp)
                    .background(Accent.copy(alpha = 0.15f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Outlined.Event,
                    contentDescription = null,
                    tint = Accent,
                    modifier = Modifier.size(48.dp)
                )
            }

            Column(
                modifier = Modifier.padding(horizontal = 16.dp, vertical = 14.dp)
            ) {
                // Title
                Text(
                    text = meeting.type?.description
                        ?: meeting.description?.take(40)
                        ?: "Мероприятие",
                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                    color = TextPrimary,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )

                Spacer(Modifier.height(8.dp))

                // Date
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        Icons.Outlined.CalendarMonth,
                        contentDescription = null,
                        tint = TextSecondary,
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(Modifier.width(6.dp))
                    Text(
                        text = dateFormatted ?: "Дата не указана",
                        style = MaterialTheme.typography.bodyMedium,
                        color = TextSecondary
                    )
                }

                Spacer(Modifier.height(4.dp))

                // Address
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        Icons.Outlined.LocationOn,
                        contentDescription = null,
                        tint = TextSecondary,
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(Modifier.width(6.dp))
                    Text(
                        text = meeting.address ?: "Адрес не указан",
                        style = MaterialTheme.typography.bodyMedium,
                        color = TextSecondary,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }

                Spacer(Modifier.height(4.dp))

                // Price
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        Icons.Outlined.ConfirmationNumber,
                        contentDescription = null,
                        tint = TextSecondary,
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(Modifier.width(6.dp))
                    Text(
                        text = if (meeting.ticketPrice != null && meeting.ticketPrice > 0)
                            "${meeting.ticketPrice} \u20BD"
                        else "Бесплатно",
                        style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.SemiBold),
                        color = TextPrimary
                    )
                }
            }
        }
    }
}

private fun parseDateSafe(dateStr: String?): OffsetDateTime? {
    if (dateStr.isNullOrBlank()) return null
    return try {
        OffsetDateTime.parse(dateStr)
    } catch (e: Exception) {
        try {
            val local = java.time.LocalDateTime.parse(dateStr)
            local.atOffset(java.time.ZoneOffset.UTC)
        } catch (e2: Exception) {
            null
        }
    }
}

private fun formatMeetingDate(dateStr: String?): String? {
    val dt = parseDateSafe(dateStr) ?: return null
    val fmt = DateTimeFormatter.ofPattern("d MMM, HH:mm", Locale("ru"))
    return dt.format(fmt)
}
