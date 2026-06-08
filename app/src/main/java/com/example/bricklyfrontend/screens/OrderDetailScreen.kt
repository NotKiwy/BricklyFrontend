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
import kotlin.math.roundToInt

@Composable
fun OrderDetailScreen(
    orderId: Long,
    onBack: () -> Unit,
    onNavigate: (String) -> Unit = {}
) {
    SetStatusBarColor(Accent)
    val context = LocalContext.current
    val scope = rememberCoroutineScope()

    val userId = remember { UserPreferences.getUserId(context) }
    val username = remember { UserPreferences.getUsername(context) }

    val imageLoader = remember {
        val u = UserPreferences.getUsername(context)
        val p = UserPreferences.getPassword(context)
        val client = OkHttpClient.Builder()
            .addInterceptor { chain ->
                chain.proceed(chain.request().newBuilder().header("Authorization", Credentials.basic(u, p)).build())
            }
            .build()
        ImageLoader.Builder(context).okHttpClient(client).build()
    }

    var order by remember { mutableStateOf<OrderDefaultDTO?>(null) }
    var listings by remember { mutableStateOf<Map<Long, ListingDefaultDTO>>(emptyMap()) }
    var isLoading by remember { mutableStateOf(true) }
    var errorMessage by remember { mutableStateOf<String?>(null) }
    var updatingItemId by remember { mutableStateOf<Long?>(null) }
    var submittedFeedbacks by remember { mutableStateOf<Set<Long>>(emptySet()) }

    var feedbackItem by remember { mutableStateOf<OrderItemDefaultDTO?>(null) }
    var feedbackRating by remember { mutableStateOf(5) }
    var feedbackComment by remember { mutableStateOf("") }
    var feedbackLoading by remember { mutableStateOf(false) }

    fun loadOrder() {
        scope.launch {
            isLoading = true
            try {
                val resp = RetrofitClient.api.getOrdersByUserId(userId)
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
                                    isUpdating = updatingItemId == item.id,
                                    feedbackSubmitted = item.id in submittedFeedbacks,
                                    onCancel = {
                                        scope.launch {
                                            updatingItemId = item.id
                                            try {
                                                RetrofitClient.api.updateOrderItem(item.id, OrderItemUpdateDTO("canceled"))
                                                loadOrder()
                                            } catch (_: Exception) {}
                                            updatingItemId = null
                                        }
                                    },
                                    onConfirmReceived = {
                                        scope.launch {
                                            updatingItemId = item.id
                                            try {
                                                RetrofitClient.api.updateOrderItem(item.id, OrderItemUpdateDTO("received"))
                                                loadOrder()
                                            } catch (_: Exception) {}
                                            updatingItemId = null
                                        }
                                    },
                                    onLeaveFeedback = {
                                        feedbackItem = item
                                        feedbackRating = 5
                                        feedbackComment = ""
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

    val currentFeedbackItem = feedbackItem
    if (currentFeedbackItem != null) {
        AlertDialog(
            onDismissRequest = { if (!feedbackLoading) feedbackItem = null },
            containerColor = CardBackground,
            title = {
                Text("Оставить отзыв", fontWeight = FontWeight.Bold, color = TextPrimary)
            },
            text = {
                Column {
                    val sellerName = listings[currentFeedbackItem.listingId]?.seller?.username ?: "продавцу"
                    Text("Оценка для @$sellerName", fontSize = 13.sp, color = TextSecondary)
                    Spacer(Modifier.height(12.dp))
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            "$feedbackRating",
                            fontSize = 28.sp,
                            fontWeight = FontWeight.ExtraBold,
                            color = TextPrimary,
                            modifier = Modifier.width(36.dp)
                        )
                        Text("/10", fontSize = 14.sp, color = TextSecondary)
                    }
                    Slider(
                        value = feedbackRating.toFloat(),
                        onValueChange = { feedbackRating = it.roundToInt() },
                        valueRange = 1f..10f,
                        steps = 8,
                        colors = SliderDefaults.colors(
                            thumbColor = Accent,
                            activeTrackColor = Accent,
                            inactiveTrackColor = Divider
                        )
                    )
                    Spacer(Modifier.height(8.dp))
                    OutlinedTextField(
                        value = feedbackComment,
                        onValueChange = { feedbackComment = it },
                        modifier = Modifier.fillMaxWidth(),
                        placeholder = { Text("Комментарий (необязательно)", color = TextSecondary.copy(alpha = 0.5f)) },
                        shape = RoundedCornerShape(12.dp),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = Accent,
                            unfocusedBorderColor = Divider,
                            focusedContainerColor = Color.White,
                            unfocusedContainerColor = Color.White,
                            cursorColor = Accent
                        ),
                        minLines = 2,
                        maxLines = 4,
                        textStyle = MaterialTheme.typography.bodyMedium.copy(color = TextPrimary)
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        val item = feedbackItem ?: return@Button
                        val sellerId = listings[item.listingId]?.seller?.id ?: return@Button
                        scope.launch {
                            feedbackLoading = true
                            try {
                                RetrofitClient.api.createFeedback(
                                    FeedbackCreateDTO(
                                        target_id = sellerId,
                                        author = UserShortDTO(id = userId, username = username, name = null),
                                        rate = feedbackRating,
                                        comment = feedbackComment.takeIf { it.isNotBlank() }
                                    )
                                )
                                submittedFeedbacks = submittedFeedbacks + item.id
                                feedbackItem = null
                            } catch (_: Exception) {}
                            feedbackLoading = false
                        }
                    },
                    enabled = !feedbackLoading,
                    shape = RoundedCornerShape(12.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = Accent, contentColor = TextPrimary)
                ) {
                    if (feedbackLoading) {
                        CircularProgressIndicator(modifier = Modifier.size(16.dp), color = TextPrimary, strokeWidth = 2.dp)
                    } else {
                        Text("Отправить", fontWeight = FontWeight.SemiBold)
                    }
                }
            },
            dismissButton = {
                TextButton(
                    onClick = { feedbackItem = null },
                    enabled = !feedbackLoading
                ) {
                    Text("Отмена", color = TextSecondary)
                }
            }
        )
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
    isUpdating: Boolean,
    feedbackSubmitted: Boolean,
    onCancel: () -> Unit,
    onConfirmReceived: () -> Unit,
    onLeaveFeedback: () -> Unit
) {
    val imageUrl = listing?.listingImage
        ?.firstOrNull { it.positionId == 0 }?.imagePath
        ?.takeIf { it.isNotBlank() }
        ?.let { if (it.startsWith("http")) it else "${RetrofitClient.BASE_URL}/${it.trimStart('/')}" }

    val isCanceled = item.status == "canceled"
    val isReceived = item.status == "received"
    val isProcessing = item.status == "processing"
    val isOnConfirmation = item.status == "on_confirmation"

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.cardColors(
            containerColor = if (isCanceled) Color.White.copy(alpha = 0.5f) else Color.White
        ),
        elevation = CardDefaults.cardElevation(0.dp)
    ) {
        Column(modifier = Modifier.fillMaxWidth().padding(10.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
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
                    Spacer(Modifier.height(4.dp))
                    val statusText = when (item.status) {
                        "on_confirmation" -> "Ожидает подтверждения"
                        "processing" -> "В обработке"
                        "received" -> "Получен"
                        "canceled" -> "Отменён"
                        else -> item.status ?: ""
                    }
                    val statusColor = when (item.status) {
                        "processing" -> Color(0xFF2E7D32)
                        "received" -> Color(0xFF1565C0)
                        "canceled" -> ErrorColor
                        else -> TextSecondary
                    }
                    if (statusText.isNotBlank()) {
                        Text(statusText, fontSize = 11.sp, fontWeight = FontWeight.Medium, color = statusColor)
                    }
                }

                if (isOnConfirmation) {
                    TextButton(onClick = onCancel, enabled = !isUpdating) {
                        if (isUpdating) {
                            CircularProgressIndicator(modifier = Modifier.size(14.dp), color = ErrorColor, strokeWidth = 2.dp)
                        } else {
                            Text("Отменить", fontSize = 12.sp, color = ErrorColor)
                        }
                    }
                }
            }

            if (isProcessing) {
                Spacer(Modifier.height(8.dp))
                Button(
                    onClick = onConfirmReceived,
                    enabled = !isUpdating,
                    modifier = Modifier.fillMaxWidth().height(40.dp),
                    shape = RoundedCornerShape(10.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = Accent, contentColor = TextPrimary)
                ) {
                    if (isUpdating) {
                        CircularProgressIndicator(modifier = Modifier.size(16.dp), color = TextPrimary, strokeWidth = 2.dp)
                    } else {
                        Text("Подтвердить получение", fontSize = 13.sp, fontWeight = FontWeight.SemiBold)
                    }
                }
            }

            if (isReceived) {
                Spacer(Modifier.height(8.dp))
                if (feedbackSubmitted) {
                    Text(
                        "Отзыв оставлен",
                        fontSize = 12.sp,
                        color = Color(0xFF1565C0),
                        fontWeight = FontWeight.Medium,
                        modifier = Modifier.fillMaxWidth()
                    )
                } else {
                    OutlinedButton(
                        onClick = onLeaveFeedback,
                        modifier = Modifier.fillMaxWidth().height(40.dp),
                        shape = RoundedCornerShape(10.dp),
                        colors = ButtonDefaults.outlinedButtonColors(contentColor = Accent)
                    ) {
                        Icon(Icons.Outlined.StarOutline, null, modifier = Modifier.size(16.dp))
                        Spacer(Modifier.width(6.dp))
                        Text("Оставить отзыв", fontSize = 13.sp, fontWeight = FontWeight.SemiBold)
                    }
                }
            }
        }
    }
}
