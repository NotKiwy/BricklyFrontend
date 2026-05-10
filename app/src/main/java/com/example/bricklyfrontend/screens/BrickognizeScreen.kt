package com.example.bricklyfrontend.screens

import android.Manifest
import android.content.Context
import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.border
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
import com.example.bricklyfrontend.ui.theme.TextPrimary
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

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun BrickognizeScreen(onBack: () -> Unit) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()

    var state by remember { mutableStateOf<BrickState>(BrickState.Idle) }
    var showConfirmDialog by remember { mutableStateOf(false) }
    var pendingUri by remember { mutableStateOf<Uri?>(null) }
    val cameraUri = remember { mutableStateOf<Uri?>(null) }

    val cameraLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.TakePicture()
    ) { success ->
        if (success) {
            cameraUri.value?.let { uri ->
                pendingUri = uri
                showConfirmDialog = true
            }
        }
    }

    val galleryLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.GetContent()
    ) { uri ->
        uri?.let {
            pendingUri = it
            showConfirmDialog = true
        }
    }

    val cameraPermissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { granted ->
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
            onDismiss = {
                showConfirmDialog = false
                pendingUri = null
            }
        )
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        "Распознать деталь",
                        color = TextPrimary,
                        fontWeight = FontWeight.Bold
                    )
                },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.Default.Close, contentDescription = "Назад", tint = TextPrimary)
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = Accent)
            )
        }
    ) { padding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding),
            contentAlignment = Alignment.Center
        ) {
            when (val s = state) {
                is BrickState.Idle -> IdleContent(
                    onCamera = { cameraPermissionLauncher.launch(Manifest.permission.CAMERA) },
                    onGallery = { galleryLauncher.launch("image/*") }
                )

                is BrickState.Preview -> {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        AsyncImage(
                            model = s.uri,
                            contentDescription = null,
                            modifier = Modifier
                                .size(240.dp)
                                .clip(RoundedCornerShape(16.dp)),
                            contentScale = ContentScale.Crop
                        )
                        Spacer(Modifier.height(16.dp))
                        CircularProgressIndicator(color = Accent)
                        Spacer(Modifier.height(8.dp))
                        Text("Отправка...")
                    }
                }

                is BrickState.Loading -> {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        CircularProgressIndicator(color = Accent)
                        Spacer(Modifier.height(12.dp))
                        Text("Распознаём деталь...")
                    }
                }

                is BrickState.Result -> ResultContent(
                    items = s.items,
                    onReset = { state = BrickState.Idle }
                )

                is BrickState.Error -> ErrorContent(
                    message = s.message,
                    onReset = { state = BrickState.Idle }
                )
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
        Text(
            text = "Распознать LEGO-деталь",
            fontSize = 22.sp,
            fontWeight = FontWeight.Bold,
            textAlign = TextAlign.Center,
            color = TextPrimary
        )
        Spacer(Modifier.height(8.dp))
        Text(
            text = "Сфотографируйте деталь или выберите фото из галереи",
            fontSize = 14.sp,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center
        )
        Spacer(Modifier.height(40.dp))

        Button(
            onClick = onCamera,
            modifier = Modifier
                .fillMaxWidth()
                .height(56.dp),
            shape = RoundedCornerShape(12.dp),
            colors = ButtonDefaults.buttonColors(containerColor = Accent, contentColor = TextPrimary)
        ) {
            Icon(Icons.Default.CameraAlt, contentDescription = null)
            Spacer(Modifier.width(8.dp))
            Text("Сделать снимок", fontWeight = FontWeight.SemiBold)
        }

        Spacer(Modifier.height(16.dp))

        OutlinedButton(
            onClick = onGallery,
            modifier = Modifier
                .fillMaxWidth()
                .height(56.dp),
            shape = RoundedCornerShape(12.dp)
        ) {
            Icon(Icons.Default.PhotoLibrary, contentDescription = null)
            Spacer(Modifier.width(8.dp))
            Text("Выбрать из галереи")
        }
    }
}

@Composable
private fun ConfirmDialog(imageUri: Uri, onConfirm: () -> Unit, onDismiss: () -> Unit) {
    Dialog(onDismissRequest = onDismiss) {
        Surface(
            shape = RoundedCornerShape(20.dp),
            tonalElevation = 8.dp
        ) {
            Column(
                modifier = Modifier.padding(20.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    text = "Отправить на распознавание?",
                    fontWeight = FontWeight.SemiBold,
                    fontSize = 16.sp,
                    textAlign = TextAlign.Center
                )
                Spacer(Modifier.height(16.dp))
                AsyncImage(
                    model = imageUri,
                    contentDescription = null,
                    modifier = Modifier
                        .size(220.dp)
                        .clip(RoundedCornerShape(12.dp))
                        .border(1.dp, MaterialTheme.colorScheme.outline, RoundedCornerShape(12.dp)),
                    contentScale = ContentScale.Crop
                )
                Spacer(Modifier.height(20.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                    FilledIconButton(
                        onClick = onDismiss,
                        modifier = Modifier.size(56.dp),
                        colors = IconButtonDefaults.filledIconButtonColors(
                            containerColor = MaterialTheme.colorScheme.errorContainer,
                            contentColor = MaterialTheme.colorScheme.onErrorContainer
                        )
                    ) {
                        Icon(Icons.Default.Close, contentDescription = "Отмена", modifier = Modifier.size(28.dp))
                    }
                    FilledIconButton(
                        onClick = onConfirm,
                        modifier = Modifier.size(56.dp),
                        colors = IconButtonDefaults.filledIconButtonColors(
                            containerColor = Accent,
                            contentColor = TextPrimary
                        )
                    ) {
                        Icon(Icons.Default.Check, contentDescription = "Отправить", modifier = Modifier.size(28.dp))
                    }
                }
            }
        }
    }
}

@Composable
private fun ResultContent(items: List<BrickognizeItem>, onReset: () -> Unit) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = if (items.isEmpty()) Arrangement.Center else Arrangement.Top
    ) {
        if (items.isEmpty()) {
            Spacer(Modifier.weight(1f))
            Text(
                "Ничего не найдено",
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                fontSize = 16.sp,
                textAlign = TextAlign.Center
            )
            Spacer(Modifier.height(20.dp))
            Button(
                onClick = onReset,
                colors = ButtonDefaults.buttonColors(containerColor = Accent, contentColor = TextPrimary),
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier.height(48.dp)
            ) {
                Text("Распознать ещё", fontWeight = FontWeight.SemiBold)
            }
            Spacer(Modifier.weight(1f))
        } else {
            items.forEachIndexed { index, item ->
                BrickResultCard(item = item, rank = index + 1)
                Spacer(Modifier.height(12.dp))
            }
            Spacer(Modifier.height(8.dp))
            Button(
                onClick = onReset,
                colors = ButtonDefaults.buttonColors(containerColor = Accent, contentColor = TextPrimary),
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier.height(48.dp)
            ) {
                Text("Распознать ещё", fontWeight = FontWeight.SemiBold)
            }
        }
    }
}

@Composable
private fun BrickResultCard(item: BrickognizeItem, rank: Int) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            if (!item.img_url.isNullOrBlank()) {
                AsyncImage(
                    model = item.img_url,
                    contentDescription = item.name,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(200.dp)
                        .clip(RoundedCornerShape(12.dp)),
                    contentScale = ContentScale.Fit
                )
                Spacer(Modifier.height(12.dp))
            }

            Row(verticalAlignment = Alignment.CenterVertically) {
                Surface(
                    shape = CircleShape,
                    color = Accent,
                    modifier = Modifier.size(28.dp)
                ) {
                    Box(contentAlignment = Alignment.Center, modifier = Modifier.fillMaxSize()) {
                        Text("$rank", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = TextPrimary)
                    }
                }
                Spacer(Modifier.width(8.dp))
                Text(
                    text = item.name ?: item.id,
                    fontWeight = FontWeight.SemiBold,
                    fontSize = 16.sp,
                    color = TextPrimary
                )
            }

            Spacer(Modifier.height(4.dp))
            Text(
                text = "ID: ${item.id}",
                fontSize = 13.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            val pct = (item.score * 100).toInt()
            Text(
                text = "Точность: $pct%",
                fontSize = 13.sp,
                color = if (pct >= 80) Color(0xFF2E7D32) else MaterialTheme.colorScheme.onSurfaceVariant
            )
            item.external_sites?.forEach { link ->
                Spacer(Modifier.height(2.dp))
                Text(
                    text = "${link.name}: ${link.url}",
                    fontSize = 12.sp,
                    color = MaterialTheme.colorScheme.primary
                )
            }
        }
    }
}

@Composable
private fun ErrorContent(message: String, onReset: () -> Unit) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier.padding(32.dp)
    ) {
        Text("Ошибка", fontWeight = FontWeight.Bold, fontSize = 18.sp, color = MaterialTheme.colorScheme.error)
        Spacer(Modifier.height(8.dp))
        Text(message, textAlign = TextAlign.Center, color = MaterialTheme.colorScheme.onSurfaceVariant)
        Spacer(Modifier.height(24.dp))
        Button(
            onClick = onReset,
            colors = ButtonDefaults.buttonColors(containerColor = Accent, contentColor = TextPrimary),
            shape = RoundedCornerShape(12.dp)
        ) {
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
        val stream: InputStream = context.contentResolver.openInputStream(uri)
            ?: return BrickState.Error("Не удалось открыть изображение")
        val bytes = stream.readBytes()
        stream.close()

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
