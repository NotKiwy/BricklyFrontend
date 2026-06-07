package com.example.bricklyfrontend.screens

import android.Manifest
import android.content.Context
import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CameraAlt
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.PhotoLibrary
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
import androidx.compose.ui.window.Dialog
import androidx.core.content.FileProvider
import coil.compose.AsyncImage
import com.example.bricklyfrontend.data.BrickognizeClient
import com.example.bricklyfrontend.data.BrickognizeItem
import com.example.bricklyfrontend.ui.theme.Accent
import com.example.bricklyfrontend.ui.theme.Background
import com.example.bricklyfrontend.ui.theme.CardBackground
import com.example.bricklyfrontend.ui.theme.TextPrimary
import com.example.bricklyfrontend.ui.theme.TextSecondary
import kotlinx.coroutines.launch
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.MultipartBody
import okio.BufferedSink
import java.io.File
import java.io.InputStream

private sealed class BrickState {
    object Idle : BrickState()
    data class Preview(val uri: Uri) : BrickState()
    object Loading : BrickState()
    data class Result(val items: List<BrickognizeItem>) : BrickState()
    data class Error(val message: String) : BrickState()
}

@Composable
fun BrickognizeScreen(
    onNavigateToMeetings: () -> Unit = {},
    onNavigateToHome: () -> Unit = {},
    onNavigateToCart: () -> Unit = {},
    onNavigateToProfile: () -> Unit = {},
    onNavigateToListingsByItem: (String) -> Unit = {}
) {
    SetStatusBarColor(Accent)
    
    val context = LocalContext.current
    val scope = rememberCoroutineScope()

    var state by remember { mutableStateOf<BrickState>(BrickState.Idle) }
    var showConfirmDialog by remember { mutableStateOf(false) }
    var pendingUri by remember { mutableStateOf<Uri?>(null) }
    val cameraUri = remember { mutableStateOf<Uri?>(null) }

    val cameraLauncher = rememberLauncherForActivityResult(ActivityResultContracts.TakePicture()) { success ->
        if (success) cameraUri.value?.let { pendingUri = it; showConfirmDialog = true }
    }

    val galleryLauncher = rememberLauncherForActivityResult(ActivityResultContracts.GetContent()) { uri ->
        uri?.let { pendingUri = it; showConfirmDialog = true }
    }

    val cameraPermissionLauncher = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { granted ->
        if (granted) {
            val uri = createCameraUri(context)
            cameraUri.value = uri
            cameraLauncher.launch(uri)
        }
    }

    if (showConfirmDialog && pendingUri != null) {
        ConfirmDialog(
            imageUri = pendingUri!!,
            onConfirm = {
                showConfirmDialog = false
                val uri = pendingUri!!
                state = BrickState.Preview(uri)
                scope.launch {
                    state = BrickState.Loading
                    state = uploadImage(context, uri)
                }
            },
            onDismiss = { showConfirmDialog = false; pendingUri = null }
        )
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
                Text(
                    text = "Распознать деталь",
                    color = TextPrimary,
                    fontWeight = FontWeight.ExtraBold,
                    fontSize = 26.sp
                )
            }
        },
        bottomBar = {
            BricklyBottomBar(
                currentRoute = "brickognize",
                onNavigate = { route ->
                    when (route) {
                        "meetings" -> onNavigateToMeetings()
                        "home" -> onNavigateToHome()
                        "cart" -> onNavigateToCart()
                        "profile" -> onNavigateToProfile()
                    }
                },
                onScanClick = {}
            )
        }
    ) { padding ->
        Box(
            modifier = Modifier.fillMaxSize().padding(padding),
            contentAlignment = Alignment.Center
        ) {
            when (val s = state) {
                is BrickState.Idle -> IdleContent(
                    onCamera = { cameraPermissionLauncher.launch(Manifest.permission.CAMERA) },
                    onGallery = { galleryLauncher.launch("image/*") }
                )

                is BrickState.Preview -> {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        modifier = Modifier.fillMaxWidth().padding(horizontal = 32.dp).padding(top = 16.dp)
                    ) {
                        AsyncImage(
                            model = s.uri,
                            contentDescription = null,
                            modifier = Modifier.size(200.dp).clip(RoundedCornerShape(20.dp)),
                            contentScale = ContentScale.Fit
                        )
                        Spacer(Modifier.height(24.dp))
                        CircularProgressIndicator(color = Accent)
                        Spacer(Modifier.height(8.dp))
                        Text("Отправка...", color = TextSecondary)
                    }
                }

                is BrickState.Loading -> {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        CircularProgressIndicator(color = Accent)
                        Spacer(Modifier.height(12.dp))
                        Text("Распознаём деталь...", color = TextSecondary)
                    }
                }

                is BrickState.Result -> ResultContent(items = s.items, onReset = { state = BrickState.Idle }, onNavigateToListingsByItem = onNavigateToListingsByItem)

                is BrickState.Error -> ErrorContent(message = s.message, onReset = { state = BrickState.Idle })
            }
        }
    }
}

@Composable
private fun IdleContent(onCamera: () -> Unit, onGallery: () -> Unit) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
        modifier = Modifier.padding(32.dp)
    ) {
        Box(
            modifier = Modifier.size(100.dp).clip(CircleShape).background(Accent),
            contentAlignment = Alignment.Center
        ) {
            Icon(Icons.Default.CameraAlt, contentDescription = null, tint = TextPrimary, modifier = Modifier.size(48.dp))
        }
        Spacer(Modifier.height(24.dp))
        Text("Распознать LEGO-деталь", fontSize = 22.sp, fontWeight = FontWeight.Bold, textAlign = TextAlign.Center, color = TextPrimary)
        Spacer(Modifier.height(8.dp))
        Text("Сфотографируйте деталь или выберите фото из галереи", fontSize = 14.sp, color = TextSecondary, textAlign = TextAlign.Center)
        Spacer(Modifier.height(40.dp))
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            Button(
                onClick = onCamera,
                modifier = Modifier.weight(1f).height(56.dp),
                shape = RoundedCornerShape(16.dp),
                colors = ButtonDefaults.buttonColors(containerColor = Accent, contentColor = TextPrimary)
            ) {
                Icon(Icons.Default.CameraAlt, contentDescription = "Камера", modifier = Modifier.size(26.dp))
            }
            Button(
                onClick = onGallery,
                modifier = Modifier.weight(1f).height(56.dp),
                shape = RoundedCornerShape(16.dp),
                colors = ButtonDefaults.buttonColors(containerColor = TextPrimary, contentColor = Color.White)
            ) {
                Icon(Icons.Default.PhotoLibrary, contentDescription = "Галерея", modifier = Modifier.size(26.dp))
            }
        }
    }
}

@Composable
private fun ConfirmDialog(imageUri: Uri, onConfirm: () -> Unit, onDismiss: () -> Unit) {
    Dialog(onDismissRequest = onDismiss) {
        Surface(shape = RoundedCornerShape(28.dp), color = Background, tonalElevation = 0.dp) {
            Column(modifier = Modifier.padding(24.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                Text("Отправить на распознавание?", fontWeight = FontWeight.Bold, fontSize = 17.sp, textAlign = TextAlign.Center, color = TextPrimary)
                Spacer(Modifier.height(16.dp))
                AsyncImage(
                    model = imageUri,
                    contentDescription = null,
                    modifier = Modifier.size(220.dp).clip(RoundedCornerShape(20.dp)),
                    contentScale = ContentScale.Fit
                )
                Spacer(Modifier.height(20.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    OutlinedButton(onClick = onDismiss, modifier = Modifier.weight(1f).height(52.dp), shape = RoundedCornerShape(14.dp), colors = ButtonDefaults.outlinedButtonColors(contentColor = TextPrimary)) {
                        Icon(Icons.Default.Close, contentDescription = "Отмена", modifier = Modifier.size(24.dp))
                    }
                    Button(onClick = onConfirm, modifier = Modifier.weight(1f).height(52.dp), shape = RoundedCornerShape(14.dp), colors = ButtonDefaults.buttonColors(containerColor = Accent, contentColor = TextPrimary)) {
                        Icon(Icons.Default.Check, contentDescription = "Отправить", modifier = Modifier.size(24.dp))
                    }
                }
            }
        }
    }
}

@Composable
private fun ResultContent(
    items: List<BrickognizeItem>,
    onReset: () -> Unit,
    onNavigateToListingsByItem: (String) -> Unit = {}
) {
    if (items.isEmpty()) {
        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            EmptyStateBox(
                icon = Icons.Outlined.ImageSearch,
                title = "Ничего не найдено",
                subtitle = "Попробуйте сделать более чёткий снимок",
                onRefresh = onReset
            )
        }
    } else {
        Column(modifier = Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(16.dp), horizontalAlignment = Alignment.CenterHorizontally) {
            items.forEachIndexed { index, item ->
                BrickResultCard(item = item, rank = index + 1, onSearch = { onNavigateToListingsByItem(item.id) })
                Spacer(Modifier.height(12.dp))
            }
            Spacer(Modifier.height(8.dp))
            val topItemId = items.firstOrNull()?.id ?: ""
            Button(
                onClick = { onNavigateToListingsByItem(topItemId) },
                colors = ButtonDefaults.buttonColors(containerColor = Accent, contentColor = TextPrimary),
                shape = RoundedCornerShape(14.dp),
                modifier = Modifier.fillMaxWidth().height(52.dp)
            ) {
                Text("Показать предложения", fontWeight = FontWeight.SemiBold)
            }
            Spacer(Modifier.height(10.dp))
            TextButton(
                onClick = onReset,
                modifier = Modifier.fillMaxWidth().height(52.dp),
                shape = RoundedCornerShape(14.dp)
            ) {
                Text("Распознать ещё", fontWeight = FontWeight.SemiBold, color = TextPrimary)
            }
        }
    }
}

@Composable
private fun BrickResultCard(item: BrickognizeItem, rank: Int, onSearch: () -> Unit = {}) {
    val clipboard = androidx.compose.ui.platform.LocalClipboardManager.current
    val pct = (item.score * 100).toInt()

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = CardBackground),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
    ) {
        Column {
            if (!item.img_url.isNullOrBlank()) {
                Box(modifier = Modifier.fillMaxWidth().height(200.dp)) {
                    AsyncImage(
                        model = item.img_url,
                        contentDescription = item.name,
                        modifier = Modifier.fillMaxSize().padding(top = 12.dp).clip(RoundedCornerShape(topStart = 20.dp, topEnd = 20.dp)),
                        contentScale = ContentScale.Fit
                    )
                    Box(
                        modifier = Modifier.padding(12.dp).size(32.dp).clip(CircleShape).background(Accent).align(Alignment.TopStart),
                        contentAlignment = Alignment.Center
                    ) {
                        Text("$rank", fontSize = 13.sp, fontWeight = FontWeight.Bold, color = TextPrimary)
                    }
                }
            }
            Column(modifier = Modifier.padding(16.dp)) {
                Text(text = item.name ?: item.id, fontWeight = FontWeight.Bold, fontSize = 16.sp, color = TextPrimary)
                Spacer(Modifier.height(8.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        Surface(
                            shape = RoundedCornerShape(8.dp),
                            color = Color(0xFFF0F0F0),
                            onClick = { clipboard.setText(androidx.compose.ui.text.AnnotatedString(item.id)) }
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(4.dp)
                            ) {
                                Text("ID: ${item.id}", fontSize = 12.sp, color = TextSecondary)
                                Icon(Icons.Outlined.ContentCopy, null, tint = TextSecondary, modifier = Modifier.size(12.dp))
                            }
                        }
                        Surface(shape = RoundedCornerShape(8.dp), color = if (pct >= 80) Color(0xFFE8F5E9) else Color(0xFFF0F0F0)) {
                            Text(
                                text = "$pct%",
                                fontSize = 12.sp,
                                color = if (pct >= 80) Color(0xFF2E7D32) else TextSecondary,
                                fontWeight = FontWeight.SemiBold,
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                            )
                        }
                    }
                    Surface(
                        shape = RoundedCornerShape(10.dp),
                        color = Accent,
                        onClick = onSearch
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            Icon(Icons.Outlined.Search, null, tint = TextPrimary, modifier = Modifier.size(14.dp))
                            Text("Искать", fontSize = 12.sp, fontWeight = FontWeight.SemiBold, color = TextPrimary)
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun ErrorContent(message: String, onReset: () -> Unit) {
    Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.padding(32.dp)) {
        Text("Ошибка", fontWeight = FontWeight.Bold, fontSize = 18.sp, color = MaterialTheme.colorScheme.error)
        Spacer(Modifier.height(8.dp))
        Text(message, textAlign = TextAlign.Center, color = TextSecondary)
        Spacer(Modifier.height(24.dp))
        Button(onClick = onReset, colors = ButtonDefaults.buttonColors(containerColor = Accent, contentColor = TextPrimary), shape = RoundedCornerShape(14.dp), modifier = Modifier.fillMaxWidth().height(52.dp)) {
            Text("Попробовать снова", fontWeight = FontWeight.SemiBold)
        }
    }
}

private fun createCameraUri(context: Context): Uri {
    val dir = File(context.cacheDir, "images").also { it.mkdirs() }
    val file = File(dir, "camera_${System.currentTimeMillis()}.jpg")
    return FileProvider.getUriForFile(context, "${context.packageName}.fileprovider", file)
}

private suspend fun uploadImage(context: Context, uri: Uri): BrickState {
    return try {
        val bytes = compressImageBytes(context, uri)
        val mediaType = "image/jpeg".toMediaTypeOrNull()
        val requestBody = object : okhttp3.RequestBody() {
            override fun contentType() = mediaType
            override fun writeTo(sink: BufferedSink) { sink.write(bytes) }
        }
        val part = MultipartBody.Part.createFormData("query_image", "photo.jpg", requestBody)
        val response = BrickognizeClient.api.predictPart(part)
        if (response.isSuccessful) {
            BrickState.Result(response.body()?.items ?: emptyList())
        } else {
            BrickState.Error("Сервер вернул ошибку: ${response.code()}")
        }
    } catch (e: Exception) {
        BrickState.Error(e.message ?: "Неизвестная ошибка")
    }
}
