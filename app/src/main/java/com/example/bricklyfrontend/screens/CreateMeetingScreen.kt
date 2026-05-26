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
import java.util.Calendar

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CreateMeetingScreen(
    onBack: () -> Unit,
    onMeetingCreated: () -> Unit = {}
) {
    SetStatusBarColor(Accent)
    
    val context = LocalContext.current
    val scope = rememberCoroutineScope()

    var title by remember { mutableStateOf("") }
    var selectedDateMillis by remember { mutableStateOf<Long?>(null) }
    var selectedHour by remember { mutableStateOf<Int?>(null) }
    var selectedMinute by remember { mutableStateOf<Int?>(null) }
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
    var discountFromAnnounce by remember { mutableStateOf(true) }

    var isSaving by remember { mutableStateOf(false) }
    var errorMessage by remember { mutableStateOf<String?>(null) }

    val imagePicker = rememberLauncherForActivityResult(ActivityResultContracts.GetContent()) { uri ->
        uri?.let { selectedImageUri = it }
    }

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
                        text = "Создать мероприятие",
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

            Text("Изображение *", style = MaterialTheme.typography.labelLarge, color = TextPrimary)
            Spacer(Modifier.height(8.dp))
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(180.dp)
                    .clip(RoundedCornerShape(12.dp))
                    .background(CardBackground)
                    .border(
                        1.dp,
                        Divider,
                        RoundedCornerShape(12.dp)
                    )
                    .clickable { imagePicker.launch("image/*") },
                contentAlignment = Alignment.Center
            ) {
                if (selectedImageUri != null) {
                    AsyncImage(
                        model = selectedImageUri,
                        contentDescription = null,
                        modifier = Modifier.fillMaxSize().clip(RoundedCornerShape(12.dp)),
                        contentScale = ContentScale.Crop
                    )
                } else {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Icon(Icons.Outlined.AddPhotoAlternate, null, tint = IconInactive, modifier = Modifier.size(40.dp))
                        Spacer(Modifier.height(8.dp))
                        Text("Нажмите для выбора фото", color = TextSecondary, fontSize = 13.sp)
                    }
                }
            }
            Spacer(Modifier.height(16.dp))

            BricklyTextField(
                value = title,
                onValueChange = { title = it },
                label = "Название *",
                placeholder = "Встреча любителей LEGO"
            )
            Spacer(Modifier.height(16.dp))

            BricklyTextField(
                value = address,
                onValueChange = { address = it },
                label = "Адрес *",
                placeholder = "примерная улица 1"
            )
            Spacer(Modifier.height(16.dp))

            Text("Дата *", style = MaterialTheme.typography.labelLarge, color = TextPrimary)
            Spacer(Modifier.height(8.dp))
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(56.dp)
                    .clip(RoundedCornerShape(12.dp))
                    .background(CardBackground)
                    .border(1.dp, Divider, RoundedCornerShape(12.dp))
                    .clickable {
                        val calendar = Calendar.getInstance()
                        if (selectedDateMillis != null) {
                            calendar.timeInMillis = selectedDateMillis!!
                        }
                        android.app.DatePickerDialog(
                            context,
                            { _, year, month, day ->
                                val cal = Calendar.getInstance()
                                cal.set(year, month, day)
                                selectedDateMillis = cal.timeInMillis
                            },
                            calendar.get(Calendar.YEAR),
                            calendar.get(Calendar.MONTH),
                            calendar.get(Calendar.DAY_OF_MONTH)
                        ).apply {
                            datePicker.minDate = System.currentTimeMillis() - 1000
                        }.show()
                    }
                    .padding(horizontal = 16.dp),
                contentAlignment = Alignment.CenterStart
            ) {
                Text(
                    text = if (selectedDateMillis != null) {
                        val cal = Calendar.getInstance()
                        cal.timeInMillis = selectedDateMillis!!
                        "%02d.%02d.%04d".format(
                            cal.get(Calendar.DAY_OF_MONTH),
                            cal.get(Calendar.MONTH) + 1,
                            cal.get(Calendar.YEAR)
                        )
                    } else "Выберите дату",
                    color = if (selectedDateMillis != null) TextPrimary else TextSecondary,
                    fontSize = 16.sp
                )
            }
            Spacer(Modifier.height(16.dp))

            Text("Время *", style = MaterialTheme.typography.labelLarge, color = TextPrimary)
            Spacer(Modifier.height(8.dp))
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(56.dp)
                    .clip(RoundedCornerShape(12.dp))
                    .background(CardBackground)
                    .border(1.dp, Divider, RoundedCornerShape(12.dp))
                    .clickable {
                        val calendar = Calendar.getInstance()
                        android.app.TimePickerDialog(
                            context,
                            { _, hour, minute ->
                                selectedHour = hour
                                selectedMinute = minute
                            },
                            selectedHour ?: calendar.get(Calendar.HOUR_OF_DAY),
                            selectedMinute ?: calendar.get(Calendar.MINUTE),
                            true
                        ).show()
                    }
                    .padding(horizontal = 16.dp),
                contentAlignment = Alignment.CenterStart
            ) {
                Text(
                    text = if (selectedHour != null && selectedMinute != null) {
                        "%02d:%02d".format(selectedHour, selectedMinute)
                    } else "Выберите время",
                    color = if (selectedHour != null) TextPrimary else TextSecondary,
                    fontSize = 16.sp
                )
            }
            Spacer(Modifier.height(16.dp))

            BricklyTextField(
                value = duration,
                onValueChange = { duration = it.filter { c -> c.isDigit() } },
                label = "Длительность (часы) *",
                placeholder = "2",
                keyboardType = KeyboardType.Number
            )
            Spacer(Modifier.height(16.dp))

            BricklyTextField(
                value = description,
                onValueChange = { description = it },
                label = "Описание",
                placeholder = "Детали мероприятия"
            )
            Spacer(Modifier.height(16.dp))

            Text("Тип мероприятия *", style = MaterialTheme.typography.labelLarge, color = TextPrimary)
            Spacer(Modifier.height(8.dp))

            if (meetingTypes.isNotEmpty()) {
                LazyRow(
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
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
                Text("Загрузка...", color = TextSecondary, fontSize = 14.sp)
            }

            Spacer(Modifier.height(16.dp))

            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(10.dp))
                    .background(CardBackground)
                    .border(1.dp, Divider, RoundedCornerShape(10.dp))
                    .clickable { isPaidEntry = !isPaidEntry }
                    .padding(horizontal = 14.dp, vertical = 10.dp)
            ) {
                Checkbox(
                    checked = isPaidEntry,
                    onCheckedChange = { isPaidEntry = it },
                    colors = CheckboxDefaults.colors(checkedColor = Accent)
                )
                Spacer(Modifier.width(8.dp))
                Text("Платный вход", color = TextPrimary, fontSize = 15.sp, fontWeight = FontWeight.Normal)
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

                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(10.dp))
                        .background(CardBackground)
                        .border(1.dp, Divider, RoundedCornerShape(10.dp))
                        .clickable { hasDiscount = !hasDiscount }
                        .padding(horizontal = 14.dp, vertical = 10.dp)
                ) {
                    Checkbox(
                        checked = hasDiscount,
                        onCheckedChange = { hasDiscount = it },
                        colors = CheckboxDefaults.colors(checkedColor = Accent)
                    )
                    Spacer(Modifier.width(8.dp))
                    Text("Добавить скидку", color = TextPrimary, fontSize = 15.sp, fontWeight = FontWeight.Normal)
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

            if (errorMessage != null) {
                Text(errorMessage!!, color = ErrorColor, style = MaterialTheme.typography.bodySmall)
                Spacer(Modifier.height(8.dp))
            }

            val isFormValid = title.isNotBlank() &&
                    address.isNotBlank() &&
                    selectedDateMillis != null &&
                    selectedHour != null &&
                    selectedMinute != null &&
                    duration.isNotBlank() &&
                    selectedType != null &&
                    selectedImageUri != null &&
                    (!isPaidEntry || ticketPrice.isNotBlank()) &&
                    (!hasDiscount || (discountDuration.isNotBlank() && discountAmount.isNotBlank()))

            Button(
                onClick = {
                    scope.launch {
                        isSaving = true
                        errorMessage = null
                        try {
                            val imageFile = uriToFile(context, selectedImageUri!!)
                            val imagePart = MultipartBody.Part.createFormData(
                                "previewImage",
                                imageFile.name,
                                imageFile.asRequestBody("image/*".toMediaTypeOrNull())
                            )

                            val calendar = Calendar.getInstance()
                            calendar.timeInMillis = selectedDateMillis!!
                            calendar.set(Calendar.HOUR_OF_DAY, selectedHour!!)
                            calendar.set(Calendar.MINUTE, selectedMinute!!)
                            calendar.set(Calendar.SECOND, 0)
                            calendar.set(Calendar.MILLISECOND, 0)
                            
                            val dateIso = java.time.ZonedDateTime.ofInstant(
                                java.time.Instant.ofEpochMilli(calendar.timeInMillis),
                                java.time.ZoneId.systemDefault()
                            ).format(java.time.format.DateTimeFormatter.ISO_OFFSET_DATE_TIME)
                            
                            val announceCalendar = Calendar.getInstance()
                            val announceDate = java.time.ZonedDateTime.ofInstant(
                                java.time.Instant.ofEpochMilli(announceCalendar.timeInMillis),
                                java.time.ZoneId.systemDefault()
                            ).format(java.time.format.DateTimeFormatter.ISO_OFFSET_DATE_TIME)
                            
                            val priceValue = if (isPaidEntry) ticketPrice.toIntOrNull() ?: 0 else 0
                            
                            val durationInMinutes = (duration.toIntOrNull() ?: 0) * 60

                            val response = RetrofitClient.api.createMeeting(
                                date = dateIso.toRequestBody("text/plain".toMediaTypeOrNull()),
                                title = title.toRequestBody("text/plain".toMediaTypeOrNull()),
                                announceDate = announceDate.toRequestBody("text/plain".toMediaTypeOrNull()),
                                duration = durationInMinutes.toString().toRequestBody("text/plain".toMediaTypeOrNull()),
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
                                onMeetingCreated()
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
                    Text("Создать мероприятие", fontWeight = FontWeight.SemiBold, fontSize = 16.sp, color = TextPrimary)
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
            .width(140.dp)
            .height(90.dp)
            .clip(RoundedCornerShape(10.dp))
            .background(if (isSelected) Accent else CardBackground)
            .border(
                width = 1.dp,
                color = if (isSelected) AccentDark else Divider,
                shape = RoundedCornerShape(10.dp)
            )
            .clickable(onClick = onClick)
            .padding(10.dp),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = type.description ?: "Тип #${type.id}",
            color = if (isSelected) Color(0xFF1A1A1A) else TextPrimary,
            fontSize = 13.sp,
            fontWeight = if (isSelected) FontWeight.SemiBold else FontWeight.Normal,
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
            .clip(RoundedCornerShape(8.dp))
            .background(if (isSelected) Accent else CardBackground)
            .border(
                width = 1.dp,
                color = if (isSelected) AccentDark else Divider,
                shape = RoundedCornerShape(8.dp)
            )
            .clickable(onClick = onClick),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = label,
            color = if (isSelected) Color(0xFF1A1A1A) else TextPrimary,
            fontSize = 14.sp,
            fontWeight = if (isSelected) FontWeight.Medium else FontWeight.Normal
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
