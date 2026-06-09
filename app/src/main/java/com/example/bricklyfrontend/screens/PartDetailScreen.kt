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
import com.example.bricklyfrontend.data.MinifigContainingBlPartDTO
import com.example.bricklyfrontend.data.PartDTO
import com.example.bricklyfrontend.data.RetrofitClient
import com.example.bricklyfrontend.data.SetContainingBlPartDTO
import com.example.bricklyfrontend.ui.theme.*
import kotlinx.coroutines.launch

private enum class PartInventoryTab { SETS, MINIFIGS }

@Composable
fun PartDetailScreen(
    blId: String,
    onBack: () -> Unit,
    onNavigateToMeetings: () -> Unit = {},
    onNavigateToProfile: () -> Unit = {},
    onNavigateToCart: () -> Unit = {},
    onNavigateToBrickognize: () -> Unit = {},
    onNavigateToListings: (String) -> Unit = {},
    onNavigateToSetDetail: (String) -> Unit = {},
    onNavigateToMinifigDetail: (String) -> Unit = {}
) {
    SetStatusBarColor(Accent)

    var part by remember { mutableStateOf<PartDTO?>(null) }
    var isLoading by remember { mutableStateOf(true) }
    var errorMessage by remember { mutableStateOf<String?>(null) }

    var activeTab by remember { mutableStateOf<PartInventoryTab?>(null) }

    var sets by remember { mutableStateOf<List<SetContainingBlPartDTO>>(emptyList()) }
    var setsPage by remember { mutableStateOf(0) }
    var setsTotalPages by remember { mutableStateOf(1) }
    var setsLoadingFirst by remember { mutableStateOf(false) }
    var setsLoadingMore by remember { mutableStateOf(false) }

    var minifigs by remember { mutableStateOf<List<MinifigContainingBlPartDTO>>(emptyList()) }
    var minigifsPage by remember { mutableStateOf(0) }
    var minifigsTotalPages by remember { mutableStateOf(1) }
    var minigifsLoadingFirst by remember { mutableStateOf(false) }
    var minigifsLoadingMore by remember { mutableStateOf(false) }

    val scope = rememberCoroutineScope()

    suspend fun loadSets(page: Int) {
        if (page == 0) setsLoadingFirst = true else setsLoadingMore = true
        try {
            val resp = RetrofitClient.api.getSetsContainingPart(blId, page)
            if (resp.isSuccessful) {
                val body = resp.body()
                val content = body?.content ?: emptyList()
                sets = if (page == 0) content else sets + content
                setsPage = page
                setsTotalPages = body?.page?.totalPages?.toInt() ?: 1
            }
        } catch (_: Exception) {}
        setsLoadingFirst = false
        setsLoadingMore = false
    }

    suspend fun loadMinifigs(page: Int) {
        if (page == 0) minigifsLoadingFirst = true else minigifsLoadingMore = true
        try {
            val resp = RetrofitClient.api.getMinigifsContainingPart(blId, page)
            if (resp.isSuccessful) {
                val body = resp.body()
                val content = body?.content ?: emptyList()
                minifigs = if (page == 0) content else minifigs + content
                minigifsPage = page
                minifigsTotalPages = body?.page?.totalPages?.toInt() ?: 1
            }
        } catch (_: Exception) {}
        minigifsLoadingFirst = false
        minigifsLoadingMore = false
    }

    LaunchedEffect(blId) {
        try {
            val response = RetrofitClient.api.getPartByBlId(blId)
            if (response.isSuccessful) {
                part = response.body()?.firstOrNull()
                if (part == null) errorMessage = "Деталь не найдена"
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
            PartInventoryTab.SETS -> if (sets.isEmpty()) loadSets(0)
            PartInventoryTab.MINIFIGS -> if (minifigs.isEmpty()) loadMinifigs(0)
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
                    text = "Информация о детали",
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

            part != null -> {
                val p = part!!
                val imageUrl = p.blPart?.imageUrl?.takeIf { it.isNotBlank() }

                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(padding)
                        .verticalScroll(rememberScrollState())
                ) {
                    Spacer(Modifier.height(16.dp))

                    if (!imageUrl.isNullOrBlank()) {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 20.dp)
                                .height(260.dp)
                                .clip(RoundedCornerShape(24.dp))
                                .background(Color.White)
                        ) {
                            AsyncImage(
                                model = imageUrl,
                                contentDescription = p.name,
                                modifier = Modifier
                                    .fillMaxSize()
                                    .padding(8.dp)
                                    .clip(RoundedCornerShape(16.dp)),
                                contentScale = ContentScale.Fit
                            )
                        }
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
                            Icon(Icons.Outlined.Extension, null, tint = Accent, modifier = Modifier.size(64.dp))
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
                                text = p.name ?: p.id,
                                style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.ExtraBold),
                                color = TextPrimary
                            )
                            Spacer(Modifier.height(16.dp))
                            PartInfoRow(label = "BrickLink ID", value = p.blPart?.id ?: blId)
                            Spacer(Modifier.height(10.dp))
                            PartInfoRow(label = "Rebrickable ID", value = p.id)
                            Spacer(Modifier.height(10.dp))
                            PartInfoRow(label = "Категория", value = p.category?.name ?: "Не указана")
                        }
                    }

                    Spacer(Modifier.height(16.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth().padding(horizontal = 20.dp),
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        InventoryTabButton(
                            label = "Наборы",
                            icon = Icons.Outlined.GridView,
                            isActive = activeTab == PartInventoryTab.SETS,
                            isLoadingFirst = setsLoadingFirst,
                            onClick = { activeTab = if (activeTab == PartInventoryTab.SETS) null else PartInventoryTab.SETS },
                            modifier = Modifier.weight(1f)
                        )
                        InventoryTabButton(
                            label = "Минифигурки",
                            icon = Icons.Outlined.Person,
                            isActive = activeTab == PartInventoryTab.MINIFIGS,
                            isLoadingFirst = minigifsLoadingFirst,
                            onClick = { activeTab = if (activeTab == PartInventoryTab.MINIFIGS) null else PartInventoryTab.MINIFIGS },
                            modifier = Modifier.weight(1f)
                        )
                    }

                    if (activeTab == PartInventoryTab.SETS) {
                        Spacer(Modifier.height(10.dp))
                        Box(modifier = Modifier.padding(horizontal = 20.dp)) {
                            InventoryPanelBox(
                                isLoadingFirst = setsLoadingFirst,
                                isEmpty = sets.isEmpty(),
                                isLoadingMore = setsLoadingMore,
                                onLoadMore = {
                                    if (!setsLoadingMore && setsPage + 1 < setsTotalPages) {
                                        scope.launch { loadSets(setsPage + 1) }
                                    }
                                }
                            ) {
                                itemsIndexed(sets, key = { index, _ -> index }) { _, set ->
                                    InventorySetCard(
                                        id = set.id,
                                        name = set.name,
                                        year = set.year,
                                        imageUrl = set.imageUrl,
                                        countLabel = "×${set.partQuantity ?: 1}",
                                        onClick = { onNavigateToSetDetail(set.id) }
                                    )
                                }
                            }
                        }
                    }

                    if (activeTab == PartInventoryTab.MINIFIGS) {
                        Spacer(Modifier.height(10.dp))
                        Box(modifier = Modifier.padding(horizontal = 20.dp)) {
                            InventoryPanelBox(
                                isLoadingFirst = minigifsLoadingFirst,
                                isEmpty = minifigs.isEmpty(),
                                isLoadingMore = minigifsLoadingMore,
                                onLoadMore = {
                                    if (!minigifsLoadingMore && minigifsPage + 1 < minifigsTotalPages) {
                                        scope.launch { loadMinifigs(minigifsPage + 1) }
                                    }
                                }
                            ) {
                                itemsIndexed(minifigs, key = { index, _ -> index }) { _, fig ->
                                    InventoryMinifigCard(
                                        blId = fig.blId,
                                        name = fig.blName ?: fig.name,
                                        imageUrl = fig.blImageUrl ?: fig.imageUrl,
                                        category = fig.blCategoryName,
                                        countLabel = "×${fig.partQuantity ?: 1}",
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
private fun PartInfoRow(label: String, value: String) {
    Column {
        Text(label, style = MaterialTheme.typography.labelSmall, color = TextSecondary)
        Text(
            value,
            style = MaterialTheme.typography.bodyLarge.copy(fontWeight = FontWeight.SemiBold),
            color = TextPrimary
        )
    }
}
