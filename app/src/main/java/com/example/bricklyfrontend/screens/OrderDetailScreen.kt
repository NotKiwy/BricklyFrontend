package com.example.bricklyfrontend.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
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
import java.time.OffsetDateTime
import java.time.format.DateTimeFormatter
import java.util.Locale

@Composable
fun OrderDetailScreen(
    orderId: Long,
    onBack: () -> Unit,
    onNavigate: (String) -> Unit = {}
) {
    SetStatusBarColor(Accent)
    val context = LocalContext.current
    val scope = rememberCoroutineScope()

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

    var order by remember { mutableStateOf<OrderDefaultDTO?>(null) }
    var listings by remember { mutableStateOf<Map<Long, ListingDefaultDTO>>(emptyMap()) }
    var isLoading by remember { mutableStateOf(true) }
    var errorMessage by remember { mutableStateOf<String?>(null) }

    fun loadOrder() {
        scope.launch {
            isLoading = true
            try {
                val resp = RetrofitClient.api.getOrdersByUserId(UserPreferences.getUserId(context))
                if (resp.isSuccessful) {
                    val found = resp.body()?.content?.find { it.id == orderId }
                    order = found
                    if (found == null) {
                        errorMessage = "Заказ не найден"
                    } else {
                        val map = mutableMapOf<Long, ListingDefaultDTO>()
                        found.orderItems?.forEach { item ->
                            try {
                                val lr = RetrofitClient.api.getListingById(item.listingId)
                                if (lr.isSuccessful) lr.body()?.let { map[item.listingId] = it }
                            } catch (_: Exception) {}
                        }
                        listings = map
                    }
                } else {
                    errorMessage = "Ошибка загрузки (${resp.code()})"
                }
            } catch (_: Exception) {
                errorMessage = "Нет соединения"
            }
            isLoading = false
        }
    }

    LaunchedEffect(orderId) { loadOrder() }

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
                    "Заказ #$orderId",
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
            order != null -> {
                val o = order!!
                val dateStr = o.createdAt?.let { dateStr ->
                    try {
                        OffsetDateTime.parse(dateStr).format(DateTimeFormatter.ofPattern("d MMMM yyyy, HH:mm", Locale("ru")))
                    } catch (_: Exception) { dateStr }
                }

                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(padding)
                        .verticalScroll(rememberScrollState())
                ) {
                    Spacer(Modifier.height(16.dp))

                    Card(
                        modifier = Modifier.fillMaxWidth().padding(horizontal = 20.dp),
                        shape = RoundedCornerShape(24.dp),
                        colors = CardDefaults.cardColors(containerColor = CardBackground),
                        elevation = CardDefaults.cardElevation(0.dp)
                    ) {
                        Column(modifier = Modifier.padding(20.dp)) {
                            Text(
                                "Информация о заказе",
                                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                                color = TextPrimary
                            )
                            Spacer(Modifier.height(16.dp))
                            OrderInfoRow(label = "Дата создания", value = dateStr ?: "—")
                            Spacer(Modifier.height(10.dp))
                            OrderInfoRow(
                                label = "Способ получения",
                                value = when (o.shippingMethod) {
                                    "shipping" -> "Самовывоз"
                                    "delivery" -> "Доставка"
                                    else -> o.shippingMethod ?: "—"
                                }
                            )
                            Spacer(Modifier.height(10.dp))
                            OrderInfoRow(label = "Адрес", value = o.shippingAddress ?: "—")
                        }
                    }

                    Spacer(Modifier.height(16.dp))

                    Text(
                        "ТОВАРЫ",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = TextPrimary.copy(alpha = 0.5f),
                        letterSpacing = 0.8.sp,
                        modifier = Modifier.padding(horizontal = 32.dp).padding(bottom = 8.dp)
                    )

                    Card(
                        modifier = Modifier.fillMaxWidth().padding(horizontal = 20.dp),
                        shape = RoundedCornerShape(20.dp),
                        colors = CardDefaults.cardColors(containerColor = CardBackground),
                        elevation = CardDefaults.cardElevation(0.dp)
                    ) {
                        Column(modifier = Modifier.padding(10.dp)) {
                            o.orderItems?.forEach { item ->
                                OrderItemCard(
                                    item = item,
                                    listing = listings[item.listingId],
                                    imageLoader = imageLoader,
                                    onCancel = {
                                        scope.launch {
                                            try {
                                                RetrofitClient.api.updateOrderItem(item.id, OrderItemUpdateDTO("canceled"))
                                                loadOrder()
                                            } catch (_: Exception) {}
                                        }
                                    }
                                )
                                Spacer(Modifier.height(8.dp))
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
private fun OrderInfoRow(label: String, value: String) {
    Column {
        Text(label, style = MaterialTheme.typography.labelSmall, color = TextSecondary)
        Text(
            value,
            style = MaterialTheme.typography.bodyLarge.copy(fontWeight = FontWeight.SemiBold),
            color = TextPrimary
        )
    }
}

@Composable
private fun OrderItemCard(
    item: OrderItemDefaultDTO,
    listing: ListingDefaultDTO?,
    imageLoader: ImageLoader,
    onCancel: () -> Unit
) {
    val imageUrl = listing?.listingImage
        ?.firstOrNull { it.positionId == 0 }?.imagePath
        ?.takeIf { it.isNotBlank() }
        ?.let { if (it.startsWith("http")) it else "${RetrofitClient.BASE_URL}/${it.trimStart('/')}" }

    val isCanceled = item.status == "canceled"

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.cardColors(
            containerColor = if (isCanceled) Color.White.copy(alpha = 0.5f) else Color.White
        ),
        elevation = CardDefaults.cardElevation(0.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(10.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            SubcomposeAsyncImage(
                model = ImageRequest.Builder(LocalContext.current).data(imageUrl).crossfade(true).build(),
                imageLoader = imageLoader,
                contentDescription = null,
                modifier = Modifier.size(70.dp).clip(RoundedCornerShape(10.dp)),
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
            Spacer(Modifier.width(10.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    listing?.itemId ?: "Товар",
                    fontSize = 14.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = if (isCanceled) TextSecondary else TextPrimary,
                    maxLines = 2
                )
                Spacer(Modifier.height(4.dp))
                Text(
                    "×${item.quantity} · ${item.price} ₽",
                    fontSize = 12.sp,
                    color = TextSecondary
                )
                if (isCanceled) {
                    Spacer(Modifier.height(4.dp))
                    Text(
                        "Отменен",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Medium,
                        color = ErrorColor
                    )
                }
            }
            if (!isCanceled) {
                Spacer(Modifier.width(8.dp))
                TextButton(onClick = onCancel) {
                    Text("Отменить", fontSize = 12.sp, color = ErrorColor)
                }
            }
        }
    }
}
