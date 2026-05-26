package com.example.bricklyfrontend.screens

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.*
import kotlinx.coroutines.launch
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
import coil.request.ImageRequest
import androidx.compose.ui.platform.LocalContext
import coil.ImageLoader
import coil.compose.SubcomposeAsyncImage
import okhttp3.Credentials
import okhttp3.OkHttpClient
import com.example.bricklyfrontend.data.UserPreferences
import com.example.bricklyfrontend.data.ListingDefaultDTO
import com.example.bricklyfrontend.data.RetrofitClient
import com.example.bricklyfrontend.ui.theme.*

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun ListingDetailScreen(
    listingId: Long,
    onBack: () -> Unit,
    onNavigateToSetDetail: (String) -> Unit = {},
    onNavigateToMeetings: () -> Unit = {},
    onNavigateToProfile: () -> Unit = {},
    onNavigateToCart: () -> Unit = {},
    onNavigateToBrickognize: () -> Unit = {}
) {
    SetStatusBarColor(Accent)
    
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val userId = UserPreferences.getUserId(context)
    
    var listing by remember { mutableStateOf<ListingDefaultDTO?>(null) }
    var isLoading by remember { mutableStateOf(true) }
    var errorMessage by remember { mutableStateOf<String?>(null) }

    LaunchedEffect(listingId) {
        try {
            val response = RetrofitClient.api.getListingById(listingId)
            if (response.isSuccessful) {
                listing = response.body()
            } else {
                errorMessage = "Ошибка загрузки (${response.code()})"
            }
        } catch (e: Exception) {
            errorMessage = "Нет соединения с сервером"
        }
        isLoading = false
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
                    .padding(horizontal = 8.dp, vertical = 12.dp)
            ) {
                IconButton(onClick = onBack, modifier = Modifier.align(Alignment.CenterStart)) {
                    Icon(Icons.Outlined.ArrowBackIosNew, "Назад", tint = TextPrimary)
                }
                Text(
                    text = "Объявление",
                    color = Color(0xFF1A1A1A),
                    fontWeight = FontWeight.Bold,
                    fontSize = 18.sp,
                    modifier = Modifier.align(Alignment.Center)
                )
            }
        }
    ) { padding ->
        when {
            isLoading -> Box(
                Modifier.fillMaxSize(),
                contentAlignment = Alignment.Center
            ) {
                CircularProgressIndicator(color = Accent)
            }

            errorMessage != null -> Box(
                Modifier.fillMaxSize().padding(32.dp),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    errorMessage!!,
                    color = ErrorColor,
                    textAlign = TextAlign.Center
                )
            }

            listing != null -> {
                val item = listing!!
                val images = item.listingImage?.sortedBy { it.positionId } ?: emptyList()
                val pagerState = rememberPagerState(pageCount = { images.size })

                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(padding)
                        .verticalScroll(rememberScrollState())
                ) {
                    Spacer(Modifier.height(16.dp))

                    var isInCart by remember { mutableStateOf(ListingCartState.items.any { it.listingId == item.id }) }
                    val cartItem by remember { derivedStateOf { ListingCartState.items.find { it.listingId == item.id } } }
                    isInCart = cartItem != null
                    
                    val imageLoader = remember {
                        val username = UserPreferences.getUsername(context)
                        val password = UserPreferences.getPassword(context)
                        val okHttpClient = OkHttpClient.Builder()
                            .addInterceptor { chain ->
                                val request = chain.request().newBuilder()
                                    .header("Authorization", Credentials.basic(username, password))
                                    .build()
                                chain.proceed(request)
                            }
                            .build()
                        ImageLoader.Builder(context)
                            .okHttpClient(okHttpClient)
                            .build()
                    }

                    if (images.isNotEmpty()) {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(300.dp)
                                .padding(horizontal = 20.dp)
                        ) {
                            HorizontalPager(
                                state = pagerState,
                                modifier = Modifier
                                    .fillMaxSize()
                                    .clip(RoundedCornerShape(24.dp))
                            ) { page ->
                                val imageUrl = "${RetrofitClient.BASE_URL}${images[page].imagePath}"
                                SubcomposeAsyncImage(
                                    model = ImageRequest.Builder(LocalContext.current)
                                        .data(imageUrl)
                                        .crossfade(true)
                                        .build(),
                                    imageLoader = imageLoader,
                                    contentDescription = null,
                                    modifier = Modifier.fillMaxSize(),
                                    contentScale = ContentScale.Crop,
                                    error = {
                                        Box(
                                            modifier = Modifier.fillMaxSize().background(Accent.copy(alpha = 0.12f)),
                                            contentAlignment = Alignment.Center
                                        ) {
                                            Icon(Icons.Outlined.ImageNotSupported, null, tint = Accent, modifier = Modifier.size(32.dp))
                                        }
                                    },
                                    loading = {
                                        Box(
                                            modifier = Modifier.fillMaxSize().background(Accent.copy(alpha = 0.12f)),
                                            contentAlignment = Alignment.Center
                                        ) {
                                            CircularProgressIndicator(color = Accent, modifier = Modifier.size(24.dp))
                                        }
                                    }
                                )
                            }

                            if (images.size > 1) {
                                Row(
                                    modifier = Modifier
                                        .align(Alignment.BottomCenter)
                                        .padding(16.dp),
                                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                                ) {
                                    repeat(images.size) { index ->
                                        Box(
                                            modifier = Modifier
                                                .size(if (pagerState.currentPage == index) 8.dp else 6.dp)
                                                .clip(CircleShape)
                                                .background(
                                                    if (pagerState.currentPage == index) Accent else Color.White.copy(alpha = 0.5f)
                                                )
                                        )
                                    }
                                }
                            }
                        }
                    } else {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(300.dp)
                                .padding(horizontal = 20.dp)
                                .clip(RoundedCornerShape(24.dp))
                                .background(Accent.copy(alpha = 0.12f)),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                Icons.Outlined.Image,
                                null,
                                tint = Accent,
                                modifier = Modifier.size(64.dp)
                            )
                        }
                    }

                    Spacer(Modifier.height(16.dp))

                    // Main Info Card
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 20.dp),
                        shape = RoundedCornerShape(24.dp),
                        colors = CardDefaults.cardColors(containerColor = CardBackground),
                        elevation = CardDefaults.cardElevation(0.dp)
                    ) {
                        Column(modifier = Modifier.padding(20.dp)) {

                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = item.itemId ?: "Не указан",
                                    style = MaterialTheme.typography.titleLarge
                                        .copy(fontWeight = FontWeight.ExtraBold),
                                    color = TextPrimary,
                                    modifier = Modifier.weight(1f)
                                )
                                if (!item.itemId.isNullOrBlank()) {
                                    IconButton(
                                        onClick = { onNavigateToSetDetail(item.itemId) },
                                        modifier = Modifier
                                            .size(36.dp)
                                            .clip(RoundedCornerShape(10.dp))
                                            .background(Accent)
                                    ) {
                                        Icon(
                                            Icons.Outlined.ArrowForwardIos,
                                            contentDescription = "Информация о наборе",
                                            tint = Color.Black,
                                            modifier = Modifier.size(18.dp)
                                        )
                                    }
                                }
                            }

                            Spacer(Modifier.height(16.dp))


                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = "${item.price ?: 0} ₽",
                                    style = MaterialTheme.typography.headlineMedium
                                        .copy(fontWeight = FontWeight.ExtraBold),
                                    color = Color.Black
                                )
                                

                                Text(
                                    text = "× ${item.quantity ?: 1}",
                                    style = MaterialTheme.typography.titleMedium,
                                    color = TextSecondary
                                )
                            }

                            Spacer(Modifier.height(16.dp))


                            ListingInfoRow(
                                icon = Icons.Outlined.Star,
                                label = "Состояние",
                                value = "${if (item.condition == "NEW") "Новое" else "Б/У"} (${item.conditionRate ?: 0}/10)"
                            )

                            Spacer(Modifier.height(10.dp))


                            ListingInfoRow(
                                icon = Icons.Outlined.Person,
                                label = "Продавец",
                                value = item.seller?.username ?: "Неизвестен"
                            )


                        }
                    }


                    if (!item.description.isNullOrBlank()) {
                        Spacer(Modifier.height(12.dp))
                        Card(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 20.dp),
                            shape = RoundedCornerShape(24.dp),
                            colors = CardDefaults.cardColors(containerColor = CardBackground),
                            elevation = CardDefaults.cardElevation(0.dp)
                        ) {
                            Column(modifier = Modifier.padding(20.dp)) {
                                Text(
                                    "Описание",
                                    style = MaterialTheme.typography.titleMedium
                                        .copy(fontWeight = FontWeight.Bold),
                                    color = TextPrimary
                                )
                                Spacer(Modifier.height(8.dp))
                                Text(
                                    item.description,
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = TextSecondary,
                                    lineHeight = 22.sp
                                )
                            }
                        }
                    }

                    Spacer(Modifier.height(16.dp))


                    if (!isInCart) {
                        Button(
                            onClick = {
                                scope.launch {
                                    ListingCartState.addItemWithApi(
                                        ListingCartItem(
                                            listingId = item.id,
                                            title = item.itemId ?: "Unknown",
                                            unitPrice = item.price ?: 0,
                                            quantity = 1,
                                            maxQuantity = item.quantity ?: 1
                                        ),
                                        userId = userId
                                    )
                                }
                            },
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 20.dp)
                                .height(56.dp),
                            shape = RoundedCornerShape(16.dp),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = Accent,
                                contentColor = TextPrimary
                            )
                        ) {
                            Icon(Icons.Outlined.ShoppingCart, null, modifier = Modifier.size(20.dp))
                            Spacer(Modifier.width(8.dp))
                            Text("Купить", style = MaterialTheme.typography.labelLarge.copy(fontSize = 16.sp))
                        }
                    } else {
                        val currentItem = cartItem
                        if (currentItem != null) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = 20.dp)
                                    .height(56.dp)
                                    .clip(RoundedCornerShape(16.dp))
                                    .background(Accent),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                IconButton(
                                    onClick = { ListingCartState.decrementQuantity(item.id) },
                                    modifier = Modifier.size(56.dp)
                                ) {
                                    Icon(
                                        if (currentItem.quantity == 1) Icons.Outlined.Delete else Icons.Outlined.Remove,
                                        null, tint = Color.Black, modifier = Modifier.size(22.dp)
                                    )
                                }
                                Text(
                                    "${currentItem.quantity}",
                                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.ExtraBold, fontSize = 18.sp),
                                    color = Color.Black
                                )
                                IconButton(
                                    onClick = {
                                        if (currentItem.quantity < (item.quantity ?: 1))
                                            ListingCartState.incrementQuantity(item.id)
                                    },
                                    modifier = Modifier.size(56.dp),
                                    enabled = currentItem.quantity < (item.quantity ?: 1)
                                ) {
                                    Icon(
                                        Icons.Outlined.Add, null,
                                        tint = if (currentItem.quantity < (item.quantity ?: 1)) Color.Black else Color.Black.copy(alpha = 0.3f),
                                        modifier = Modifier.size(22.dp)
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
private fun ListingInfoRow(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    label: String,
    value: String
) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier.fillMaxWidth()
    ) {
        Box(
            modifier = Modifier
                .size(36.dp)
                .clip(RoundedCornerShape(10.dp))
                .background(Accent.copy(alpha = 0.15f)),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                icon,
                null,
                tint = Accent,
                modifier = Modifier.size(18.dp)
            )
        }
        Spacer(Modifier.width(12.dp))
        Column {
            Text(
                label,
                style = MaterialTheme.typography.labelSmall,
                color = TextSecondary
            )
            Text(
                value,
                style = MaterialTheme.typography.bodyLarge
                    .copy(fontWeight = FontWeight.SemiBold),
                color = TextPrimary
            )
        }
    }
}
