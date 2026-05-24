package com.example.bricklyfrontend.screens

import android.content.Context
import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
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
import okhttp3.RequestBody.Companion.asRequestBody
import okhttp3.RequestBody.Companion.toRequestBody
import java.io.File
import java.io.FileOutputStream

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CreateListingScreen(
    onBack: () -> Unit
) {
    SetStatusBarColor(Accent)
    
    val context = LocalContext.current
    val scope = rememberCoroutineScope()

    var itemId by remember { mutableStateOf("") }
    var quantity by remember { mutableStateOf("") }
    var description by remember { mutableStateOf("") }
    var price by remember { mutableStateOf("") }
    var conditionRate by remember { mutableStateOf("") }
    
    var selectedItemType by remember { mutableStateOf("P") }
    
    var selectedCondition by remember { mutableStateOf("NEW") }
    
    var selectedImageUris by remember { mutableStateOf<List<Uri>>(emptyList()) }
    
    var isSaving by remember { mutableStateOf(false) }
    var errorMessage by remember { mutableStateOf<String?>(null) }
    var successMessage by remember { mutableStateOf<String?>(null) }

    val imagePicker = rememberLauncherForActivityResult(ActivityResultContracts.GetMultipleContents()) { uris ->
        if (uris.isNotEmpty()) {
            selectedImageUris = uris.take(5)
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
                    IconButton(onClick = onBack) {
                        Icon(
                            Icons.Outlined.ArrowBackIosNew,
                            contentDescription = "Назад",
                            tint = TextPrimary
                        )
                    }
                    Spacer(Modifier.width(4.dp))
                    Text(
                        text = "Создать карточку товара",
                        color = TextPrimary,
                        fontWeight = FontWeight.ExtraBold,
                        fontSize = 20.sp
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
                .padding(horizontal = 20.dp)
        ) {
            Spacer(Modifier.height(16.dp))

            Text("Фотографии (до 5)", style = MaterialTheme.typography.labelLarge, color = TextPrimary)
            Spacer(Modifier.height(8.dp))
            
            if (selectedImageUris.isEmpty()) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(140.dp)
                        .clip(RoundedCornerShape(12.dp))
                        .background(CardBackground)
                        .border(1.dp, Divider, RoundedCornerShape(12.dp))
                        .clickable { imagePicker.launch("image/*") },
                    contentAlignment = Alignment.Center
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Icon(Icons.Outlined.AddPhotoAlternate, null, tint = IconInactive, modifier = Modifier.size(40.dp))
                        Spacer(Modifier.height(8.dp))
                        Text("Нажмите для выбора фото", color = TextSecondary, fontSize = 13.sp)
                    }
                }
            } else {
                LazyRow(
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    itemsIndexed(selectedImageUris) { index, uri ->
                        Box(
                            modifier = Modifier
                                .size(120.dp)
                                .clip(RoundedCornerShape(12.dp))
                                .background(CardBackground)
                                .border(1.dp, Divider, RoundedCornerShape(12.dp))
                        ) {
                                AsyncImage(
                                    model = uri,
                                    contentDescription = null,
                                    modifier = Modifier.fillMaxSize(),
                                    contentScale = ContentScale.Crop
                                )
                                Box(
                                modifier = Modifier
                                    .align(Alignment.TopEnd)
                                    .padding(6.dp)
                                    .size(28.dp)
                                    .clip(CircleShape)
                                    .background(ErrorColor.copy(alpha = 0.9f))
                                    .clickable {
                                        selectedImageUris = selectedImageUris.filterIndexed { i, _ -> i != index }
                                    },
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    Icons.Outlined.Close,
                                    contentDescription = "Удалить",
                                    tint = Color.White,
                                    modifier = Modifier.size(18.dp)
                                )
                            }
                        }
                    }
                    
                    if (selectedImageUris.size < 5) {
                        item {
                            Box(
                                modifier = Modifier
                                    .size(120.dp)
                                    .clip(RoundedCornerShape(12.dp))
                                    .background(CardBackground)
                                    .border(1.dp, Divider, RoundedCornerShape(12.dp))
                                    .clickable { imagePicker.launch("image/*") },
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    Icons.Outlined.Add,
                                    contentDescription = "Добавить фото",
                                    tint = IconInactive,
                                    modifier = Modifier.size(32.dp)
                                )
                            }
                        }
                    }
                }
            }
            
            Spacer(Modifier.height(16.dp))

            Text("Тип товара *", style = MaterialTheme.typography.labelLarge, color = TextPrimary)
            Spacer(Modifier.height(8.dp))
            Row(
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                ItemTypeButton(
                    label = "Деталь",
                    code = "P",
                    isSelected = selectedItemType == "P",
                    onClick = { selectedItemType = "P" },
                    modifier = Modifier.weight(1f)
                )
                ItemTypeButton(
                    label = "Минифигурка",
                    code = "M",
                    isSelected = selectedItemType == "M",
                    onClick = { selectedItemType = "M" },
                    modifier = Modifier.weight(1f)
                )
                ItemTypeButton(
                    label = "Набор",
                    code = "S",
                    isSelected = selectedItemType == "S",
                    onClick = { selectedItemType = "S" },
                    modifier = Modifier.weight(1f)
                )
            }
            Spacer(Modifier.height(16.dp))

            BricklyTextField(
                value = itemId,
                onValueChange = { itemId = it },
                label = "Артикул товара *",
                placeholder = "3001"
            )
            Spacer(Modifier.height(16.dp))

            BricklyTextField(
                value = quantity,
                onValueChange = { quantity = it.filter { c -> c.isDigit() } },
                label = "Количество *",
                placeholder = "10",
                keyboardType = KeyboardType.Number
            )
            Spacer(Modifier.height(16.dp))

            BricklyTextField(
                value = description,
                onValueChange = { description = it },
                label = "Описание",
                placeholder = "Дополнительная информация о товаре"
            )
            Spacer(Modifier.height(16.dp))

            Text("Состояние *", style = MaterialTheme.typography.labelLarge, color = TextPrimary)
            Spacer(Modifier.height(8.dp))
            Row(
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                ConditionButton(
                    label = "Новое",
                    code = "NEW",
                    isSelected = selectedCondition == "NEW",
                    onClick = { selectedCondition = "NEW" },
                    modifier = Modifier.weight(1f)
                )
                ConditionButton(
                    label = "Б/У",
                    code = "USED",
                    isSelected = selectedCondition == "USED",
                    onClick = { selectedCondition = "USED" },
                    modifier = Modifier.weight(1f)
                )
            }
            Spacer(Modifier.height(16.dp))

            BricklyTextField(
                value = conditionRate,
                onValueChange = { 
                    val filtered = it.filter { c -> c.isDigit() }
                    val num = filtered.toIntOrNull()
                    conditionRate = when {
                        filtered.isEmpty() -> ""
                        num == null -> conditionRate
                        num < 1 -> "1"
                        num > 10 -> "10"
                        else -> filtered
                    }
                },
                label = "Оценка состояния (1-10) *",
                placeholder = "8",
                keyboardType = KeyboardType.Number
            )
            Spacer(Modifier.height(16.dp))
 
            BricklyTextField(
                value = price,
                onValueChange = { price = it.filter { c -> c.isDigit() } },
                label = "Цена (₽) *",
                placeholder = "1000",
                keyboardType = KeyboardType.Number
            )
            Spacer(Modifier.height(24.dp))

            if (errorMessage != null) {
                Text(errorMessage!!, color = ErrorColor, style = MaterialTheme.typography.bodySmall)
                Spacer(Modifier.height(8.dp))
            }
            if (successMessage != null) {
                Text(
                    successMessage!!,
                    color = AccentDark,
                    style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.SemiBold)
                )
                Spacer(Modifier.height(8.dp))
            }

            val isFormValid = itemId.isNotBlank() &&
                    quantity.isNotBlank() &&
                    conditionRate.isNotBlank() &&
                    price.isNotBlank() &&
                    selectedImageUris.isNotEmpty()

            Button(
                onClick = {
                    scope.launch {
                        isSaving = true
                        errorMessage = null
                        successMessage = null
                        try {
                            val sellerId = UserPreferences.getUserId(context)
                            
                            val parts = mutableListOf<MultipartBody.Part>()
                            
                            selectedImageUris.forEachIndexed { index, uri ->
                                val imageFile = uriToFile(context, uri)
                                val imagePart = MultipartBody.Part.createFormData(
                                    "images",
                                    imageFile.name,
                                    imageFile.asRequestBody("image/*".toMediaTypeOrNull())
                                )
                                parts.add(imagePart)
                            }

                            val response = RetrofitClient.api.createListing(
                                sellerId = sellerId.toString().toRequestBody("text/plain".toMediaTypeOrNull()),
                                itemType = selectedItemType.toRequestBody("text/plain".toMediaTypeOrNull()),
                                itemId = itemId.toRequestBody("text/plain".toMediaTypeOrNull()),
                                quantity = quantity.toRequestBody("text/plain".toMediaTypeOrNull()),
                                description = description.ifBlank { null }?.toRequestBody("text/plain".toMediaTypeOrNull()),
                                condition = selectedCondition.toRequestBody("text/plain".toMediaTypeOrNull()),
                                conditionRate = conditionRate.toRequestBody("text/plain".toMediaTypeOrNull()),
                                price = price.toRequestBody("text/plain".toMediaTypeOrNull()),
                                status = "sell".toRequestBody("text/plain".toMediaTypeOrNull()),
                                images = parts
                            )

                            if (response.isSuccessful) {
                                successMessage = "Карточка успешно создана!"
                                itemId = ""
                                quantity = ""
                                description = ""
                                price = ""
                                conditionRate = ""
                                selectedItemType = "P"
                                selectedCondition = "NEW"
                                selectedImageUris = emptyList()
                            } else {
                                errorMessage = "Ошибка создания (${response.code()}): ${response.message()}"
                            }
                        } catch (e: Exception) {
                            errorMessage = "Ошибка: ${e.localizedMessage ?: "Неизвестная ошибка"}"
                        }
                        isSaving = false
                    }
                },
                enabled = !isSaving && isFormValid,
                modifier = Modifier.fillMaxWidth().height(56.dp),
                shape = RoundedCornerShape(14.dp),
                colors = ButtonDefaults.buttonColors(containerColor = Accent, contentColor = TextPrimary)
            ) {
                if (isSaving) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(20.dp),
                        color = TextPrimary,
                        strokeWidth = 2.dp
                    )
                } else {
                    Text("Создать карточку", fontWeight = FontWeight.SemiBold, fontSize = 16.sp, color = TextPrimary)
                }
            }

            Spacer(Modifier.height(32.dp))
        }
    }
}

@Composable
fun ItemTypeButton(
    label: String,
    code: String,
    isSelected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .height(56.dp)
            .clip(RoundedCornerShape(12.dp))
            .background(if (isSelected) Accent else CardBackground)
            .border(
                width = 1.dp,
                color = if (isSelected) AccentDark else Divider,
                shape = RoundedCornerShape(12.dp)
            )
            .clickable(onClick = onClick),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = label,
            color = if (isSelected) Color(0xFF1A1A1A) else TextPrimary,
            fontSize = 14.sp,
            fontWeight = if (isSelected) FontWeight.SemiBold else FontWeight.Normal
        )
    }
}

@Composable
fun ConditionButton(
    label: String,
    code: String,
    isSelected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .height(56.dp)
            .clip(RoundedCornerShape(12.dp))
            .background(if (isSelected) Accent else CardBackground)
            .border(
                width = 1.dp,
                color = if (isSelected) AccentDark else Divider,
                shape = RoundedCornerShape(12.dp)
            )
            .clickable(onClick = onClick),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = label,
            color = if (isSelected) Color(0xFF1A1A1A) else TextPrimary,
            fontSize = 14.sp,
            fontWeight = if (isSelected) FontWeight.SemiBold else FontWeight.Normal
        )
    }
}

private fun uriToFile(context: Context, uri: Uri): File {
    val inputStream = context.contentResolver.openInputStream(uri)
        ?: throw IllegalArgumentException("Cannot open input stream for URI: $uri")
    val file = File(context.cacheDir, "temp_upload_${System.currentTimeMillis()}.jpg")
    FileOutputStream(file).use { output ->
        inputStream.copyTo(output)
    }
    inputStream.close()
    return file
}
