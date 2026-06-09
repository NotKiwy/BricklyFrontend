package com.example.bricklyfrontend.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.platform.LocalContext
import coil.ImageLoader
import coil.compose.SubcomposeAsyncImage
import coil.request.ImageRequest
import okhttp3.Credentials
import okhttp3.OkHttpClient
import com.example.bricklyfrontend.data.ListingDefaultDTO
import com.example.bricklyfrontend.data.RetrofitClient
import com.example.bricklyfrontend.data.UserPreferences
import com.example.bricklyfrontend.ui.theme.*
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CatalogScreen(
    onNavigateToMeetings: () -> Unit = {},
    onNavigateToProfile: () -> Unit = {},
    onNavigateToCart: () -> Unit = {},
    onNavigateToMeetingDetail: (Long) -> Unit = {},
    onNavigateToBrickognize: () -> Unit = {},
    onNavigateToCreateListing: () -> Unit = {},
    onNavigateToListingDetail: (Long) -> Unit = {},
    onNavigateToSetDetail: (String) -> Unit = {},
    initialSearchQuery: String = ""
) {
    SetStatusBarColor(Accent)

    val context = androidx.compose.ui.platform.LocalContext.current
    val scope = rememberCoroutineScope()
    val keyboardController = LocalSoftwareKeyboardController.current
    val focusManager = LocalFocusManager.current

    var isSeller by remember { mutableStateOf(UserPreferences.isSeller(context)) }
    val userId = remember { UserPreferences.getUserId(context) }

    var listings by remember { mutableStateOf<List<ListingDefaultDTO>>(emptyList()) }
    var isLoading by remember { mutableStateOf(true) }
    var isRefreshing by remember { mutableStateOf(false) }
    var errorMessage by remember { mutableStateOf<String?>(null) }
    var searchQuery by rememberSaveable { mutableStateOf(initialSearchQuery) }

    fun loadListings(silent: Boolean = false) {
        scope.launch {
            if (silent) isRefreshing = true else isLoading = true
            errorMessage = null
            try {
                val query = searchQuery.trim()
                if (query.isNotBlank()) {
                    val response = RetrofitClient.api.searchListings(query)
                    if (response.isSuccessful) {
                        listings = (response.body()?.content ?: emptyList()).filter { it.status == "active" && it.seller?.id != userId }
                    } else {
                        errorMessage = "Ошибка загрузки (${response.code()})"
                    }
                } else {
                    val response = RetrofitClient.api.getListingsByStatus("active", page = 0, size = 50)
                    if (response.isSuccessful) {
                        listings = (response.body()?.content ?: emptyList()).filter { it.seller?.id != userId }
                    } else {
                        errorMessage = "Ошибка загрузки (${response.code()})"
                    }
                }
            } catch (e: Exception) {
                errorMessage = "Нет соединения: ${e.message}"
            } finally {
                isLoading = false
                isRefreshing = false
            }
        }
    }

    LaunchedEffect(Unit) {
        try {
            val resp = RetrofitClient.api.getUserById(userId)
            if (resp.isSuccessful) {
                val serverRole = UserPreferences.extractRole(resp.body()?.authorities)
                if (serverRole != UserPreferences.getRole(context)) {
                    UserPreferences.saveUser(
                        context = context,
                        id = userId,
                        username = UserPreferences.getUsername(context),
                        password = UserPreferences.getPassword(context),
                        role = serverRole
                    )
                }
                isSeller = UserPreferences.isSeller(context)
            }
        } catch (_: Exception) {}
    }

    LaunchedEffect(searchQuery) {
        delay(300L)
        loadListings()
    }

    val filteredListings = listings

    Scaffold(
        containerColor = Background,
        topBar = {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(bottomStart = 28.dp, bottomEnd = 28.dp))
                    .background(Accent)
                    .statusBarsPadding()
                    .padding(horizontal = 20.dp)
                    .padding(top = 20.dp, bottom = 20.dp)
            ) {
                Column {
                    Text("Маркет", fontSize = 26.sp, fontWeight = FontWeight.ExtraBold, color = Color(0xFF1A1A1A))
                    Spacer(Modifier.height(12.dp))
                    OutlinedTextField(
                        value = searchQuery,
                        onValueChange = { searchQuery = it },
                        modifier = Modifier.fillMaxWidth(),
                        placeholder = { Text("Искать...", color = TextSecondary, style = MaterialTheme.typography.bodyLarge) },
                        leadingIcon = { Icon(Icons.Outlined.Search, null, tint = TextSecondary, modifier = Modifier.size(20.dp)) },
                        trailingIcon = {
                            if (searchQuery.isNotEmpty()) {
                                IconButton(onClick = {
                                    searchQuery = ""
                                    keyboardController?.hide()
                                    focusManager.clearFocus()
                                }) {
                                    Icon(Icons.Outlined.Close, null, tint = TextSecondary, modifier = Modifier.size(18.dp))
                                }
                            }
                        },
                        shape = RoundedCornerShape(16.dp),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = Color.Transparent,
                            unfocusedBorderColor = Color.Transparent,
                            focusedContainerColor = Color.White,
                            unfocusedContainerColor = Color.White.copy(alpha = 0.9f),
                            cursorColor = TextPrimary
                        ),
                        singleLine = true,
                        textStyle = MaterialTheme.typography.bodyLarge.copy(color = TextPrimary)
                    )
                }
            }
        },
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
        floatingActionButton = {
            if (isSeller) {
                FloatingActionButton(
                    onClick = onNavigateToCreateListing,
                    containerColor = Accent,
                    contentColor = Color.Black,
                    modifier = Modifier.size(64.dp)
                ) {
                    Icon(
                        Icons.Outlined.Add,
                        contentDescription = "Создать товар",
                        modifier = Modifier.size(28.dp)
                    )
                }
            }
        }
    ) { padding ->
        PullToRefreshBox(
            isRefreshing = isRefreshing,
            onRefresh = { loadListings(silent = true) },
            modifier = Modifier.fillMaxSize().padding(padding)
        ) {
        when {
            isLoading -> ListingGridSkeleton(modifier = Modifier.fillMaxSize())

            errorMessage != null -> Column(
                modifier = Modifier.fillMaxSize(),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center
            ) {
                Text(errorMessage!!, color = ErrorColor)
                Spacer(Modifier.height(8.dp))
                Button(
                    onClick = { loadListings() },
                    colors = ButtonDefaults.buttonColors(
                        containerColor = Accent,
                        contentColor = TextPrimary
                    )
                ) {
                    Text("Повторить")
                }
            }

            filteredListings.isEmpty() -> Box(
                modifier = Modifier.fillMaxSize(),
                contentAlignment = Alignment.Center
            ) {
                EmptyStateBox(
                    icon = Icons.Outlined.Inventory2,
                    title = "Объявлений пока нет",
                    subtitle = "Здесь будут отображаться объявления продавцов",
                    onRefresh = { loadListings() }
                )
            }

            else -> {
                LazyVerticalGrid(
                    columns = GridCells.Fixed(2),
                    modifier = Modifier.fillMaxSize().padding(horizontal = 16.dp),
                    contentPadding = PaddingValues(vertical = 16.dp),
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    items(filteredListings) { listing ->
                        ListingCard(
                            listing = listing,
                            onClick = { onNavigateToListingDetail(listing.id) },
                            onNavigateToSetDetail = onNavigateToSetDetail
                        )
                    }
                }
            }
        }
        }
    }
}

@Composable
private fun ListingCard(listing: ListingDefaultDTO, onClick: () -> Unit, onNavigateToSetDetail: (String) -> Unit = {}) {
    val context = LocalContext.current

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

    val imageUrl = listing.listingImage?.firstOrNull { it.positionId == 0 }?.imagePath?.let { path ->
        if (path.isBlank()) {
            null
        } else {
            val cleanPath = path.trim().trimStart('/')
            if (cleanPath.startsWith("http")) {
                cleanPath
            } else {
                "${RetrofitClient.BASE_URL}/$cleanPath"
            }
        }
    }

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = CardBackground),
        elevation = CardDefaults.cardElevation(0.dp)
    ) {
        Column {
            Box(modifier = Modifier.fillMaxWidth().aspectRatio(1f)) {
                if (imageUrl != null) {
                    SubcomposeAsyncImage(
                        model = ImageRequest.Builder(context)
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
                } else {
                    Box(
                        modifier = Modifier.fillMaxSize().background(Accent.copy(alpha = 0.12f)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(Icons.Outlined.Image, null, tint = Accent, modifier = Modifier.size(32.dp))
                    }
                }
            }

            Column(
                modifier = Modifier.padding(12.dp)
            ) {
                Text(
                    text = listing.itemId?.takeIf { it.isNotBlank() } ?: "ID",
                    fontSize = 15.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = TextSecondary,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )

                Spacer(Modifier.height(6.dp))

                Text(
                    text = "${listing.price ?: 0} ₽",
                    fontSize = 18.sp,
                    fontWeight = FontWeight.ExtraBold,
                    color = TextPrimary
                )

                listing.quantity?.let { qty ->
                    Text(
                        text = "В наличии: $qty",
                        fontSize = 11.sp,
                        color = TextSecondary
                    )
                }
            }
        }
    }
}