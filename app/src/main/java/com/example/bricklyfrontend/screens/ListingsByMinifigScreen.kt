package com.example.bricklyfrontend.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
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
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.ImageLoader
import coil.compose.SubcomposeAsyncImage
import coil.request.ImageRequest
import com.example.bricklyfrontend.data.ListingDefaultDTO
import com.example.bricklyfrontend.data.RetrofitClient
import com.example.bricklyfrontend.data.UserPreferences
import com.example.bricklyfrontend.ui.theme.*
import kotlinx.coroutines.launch
import okhttp3.Credentials
import okhttp3.OkHttpClient

@Composable
fun ListingsByMinifigScreen(
    itemId: String,
    onBack: () -> Unit,
    onNavigateToListingDetail: (Long) -> Unit = {}
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
            }.build()
        ImageLoader.Builder(context).okHttpClient(client).build()
    }

    var listings by remember { mutableStateOf<List<ListingDefaultDTO>>(emptyList()) }
    var isLoading by remember { mutableStateOf(true) }
    var errorMessage by remember { mutableStateOf<String?>(null) }

    val itemIdList = remember(itemId) { itemId.split(",").map { it.trim() }.filter { it.isNotEmpty() } }

    LaunchedEffect(itemId) {
        isLoading = true
        try {
            val resp = RetrofitClient.api.getListingsByStatus("active", page = 0, size = 100)
            if (resp.isSuccessful) {
                listings = (resp.body()?.content ?: emptyList())
                    .filter { listing -> itemIdList.any { id -> listing.itemId?.equals(id, ignoreCase = true) == true } }
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
                Column(modifier = Modifier.align(Alignment.Center), horizontalAlignment = Alignment.CenterHorizontally) {
                    Text("Объявления", color = TextPrimary, fontWeight = FontWeight.Bold, fontSize = 18.sp)
                    Text(itemId, color = TextPrimary.copy(alpha = 0.6f), fontSize = 12.sp)
                }
            }
        }
    ) { padding ->
        when {
            isLoading -> Box(Modifier.fillMaxSize().padding(padding), contentAlignment = Alignment.Center) {
                CircularProgressIndicator(color = Accent)
            }

            errorMessage != null -> Box(Modifier.fillMaxSize().padding(padding), contentAlignment = Alignment.Center) {
                EmptyStateBox(
                    icon = Icons.Outlined.ErrorOutline,
                    title = "Не удалось загрузить",
                    subtitle = errorMessage!!,
                    onRefresh = {
                        scope.launch {
                            isLoading = true
                            errorMessage = null
                            try {
                                val resp = RetrofitClient.api.getListingsByStatus("active", page = 0, size = 100)
                                if (resp.isSuccessful) {
                                    listings = (resp.body()?.content ?: emptyList())
                                        .filter { it.itemId?.equals(itemId, ignoreCase = true) == true }
                                } else errorMessage = "Ошибка загрузки (${resp.code()})"
                            } catch (_: Exception) { errorMessage = "Нет соединения" }
                            isLoading = false
                        }
                    }
                )
            }

            listings.isEmpty() -> Box(Modifier.fillMaxSize().padding(padding), contentAlignment = Alignment.Center) {
                EmptyStateBox(
                    icon = Icons.Outlined.Inventory2,
                    title = "Объявлений не найдено",
                    subtitle = "Нет активных объявлений для ${itemIdList.joinToString(", ")}",
                    onRefresh = {}
                )
            }

            else -> LazyVerticalGrid(
                columns = GridCells.Fixed(2),
                modifier = Modifier.fillMaxSize().padding(padding).padding(horizontal = 12.dp),
                contentPadding = PaddingValues(vertical = 16.dp),
                horizontalArrangement = Arrangement.spacedBy(10.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                items(listings, key = { it.id }) { listing ->
                    MinifigListingCard(
                        listing = listing,
                        imageLoader = imageLoader,
                        onClick = { onNavigateToListingDetail(listing.id) }
                    )
                }
            }
        }
    }
}

@Composable
private fun MinifigListingCard(
    listing: ListingDefaultDTO,
    imageLoader: ImageLoader,
    onClick: () -> Unit
) {
    val context = LocalContext.current
    val imageUrl = listing.listingImage
        ?.firstOrNull { it.positionId == 0 }?.imagePath
        ?.takeIf { it.isNotBlank() }
        ?.let { if (it.startsWith("http")) it else "${RetrofitClient.BASE_URL}/${it.trimStart('/')}" }

    Card(
        modifier = Modifier.fillMaxWidth().clickable(onClick = onClick),
        shape = RoundedCornerShape(18.dp),
        colors = CardDefaults.cardColors(containerColor = CardBackground),
        elevation = CardDefaults.cardElevation(0.dp)
    ) {
        Column {
            Box(modifier = Modifier.fillMaxWidth().height(130.dp)) {
                SubcomposeAsyncImage(
                    model = ImageRequest.Builder(context).data(imageUrl).crossfade(true).build(),
                    imageLoader = imageLoader,
                    contentDescription = null,
                    modifier = Modifier.fillMaxSize().clip(RoundedCornerShape(topStart = 18.dp, topEnd = 18.dp)),
                    contentScale = ContentScale.Crop,
                    loading = { Box(Modifier.fillMaxSize().background(Accent.copy(alpha = 0.12f))) },
                    error = {
                        Box(Modifier.fillMaxSize().background(Accent.copy(alpha = 0.12f)), contentAlignment = Alignment.Center) {
                            Icon(Icons.Outlined.Inventory2, null, tint = Accent, modifier = Modifier.size(28.dp))
                        }
                    }
                )
            }
            Column(modifier = Modifier.padding(10.dp)) {
                Text(listing.itemId ?: "—", fontSize = 13.sp, fontWeight = FontWeight.SemiBold, color = TextPrimary, maxLines = 1, overflow = TextOverflow.Ellipsis)
                Text("${listing.price ?: 0} ₽", fontSize = 16.sp, fontWeight = FontWeight.ExtraBold, color = TextPrimary)
                Text("В наличии: ${listing.quantity ?: 0}", fontSize = 11.sp, color = TextSecondary)
                if (!listing.condition.isNullOrBlank()) {
                    Text(listing.condition, fontSize = 11.sp, color = TextSecondary, maxLines = 1)
                }
            }
        }
    }
}
