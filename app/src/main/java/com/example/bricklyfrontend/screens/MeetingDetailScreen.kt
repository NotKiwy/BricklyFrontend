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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.viewinterop.AndroidView
import com.example.bricklyfrontend.data.TicketCreateDTO
import com.example.bricklyfrontend.data.UserPreferences
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.bricklyfrontend.data.MeetingDefaultDTO
import com.example.bricklyfrontend.data.RetrofitClient
import com.example.bricklyfrontend.ui.theme.*
import org.osmdroid.config.Configuration
import org.osmdroid.tileprovider.tilesource.TileSourceFactory
import org.osmdroid.util.GeoPoint
import org.osmdroid.views.MapView
import org.osmdroid.views.overlay.Marker
import java.io.File
import java.net.URLEncoder
import java.time.OffsetDateTime
import java.time.format.DateTimeFormatter
import java.util.Locale

@Composable
fun MeetingDetailScreen(
    meetingId: Long,
    onBack: () -> Unit,
    onNavigateToEditProfile: () -> Unit = {},
    onNavigateToMyTickets: () -> Unit = {}
) {
    SetStatusBarColor(Accent)

    val context = LocalContext.current
    val userId = UserPreferences.getUserId(context)
    val scope = rememberCoroutineScope()

    var meeting by remember { mutableStateOf<MeetingDefaultDTO?>(null) }
    var isLoading by remember { mutableStateOf(true) }
    var errorMessage by remember { mutableStateOf<String?>(null) }
    var isMapFullscreen by remember { mutableStateOf(false) }
    var parentScrollEnabled by remember { mutableStateOf(true) }
    val scrollState = rememberScrollState()
    var existingTicketId by remember { mutableStateOf<Long?>(null) }
    var isRegistering by remember { mutableStateOf(false) }

    LaunchedEffect(meetingId) {
        try {
            val response = RetrofitClient.api.getMeetingById(meetingId)
            if (response.isSuccessful) meeting = response.body()
            else errorMessage = "Ошибка загрузки (${response.code()})"

            val ticketsResp = RetrofitClient.api.getTicketsByUserId(userId)
            if (ticketsResp.isSuccessful) {
                val match = ticketsResp.body()?.find { it.meeting?.id == meetingId }
                if (match != null) existingTicketId = match.id
            }
        } catch (e: Exception) {
            errorMessage = "Нет соединения с сервером"
        }
        isLoading = false
    }

    if (isMapFullscreen && meeting != null) {
        FullscreenMap(
            address = meeting!!.address,
            onClose = { isMapFullscreen = false }
        )
        return
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
                    .padding(horizontal = 8.dp, vertical = 8.dp)
            ) {
                IconButton(onClick = onBack, modifier = Modifier.align(Alignment.CenterStart)) {
                    Icon(Icons.Outlined.ArrowBackIosNew, "Назад", tint = TextPrimary)
                }
                Text(
                    text = "Подробности",
                    color = Color(0xFF1A1A1A),
                    fontWeight = FontWeight.Bold,
                    fontSize = 18.sp,
                    modifier = Modifier.align(Alignment.Center)
                )
            }
        }
    ) { padding ->
        when {
            isLoading -> DetailPageSkeleton()

            errorMessage != null -> Box(Modifier.fillMaxSize().padding(32.dp), contentAlignment = Alignment.Center) {
                Text(errorMessage!!, color = ErrorColor, textAlign = TextAlign.Center)
            }

            meeting != null -> {
                val m = meeting!!
                val dateFormatted = m.date?.let { formatDetailDate(it) }

                Column(modifier = Modifier.fillMaxSize().padding(padding).verticalScroll(scrollState, enabled = parentScrollEnabled)) {
                    Spacer(Modifier.height(16.dp))

                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(220.dp)
                            .padding(horizontal = 20.dp)
                            .clip(RoundedCornerShape(24.dp))
                    ) {
                        MeetingMap(
                            address = m.address,
                            modifier = Modifier.fillMaxSize(),
                            onFullscreen = { isMapFullscreen = true },
                            onTouchStart = { parentScrollEnabled = false },
                            onTouchEnd = { parentScrollEnabled = true }
                        )
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
                                text = m.title?.takeIf { it.isNotBlank() } ?: m.type?.description ?: m.description?.take(50) ?: "Без названия",
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
                                text = if (m.ticketPrice != null && m.ticketPrice > 0) "${m.ticketPrice} ₽" else "Бесплатно",
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

                    val isFree = m.ticketPrice == null || m.ticketPrice == 0

                    Card(
                        modifier = Modifier.fillMaxWidth().padding(horizontal = 20.dp),
                        shape = RoundedCornerShape(24.dp),
                        colors = CardDefaults.cardColors(containerColor = CardBackground),
                        elevation = CardDefaults.cardElevation(0.dp)
                    ) {
                        Column(modifier = Modifier.fillMaxWidth().padding(20.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                            if (isFree) {
                                when {
                                    existingTicketId != null -> {
                                        Icon(Icons.Outlined.CheckCircle, null, tint = Accent, modifier = Modifier.size(36.dp))
                                        Spacer(Modifier.height(8.dp))
                                        Text(
                                            "Вы записаны на мероприятие",
                                            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                                            color = TextPrimary,
                                            textAlign = TextAlign.Center
                                        )
                                        Spacer(Modifier.height(16.dp))
                                        Button(
                                            onClick = onNavigateToMyTickets,
                                            modifier = Modifier.fillMaxWidth().height(52.dp),
                                            shape = RoundedCornerShape(14.dp),
                                            colors = ButtonDefaults.buttonColors(containerColor = Accent, contentColor = TextPrimary)
                                        ) {
                                            Icon(Icons.Outlined.ConfirmationNumber, null, modifier = Modifier.size(18.dp))
                                            Spacer(Modifier.width(8.dp))
                                            Text("Мои билеты", fontWeight = FontWeight.SemiBold)
                                        }
                                    }
                                    isRegistering -> {
                                        CircularProgressIndicator(color = Accent, modifier = Modifier.size(36.dp))
                                        Spacer(Modifier.height(8.dp))
                                        Text("Регистрируем...", style = MaterialTheme.typography.bodyMedium, color = TextSecondary)
                                    }
                                    else -> {
                                        Text(
                                            "Бесплатное мероприятие",
                                            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                                            color = TextPrimary
                                        )
                                        Spacer(Modifier.height(16.dp))
                                        Button(
                                            onClick = {
                                                scope.launch {
                                                    isRegistering = true
                                                    try {
                                                        val userResp = RetrofitClient.api.getUserById(userId)
                                                        val name = userResp.body()?.name
                                                        if (name.isNullOrBlank()) {
                                                            isRegistering = false
                                                            onNavigateToEditProfile()
                                                        } else {
                                                            val resp = RetrofitClient.api.createTicket(
                                                                TicketCreateDTO(userId, meetingId, 0, 0)
                                                            )
                                                            if (resp.isSuccessful) existingTicketId = resp.body()?.id
                                                            isRegistering = false
                                                        }
                                                    } catch (_: Exception) {
                                                        isRegistering = false
                                                    }
                                                }
                                            },
                                            modifier = Modifier.fillMaxWidth().height(52.dp),
                                            shape = RoundedCornerShape(14.dp),
                                            colors = ButtonDefaults.buttonColors(containerColor = Accent, contentColor = TextPrimary)
                                        ) {
                                            Icon(Icons.Outlined.ConfirmationNumber, null, modifier = Modifier.size(18.dp))
                                            Spacer(Modifier.width(8.dp))
                                            Text("Записаться", fontWeight = FontWeight.SemiBold)
                                        }
                                    }
                                }
                            } else {
                                Text(
                                    "Стоимость билета",
                                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                                    color = TextPrimary
                                )
                                Spacer(Modifier.height(8.dp))
                                Text(
                                    "${m.ticketPrice} ₽",
                                    style = MaterialTheme.typography.headlineMedium.copy(fontWeight = FontWeight.ExtraBold),
                                    color = TextPrimary
                                )
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

@Composable
private fun MeetingMap(
    address: String?,
    modifier: Modifier = Modifier,
    onFullscreen: () -> Unit = {},
    onTouchStart: () -> Unit = {},
    onTouchEnd: () -> Unit = {}
) {
    val context = LocalContext.current
    var geoPoint by remember { mutableStateOf<GeoPoint?>(null) }

    LaunchedEffect(address) {
        if (!address.isNullOrBlank()) {
            geoPoint = geocodeAddress(address)
        }
    }

    val mapView = remember {
        Configuration.getInstance().apply {
            userAgentValue = context.packageName
            osmdroidBasePath = File(context.cacheDir, "osmdroid")
            osmdroidTileCache = File(context.cacheDir, "osmdroid/tiles")
        }
        MapView(context).apply {
            setTileSource(TileSourceFactory.MAPNIK)
            setMultiTouchControls(true)
            controller.setZoom(15.0)
            controller.setCenter(GeoPoint(55.7558, 37.6173))
        }
    }

    DisposableEffect(Unit) {
        onDispose { mapView.onDetach() }
    }

    LaunchedEffect(geoPoint) {
        geoPoint?.let { point ->
            mapView.controller.setCenter(point)
            mapView.controller.setZoom(15.0)
            mapView.overlays.clear()
            val marker = Marker(mapView)
            marker.position = point
            marker.setAnchor(Marker.ANCHOR_CENTER, Marker.ANCHOR_BOTTOM)
            mapView.overlays.add(marker)
            mapView.invalidate()
        }
    }

    Box(modifier = modifier) {
        AndroidView(
            factory = { mapView },
            modifier = Modifier.fillMaxSize(),
            update = { view ->
                view.setOnTouchListener { _, event ->
                    when (event.action) {
                        android.view.MotionEvent.ACTION_DOWN -> onTouchStart()
                        android.view.MotionEvent.ACTION_UP,
                        android.view.MotionEvent.ACTION_CANCEL -> onTouchEnd()
                    }
                    false
                }
            }
        )
        IconButton(
            onClick = onFullscreen,
            modifier = Modifier
                .align(Alignment.TopEnd)
                .padding(8.dp)
                .size(40.dp)
                .clip(RoundedCornerShape(12.dp))
                .background(Color.White.copy(alpha = 0.92f))
        ) {
            Icon(Icons.Outlined.Fullscreen, "На весь экран", tint = TextPrimary)
        }
    }
}

@Composable
private fun FullscreenMap(address: String?, onClose: () -> Unit) {
    SetStatusBarColor(Color.Black)
    val context = LocalContext.current
    var geoPoint by remember { mutableStateOf<GeoPoint?>(null) }

    LaunchedEffect(address) {
        if (!address.isNullOrBlank()) {
            geoPoint = geocodeAddress(address)
        }
    }

    val mapView = remember {
        Configuration.getInstance().apply {
            userAgentValue = context.packageName
            osmdroidBasePath = File(context.cacheDir, "osmdroid")
            osmdroidTileCache = File(context.cacheDir, "osmdroid/tiles")
        }
        MapView(context).apply {
            setTileSource(TileSourceFactory.MAPNIK)
            setMultiTouchControls(true)
            controller.setZoom(15.0)
            controller.setCenter(GeoPoint(55.7558, 37.6173))
        }
    }

    DisposableEffect(Unit) {
        onDispose { mapView.onDetach() }
    }

    LaunchedEffect(geoPoint) {
        geoPoint?.let { point ->
            mapView.controller.setCenter(point)
            mapView.controller.setZoom(16.0)
            mapView.overlays.clear()
            val marker = Marker(mapView)
            marker.position = point
            marker.setAnchor(Marker.ANCHOR_CENTER, Marker.ANCHOR_BOTTOM)
            mapView.overlays.add(marker)
            mapView.invalidate()
        }
    }

    Box(modifier = Modifier.fillMaxSize().background(Color.Black)) {
        AndroidView(
            factory = { mapView },
            modifier = Modifier.fillMaxSize()
        )
        IconButton(
            onClick = onClose,
            modifier = Modifier
                .align(Alignment.TopStart)
                .statusBarsPadding()
                .padding(12.dp)
                .size(44.dp)
                .clip(RoundedCornerShape(14.dp))
                .background(Color.White.copy(alpha = 0.92f))
        ) {
            Icon(Icons.Outlined.ArrowBackIosNew, "Закрыть", tint = TextPrimary)
        }
    }
}

private suspend fun geocodeAddress(address: String): GeoPoint? = withContext(Dispatchers.IO) {
    try {
        val encoded = URLEncoder.encode(address, "UTF-8")
        val client = okhttp3.OkHttpClient()
        val request = okhttp3.Request.Builder()
            .url("https://nominatim.openstreetmap.org/search?q=$encoded&format=json&limit=1")
            .header("User-Agent", "BricklyApp/1.0")
            .build()
        val resp = client.newCall(request).execute()
        val body = resp.body?.string() ?: return@withContext null
        val arr = org.json.JSONArray(body)
        if (arr.length() > 0) {
            val obj = arr.getJSONObject(0)
            GeoPoint(obj.getDouble("lat"), obj.getDouble("lon"))
        } else null
    } catch (_: Exception) { null }
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
