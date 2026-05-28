package com.example.bricklyfrontend.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.bricklyfrontend.data.MeetingShortDTO
import com.example.bricklyfrontend.data.RetrofitClient
import com.example.bricklyfrontend.data.UserPreferences
import com.example.bricklyfrontend.ui.theme.*
import kotlinx.coroutines.launch
import java.time.OffsetDateTime
import java.time.format.DateTimeFormatter
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MyMeetingsScreen(
    onBack: () -> Unit,
    onNavigateToEditMeeting: (Long) -> Unit = {}
) {
    SetStatusBarColor(Accent)

    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val userId = UserPreferences.getUserId(context)

    var meetings by remember { mutableStateOf<List<MeetingShortDTO>>(emptyList()) }
    var ticketCounts by remember { mutableStateOf<Map<Long, Int>>(emptyMap()) }
    var isLoading by remember { mutableStateOf(true) }
    var isRefreshing by remember { mutableStateOf(false) }
    var errorMessage by remember { mutableStateOf<String?>(null) }

    fun loadMeetings(silent: Boolean = false) {
        scope.launch {
            if (silent) isRefreshing = true else isLoading = true
            errorMessage = null
            try {
                val resp = RetrofitClient.api.getMeetingsByCreatorId(userId)
                if (resp.isSuccessful) {
                    val list = resp.body()?.content ?: emptyList()
                    meetings = list
                    val counts = mutableMapOf<Long, Int>()
                    list.forEach { meeting ->
                        try {
                            val r = RetrofitClient.api.getTicketsByMeetingId(meeting.id)
                            if (r.isSuccessful) counts[meeting.id] = r.body()?.size ?: 0
                        } catch (_: Exception) {}
                    }
                    ticketCounts = counts
                } else {
                    errorMessage = "Ошибка загрузки (${resp.code()})"
                }
            } catch (_: Exception) {
                errorMessage = "Нет соединения"
            }
            isLoading = false
            isRefreshing = false
        }
    }

    LaunchedEffect(Unit) { loadMeetings() }

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
                    "Мои мероприятия",
                    color = Color(0xFF1A1A1A),
                    fontWeight = FontWeight.Bold,
                    fontSize = 18.sp,
                    modifier = Modifier.align(Alignment.Center)
                )
            }
        }
    ) { padding ->
        PullToRefreshBox(
            isRefreshing = isRefreshing,
            onRefresh = { loadMeetings(silent = true) },
            modifier = Modifier.fillMaxSize().padding(padding)
        ) {
            when {
                isLoading -> Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator(color = Accent)
                }

                errorMessage != null -> Box(
                    Modifier.fillMaxSize().padding(32.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(errorMessage!!, color = ErrorColor, textAlign = TextAlign.Center)
                        Spacer(Modifier.height(12.dp))
                        Button(
                            onClick = { loadMeetings() },
                            colors = ButtonDefaults.buttonColors(containerColor = Accent, contentColor = TextPrimary)
                        ) { Text("Повторить") }
                    }
                }

                meetings.isEmpty() -> Box(
                    Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    EmptyStateBox(
                        icon = Icons.Outlined.Event,
                        title = "Мероприятий нет",
                        subtitle = "Создайте первое мероприятие",
                        onRefresh = { loadMeetings() }
                    )
                }

                else -> LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(vertical = 12.dp)
                ) {
                    items(meetings, key = { it.id }) { meeting ->
                        val isPast = meeting.date?.let {
                            try { OffsetDateTime.parse(it).isBefore(OffsetDateTime.now()) }
                            catch (_: Exception) { false }
                        } ?: false
                        MyMeetingCard(
                            meeting = meeting,
                            ticketCount = ticketCounts[meeting.id],
                            isPast = isPast,
                            onEdit = { onNavigateToEditMeeting(meeting.id) }
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun MyMeetingCard(
    meeting: MeetingShortDTO,
    ticketCount: Int?,
    isPast: Boolean,
    onEdit: () -> Unit
) {
    val dateFormatted = meeting.date?.let {
        try {
            OffsetDateTime.parse(it).format(
                DateTimeFormatter.ofPattern("d MMM yyyy, HH:mm", Locale.forLanguageTag("ru"))
            )
        } catch (_: Exception) { it }
    }

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 6.dp),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(
            containerColor = if (isPast) CardBackground.copy(alpha = 0.6f) else CardBackground
        ),
        elevation = CardDefaults.cardElevation(0.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.Top
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = meeting.title ?: "Без названия",
                        fontSize = 16.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = if (isPast) TextSecondary else TextPrimary
                    )
                    if (isPast) {
                        Spacer(Modifier.height(4.dp))
                        Text(
                            "Прошедшее",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Medium,
                            color = TextSecondary
                        )
                    }
                }
                if (!isPast) {
                    IconButton(
                        onClick = onEdit,
                        modifier = Modifier
                            .size(36.dp)
                            .clip(RoundedCornerShape(10.dp))
                            .background(Accent)
                    ) {
                        Icon(
                            Icons.Outlined.Edit,
                            contentDescription = "Редактировать",
                            tint = Color.Black,
                            modifier = Modifier.size(18.dp)
                        )
                    }
                }
            }

            Spacer(Modifier.height(10.dp))

            if (dateFormatted != null) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Outlined.Schedule, null, tint = IconInactive, modifier = Modifier.size(14.dp))
                    Spacer(Modifier.width(6.dp))
                    Text(dateFormatted, fontSize = 13.sp, color = TextSecondary)
                }
                Spacer(Modifier.height(4.dp))
            }

            if (!meeting.address.isNullOrBlank()) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Outlined.LocationOn, null, tint = IconInactive, modifier = Modifier.size(14.dp))
                    Spacer(Modifier.width(6.dp))
                    Text(meeting.address, fontSize = 13.sp, color = TextSecondary)
                }
                Spacer(Modifier.height(4.dp))
            }

            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Outlined.ConfirmationNumber, null, tint = Accent, modifier = Modifier.size(14.dp))
                Spacer(Modifier.width(6.dp))
                Text(
                    text = if (ticketCount != null) "Записалось: $ticketCount" else "Загрузка...",
                    fontSize = 13.sp,
                    color = TextPrimary,
                    fontWeight = FontWeight.Medium
                )
            }
        }
    }
}
