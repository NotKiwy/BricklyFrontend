package com.example.bricklyfrontend.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.lazy.itemsIndexed
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
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.example.bricklyfrontend.data.BrickSetDTO
import com.example.bricklyfrontend.data.MinifigFromSetDTO
import com.example.bricklyfrontend.data.PartFromItemDTO
import com.example.bricklyfrontend.data.RetrofitClient
import com.example.bricklyfrontend.ui.theme.*
import kotlinx.coroutines.launch

private enum class SetInventoryTab { PARTS, MINIFIGS }

@Composable
fun SetDetailScreen(
    setId: String,
    onBack: () -> Unit,
    onNavigateToMeetings: () -> Unit = {},
    onNavigateToProfile: () -> Unit = {},
    onNavigateToCart: () -> Unit = {},
    onNavigateToBrickognize: () -> Unit = {},
    onNavigateToPartDetail: (String) -> Unit = {},
    onNavigateToMinifigDetail: (String) -> Unit = {}
) {
    SetStatusBarColor(Accent)

    var setData by remember { mutableStateOf<BrickSetDTO?>(null) }
    var isLoading by remember { mutableStateOf(true) }
    var errorMessage by remember { mutableStateOf<String?>(null) }

    var activeTab by remember { mutableStateOf<SetInventoryTab?>(null) }

    var parts by remember { mutableStateOf<List<PartFromItemDTO>>(emptyList()) }
    var partsPage by remember { mutableStateOf(0) }
    var partsHasMore by remember { mutableStateOf(false) }
    var partsLoadingFirst by remember { mutableStateOf(false) }
    var partsLoadingMore by remember { mutableStateOf(false) }

    var minifigs by remember { mutableStateOf<List<MinifigFromSetDTO>>(emptyList()) }
    var minigifsLoadingFirst by remember { mutableStateOf(false) }

    val scope = rememberCoroutineScope()

    suspend fun loadParts(page: Int) {
        if (page == 0) partsLoadingFirst = true else partsLoadingMore = true
        try {
            val resp = RetrofitClient.api.getPartsFromSet(setId, page)
            if (resp.isSuccessful) {
                val body = resp.body()
                val content = body?.content ?: emptyList()
                if (content.isNotEmpty()) {
                    parts = if (page == 0) content else parts + content
                    partsPage = page
                }
                val apiTotalPages = body?.page?.totalPages?.toInt()
                partsHasMore = when {
                    apiTotalPages != null && apiTotalPages > 0 -> page + 1 < apiTotalPages
                    else -> content.size >= 20
                }
            }
        } catch (_: Exception) {}
        partsLoadingFirst = false
        partsLoadingMore = false
    }

    suspend fun loadMinifigs() {
        minigifsLoadingFirst = true
        try {
            val resp = RetrofitClient.api.getMinigifsFromSet(setId)
            if (resp.isSuccessful) {
                minifigs = resp.body() ?: emptyList()
            }
        } catch (_: Exception) {}
        minigifsLoadingFirst = false
    }

    LaunchedEffect(setId) {
        try {
            val response = RetrofitClient.api.getSetById(setId)
            if (response.isSuccessful) {
                setData = response.body()
            } else {
                errorMessage = "Ошибка загрузки (${response.code()})"
            }
        } catch (_: Exception) {
            errorMessage = "Нет соединения с сервером"
        }
        isLoading = false
    }

    LaunchedEffect(activeTab) {
        when (activeTab) {
            SetInventoryTab.PARTS -> if (parts.isEmpty()) loadParts(0)
            SetInventoryTab.MINIFIGS -> if (minifigs.isEmpty()) loadMinifigs()
            null -> {}
        }
    }

    Scaffold(
        containerColor = Background,
        bottomBar = {
            BricklyBottomBar(currentRoute = "home", onNavigate = { route ->
                when (route) {
                    "profile" -> onNavigateToProfile()
                    "cart" -> onNavigateToCart()
                    "meetings" -> onNavigateToMeetings()
                    "brickognize" -> onNavigateToBrickognize()
                }
            }, onScanClick = onNavigateToBrickognize)
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
                    text = "Информация о наборе",
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

            errorMessage != null -> Box(
                Modifier.fillMaxSize().padding(32.dp),
                contentAlignment = Alignment.Center
            ) {
                Text(errorMessage!!, color = ErrorColor, textAlign = TextAlign.Center)
            }

            setData != null -> {
                val s = setData!!
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(padding)
                        .verticalScroll(rememberScrollState())
                ) {
                    Spacer(Modifier.height(16.dp))

                    if (!s.imageUrl.isNullOrBlank()) {
                        AsyncImage(
                            model = s.imageUrl,
                            contentDescription = s.name,
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(260.dp)
                                .padding(horizontal = 20.dp)
                                .clip(RoundedCornerShape(24.dp))
                                .background(Color.White),
                            contentScale = ContentScale.Fit
                        )
                    } else {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(260.dp)
                                .padding(horizontal = 20.dp)
                                .clip(RoundedCornerShape(24.dp))
                                .background(Accent.copy(alpha = 0.12f)),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(Icons.Outlined.Image, null, tint = Accent, modifier = Modifier.size(64.dp))
                        }
                    }

                    Spacer(Modifier.height(20.dp))

                    Card(
                        modifier = Modifier.fillMaxWidth().padding(horizontal = 20.dp),
                        shape = RoundedCornerShape(24.dp),
                        colors = CardDefaults.cardColors(containerColor = CardBackground),
                        elevation = CardDefaults.cardElevation(0.dp)
                    ) {
                        Column(modifier = Modifier.padding(20.dp)) {
                            Text(
                                text = s.name ?: s.id,
                                style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.ExtraBold),
                                color = TextPrimary
                            )
                            Spacer(Modifier.height(16.dp))
                            SetInfoRow(label = "Номер набора", value = s.id)
                            Spacer(Modifier.height(10.dp))
                            SetInfoRow(label = "Год выпуска", value = s.year?.toString() ?: "Не указан")
                            Spacer(Modifier.height(10.dp))
                            SetInfoRow(label = "Тема", value = s.theme?.name ?: "Не указана")
                            Spacer(Modifier.height(10.dp))
                            SetInfoRow(label = "Количество деталей", value = s.numParts?.toString() ?: "Не указано")
                        }
                    }

                    Spacer(Modifier.height(16.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth().padding(horizontal = 20.dp),
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        InventoryTabButton(
                            label = "Детали",
                            icon = Icons.Outlined.Extension,
                            isActive = activeTab == SetInventoryTab.PARTS,
                            isLoadingFirst = partsLoadingFirst,
                            onClick = { activeTab = if (activeTab == SetInventoryTab.PARTS) null else SetInventoryTab.PARTS },
                            modifier = Modifier.weight(1f)
                        )
                        InventoryTabButton(
                            label = "Минифигурки",
                            icon = Icons.Outlined.Person,
                            isActive = activeTab == SetInventoryTab.MINIFIGS,
                            isLoadingFirst = minigifsLoadingFirst,
                            onClick = { activeTab = if (activeTab == SetInventoryTab.MINIFIGS) null else SetInventoryTab.MINIFIGS },
                            modifier = Modifier.weight(1f)
                        )
                    }

                    if (activeTab == SetInventoryTab.PARTS) {
                        Spacer(Modifier.height(10.dp))
                        Box(modifier = Modifier.padding(horizontal = 20.dp)) {
                            InventoryPanelBox(
                                isLoadingFirst = partsLoadingFirst,
                                isEmpty = parts.isEmpty(),
                                isLoadingMore = partsLoadingMore,
                                onLoadMore = {
                                    if (!partsLoadingMore && partsHasMore) {
                                        scope.launch { loadParts(partsPage + 1) }
                                    }
                                }
                            ) {
                                itemsIndexed(parts, key = { index, _ -> index }) { _, part ->
                                    InventoryPartCard(
                                        blId = part.blId,
                                        name = part.name,
                                        imageUrl = part.imageUrl,
                                        colorName = part.colorName,
                                        colorRgb = part.colorRgb,
                                        countLabel = "×${part.quantity ?: 1}",
                                        onClick = { onNavigateToPartDetail(part.blId ?: part.id) }
                                    )
                                }
                            }
                        }
                    }

                    if (activeTab == SetInventoryTab.MINIFIGS) {
                        Spacer(Modifier.height(10.dp))
                        Box(modifier = Modifier.padding(horizontal = 20.dp)) {
                            InventoryPanelBox(
                                isLoadingFirst = minigifsLoadingFirst,
                                isEmpty = minifigs.isEmpty(),
                                isLoadingMore = false,
                                onLoadMore = {}
                            ) {
                                itemsIndexed(minifigs, key = { index, _ -> index }) { _, fig ->
                                    InventoryMinifigCard(
                                        blId = fig.blId,
                                        name = fig.blName ?: fig.name,
                                        imageUrl = fig.blImageUrl ?: fig.imageUrl,
                                        category = fig.blCategoryName,
                                        countLabel = "×${fig.quantity ?: 1}",
                                        onClick = { onNavigateToMinifigDetail(fig.blId ?: fig.id) }
                                    )
                                }
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
private fun SetInfoRow(label: String, value: String) {
    Column {
        Text(label, style = MaterialTheme.typography.labelSmall, color = TextSecondary)
        Text(
            value,
            style = MaterialTheme.typography.bodyLarge.copy(fontWeight = FontWeight.SemiBold),
            color = TextPrimary
        )
    }
}
