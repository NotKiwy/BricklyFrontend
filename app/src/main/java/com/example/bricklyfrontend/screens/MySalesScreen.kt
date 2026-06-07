package com.example.bricklyfrontend.screens

import androidx.compose.foundation.background
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
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.ImageLoader
import coil.compose.SubcomposeAsyncImage
import coil.request.ImageRequest
import com.example.bricklyfrontend.data.*
import com.example.bricklyfrontend.ui.theme.*
import kotlinx.coroutines.launch
import okhttp3.Credentials
import okhttp3.OkHttpClient

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MySalesScreen(onBack: () -> Unit, onNavigate: (String) -> Unit = {}) {
    SetStatusBarColor(Accent)

    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val userId = UserPreferences.getUserId(context)

    val imageLoader = remember {
        val username = UserPreferences.getUsername(context)
        val password = UserPreferences.getPassword(context)
        val client = OkHttpClient.Builder()
            .addInterceptor { chain ->
                chain.proceed(
                    chain.request().newBuilder()
                        .header("Authorization", Credentials.basic(username, password))
                        .build()
                )
            }
            .build()
        ImageLoader.Builder(context).okHttpClient(client).build()
    }

    var items by remember { mutableStateOf<List<OrderItemDefaultDTO>>(emptyList()) }
    var listings by remember { mutableStateOf<Map<Long, ListingDefaultDTO>>(emptyMap()) }
    var isLoading by remember { mutableStateOf(true) }
    var isRefreshing by remember { mutableStateOf(false) }
    var errorMessage by remember { mutableStateOf<String?>(null) }
    var updatingId by remember { mutableStateOf<Long?>(null) }

    fun load(silent: Boolean = false) {
        scope.launch {
            if (silent) isRefreshing = true else isLoading = true
            errorMessage = null
            try {
                val resp = RetrofitClient.api.getOrderItemsBySellerId(userId)
                if (resp.isSuccessful) {
                    val list = resp.body() ?: emptyList()
                    items = list.sortedBy { statusOrder(it.status) }
                    val map = mutableMapOf<Long, ListingDefaultDTO>()
                    list.map { it.listingId }.toSet().forEach { lid ->
                        try {
                            val lr = RetrofitClient.api.getListingById(lid)
                            if (lr.isSuccessful) lr.body()?.let { map[lid] = it }
                        } catch (_: Exception) {}
                    }
                    listings = map
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

    suspend fun updateStatus(item: OrderItemDefaultDTO, newStatus: String) {
        updatingId = item.id
        try {
            val resp = RetrofitClient.api.updateOrderItem(item.id, OrderItemUpdateDTO(newStatus))
            if (resp.isSuccessful) {
                items = items
                    .map { if (it.id == item.id) it.copy(status = newStatus) else it }
                    .sortedBy { statusOrder(it.status) }
            }
        } catch (_: Exception) {}
        updatingId = null
    }

    LaunchedEffect(Unit) { load() }

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
                    Icon(Icons.Outlined.ArrowBackIosNew, "Назад", tint = TextPrimary)
                }
                Text(
                    "Мои продажи",
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
            onRefresh = { load(silent = true) },
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
                            onClick = { load() },
                            colors = ButtonDefaults.buttonColors(containerColor = Accent, contentColor = TextPrimary)
                        ) { Text("Повторить") }
                    }
                }

                items.isEmpty() -> Box(
                    Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    EmptyStateBox(
                        icon = Icons.Outlined.Storefront,
                        title = "Продаж пока нет",
                        subtitle = "Когда покупатели оформят заказ, он появится здесь",
                        onRefresh = { load() }
                    )
                }

                else -> LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(vertical = 12.dp)
                ) {
                    items(items, key = { it.id }) { item ->
                        SaleItemCard(
                            item = item,
                            listing = listings[item.listingId],
                            imageLoader = imageLoader,
                            isUpdating = updatingId == item.id,
                            onConfirm = { scope.launch { updateStatus(item, "processing") } },
                            onCancel = { scope.launch { updateStatus(item, "canceled") } }
                        )
                    }
                }
            }
        }
    }
}

private fun statusOrder(status: String?): Int = when (status) {
    "on_confirmation" -> 0
    "processing" -> 1
    "canceled" -> 2
    else -> 3
}

@Composable
private fun SaleItemCard(
    item: OrderItemDefaultDTO,
    listing: ListingDefaultDTO?,
    imageLoader: ImageLoader,
    isUpdating: Boolean,
    onConfirm: () -> Unit,
    onCancel: () -> Unit
) {
    val imageUrl = listing?.listingImage
        ?.firstOrNull { it.positionId == 0 }?.imagePath
        ?.takeIf { it.isNotBlank() }
        ?.let { if (it.startsWith("http")) it else "${RetrofitClient.BASE_URL}/${it.trimStart('/')}" }

    val statusInfo = statusLabel(item.status)

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 6.dp),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = CardBackground),
        elevation = CardDefaults.cardElevation(0.dp)
    ) {
        Column(modifier = Modifier.padding(12.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                SubcomposeAsyncImage(
                    model = ImageRequest.Builder(LocalContext.current).data(imageUrl).crossfade(true).build(),
                    imageLoader = imageLoader,
                    contentDescription = null,
                    modifier = Modifier.size(72.dp).clip(RoundedCornerShape(12.dp)),
                    contentScale = ContentScale.Crop,
                    loading = { Box(Modifier.fillMaxSize().background(Accent.copy(alpha = 0.12f))) },
                    error = {
                        Box(
                            Modifier.fillMaxSize().background(Accent.copy(alpha = 0.12f)),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(Icons.Outlined.Inventory2, null, tint = Accent, modifier = Modifier.size(28.dp))
                        }
                    }
                )
                Spacer(Modifier.width(12.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        listing?.itemId?.takeIf { it.isNotBlank() } ?: "Товар #${item.listingId}",
                        fontSize = 15.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = TextPrimary,
                        maxLines = 2
                    )
                    Spacer(Modifier.height(4.dp))
                    Text(
                        "Заказ #${item.orderId} · ×${item.quantity ?: 1}",
                        fontSize = 12.sp,
                        color = TextSecondary
                    )
                    Spacer(Modifier.height(4.dp))
                    Text(
                        "${(item.price ?: 0) * (item.quantity ?: 1)} ₽",
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Bold,
                        color = TextPrimary
                    )
                }
                Surface(
                    shape = RoundedCornerShape(8.dp),
                    color = statusInfo.color.copy(alpha = 0.15f)
                ) {
                    Text(
                        statusInfo.label,
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                        fontSize = 11.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = statusInfo.color
                    )
                }
            }

            if (item.status == "on_confirmation") {
                Spacer(Modifier.height(12.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Button(
                        onClick = onConfirm,
                        enabled = !isUpdating,
                        modifier = Modifier.weight(1f).height(42.dp),
                        shape = RoundedCornerShape(12.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = Accent, contentColor = TextPrimary)
                    ) {
                        if (isUpdating) {
                            CircularProgressIndicator(modifier = Modifier.size(16.dp), color = TextPrimary, strokeWidth = 2.dp)
                        } else {
                            Text("Подтвердить", fontSize = 13.sp, fontWeight = FontWeight.SemiBold)
                        }
                    }
                    OutlinedButton(
                        onClick = onCancel,
                        enabled = !isUpdating,
                        modifier = Modifier.weight(1f).height(42.dp),
                        shape = RoundedCornerShape(12.dp),
                        colors = ButtonDefaults.outlinedButtonColors(contentColor = ErrorColor)
                    ) {
                        Text("Отменить", fontSize = 13.sp, fontWeight = FontWeight.SemiBold)
                    }
                }
            }
        }
    }
}

private data class StatusInfo(val label: String, val color: Color)

private fun statusLabel(status: String?): StatusInfo = when (status) {
    "on_confirmation" -> StatusInfo("Ожидает подтверждения", Color(0xFFE65100))
    "processing" -> StatusInfo("В обработке", Color(0xFF2E7D32))
    "canceled" -> StatusInfo("Отменён", ErrorColor)
    else -> StatusInfo(status ?: "—", TextSecondary)
}
