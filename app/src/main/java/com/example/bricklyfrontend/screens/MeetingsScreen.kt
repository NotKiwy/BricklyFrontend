package com.example.bricklyfrontend.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.example.bricklyfrontend.data.MeetingDefaultDTO
import com.example.bricklyfrontend.data.RetrofitClient
import com.example.bricklyfrontend.data.UserPreferences
import com.example.bricklyfrontend.ui.theme.*
import kotlinx.coroutines.launch
import java.time.OffsetDateTime
import java.time.format.DateTimeFormatter
import java.util.Locale

@Composable
fun MeetingsScreen(
    onNavigateToProfile: () -> Unit = {},
    onNavigateToMeetingDetail: (Long) -> Unit = {},
    onNavigateToCart: () -> Unit = {},
    onNavigateToHome: () -> Unit = {},
    onNavigateToBrickognize: () -> Unit = {},
    onNavigateToCreateMeeting: () -> Unit = {}
) {
    SetStatusBarColor(Accent)
    
    val context = LocalContext.current
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
                if (response.isSuccessful) meetings = response.body() ?: emptyList()
                else errorMessage = "Ошибка загрузки (${response.code()})"
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

    val upcoming = filteredMeetings.sortedWith(compareBy(nullsLast()) { parseDateSafe(it.date) })

    Scaffold(
        containerColor = Background,
        topBar = {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(bottomStart = 28.dp, bottomEnd = 28.dp))
                    .background(Accent)
                    .statusBarsPadding()
                    .padding(horizontal = 20.dp)
                    .padding(top = 20.dp, bottom = 20.dp)
            ) {
                Column {
                    Text("Сходки", fontSize = 26.sp, fontWeight = FontWeight.ExtraBold, color = TextPrimary)
                    Spacer(Modifier.height(12.dp))
                    OutlinedTextField(
                        value = searchQuery,
                        onValueChange = { searchQuery = it },
                        modifier = Modifier.fillMaxWidth(),
                        placeholder = { Text("Искать...", color = TextSecondary, style = MaterialTheme.typography.bodyLarge) },
                        leadingIcon = { Icon(Icons.Outlined.Search, null, tint = TextSecondary, modifier = Modifier.size(20.dp)) },
                        shape = RoundedCornerShape(16.dp),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = Color.Transparent,
                            unfocusedBorderColor = Color.Transparent,
                            focusedContainerColor = Color.White,
                            unfocusedContainerColor = Color.White.copy(alpha = 0.9f),
                            cursorColor = TextPrimary
                        ),
                        singleLine = true,
                        textStyle = MaterialTheme.typography.bodyLarge.copy(color = TextPrimary)
                    )
                }
            }
        },
        bottomBar = {
            BricklyBottomBar(currentRoute = "meetings", onNavigate = { route ->
                when (route) {
                    "profile" -> onNavigateToProfile()
                    "cart" -> onNavigateToCart()
                    "home" -> onNavigateToHome()
                    "brickognize" -> onNavigateToBrickognize()
                }
            }, onScanClick = onNavigateToBrickognize)
        },
        floatingActionButton = {
            if (UserPreferences.isMeetingCreator(context)) {
                FloatingActionButton(
                    onClick = onNavigateToCreateMeeting,
                    containerColor = Color(0xFFFFD54F),
                    contentColor = Color.Black
                ) {
                    Icon(Icons.Outlined.Add, contentDescription = "Создать митинг")
                }
            }
        }
    ) { padding ->
        Column(modifier = Modifier.fillMaxSize().padding(padding)) {
            Column(modifier = Modifier.fillMaxSize().verticalScroll(rememberScrollState())) {
                Spacer(Modifier.height(20.dp))

            when {
                isLoading -> Box(Modifier.fillMaxWidth().height(180.dp), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator(color = Accent)
                }

                errorMessage != null -> Column(
                    Modifier.fillMaxWidth().padding(20.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(errorMessage!!, color = ErrorColor)
                    Spacer(Modifier.height(8.dp))
                    Button(onClick = { loadMeetings() }, colors = ButtonDefaults.buttonColors(containerColor = Accent, contentColor = TextPrimary)) {
                        Text("Повторить")
                    }
                }

                upcoming.isEmpty() -> Row(Modifier.fillMaxWidth().padding(20.dp), verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Outlined.Block, null, tint = IconInactive, modifier = Modifier.size(32.dp))
                    Spacer(Modifier.width(12.dp))
                    Text("Мероприятий пока не запланировано", style = MaterialTheme.typography.bodyLarge, color = IconInactive)
                }

                else -> {
                    Text(
                        text = "Все мероприятия",
                        style = MaterialTheme.typography.headlineSmall.copy(fontWeight = FontWeight.ExtraBold),
                        color = TextPrimary,
                        modifier = Modifier.padding(horizontal = 20.dp)
                    )
                    Spacer(Modifier.height(14.dp))
                    upcoming.forEach { meeting ->
                        LargeMeetingCard(meeting = meeting, onClick = { onNavigateToMeetingDetail(meeting.id) })
                        Spacer(Modifier.height(12.dp))
                    }
                }
            }

            Spacer(Modifier.height(20.dp))

            Text(
                text = "Фото с мероприятий",
                style = MaterialTheme.typography.headlineSmall.copy(fontWeight = FontWeight.ExtraBold),
                color = TextPrimary,
                modifier = Modifier.padding(horizontal = 20.dp)
            )
            Spacer(Modifier.height(14.dp))

            listOf(
                "https://images.unsplash.com/photo-1540575467063-178a50c2df87?w=600",
                "https://images.unsplash.com/photo-1511578314322-379afb476865?w=600",
                "https://images.unsplash.com/photo-1475721027785-f74eccf877e2?w=600",
                "https://images.unsplash.com/photo-1523580494863-6f3031224c94?w=600"
            ).forEach { url ->
                AsyncImage(
                    model = url,
                    contentDescription = null,
                    modifier = Modifier.fillMaxWidth().padding(horizontal = 20.dp).height(200.dp).clip(RoundedCornerShape(24.dp)),
                    contentScale = ContentScale.Crop
                )
                Spacer(Modifier.height(12.dp))
            }

                Spacer(Modifier.height(24.dp))
            }
        }
    }
}

@Composable
private fun LargeMeetingCard(meeting: MeetingDefaultDTO, onClick: () -> Unit) {
    val dateFormatted = meeting.date?.let { formatMeetingDate(it) }

    Card(
        modifier = Modifier.fillMaxWidth().padding(horizontal = 20.dp).clickable(onClick = onClick),
        shape = RoundedCornerShape(24.dp),
        colors = CardDefaults.cardColors(containerColor = CardBackground),
        elevation = CardDefaults.cardElevation(0.dp)
    ) {
        Column {
            val previewUrl = meeting.previewImagePath?.let { path ->
                if (path.startsWith("http", ignoreCase = true)) path else "${RetrofitClient.BASE_URL}${if (path.startsWith("/")) path else "/$path"}"
            }

            Box(
                modifier = Modifier.fillMaxWidth().height(130.dp),
                contentAlignment = Alignment.Center
            ) {
                if (previewUrl != null) {
                    AsyncImage(
                        model = previewUrl,
                        contentDescription = null,
                        modifier = Modifier.fillMaxSize(),
                        contentScale = ContentScale.Crop
                    )
                } else {
                    Box(
                        modifier = Modifier.fillMaxSize().background(Accent.copy(alpha = 0.12f)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(Icons.Outlined.Event, null, tint = Accent, modifier = Modifier.size(48.dp))
                    }
                }
            }

            Column(modifier = Modifier.padding(horizontal = 16.dp, vertical = 14.dp)) {
                Text(
                    text = meeting.title?.takeIf { it.isNotBlank() } ?: meeting.type?.description ?: meeting.description?.take(40) ?: "Мероприятие",
                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                    color = TextPrimary,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Spacer(Modifier.height(10.dp))

                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    if (dateFormatted != null) {
                        InfoChip(icon = Icons.Outlined.CalendarMonth, text = dateFormatted)
                    }
                    InfoChip(
                        icon = Icons.Outlined.ConfirmationNumber,
                        text = if (meeting.ticketPrice != null && meeting.ticketPrice > 0) "${meeting.ticketPrice} \u20BD" else "Бесплатно"
                    )
                }

                if (!meeting.address.isNullOrBlank()) {
                    Spacer(Modifier.height(6.dp))
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Outlined.LocationOn, null, tint = TextSecondary, modifier = Modifier.size(14.dp))
                        Spacer(Modifier.width(4.dp))
                        Text(meeting.address, style = MaterialTheme.typography.bodySmall, color = TextSecondary, maxLines = 1, overflow = TextOverflow.Ellipsis)
                    }
                }
            }
        }
    }
}

@Composable
private fun InfoChip(icon: androidx.compose.ui.graphics.vector.ImageVector, text: String) {
    Surface(shape = RoundedCornerShape(10.dp), color = Accent.copy(alpha = 0.15f)) {
        Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)) {
            Icon(icon, null, tint = Accent, modifier = Modifier.size(13.dp))
            Spacer(Modifier.width(4.dp))
            Text(text, fontSize = 12.sp, fontWeight = FontWeight.SemiBold, color = TextPrimary)
        }
    }
}

private fun parseDateSafe(dateStr: String?): OffsetDateTime? {
    if (dateStr.isNullOrBlank()) return null
    return try { OffsetDateTime.parse(dateStr) } catch (e: Exception) {
        try { java.time.LocalDateTime.parse(dateStr).atOffset(java.time.ZoneOffset.UTC) } catch (e2: Exception) { null }
    }
}

private fun formatMeetingDate(dateStr: String): String? {
    val dt = parseDateSafe(dateStr) ?: return null
    return dt.format(DateTimeFormatter.ofPattern("d MMM, HH:mm", Locale("ru")))
}
