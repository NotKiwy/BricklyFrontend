package com.example.bricklyfrontend.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
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
import androidx.compose.ui.text.input.KeyboardType
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

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MyListingsScreen(
    onBack: () -> Unit
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

    var listings by remember { mutableStateOf<List<ListingDefaultDTO>>(emptyList()) }
    var isLoading by remember { mutableStateOf(true) }
    var errorMessage by remember { mutableStateOf<String?>(null) }

    var editTarget by remember { mutableStateOf<ListingDefaultDTO?>(null) }
    var editPrice by remember { mutableStateOf("") }
    var editQuantity by remember { mutableStateOf("") }
    var editDescription by remember { mutableStateOf("") }
    var isSaving by remember { mutableStateOf(false) }

    fun load() {
        scope.launch {
            isLoading = true
            errorMessage = null
            try {
                val resp = RetrofitClient.api.getListingsBySellerId(userId)
                if (resp.isSuccessful) {
                    listings = resp.body()?.content ?: emptyList()
                } else {
                    errorMessage = "Ошибка загрузки (${resp.code()})"
                }
            } catch (_: Exception) {
                errorMessage = "Нет соединения"
            }
            isLoading = false
        }
    }

    LaunchedEffect(Unit) { load() }

    fun openEdit(listing: ListingDefaultDTO) {
        editTarget = listing
        editPrice = listing.price?.toString() ?: ""
        editQuantity = listing.quantity?.toString() ?: ""
        editDescription = listing.description ?: ""
    }

    fun saveEdit() {
        val target = editTarget ?: return
        val price = editPrice.toIntOrNull() ?: return
        val qty = editQuantity.toIntOrNull() ?: return
        isSaving = true
        scope.launch {
            try {
                val resp = RetrofitClient.api.updateListing(
                    target.id,
                    ListingUpdateDTO(
                        status = target.status ?: "active",
                        quantity = qty,
                        description = editDescription,
                        price = price,
                        viewsCount = target.viewsCount ?: 0
                    )
                )
                if (resp.isSuccessful) {
                    val updated = resp.body()
                    if (updated != null) {
                        listings = listings.map { if (it.id == updated.id) updated else it }
                    }
                    editTarget = null
                }
            } catch (_: Exception) {}
            isSaving = false
        }
    }

    fun toggleListingStatus(listing: ListingDefaultDTO) {
        val newStatus = if (listing.status == "archived") "active" else "archived"
        scope.launch {
            try {
                val resp = RetrofitClient.api.updateListing(
                    listing.id,
                    ListingUpdateDTO(
                        status = newStatus,
                        quantity = listing.quantity ?: 0,
                        description = listing.description ?: "",
                        price = listing.price ?: 0,
                        viewsCount = listing.viewsCount ?: 0
                    )
                )
                if (resp.isSuccessful) {
                    val updated = resp.body()
                    if (updated != null) {
                        listings = listings.map { if (it.id == updated.id) updated else it }
                    }
                }
            } catch (_: Exception) {}
        }
    }

    if (editTarget != null) {
        ModalBottomSheet(
            onDismissRequest = { editTarget = null },
            containerColor = Background
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 24.dp)
                    .padding(bottom = 32.dp)
            ) {
                Text(
                    "Редактировать объявление",
                    fontWeight = FontWeight.ExtraBold,
                    fontSize = 20.sp,
                    color = TextPrimary
                )
                Spacer(Modifier.height(4.dp))
                Text(
                    editTarget?.itemId ?: "",
                    fontSize = 13.sp,
                    color = TextSecondary
                )
                Spacer(Modifier.height(20.dp))

                Text("ЦЕНА", fontSize = 10.sp, fontWeight = FontWeight.SemiBold, color = TextPrimary.copy(alpha = 0.5f), letterSpacing = 0.6.sp)
                Spacer(Modifier.height(4.dp))
                OutlinedTextField(
                    value = editPrice,
                    onValueChange = { editPrice = it },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true,
                    suffix = { Text("₽", color = TextSecondary) },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    shape = RoundedCornerShape(14.dp),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = Accent,
                        unfocusedBorderColor = Divider,
                        focusedContainerColor = CardBackground,
                        unfocusedContainerColor = CardBackground,
                        cursorColor = Accent
                    )
                )

                Spacer(Modifier.height(14.dp))
                Text("КОЛИЧЕСТВО", fontSize = 10.sp, fontWeight = FontWeight.SemiBold, color = TextPrimary.copy(alpha = 0.5f), letterSpacing = 0.6.sp)
                Spacer(Modifier.height(4.dp))
                OutlinedTextField(
                    value = editQuantity,
                    onValueChange = { editQuantity = it },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    shape = RoundedCornerShape(14.dp),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = Accent,
                        unfocusedBorderColor = Divider,
                        focusedContainerColor = CardBackground,
                        unfocusedContainerColor = CardBackground,
                        cursorColor = Accent
                    )
                )

                Spacer(Modifier.height(14.dp))
                Text("ОПИСАНИЕ", fontSize = 10.sp, fontWeight = FontWeight.SemiBold, color = TextPrimary.copy(alpha = 0.5f), letterSpacing = 0.6.sp)
                Spacer(Modifier.height(4.dp))
                OutlinedTextField(
                    value = editDescription,
                    onValueChange = { editDescription = it },
                    modifier = Modifier.fillMaxWidth().height(100.dp),
                    maxLines = 4,
                    shape = RoundedCornerShape(14.dp),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = Accent,
                        unfocusedBorderColor = Divider,
                        focusedContainerColor = CardBackground,
                        unfocusedContainerColor = CardBackground,
                        cursorColor = Accent
                    )
                )

                Spacer(Modifier.height(24.dp))
                Button(
                    onClick = { saveEdit() },
                    modifier = Modifier.fillMaxWidth().height(52.dp),
                    enabled = !isSaving,
                    shape = RoundedCornerShape(14.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = Accent, contentColor = TextPrimary)
                ) {
                    if (isSaving) CircularProgressIndicator(modifier = Modifier.size(20.dp), color = TextPrimary, strokeWidth = 2.dp)
                    else Text("Сохранить", fontWeight = FontWeight.SemiBold)
                }
                Spacer(Modifier.height(8.dp))
                TextButton(
                    onClick = { editTarget = null },
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text("Отмена", color = TextSecondary)
                }
            }
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
                    .padding(horizontal = 8.dp, vertical = 12.dp)
            ) {
                IconButton(onClick = onBack, modifier = Modifier.align(Alignment.CenterStart)) {
                    Icon(Icons.Outlined.ArrowBackIosNew, null, tint = TextPrimary)
                }
                Text(
                    "Мои объявления",
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
                EmptyStateBox(
                    icon = Icons.Outlined.ErrorOutline,
                    title = "Не удалось загрузить",
                    subtitle = errorMessage!!,
                    onRefresh = { load() }
                )
            }

            listings.isEmpty() -> Box(Modifier.fillMaxSize().padding(padding), contentAlignment = Alignment.Center) {
                EmptyStateBox(
                    icon = Icons.Outlined.Inventory2,
                    title = "Объявлений нет",
                    subtitle = "Создайте своё первое объявление в разделе Маркет",
                    onRefresh = { load() }
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
                    MyListingCard(
                        listing = listing,
                        imageLoader = imageLoader,
                        onEdit = { openEdit(listing) },
                        onToggleStatus = { toggleListingStatus(listing) }
                    )
                }
            }
        }
    }
}

@Composable
private fun MyListingCard(
    listing: ListingDefaultDTO,
    imageLoader: ImageLoader,
    onEdit: () -> Unit,
    onToggleStatus: () -> Unit
) {
    val context = LocalContext.current
    val imageUrl = listing.listingImage
        ?.firstOrNull { it.positionId == 0 }?.imagePath
        ?.takeIf { it.isNotBlank() }
        ?.let { if (it.startsWith("http")) it else "${RetrofitClient.BASE_URL}/${it.trimStart('/')}" }

    val isArchived = listing.status == "archived"

    Card(
        shape = RoundedCornerShape(18.dp),
        colors = CardDefaults.cardColors(containerColor = CardBackground),
        elevation = CardDefaults.cardElevation(0.dp)
    ) {
        Column {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(130.dp)
            ) {
                SubcomposeAsyncImage(
                    model = ImageRequest.Builder(context).data(imageUrl).crossfade(true).build(),
                    imageLoader = imageLoader,
                    contentDescription = null,
                    modifier = Modifier.fillMaxSize().clip(RoundedCornerShape(topStart = 18.dp, topEnd = 18.dp)),
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

                if (isArchived) {
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .background(TextPrimary.copy(alpha = 0.45f))
                            .clip(RoundedCornerShape(topStart = 18.dp, topEnd = 18.dp)),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            "АРХИВ",
                            fontSize = 12.sp,
                            fontWeight = FontWeight.ExtraBold,
                            color = CardBackground,
                            letterSpacing = 1.sp
                        )
                    }
                }
            }

            Column(modifier = Modifier.padding(10.dp)) {
                Text(
                    listing.itemId ?: "—",
                    fontSize = 13.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = TextPrimary,
                    maxLines = 1
                )
                Spacer(Modifier.height(2.dp))
                Text(
                    "${listing.price ?: 0} ₽",
                    fontSize = 15.sp,
                    fontWeight = FontWeight.ExtraBold,
                    color = TextPrimary
                )
                Text(
                    "В наличии: ${listing.quantity ?: 0}",
                    fontSize = 11.sp,
                    color = TextSecondary
                )

                Spacer(Modifier.height(8.dp))

                Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    OutlinedButton(
                        onClick = onEdit,
                        modifier = Modifier.weight(1f).height(34.dp),
                        shape = RoundedCornerShape(10.dp),
                        contentPadding = PaddingValues(0.dp),
                        border = ButtonDefaults.outlinedButtonBorder(enabled = true).copy(width = 1.dp)
                    ) {
                        Icon(Icons.Outlined.Edit, null, modifier = Modifier.size(14.dp), tint = TextPrimary)
                    }
                    OutlinedButton(
                        onClick = onToggleStatus,
                        modifier = Modifier.weight(1f).height(34.dp),
                        shape = RoundedCornerShape(10.dp),
                        contentPadding = PaddingValues(0.dp),
                        colors = ButtonDefaults.outlinedButtonColors(
                            contentColor = if (isArchived) TextPrimary else ErrorColor
                        ),
                        border = ButtonDefaults.outlinedButtonBorder(enabled = true).copy(width = 1.dp)
                    ) {
                        Icon(
                            if (isArchived) Icons.Outlined.Unarchive else Icons.Outlined.Archive,
                            null,
                            modifier = Modifier.size(14.dp)
                        )
                    }
                }
            }
        }
    }
}
