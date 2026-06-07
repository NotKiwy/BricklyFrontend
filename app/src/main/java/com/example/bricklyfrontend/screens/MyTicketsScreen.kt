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
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.ImageLoader
import coil.compose.SubcomposeAsyncImage
import coil.request.ImageRequest
import com.example.bricklyfrontend.data.RetrofitClient
import com.example.bricklyfrontend.data.TicketDefaultDTO
import com.example.bricklyfrontend.data.UserPreferences
import com.example.bricklyfrontend.ui.theme.*
import kotlinx.coroutines.launch
import okhttp3.Credentials
import okhttp3.OkHttpClient
import java.time.OffsetDateTime
import java.time.format.DateTimeFormatter
import java.util.Locale

@Composable
fun MyTicketsScreen(
    onBack: () -> Unit,
    onNavigateToMeetingDetail: (Long) -> Unit,
    onNavigate: (String) -> Unit = {}
) {
    SetStatusBarColor(Accent)
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val userId = UserPreferences.getUserId(context)

    val imageLoader = remember {
        val username = UserPreferences.getUsername(context)
        val password = UserPreferences.getPassword(context)
        val client = OkHttpClient.Builder()
            .addInterceptor { chain ->
                chain.proceed(chain.request().newBuilder().header("Authorization", Credentials.basic(username, password)).build())
            }
            .build()
        ImageLoader.Builder(context).okHttpClient(client).build()
    }

    var tickets by remember { mutableStateOf<List<TicketDefaultDTO>>(emptyList()) }
    var isLoading by remember { mutableStateOf(true) }
    var errorMessage by remember { mutableStateOf<String?>(null) }

    LaunchedEffect(Unit) {
        try {
            val resp = RetrofitClient.api.getTicketsByUserId(userId)
            if (resp.isSuccessful) {
                tickets = (resp.body() ?: emptyList()).filter { it.state == 0 }
            } else {
                errorMessage = "Ошибка загрузки (${resp.code()})"
            }
        } catch (_: Exception) {
            errorMessage = "Нет соединения"
        }
        isLoading = false
    }

    Scaffold(
        containerColor = Background,
        bottomBar = {
            BricklyBottomBar(currentRoute = "profile", onNavigate = onNavigate)
        },
        topBar = {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(bottomStart = 28.dp, bottomEnd = 28.dp))
                    .background(Accent)
                    .statusBarsPadding()
                    .padding(horizontal = 8.dp, vertical = 8.dp)
            ) {
                IconButton(onClick = onBack, modifier = Modifier.align(Alignment.CenterStart)) {
                    Icon(Icons.Outlined.ArrowBackIosNew, null, tint = TextPrimary)
                }
                Text(
                    "Мои билеты",
                    color = TextPrimary,
                    fontWeight = FontWeight.Bold,
                    fontSize = 18.sp,
                    modifier = Modifier.align(Alignment.Center)
                )
            }
        }
    ) { padding ->
        when {
            isLoading -> Box(Modifier.fillMaxSize().padding(padding), contentAlignment = Alignment.Center) {
                CircularProgressIndicator(color = Accent)
            }
            errorMessage != null -> Box(Modifier.fillMaxSize().padding(padding), contentAlignment = Alignment.Center) {
                Text(errorMessage!!, color = ErrorColor, textAlign = TextAlign.Center)
            }
            tickets.isEmpty() -> Box(Modifier.fillMaxSize().padding(padding), contentAlignment = Alignment.Center) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Box(
                        modifier = Modifier.size(80.dp).clip(RoundedCornerShape(24.dp)).background(Accent.copy(alpha = 0.15f)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(Icons.Outlined.ConfirmationNumber, null, tint = Accent, modifier = Modifier.size(36.dp))
                    }
                    Spacer(Modifier.height(16.dp))
                    Text(
                        "Нет активных билетов",
                        style = MaterialTheme.typography.bodyLarge.copy(fontWeight = FontWeight.Bold),
                        color = TextPrimary
                    )
                    Spacer(Modifier.height(6.dp))
                    Text(
                        "Запишитесь на бесплатное мероприятие",
                        style = MaterialTheme.typography.bodyMedium,
                        color = TextSecondary,
                        textAlign = TextAlign.Center
                    )
                }
            }
            else -> LazyColumn(
                modifier = Modifier.fillMaxSize().padding(padding),
                contentPadding = PaddingValues(vertical = 12.dp)
            ) {
                items(tickets, key = { it.id }) { ticket ->
                    TicketCard(
                        ticket = ticket,
                        imageLoader = imageLoader,
                        onClick = { ticket.meeting?.id?.let { onNavigateToMeetingDetail(it) } }
                    )
                }
            }
        }
    }
}

@Composable
private fun TicketCard(
    ticket: TicketDefaultDTO,
    imageLoader: ImageLoader,
    onClick: () -> Unit
) {
    val meeting = ticket.meeting
    val imageUrl = meeting?.previewImagePath
        ?.takeIf { it.isNotBlank() }
        ?.let { "${RetrofitClient.BASE_URL}/${it.trimStart('/')}" }

    val title = meeting?.title?.takeIf { it.isNotBlank() } ?: meeting?.type?.description ?: "Мероприятие"

    val dateStr = meeting?.date?.let { dateStr ->
        try {
            OffsetDateTime.parse(dateStr).format(DateTimeFormatter.ofPattern("d MMMM, HH:mm", Locale("ru")))
        } catch (_: Exception) { dateStr }
    }

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 6.dp)
            .clickable(onClick = onClick),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = CardBackground),
        elevation = CardDefaults.cardElevation(0.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth().height(110.dp).padding(10.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            SubcomposeAsyncImage(
                model = ImageRequest.Builder(LocalContext.current).data(imageUrl).crossfade(true).build(),
                imageLoader = imageLoader,
                contentDescription = null,
                modifier = Modifier.fillMaxHeight().width(90.dp).clip(RoundedCornerShape(14.dp)),
                contentScale = ContentScale.Crop,
                loading = { Box(Modifier.fillMaxSize().background(Accent.copy(alpha = 0.12f))) },
                error = {
                    Box(
                        Modifier.fillMaxSize().background(Accent.copy(alpha = 0.12f)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(Icons.Outlined.ConfirmationNumber, null, tint = Accent, modifier = Modifier.size(28.dp))
                    }
                }
            )
            Spacer(Modifier.width(12.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(title, fontSize = 16.sp, fontWeight = FontWeight.SemiBold, color = TextPrimary, maxLines = 2)
                if (dateStr != null) {
                    Spacer(Modifier.height(4.dp))
                    Text(dateStr, fontSize = 13.sp, color = TextSecondary)
                }
                if (!meeting?.address.isNullOrBlank()) {
                    Spacer(Modifier.height(2.dp))
                    Text(meeting!!.address!!, fontSize = 12.sp, color = TextSecondary, maxLines = 1)
                }
            }
            Icon(Icons.Outlined.ChevronRight, null, tint = IconInactive, modifier = Modifier.size(20.dp))
        }
    }
}
