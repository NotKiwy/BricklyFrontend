package com.example.bricklyfrontend.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
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
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.bricklyfrontend.data.OrderDefaultDTO
import com.example.bricklyfrontend.data.RetrofitClient
import com.example.bricklyfrontend.data.UserPreferences
import com.example.bricklyfrontend.ui.theme.*
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.launch
import java.time.OffsetDateTime
import java.time.format.DateTimeFormatter
import java.util.Locale

@Composable
fun MyOrdersScreen(
    onBack: () -> Unit,
    onNavigateToOrderDetail: (Long) -> Unit
) {
    SetStatusBarColor(Accent)
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val userId = UserPreferences.getUserId(context)

    var orders by remember { mutableStateOf<List<OrderDefaultDTO>>(emptyList()) }
    var isLoading by remember { mutableStateOf(true) }
    var isLoadingMore by remember { mutableStateOf(false) }
    var errorMessage by remember { mutableStateOf<String?>(null) }
    var currentPage by remember { mutableIntStateOf(0) }
    var hasMore by remember { mutableStateOf(true) }

    val listState = rememberLazyListState()
    val onLoadMoreLatest by rememberUpdatedState {
        if (!isLoadingMore && hasMore) {
            scope.launch {
                isLoadingMore = true
                try {
                    val resp = RetrofitClient.api.getOrdersByUserId(userId, currentPage + 1)
                    if (resp.isSuccessful) {
                        val body = resp.body()
                        val content = body?.content ?: emptyList()
                        if (content.isNotEmpty()) {
                            orders = orders + content
                            currentPage++
                        }
                        val totalPages = body?.page?.totalPages?.toInt() ?: 1
                        hasMore = currentPage + 1 < totalPages
                    }
                } catch (_: Exception) {}
                isLoadingMore = false
            }
        }
    }

    LaunchedEffect(listState) {
        snapshotFlow {
            val info = listState.layoutInfo
            if (info.totalItemsCount == 0) return@snapshotFlow false
            val lastVisible = info.visibleItemsInfo.lastOrNull()?.index ?: return@snapshotFlow false
            lastVisible >= info.totalItemsCount - 3
        }
            .distinctUntilChanged()
            .collect { nearEnd -> if (nearEnd) onLoadMoreLatest() }
    }

    LaunchedEffect(Unit) {
        try {
            val resp = RetrofitClient.api.getOrdersByUserId(userId, 0)
            if (resp.isSuccessful) {
                val body = resp.body()
                orders = body?.content ?: emptyList()
                currentPage = 0
                val totalPages = body?.page?.totalPages?.toInt() ?: 1
                hasMore = currentPage + 1 < totalPages
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
                    Icon(Icons.Outlined.ArrowBackIosNew, null, tint = TextPrimary)
                }
                Text(
                    "Мои заказы",
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
            orders.isEmpty() -> Box(Modifier.fillMaxSize().padding(padding), contentAlignment = Alignment.Center) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Box(
                        modifier = Modifier.size(80.dp).clip(RoundedCornerShape(24.dp)).background(Accent.copy(alpha = 0.15f)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(Icons.Outlined.ShoppingBag, null, tint = Accent, modifier = Modifier.size(36.dp))
                    }
                    Spacer(Modifier.height(16.dp))
                    Text(
                        "Нет заказов",
                        style = MaterialTheme.typography.bodyLarge.copy(fontWeight = FontWeight.Bold),
                        color = TextPrimary
                    )
                    Spacer(Modifier.height(6.dp))
                    Text(
                        "Оформите первый заказ в магазине",
                        style = MaterialTheme.typography.bodyMedium,
                        color = TextSecondary,
                        textAlign = TextAlign.Center
                    )
                }
            }
            else -> LazyColumn(
                modifier = Modifier.fillMaxSize().padding(padding),
                state = listState,
                contentPadding = PaddingValues(vertical = 12.dp)
            ) {
                items(orders, key = { it.id }) { order ->
                    OrderCard(
                        order = order,
                        onClick = { onNavigateToOrderDetail(order.id) }
                    )
                }
                if (isLoadingMore) {
                    item {
                        Box(
                            modifier = Modifier.fillMaxWidth().padding(vertical = 12.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            CircularProgressIndicator(modifier = Modifier.size(24.dp), color = Accent)
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun OrderCard(
    order: OrderDefaultDTO,
    onClick: () -> Unit
) {
    val dateStr = order.createdAt?.let { dateStr ->
        try {
            OffsetDateTime.parse(dateStr).format(DateTimeFormatter.ofPattern("d MMMM yyyy, HH:mm", Locale("ru")))
        } catch (_: Exception) { dateStr }
    }

    val itemsCount = order.orderItems?.sumOf { it.quantity ?: 0 } ?: 0
    val totalPrice = order.orderItems?.sumOf { (it.price ?: 0) * (it.quantity ?: 1) } ?: 0

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 6.dp)
            .clickable(onClick = onClick),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = CardBackground),
        elevation = CardDefaults.cardElevation(0.dp)
    ) {
        Column(modifier = Modifier.fillMaxWidth().padding(16.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    "Заказ #${order.id}",
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Bold,
                    color = TextPrimary
                )
                Icon(Icons.Outlined.ChevronRight, null, tint = IconInactive, modifier = Modifier.size(20.dp))
            }
            if (dateStr != null) {
                Spacer(Modifier.height(6.dp))
                Text(dateStr, fontSize = 13.sp, color = TextSecondary)
            }
            Spacer(Modifier.height(8.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    "$itemsCount ${pluralItems(itemsCount)}",
                    fontSize = 14.sp,
                    color = TextSecondary
                )
                Text(
                    "$totalPrice ₽",
                    fontSize = 16.sp,
                    fontWeight = FontWeight.ExtraBold,
                    color = TextPrimary
                )
            }
        }
    }
}

private fun pluralItems(n: Int): String = when {
    n % 100 in 11..19 -> "товаров"
    n % 10 == 1 -> "товар"
    n % 10 in 2..4 -> "товара"
    else -> "товаров"
}
