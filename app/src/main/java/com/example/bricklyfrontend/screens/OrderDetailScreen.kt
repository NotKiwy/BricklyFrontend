package com.example.bricklyfrontend.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.interaction.MutableInteractionSource
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

@OptIn(ExperimentalMaterial3Api::class)
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
    var existingFeedbackSellerIds by remember { mutableStateOf<Set<Long>>(emptySet()) }
    var submittedFeedbackSellerIds by remember { mutableStateOf<Set<Long>>(emptySet()) }

    var feedbackItem by remember { mutableStateOf<OrderItemDefaultDTO?>(null) }
    var feedbackRating by remember { mutableStateOf(5) }
    var feedbackComment by remember { mutableStateOf("") }
    var feedbackLoading by remember { mutableStateOf(false) }
    var feedbackError by remember { mutableStateOf<String?>(null) }

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

                        try {
                            val fbResp = RetrofitClient.api.getFeedbacksByAuthorId(userId)
                            if (fbResp.isSuccessful) {
                                existingFeedbackSellerIds = fbResp.body()
                                    ?.map { it.target_id }
                                    ?.toSet() ?: emptySet()
                            }
                        } catch (_: Exception) {}
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

                val reviewedSellerIds = existingFeedbackSellerIds + submittedFeedbackSellerIds

                val firstReceivedItemBySeller: Map<Long, Long> = o.orderItems
                    ?.filter { it.status == "received" }
                    ?.groupBy { listings[it.listingId]?.seller?.id }
                    ?.filterKeys { it != null }
                    ?.mapKeys { it.key!! }
                    ?.mapValues { (_, items) -> items.first().id }
                    ?: emptyMap()

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
                                val sellerId = listings[item.listingId]?.seller?.id
                                val isFirstReceivedForSeller = sellerId != null &&
                                    firstReceivedItemBySeller[sellerId] == item.id
                                val sellerAlreadyReviewed = sellerId != null && sellerId in reviewedSellerIds

                                OrderItemCard(
                                    item = item,
                                    listing = listings[item.listingId],
                                    imageLoader = imageLoader,
                                    isUpdating = updatingItemId == item.id,
                                    showFeedbackButton = isFirstReceivedForSeller && !sellerAlreadyReviewed,
                                    feedbackAlreadyLeft = isFirstReceivedForSeller && sellerAlreadyReviewed,
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
                                        feedbackError = null
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
                    Spacer(Modifier.height(16.dp))
                    Text(
                        "$feedbackRating/5",
                        fontSize = 22.sp,
                        fontWeight = FontWeight.Bold,
                        color = TextPrimary,
                        modifier = Modifier.fillMaxWidth(),
                        textAlign = TextAlign.Center
                    )
                    Spacer(Modifier.height(4.dp))
                    Slider(
                        value = feedbackRating.toFloat(),
                        onValueChange = { feedbackRating = it.roundToInt() },
                        valueRange = 1f..5f,
                        interactionSource = remember { MutableInteractionSource() },
                        thumb = {
                            Box(
                                modifier = Modifier
                                    .size(16.dp)
                                    .background(Color.White, CircleShape)
                                    .border(2.dp, Color.Black, CircleShape)
                            )
                        },
                        track = { sliderState ->
                            val fraction = ((sliderState.value - sliderState.valueRange.start) /
                                    (sliderState.valueRange.endInclusive - sliderState.valueRange.start))
                                .coerceIn(0f, 1f)
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(6.dp)
                                    .border(1.dp, Color.Black, RoundedCornerShape(3.dp))
                                    .clip(RoundedCornerShape(3.dp))
                                    .background(Color(0xFFE0E0E0))
                            ) {
                                Box(
                                    modifier = Modifier
                                        .fillMaxWidth(fraction)
                                        .fillMaxHeight()
                                        .background(Accent)
                                )
                            }
                        }
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
                    if (feedbackError != null) {
                        Spacer(Modifier.height(8.dp))
                        Text(feedbackError!!, color = ErrorColor, fontSize = 12.sp)
                    }
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
                                val resp = RetrofitClient.api.createFeedback(
                                    FeedbackCreateDTO(
                                        target_id = sellerId,
                                        author_id = userId,
                                        rate = feedbackRating,
                                        comment = feedbackComment.takeIf { it.isNotBlank() }
                                    )
                                )
                                if (resp.isSuccessful) {
                                    submittedFeedbackSellerIds = submittedFeedbackSellerIds + sellerId
                                    feedbackItem = null
                                } else if (resp.code() == 400) {
                                    feedbackError = "Нельзя оставить отзыв самому себе"
                                } else {
                                    feedbackError = "Ошибка отправки (${resp.code()})"
                                }
                            } catch (_: Exception) {
                                feedbackError = "Нет соединения"
                            }
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
    showFeedbackButton: Boolean,
    feedbackAlreadyLeft: Boolean,
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
                    modifier = Modifier.size(80.dp).clip(RoundedCornerShape(14.dp)),
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
                }
                Spacer(Modifier.width(8.dp))
                val statusLabel = when (item.status) {
                    "on_confirmation" -> "Ожидает"
                    "processing" -> "В обработке"
                    "received" -> "Получен"
                    "canceled" -> "Отменён"
                    else -> item.status ?: ""
                }
                val statusColor = when (item.status) {
                    "on_confirmation" -> Color(0xFFE65100)
                    "processing" -> Color(0xFF2E7D32)
                    "received" -> Color(0xFF1565C0)
                    "canceled" -> ErrorColor
                    else -> TextSecondary
                }
                if (statusLabel.isNotBlank()) {
                    Surface(
                        shape = RoundedCornerShape(8.dp),
                        color = statusColor.copy(alpha = 0.15f)
                    ) {
                        Text(
                            statusLabel,
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                            fontSize = 11.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = statusColor
                        )
                    }
                }
            }

            if (isOnConfirmation) {
                Spacer(Modifier.height(8.dp))
                OutlinedButton(
                    onClick = onCancel,
                    enabled = !isUpdating,
                    modifier = Modifier.fillMaxWidth().height(40.dp),
                    shape = RoundedCornerShape(10.dp),
                    colors = ButtonDefaults.outlinedButtonColors(contentColor = ErrorColor)
                ) {
                    if (isUpdating) {
                        CircularProgressIndicator(modifier = Modifier.size(16.dp), color = ErrorColor, strokeWidth = 2.dp)
                    } else {
                        Text("Отменить заказ", fontSize = 13.sp, fontWeight = FontWeight.SemiBold)
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
                when {
                    feedbackAlreadyLeft -> {
                        Spacer(Modifier.height(8.dp))
                        Text(
                            "Отзыв оставлен",
                            fontSize = 12.sp,
                            color = Color(0xFF1565C0),
                            fontWeight = FontWeight.Medium,
                            modifier = Modifier.fillMaxWidth()
                        )
                    }
                    showFeedbackButton -> {
                        Spacer(Modifier.height(8.dp))
                        OutlinedButton(
                            onClick = onLeaveFeedback,
                            modifier = Modifier.fillMaxWidth().height(40.dp),
                            shape = RoundedCornerShape(10.dp),
                            colors = ButtonDefaults.outlinedButtonColors(contentColor = TextPrimary),
                            border = androidx.compose.foundation.BorderStroke(1.dp, Divider)
                        ) {
                            Icon(Icons.Outlined.StarOutline, null, modifier = Modifier.size(16.dp), tint = TextPrimary)
                            Spacer(Modifier.width(6.dp))
                            Text("Оставить отзыв", fontSize = 13.sp, fontWeight = FontWeight.SemiBold, color = TextPrimary)
                        }
                    }
                }
            }
        }
    }
}
