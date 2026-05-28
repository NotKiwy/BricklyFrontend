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

private data class CartEntry(
    val cartItem: CartItemDefaultDTO,
    val meeting: MeetingDefaultDTO? = null,
    val listing: ListingDefaultDTO? = null
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CartScreen(
    onNavigateToMeetings: () -> Unit = {},
    onNavigateToProfile: () -> Unit = {},
    onNavigateToHome: () -> Unit = {},
    onNavigateToBrickognize: () -> Unit = {},
    onNavigateToCheckout: (Int) -> Unit = {},
    onNavigateToListingDetail: (Long) -> Unit = {},
    onNavigateToMeetingDetail: (Long) -> Unit = {}
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

    var entries by remember { mutableStateOf<List<CartEntry>>(emptyList()) }
    var isLoading by remember { mutableStateOf(true) }
    var isRefreshing by remember { mutableStateOf(false) }
    var errorMessage by remember { mutableStateOf<String?>(null) }

    fun loadCart(silent: Boolean = false) {
        scope.launch {
            if (silent) isRefreshing = true else isLoading = true
            errorMessage = null
            try {
                val resp = RetrofitClient.api.getCartItemsByUserId(userId)
                if (resp.isSuccessful) {
                    val rawItems = resp.body() ?: emptyList()

                    val merged = mutableListOf<CartItemDefaultDTO>()
                    rawItems.groupBy { "${it.itemType}_${it.itemId}" }.forEach { (_, group) ->
                        if (group.size == 1) {
                            merged.add(group.first())
                        } else {
                            val totalQty = group.sumOf { it.quantity ?: 1 }
                            val primary = group.first()
                            group.drop(1).forEach { dup ->
                                try { RetrofitClient.api.deleteCartItem(dup.id) } catch (_: Exception) {}
                            }
                            try { RetrofitClient.api.updateCartItem(primary.id, CartItemUpdateDTO(totalQty)) } catch (_: Exception) {}
                            merged.add(primary.copy(quantity = totalQty))
                        }
                    }

                    entries = merged.map { item ->
                        when (item.itemType) {
                            "M" -> {
                                val r = item.itemId?.let { RetrofitClient.api.getMeetingById(it.toLong()) }
                                CartEntry(item, meeting = if (r?.isSuccessful == true) r.body() else null)
                            }
                            "L" -> {
                                val r = item.itemId?.let { RetrofitClient.api.getListingById(it.toLong()) }
                                CartEntry(item, listing = if (r?.isSuccessful == true) r.body() else null)
                            }
                            else -> CartEntry(item)
                        }
                    }

                    ListingCartState.items = entries
                        .filter { it.cartItem.itemType == "L" && it.listing != null }
                        .map { entry ->
                            ListingCartItem(
                                listingId = entry.cartItem.itemId?.toLong() ?: 0L,
                                title = entry.listing?.itemId ?: "",
                                unitPrice = entry.listing?.price ?: 0,
                                quantity = entry.cartItem.quantity ?: 1,
                                maxQuantity = entry.listing?.quantity ?: 1,
                                serverCartItemId = entry.cartItem.id
                            )
                        }
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

    LaunchedEffect(Unit) { loadCart() }

    fun deleteEntry(cartItemId: Long) {
        scope.launch {
            try {
                val entry = entries.find { it.cartItem.id == cartItemId }
                RetrofitClient.api.deleteCartItem(cartItemId)
                entries = entries.filter { it.cartItem.id != cartItemId }
                if (entry?.cartItem?.itemType == "L") {
                    val listingId = entry.cartItem.itemId?.toLong()
                    if (listingId != null) ListingCartState.removeItem(listingId)
                }
            } catch (_: Exception) {}
        }
    }

    fun updateQuantity(cartItemId: Long, newQty: Int, maxQty: Int) {
        if (newQty <= 0) {
            deleteEntry(cartItemId)
        } else {
            val capped = newQty.coerceAtMost(maxQty)
            entries = entries.map {
                if (it.cartItem.id == cartItemId) it.copy(cartItem = it.cartItem.copy(quantity = capped)) else it
            }
            scope.launch {
                try { RetrofitClient.api.updateCartItem(cartItemId, CartItemUpdateDTO(capped)) } catch (_: Exception) {}
            }
        }
    }

    val totalPrice = entries.sumOf { entry ->
        val qty = entry.cartItem.quantity ?: 1
        when (entry.cartItem.itemType) {
            "M" -> (entry.meeting?.ticketPrice ?: 0) * qty
            "L" -> (entry.listing?.price ?: 0) * qty
            else -> 0
        }
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
                    .padding(horizontal = 20.dp, vertical = 18.dp)
            ) {
                Text("Корзина", color = TextPrimary, fontWeight = FontWeight.ExtraBold, fontSize = 26.sp)
            }
        },
        bottomBar = {
            BricklyBottomBar(currentRoute = "cart", onNavigate = { route ->
                when (route) {
                    "meetings" -> onNavigateToMeetings()
                    "profile" -> onNavigateToProfile()
                    "home" -> onNavigateToHome()
                    "brickognize" -> onNavigateToBrickognize()
                }
            }, onScanClick = onNavigateToBrickognize)
        }
    ) { padding ->
        PullToRefreshBox(
            isRefreshing = isRefreshing,
            onRefresh = { loadCart(silent = true) },
            modifier = Modifier.fillMaxSize().padding(padding)
        ) {
        when {
            isLoading -> CartSkeleton(modifier = Modifier.fillMaxSize())

            errorMessage != null -> Box(
                Modifier.fillMaxSize().padding(32.dp),
                contentAlignment = Alignment.Center
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(errorMessage!!, color = ErrorColor, textAlign = TextAlign.Center)
                    Spacer(Modifier.height(12.dp))
                    Button(onClick = { loadCart() }, colors = ButtonDefaults.buttonColors(containerColor = Accent, contentColor = TextPrimary)) {
                        Text("Повторить")
                    }
                }
            }

            entries.isEmpty() -> Box(
                Modifier.fillMaxSize().padding(32.dp),
                contentAlignment = Alignment.Center
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Box(
                        modifier = Modifier.size(80.dp).clip(RoundedCornerShape(24.dp)).background(Accent.copy(alpha = 0.15f)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(Icons.Outlined.ShoppingCart, null, tint = Accent, modifier = Modifier.size(36.dp))
                    }
                    Spacer(Modifier.height(16.dp))
                    Text("Корзина пуста", style = MaterialTheme.typography.bodyLarge.copy(fontWeight = FontWeight.Bold), color = TextPrimary)
                    Spacer(Modifier.height(6.dp))
                    Text("Добавьте товары или билеты на мероприятия", style = MaterialTheme.typography.bodyMedium, color = TextSecondary, textAlign = TextAlign.Center)
                    Spacer(Modifier.height(16.dp))
                    TextButton(onClick = { loadCart() }) { Text("Обновить", color = TextSecondary) }
                }
            }

            else -> Column(modifier = Modifier.fillMaxSize()) {
                LazyColumn(
                    modifier = Modifier.weight(1f),
                    contentPadding = PaddingValues(vertical = 12.dp)
                ) {
                    items(entries, key = { it.cartItem.id }) { entry ->
                        val qty = entry.cartItem.quantity ?: 1
                        val maxQty = if (entry.cartItem.itemType == "L") entry.listing?.quantity ?: qty else 99
                        CartEntryCard(
                            entry = entry,
                            qty = qty,
                            maxQty = maxQty,
                            imageLoader = imageLoader,
                            onClick = {
                                when (entry.cartItem.itemType) {
                                    "L" -> entry.cartItem.itemId?.let { onNavigateToListingDetail(it.toLong()) }
                                    "M" -> entry.cartItem.itemId?.let { onNavigateToMeetingDetail(it.toLong()) }
                                }
                            },
                            onDelete = { deleteEntry(entry.cartItem.id) },
                            onDecrement = { updateQuantity(entry.cartItem.id, qty - 1, maxQty) },
                            onIncrement = { updateQuantity(entry.cartItem.id, qty + 1, maxQty) }
                        )
                    }
                }

                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(topStart = 28.dp, topEnd = 28.dp),
                    colors = CardDefaults.cardColors(containerColor = CardBackground),
                    elevation = CardDefaults.cardElevation(8.dp)
                ) {
                    Column(modifier = Modifier.padding(20.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column {
                                Text("Итого", style = MaterialTheme.typography.bodySmall, color = TextSecondary)
                                Text("$totalPrice ₽", style = MaterialTheme.typography.headlineSmall.copy(fontWeight = FontWeight.ExtraBold), color = TextPrimary)
                            }
                            Text("${entries.size} ${pluralPositions(entries.size)}", style = MaterialTheme.typography.bodyMedium, color = TextSecondary)
                        }
                        Spacer(Modifier.height(16.dp))
                        Button(
                            onClick = { onNavigateToCheckout(totalPrice) },
                            modifier = Modifier.fillMaxWidth().height(56.dp),
                            shape = RoundedCornerShape(16.dp),
                            colors = ButtonDefaults.buttonColors(containerColor = Accent, contentColor = TextPrimary)
                        ) {
                            Text("Оплатить", style = MaterialTheme.typography.bodyLarge.copy(fontWeight = FontWeight.Bold))
                        }
                    }
                }
            }
        }
        }
    }
}

@Composable
private fun CartEntryCard(
    entry: CartEntry,
    qty: Int,
    maxQty: Int,
    imageLoader: ImageLoader,
    onClick: () -> Unit,
    onDelete: () -> Unit,
    onDecrement: () -> Unit,
    onIncrement: () -> Unit
) {

    val imageUrl: String? = when (entry.cartItem.itemType) {
        "L" -> entry.listing?.listingImage
            ?.firstOrNull { it.positionId == 0 }?.imagePath
            ?.takeIf { it.isNotBlank() }
            ?.let { if (it.startsWith("http")) it else "${RetrofitClient.BASE_URL}/${it.trimStart('/')}" }
        "M" -> entry.meeting?.previewImagePath
            ?.takeIf { it.isNotBlank() }
            ?.let { "${RetrofitClient.BASE_URL}/${it.trimStart('/')}" }
        else -> null
    }

    val title = when (entry.cartItem.itemType) {
        "M" -> entry.meeting?.title?.takeIf { it.isNotBlank() } ?: entry.meeting?.type?.description ?: "Мероприятие"
        "L" -> entry.listing?.itemId?.takeIf { it.isNotBlank() } ?: "Товар"
        else -> "Товар #${entry.cartItem.itemId}"
    }

    val unitPrice = when (entry.cartItem.itemType) {
        "M" -> entry.meeting?.ticketPrice ?: 0
        "L" -> entry.listing?.price ?: 0
        else -> 0
    }

    val subtitle = if (entry.cartItem.itemType == "M") {
        entry.meeting?.date?.let { dateStr ->
            try { OffsetDateTime.parse(dateStr).format(DateTimeFormatter.ofPattern("d MMM, HH:mm", Locale.forLanguageTag("ru"))) }
            catch (_: Exception) { dateStr }
        }
    } else null

    Card(
        modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 6.dp).clickable(onClick = onClick),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = CardBackground),
        elevation = CardDefaults.cardElevation(0.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth().height(130.dp).padding(10.dp),
            verticalAlignment = Alignment.Top
        ) {
            SubcomposeAsyncImage(
                model = ImageRequest.Builder(LocalContext.current).data(imageUrl).crossfade(true).build(),
                imageLoader = imageLoader,
                contentDescription = null,
                modifier = Modifier.fillMaxHeight().width(110.dp).clip(RoundedCornerShape(14.dp)),
                contentScale = ContentScale.Crop,
                loading = { Box(Modifier.fillMaxSize().background(Accent.copy(alpha = 0.12f))) },
                error = {
                    Box(
                        Modifier.fillMaxSize().background(Accent.copy(alpha = 0.12f)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            if (entry.cartItem.itemType == "M") Icons.Outlined.ConfirmationNumber else Icons.Outlined.Inventory2,
                            null, tint = Accent, modifier = Modifier.size(32.dp)
                        )
                    }
                }
            )

            Spacer(Modifier.width(12.dp))

            Column(modifier = Modifier.weight(1f).fillMaxHeight()) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.Top
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(title, fontSize = 16.sp, fontWeight = FontWeight.SemiBold, color = TextPrimary, maxLines = 1)
                        if (subtitle != null) {
                            Spacer(Modifier.height(2.dp))
                            Text(subtitle, fontSize = 12.sp, color = TextSecondary, maxLines = 1)
                        } else if (entry.cartItem.itemType == "L") {
                            Spacer(Modifier.height(2.dp))
                            Text(
                                "${if (entry.listing?.condition == "NEW") "Новый" else "Б/У"} • ${entry.listing?.conditionRate ?: 0}/10",
                                fontSize = 12.sp,
                                color = TextSecondary,
                                maxLines = 1
                            )
                        }
                        Spacer(Modifier.height(6.dp))
                        Text("${unitPrice * qty} ₽", fontSize = 20.sp, fontWeight = FontWeight.ExtraBold, color = TextPrimary)
                    }
                    IconButton(onClick = onDelete, modifier = Modifier.size(28.dp)) {
                        Icon(Icons.Outlined.Close, null, tint = IconInactive, modifier = Modifier.size(14.dp))
                    }
                }

                Spacer(modifier = Modifier.weight(1f))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.End,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    TextButton(
                        onClick = onDecrement,
                        contentPadding = PaddingValues(0.dp),
                        modifier = Modifier.size(36.dp)
                    ) {
                        Icon(Icons.Outlined.Remove, null, tint = TextPrimary)
                    }
                    Text("$qty", fontSize = 18.sp, fontWeight = FontWeight.ExtraBold, color = TextPrimary, modifier = Modifier.padding(horizontal = 10.dp))
                    TextButton(
                        onClick = onIncrement,
                        enabled = qty < maxQty,
                        contentPadding = PaddingValues(0.dp),
                        modifier = Modifier.size(36.dp)
                    ) {
                        Icon(Icons.Outlined.Add, null, tint = if (qty < maxQty) TextPrimary else IconInactive)
                    }
                }
            }
        }
    }
}

private fun pluralPositions(n: Int): String = when {
    n % 100 in 11..19 -> "позиций"
    n % 10 == 1 -> "позиция"
    n % 10 in 2..4 -> "позиции"
    else -> "позиций"
}
