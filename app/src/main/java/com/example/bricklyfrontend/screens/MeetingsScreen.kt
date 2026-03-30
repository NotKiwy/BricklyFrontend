package com.example.bricklyfrontend.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
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
    onNavigateToProfile: () -> Unit = {}
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
        .filter { parseDateSafe(it.date) != null }
        .sortedBy { parseDateSafe(it.date) }

    Scaffold(
        containerColor = Background,
        bottomBar = { BricklyBottomBar(currentRoute = "meetings", onNavigate = { route ->
            if (route == "profile") onNavigateToProfile()
        }) }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .verticalScroll(rememberScrollState())
        ) {
            // ── Жёлтый хедер с поиском ──────────────────────────────────────
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

            // ── Ближайшие мероприятия ────────────────────────────────────────
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
                    LazyRow(
                        contentPadding = PaddingValues(horizontal = 20.dp),
                        horizontalArrangement = Arrangement.spacedBy(14.dp)
                    ) {
                        items(upcoming.take(10)) { meeting ->
                            MeetingCard(meeting = meeting)
                        }
                    }
                }
            }

            Spacer(Modifier.height(28.dp))

            // ── Галерея (заглушка) ───────────────────────────────────────────
            Text(
                text = "Галерея",
                style = MaterialTheme.typography.headlineSmall.copy(fontWeight = FontWeight.ExtraBold),
                color = TextPrimary,
                modifier = Modifier.padding(horizontal = 20.dp)
            )

            Spacer(Modifier.height(14.dp))

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                repeat(2) {
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .height(180.dp)
                            .clip(RoundedCornerShape(20.dp))
                            .background(Color.White)
                    )
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
            focusedContainerColor = Accent.copy(alpha = 0.5f),
            unfocusedContainerColor = Accent.copy(alpha = 0.5f),
            cursorColor = TextPrimary
        ),
        singleLine = true,
        textStyle = MaterialTheme.typography.bodyLarge.copy(color = TextPrimary)
    )
}

@Composable
private fun MeetingCard(meeting: MeetingDefaultDTO) {
    val dateFormatted = formatMeetingDate(meeting.date)

    Box(
        modifier = Modifier
            .width(180.dp)
            .height(220.dp)
            .clip(RoundedCornerShape(20.dp))
    ) {
        // Фото-заглушка — белый фон
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Color.White)
        )

        // Жёлтая плашка снизу
        Box(
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .fillMaxWidth()
                .clip(RoundedCornerShape(bottomStart = 20.dp, bottomEnd = 20.dp))
                .background(Accent)
                .padding(horizontal = 12.dp, vertical = 10.dp)
        ) {
            Column {
                Text(
                    text = meeting.type?.description
                        ?: meeting.description?.take(20)
                        ?: "Мероприятие",
                    style = MaterialTheme.typography.bodyLarge.copy(fontWeight = FontWeight.Bold),
                    color = TextPrimary,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Spacer(Modifier.height(2.dp))
                Text(
                    text = meeting.address?.let { addr ->
                        // Берём только первую часть адреса
                        addr.split(",").firstOrNull()?.trim() ?: addr.take(30)
                    } ?: dateFormatted ?: "Адрес не указан",
                    style = MaterialTheme.typography.bodySmall,
                    color = TextPrimary.copy(alpha = 0.7f),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                if (meeting.ticketPrice != null && meeting.ticketPrice > 0) {
                    Spacer(Modifier.height(4.dp))
                    Text(
                        text = "${meeting.ticketPrice} ₽",
                        style = MaterialTheme.typography.labelMedium.copy(
                            fontWeight = FontWeight.SemiBold,
                            fontSize = 11.sp
                        ),
                        color = TextPrimary.copy(alpha = 0.6f)
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
        null
    }
}

private fun formatMeetingDate(dateStr: String?): String? {
    val dt = parseDateSafe(dateStr) ?: return null
    val fmt = DateTimeFormatter.ofPattern("d MMM, HH:mm", Locale("ru"))
    return dt.format(fmt)
}
