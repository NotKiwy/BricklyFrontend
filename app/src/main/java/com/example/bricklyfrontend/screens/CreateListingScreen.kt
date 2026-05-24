package com.example.bricklyfrontend.screens

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.*
import androidx.compose.material.icons.outlined.ArrowBackIosNew
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.example.bricklyfrontend.data.RetrofitClient
import com.example.bricklyfrontend.data.UserPreferences
import com.example.bricklyfrontend.ui.theme.*
import kotlinx.coroutines.launch
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.MultipartBody
import okhttp3.RequestBody.Companion.toRequestBody
import java.io.File

@Composable
fun CreateListingScreen(
    onNavigateBack: () -> Unit,
    onListingCreated: () -> Unit
) {
    SetStatusBarColor(Accent)

    val context = LocalContext.current
    val scope = rememberCoroutineScope()

    var itemType by remember { mutableStateOf("") }
    var itemId by remember { mutableStateOf("") }
    var quantity by remember { mutableStateOf("") }
    var description by remember { mutableStateOf("") }
    var condition by remember { mutableStateOf("NEW") }
    var conditionRate by remember { mutableStateOf("") }
    var price by remember { mutableStateOf("") }
    var selectedImages by remember { mutableStateOf<List<Uri>>(emptyList()) }
    var isLoading by remember { mutableStateOf(false) }
    var errorMessage by remember { mutableStateOf<String?>(null) }
    var showItemTypeDialog by remember { mutableStateOf(false) }
    var showConditionDialog by remember { mutableStateOf(false) }

    val imagePickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetMultipleContents()
    ) { uris ->
        selectedImages = (selectedImages + uris).take(5)
    }

    fun createListing() {
        scope.launch {
            isLoading = true
            errorMessage = null
            try {
                val userId = UserPreferences.getUserId(context)
                if (userId == -1L) {
                    errorMessage = "Пользователь не авторизован"
                    isLoading = false
                    return@launch
                }

                val imageParts = selectedImages.mapIndexed { index, uri ->
                    val inputStream = context.contentResolver.openInputStream(uri)
                    val bytes = inputStream?.readBytes()
                    inputStream?.close()

                    if (bytes == null) {
                        errorMessage = "Ошибка чтения изображения $index"
                        return@launch
                    }

                    val requestBody = bytes.toRequestBody("image/*".toMediaTypeOrNull())
                    MultipartBody.Part.createFormData("images", "image_$index.jpg", requestBody)
                }

                val response = RetrofitClient.api.createListing(
                    sellerId = userId.toString().toRequestBody("text/plain".toMediaTypeOrNull()),
                    itemType = itemType.toRequestBody("text/plain".toMediaTypeOrNull()),
                    itemId = itemId.toRequestBody("text/plain".toMediaTypeOrNull()),
                    quantity = quantity.toRequestBody("text/plain".toMediaTypeOrNull()),
                    description = description.takeIf { it.isNotBlank() }?.toRequestBody("text/plain".toMediaTypeOrNull()),
                    condition = condition.toRequestBody("text/plain".toMediaTypeOrNull()),
                    conditionRate = conditionRate.toRequestBody("text/plain".toMediaTypeOrNull()),
                    price = price.toRequestBody("text/plain".toMediaTypeOrNull()),
                    status = "sell".toRequestBody("text/plain".toMediaTypeOrNull()),
                    images = imageParts
                )

                if (response.isSuccessful) {
                    onListingCreated()
                } else {
                    errorMessage = "Ошибка создания (${response.code()}): ${response.errorBody()?.string()}"
                }
            } catch (e: Exception) {
                errorMessage = "Ошибка: ${e.message}"
            } finally {
                isLoading = false
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
                    .padding(horizontal = 8.dp, vertical = 8.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.Outlined.ArrowBackIosNew, null, tint = TextPrimary)
                    }
                    Spacer(Modifier.width(4.dp))
                    Text(
                        "Создать объявление",
                        fontSize = 20.sp,
                        fontWeight = FontWeight.ExtraBold,
                        color = TextPrimary
                    )
                }
            }
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .verticalScroll(rememberScrollState())
                .padding(20.dp)
        ) {
            Text("Фотографии товара (до 5)", style = MaterialTheme.typography.titleSmall, color = TextPrimary)
            Spacer(Modifier.height(8.dp))

            LazyRow(
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                items(selectedImages) { uri ->
                    Box(
                        modifier = Modifier
                            .size(100.dp)
                            .clip(RoundedCornerShape(12.dp))
                    ) {
                        AsyncImage(
                            model = uri,
                            contentDescription = null,
                            modifier = Modifier.fillMaxSize(),
                            contentScale = ContentScale.Crop
                        )
                        IconButton(
                            onClick = { selectedImages = selectedImages - uri },
                            modifier = Modifier.align(Alignment.TopEnd).size(32.dp)
                        ) {
                            Icon(Icons.Outlined.Cancel, null, tint = Color.White, modifier = Modifier.size(20.dp))
                        }
                    }
                }

                if (selectedImages.size < 5) {
                    item {
                        Box(
                            modifier = Modifier
                                .size(100.dp)
                                .clip(RoundedCornerShape(12.dp))
                                .background(CardBackground)
                                .border(1.dp, Divider, RoundedCornerShape(12.dp))
                                .clickable { imagePickerLauncher.launch("image/*") },
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(Icons.Outlined.AddPhotoAlternate, null, tint = IconInactive, modifier = Modifier.size(32.dp))
                        }
                    }
                }
            }

            Spacer(Modifier.height(20.dp))

            Text("Тип товара *", style = MaterialTheme.typography.titleSmall, color = TextPrimary)
            Spacer(Modifier.height(8.dp))
            OutlinedTextField(
                value = when (itemType) {
                    "P" -> "Деталь"
                    "M" -> "Минифигурка"
                    "S" -> "Набор"
                    else -> "Выберите тип"
                },
                onValueChange = {},
                modifier = Modifier.fillMaxWidth().clickable { showItemTypeDialog = true },
                enabled = false,
                colors = OutlinedTextFieldDefaults.colors(
                    disabledBorderColor = Divider,
                    disabledTextColor = TextPrimary,
                    disabledContainerColor = Color.White
                ),
                trailingIcon = {
                    Icon(Icons.Outlined.ArrowDropDown, null)
                }
            )

            Spacer(Modifier.height(16.dp))

            BricklyTextField(
                value = itemId,
                onValueChange = { itemId = it; errorMessage = null },
                label = "ID товара *",
                placeholder = "Например: 3001"
            )

            Spacer(Modifier.height(16.dp))

            BricklyTextField(
                value = quantity,
                onValueChange = { quantity = it; errorMessage = null },
                label = "Количество *",
                placeholder = "1",
                keyboardType = KeyboardType.Number
            )

            Spacer(Modifier.height(16.dp))

            Text("Описание", style = MaterialTheme.typography.titleSmall, color = TextPrimary)
            Spacer(Modifier.height(8.dp))
            OutlinedTextField(
                value = description,
                onValueChange = { description = it },
                modifier = Modifier.fillMaxWidth(),
                placeholder = {
                    Text(
                        text = "Опишите товар",
                        style = MaterialTheme.typography.bodyLarge,
                        color = TextSecondary.copy(alpha = 0.4f)
                    )
                },
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = Accent,
                    unfocusedBorderColor = Divider,
                    cursorColor = Accent,
                    focusedContainerColor = Color.White,
                    unfocusedContainerColor = Color.White
                ),
                shape = RoundedCornerShape(12.dp),
                minLines = 3,
                maxLines = 5
            )

            Spacer(Modifier.height(16.dp))

            Text("Состояние *", style = MaterialTheme.typography.titleSmall, color = TextPrimary)
            Spacer(Modifier.height(8.dp))
            OutlinedTextField(
                value = if (condition == "NEW") "Новое" else "Б/У",
                onValueChange = {},
                modifier = Modifier.fillMaxWidth().clickable { showConditionDialog = true },
                enabled = false,
                colors = OutlinedTextFieldDefaults.colors(
                    disabledBorderColor = Divider,
                    disabledTextColor = TextPrimary,
                    disabledContainerColor = Color.White
                ),
                trailingIcon = {
                    Icon(Icons.Outlined.ArrowDropDown, null)
                }
            )

            Spacer(Modifier.height(16.dp))

            BricklyTextField(
                value = conditionRate,
                onValueChange = { if (it.isEmpty() || it.toIntOrNull() in 1..10) conditionRate = it },
                label = "Оценка состояния * (1-10)",
                placeholder = "10",
                keyboardType = KeyboardType.Number
            )

            Spacer(Modifier.height(16.dp))

            BricklyTextField(
                value = price,
                onValueChange = { price = it; errorMessage = null },
                label = "Цена * (₽)",
                placeholder = "100",
                keyboardType = KeyboardType.Number
            )

            if (errorMessage != null) {
                Spacer(Modifier.height(16.dp))
                Text(errorMessage!!, color = ErrorColor, style = MaterialTheme.typography.bodyMedium)
            }

            Spacer(Modifier.height(24.dp))

            Button(
                onClick = {
                    when {
                        itemType.isEmpty() -> errorMessage = "Выберите тип товара"
                        itemId.isBlank() -> errorMessage = "Укажите ID товара"
                        quantity.isBlank() || quantity.toIntOrNull() == null -> errorMessage = "Укажите количество"
                        conditionRate.isBlank() || conditionRate.toIntOrNull() !in 1..10 -> errorMessage = "Оценка должна быть от 1 до 10"
                        price.isBlank() || price.toIntOrNull() == null -> errorMessage = "Укажите цену"
                        selectedImages.isEmpty() -> errorMessage = "Добавьте хотя бы одну фотографию"
                        else -> createListing()
                    }
                },
                modifier = Modifier.fillMaxWidth().height(56.dp),
                shape = RoundedCornerShape(16.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = Accent,
                    contentColor = TextPrimary
                ),
                enabled = !isLoading
            ) {
                if (isLoading) {
                    CircularProgressIndicator(modifier = Modifier.size(24.dp), color = TextPrimary, strokeWidth = 2.dp)
                } else {
                    Text("Создать объявление", style = MaterialTheme.typography.labelLarge.copy(fontSize = 16.sp))
                }
            }

            Spacer(Modifier.height(32.dp))
        }
    }

    if (showItemTypeDialog) {
        AlertDialog(
            onDismissRequest = { showItemTypeDialog = false },
            title = { Text("Выберите тип товара", color = TextPrimary) },
            text = {
                Column {
                    listOf("P" to "Деталь", "M" to "Минифигурка", "S" to "Набор").forEach { (code, name) ->
                        TextButton(
                            onClick = {
                                itemType = code
                                showItemTypeDialog = false
                            },
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text(name, modifier = Modifier.fillMaxWidth(), color = TextPrimary)
                        }
                    }
                }
            },
            confirmButton = {},
            dismissButton = {
                TextButton(onClick = { showItemTypeDialog = false }) {
                    Text("Отмена", color = TextPrimary)
                }
            }
        )
    }

    if (showConditionDialog) {
        AlertDialog(
            onDismissRequest = { showConditionDialog = false },
            title = { Text("Выберите состояние", color = TextPrimary) },
            text = {
                Column {
                    listOf("NEW" to "Новое", "USED" to "Б/У").forEach { (code, name) ->
                        TextButton(
                            onClick = {
                                condition = code
                                showConditionDialog = false
                            },
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text(name, modifier = Modifier.fillMaxWidth(), color = TextPrimary)
                        }
                    }
                }
            },
            confirmButton = {},
            dismissButton = {
                TextButton(onClick = { showConditionDialog = false }) {
                    Text("Отмена", color = TextPrimary)
                }
            }
        )
    }
}
