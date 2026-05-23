package com.example.bricklyfrontend.screens

import android.content.Context
import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.AddPhotoAlternate
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
import com.example.bricklyfrontend.data.MeetingTypeDefaultDTO
import com.example.bricklyfrontend.data.RetrofitClient
import com.example.bricklyfrontend.ui.theme.*
import kotlinx.coroutines.launch
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.MultipartBody
import okhttp3.RequestBody.Companion.asRequestBody
import okhttp3.RequestBody.Companion.toRequestBody
import java.io.File
import java.io.FileOutputStream
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CreateMeetingScreen(
    onBack: () -> Unit
) {
    SetStatusBarColor(Color.White)
    
    val context = LocalContext.current
    val scope = rememberCoroutineScope()

    // === STATE ===
    var title by remember { mutableStateOf("") }
    var date by remember { mutableStateOf("") }
    var duration by remember { mutableStateOf("") }
    var address by remember { mutableStateOf("") }
    var description by remember { mutableStateOf("") }
    var selectedImageUri by remember { mutableStateOf<Uri?>(null) }

    var meetingTypes by remember { mutableStateOf<List<MeetingTypeDefaultDTO>>(emptyList()) }
    var selectedType by remember { mutableStateOf<MeetingTypeDefaultDTO?>(null) }

    var isPaidEntry by remember { mutableStateOf(false) }
    var ticketPrice by remember { mutableStateOf("") }

    var hasDiscount by remember { mutableStateOf(false) }
    var discountDuration by remember { mutableStateOf("") }
    var discountAmount by remember { mutableStateOf("") }
    var discountFromAnnounce by remember { mutableStateOf(true) } // true = от анонса (modifier=1), false = от начала (modifier=-1)

    var isSaving by remember { mutableStateOf(false) }
    var errorMessage by remember { mutableStateOf<String?>(null) }
    var successMessage by remember { mutableStateOf<String?>(null) }

    // === IMAGE PICKER ===
    val imagePicker = rememberLauncherForActivityResult(ActivityResultContracts.GetContent()) { uri ->
        uri?.let { selectedImageUri = it }
    }

    // === LOAD MEETING TYPES ===
    LaunchedEffect(Unit) {
        try {
            val resp = RetrofitClient.api.getMeetingTypes()
            if (resp.isSuccessful) {
                meetingTypes = resp.body() ?: emptyList()
            }
        } catch (_: Exception) {}
    }

    Scaffold(
        containerColor = Background,
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = "Создать митинг",
                        color = TextPrimary,
                        fontWeight = FontWeight.ExtraBold,
                        fontSize = 20.sp
                    )
                },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(
                            Icons.Outlined.ArrowBackIosNew,
                            contentDescription = "Назад",
                            tint = TextPrimary
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = Background),
                modifier = Modifier.statusBarsPadding()
            )
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

            // === ИЗОБРАЖЕНИЕ (ОБЯЗАТЕЛЬНОЕ) ===
            Text("Изображение *", style = MaterialTheme.typography.labelLarge, color = TextPrimary)
            Spacer(Modifier.height(8.dp))
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(200.dp)
                    .clip(RoundedCornerShape(16.dp))
                    .background(CardBackground)
                    .border(
                        1.5.dp,
                        if (selectedImageUri != null) Accent else Divider,
                        RoundedCornerShape(16.dp)
                    )
                    .clickable { imagePicker.launch("image/*") },
                contentAlignment = Alignment.Center
            ) {
                if (selectedImageUri != null) {
                    AsyncImage(
                        model = selectedImageUri,
                        contentDescription = null,
                        modifier = Modifier.fillMaxSize().clip(RoundedCornerShape(16.dp)),
                        contentScale = ContentScale.Crop
                    )
                } else {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Icon(Icons.Outlined.AddPhotoAlternate, null, tint = IconInactive, modifier = Modifier.size(48.dp))
                        Spacer(Modifier.height(8.dp))
                        Text("Выбрать изображение", color = TextSecondary, fontSize = 14.sp)
                    }
                }
            }
            Spacer(Modifier.height(20.dp))

            // === ПОЛЯ ===
            BricklyTextField(
                value = title,
                onValueChange = { title = it },
                label = "Название",
                placeholder = "Встреча любителей LEGO"
            )
            Spacer(Modifier.height(16.dp))

            BricklyTextField(
                value = address,
                onValueChange = { address = it },
                label = "Адрес",
                placeholder = "ул. Примерная, 1"
            )
            Spacer(Modifier.height(16.dp))

            BricklyTextField(
                value = date,
                onValueChange = { date = it },
                label = "Дата начала (ISO)",
                placeholder = "2026-06-15T18:00:00"
            )
            Spacer(Modifier.height(16.dp))

            BricklyTextField(
                value = duration,
                onValueChange = { duration = it.filter { c -> c.isDigit() } },
                label = "Длительность (минуты)",
                placeholder = "120",
                keyboardType = KeyboardType.Number
            )
            Spacer(Modifier.height(16.dp))

            BricklyTextField(
                value = description,
                onValueChange = { description = it },
                label = "Описание (необязательно)",
                placeholder = "Детали мероприятия"
            )
            Spacer(Modifier.height(20.dp))

            // === ТИП МЕРОПРИЯТИЯ (ГОРИЗОНТАЛЬНЫЙ СКРОЛЛ) ===
            Text("Тип мероприятия *", style = MaterialTheme.typography.labelLarge, color = TextPrimary)
            Spacer(Modifier.height(8.dp))

            if (meetingTypes.isNotEmpty()) {
                LazyRow(
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    items(meetingTypes) { type ->
                        MeetingTypeCard(
                            type = type,
                            isSelected = selectedType?.id == type.id,
                            onClick = { selectedType = type }
                        )
                    }
                }
            } else {
                Text("Загрузка типов...", color = TextSecondary, fontSize = 14.sp)
            }

            Spacer(Modifier.height(20.dp))

            // === ПЛАТНЫЙ ВХОД ===
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(12.dp))
                    .background(CardBackground)
                    .clickable { isPaidEntry = !isPaidEntry }
                    .padding(horizontal = 16.dp, vertical = 12.dp)
            ) {
                Checkbox(
                    checked = isPaidEntry,
                    onCheckedChange = { isPaidEntry = it },
                    colors = CheckboxDefaults.colors(checkedColor = Accent)
                )
                Spacer(Modifier.width(8.dp))
                Text("Платный вход", color = TextPrimary, fontSize = 16.sp, fontWeight = FontWeight.Medium)
            }

            if (isPaidEntry) {
                Spacer(Modifier.height(12.dp))
                BricklyTextField(
                    value = ticketPrice,
                    onValueChange = { ticketPrice = it.filter { c -> c.isDigit() } },
                    label = "Цена билета (₽)",
                    placeholder = "500",
                    keyboardType = KeyboardType.Number
                )

                Spacer(Modifier.height(16.dp))

                // === ДОБАВИТЬ СКИДКУ ===
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(12.dp))
                        .background(CardBackground)
                        .clickable { hasDiscount = !hasDiscount }
                        .padding(horizontal = 16.dp, vertical = 12.dp)
                ) {
                    Checkbox(
                        checked = hasDiscount,
                        onCheckedChange = { hasDiscount = it },
                        colors = CheckboxDefaults.colors(checkedColor = Accent)
                    )
                    Spacer(Modifier.width(8.dp))
                    Text("Добавить скидку", color = TextPrimary, fontSize = 16.sp, fontWeight = FontWeight.Medium)
                }

                if (hasDiscount) {
                    Spacer(Modifier.height(12.dp))

                    BricklyTextField(
                        value = discountDuration,
                        onValueChange = { discountDuration = it.filter { c -> c.isDigit() } },
                        label = "Длительность скидки (дни)",
                        placeholder = "7",
                        keyboardType = KeyboardType.Number
                    )
                    Spacer(Modifier.height(12.dp))

                    BricklyTextField(
                        value = discountAmount,
                        onValueChange = { discountAmount = it.filter { c -> c.isDigit() } },
                        label = "Размер скидки (₽)",
                        placeholder = "100",
                        keyboardType = KeyboardType.Number
                    )
                    Spacer(Modifier.height(12.dp))

                    // === СЕЛЕКТОР ОТ КАКОЙ ДАТЫ ОТСЧИТЫВАТЬ ===
                    Text("Отсчитывать скидку от:", style = MaterialTheme.typography.labelMedium, color = TextPrimary)
                    Spacer(Modifier.height(6.dp))

                    Row(
                        horizontalArrangement = Arrangement.spacedBy(12.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        DiscountModifierButton(
                            label = "От анонса",
                            isSelected = discountFromAnnounce,
                            onClick = { discountFromAnnounce = true },
                            modifier = Modifier.weight(1f)
                        )
                        DiscountModifierButton(
                            label = "От начала",
                            isSelected = !discountFromAnnounce,
                            onClick = { discountFromAnnounce = false },
                            modifier = Modifier.weight(1f)
                        )
                    }
                }
            }

            Spacer(Modifier.height(24.dp))

            // === ОШИБКА / УСПЕХ ===
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

            // === КНОПКА СОЗДАТЬ ===
            val isFormValid = selectedImageUri != null &&
                    title.isNotBlank() &&
                    address.isNotBlank() &&
                    date.isNotBlank() &&
                    duration.isNotBlank() &&
                    selectedType != null &&
                    (!isPaidEntry || ticketPrice.isNotBlank()) &&
                    (!hasDiscount || (discountDuration.isNotBlank() && discountAmount.isNotBlank()))

            Button(
                onClick = {
                    scope.launch {
                        isSaving = true
                        errorMessage = null
                        successMessage = null
                        try {
                            val imageFile = uriToFile(context, selectedImageUri!!)
                            val imagePart = MultipartBody.Part.createFormData(
                                "previewImage",
                                imageFile.name,
                                imageFile.asRequestBody("image/*".toMediaTypeOrNull())
                            )

                            val announceDate = LocalDateTime.now().format(DateTimeFormatter.ISO_LOCAL_DATE_TIME)
                            val priceValue = if (isPaidEntry) ticketPrice.toIntOrNull() ?: 0 else 0

                            val response = RetrofitClient.api.createMeeting(
                                date = date.toRequestBody("text/plain".toMediaTypeOrNull()),
                                title = title.toRequestBody("text/plain".toMediaTypeOrNull()),
                                announceDate = announceDate.toRequestBody("text/plain".toMediaTypeOrNull()),
                                duration = duration.toRequestBody("text/plain".toMediaTypeOrNull()),
                                address = address.toRequestBody("text/plain".toMediaTypeOrNull()),
                                typeId = selectedType!!.id.toString().toRequestBody("text/plain".toMediaTypeOrNull()),
                                ticketPrice = priceValue.toString().toRequestBody("text/plain".toMediaTypeOrNull()),
                                description = description.ifBlank { null }?.toRequestBody("text/plain".toMediaTypeOrNull()),
                                discountDuration = if (hasDiscount && discountDuration.isNotBlank())
                                    discountDuration.toRequestBody("text/plain".toMediaTypeOrNull()) else null,
                                discountAmount = if (hasDiscount && discountAmount.isNotBlank())
                                    discountAmount.toRequestBody("text/plain".toMediaTypeOrNull()) else null,
                                discountModifier = if (hasDiscount)
                                    (if (discountFromAnnounce) "1" else "-1").toRequestBody("text/plain".toMediaTypeOrNull())
                                else null,
                                previewImage = imagePart
                            )

                            if (response.isSuccessful) {
                                successMessage = "Митинг успешно создан!"
                                // Сброс формы
                                title = ""
                                address = ""
                                date = ""
                                duration = ""
                                description = ""
                                selectedImageUri = null
                                selectedType = null
                                isPaidEntry = false
                                ticketPrice = ""
                                hasDiscount = false
                                discountDuration = ""
                                discountAmount = ""
                                discountFromAnnounce = true
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
                colors = ButtonDefaults.buttonColors(containerColor = Accent, contentColor = Color.White)
            ) {
                if (isSaving) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(20.dp),
                        color = Color.White,
                        strokeWidth = 2.dp
                    )
                } else {
                    Text("Создать митинг", fontWeight = FontWeight.SemiBold, fontSize = 16.sp)
                }
            }

            Spacer(Modifier.height(32.dp))
        }
    }
}

@Composable
fun MeetingTypeCard(
    type: MeetingTypeDefaultDTO,
    isSelected: Boolean,
    onClick: () -> Unit
) {
    Box(
        modifier = Modifier
            .width(160.dp)
            .height(100.dp)
            .clip(RoundedCornerShape(12.dp))
            .background(if (isSelected) Accent else CardBackground)
            .border(
                width = if (isSelected) 2.dp else 1.dp,
                color = if (isSelected) AccentDark else Divider,
                shape = RoundedCornerShape(12.dp)
            )
            .clickable(onClick = onClick)
            .padding(12.dp),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = type.description ?: "Тип #${type.id}",
            color = if (isSelected) Color.White else TextPrimary,
            fontSize = 14.sp,
            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
            textAlign = androidx.compose.ui.text.style.TextAlign.Center
        )
    }
}

@Composable
fun DiscountModifierButton(
    label: String,
    isSelected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .height(48.dp)
            .clip(RoundedCornerShape(10.dp))
            .background(if (isSelected) Accent else CardBackground)
            .border(
                width = if (isSelected) 2.dp else 1.dp,
                color = if (isSelected) AccentDark else Divider,
                shape = RoundedCornerShape(10.dp)
            )
            .clickable(onClick = onClick),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = label,
            color = if (isSelected) Color.White else TextPrimary,
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
