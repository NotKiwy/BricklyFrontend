package com.example.bricklyfrontend.screens

import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.ArrowBackIosNew
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.bricklyfrontend.data.MeetingDefaultDTO
import com.example.bricklyfrontend.data.MeetingTypeDefaultDTO
import com.example.bricklyfrontend.data.MeetingUpdateDTO
import com.example.bricklyfrontend.data.RetrofitClient
import com.example.bricklyfrontend.ui.theme.*
import kotlinx.coroutines.launch
import java.time.OffsetDateTime
import java.time.ZoneId
import java.time.ZonedDateTime
import java.time.format.DateTimeFormatter
import java.util.Calendar

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EditMeetingScreen(
    meetingId: Long,
    onBack: () -> Unit,
    onSaved: () -> Unit = {}
) {
    SetStatusBarColor(Accent)

    val context = LocalContext.current
    val scope = rememberCoroutineScope()

    var meeting by remember { mutableStateOf<MeetingDefaultDTO?>(null) }
    var isLoadingMeeting by remember { mutableStateOf(true) }
    var meetingTypes by remember { mutableStateOf<List<MeetingTypeDefaultDTO>>(emptyList()) }

    var title by remember { mutableStateOf("") }
    var address by remember { mutableStateOf("") }
    var description by remember { mutableStateOf("") }
    var selectedDateMillis by remember { mutableStateOf<Long?>(null) }
    var selectedHour by remember { mutableStateOf<Int?>(null) }
    var selectedMinute by remember { mutableStateOf<Int?>(null) }
    var duration by remember { mutableStateOf("") }
    var selectedType by remember { mutableStateOf<MeetingTypeDefaultDTO?>(null) }
    var isPaidEntry by remember { mutableStateOf(false) }
    var ticketPrice by remember { mutableStateOf("") }
    var hasDiscount by remember { mutableStateOf(false) }
    var discountDuration by remember { mutableStateOf("") }
    var discountAmount by remember { mutableStateOf("") }
    var discountFromAnnounce by remember { mutableStateOf(true) }

    var isSaving by remember { mutableStateOf(false) }
    var errorMessage by remember { mutableStateOf<String?>(null) }

    LaunchedEffect(meetingId) {
        try {
            val typesResp = RetrofitClient.api.getMeetingTypes()
            if (typesResp.isSuccessful) meetingTypes = typesResp.body() ?: emptyList()

            val resp = RetrofitClient.api.getMeetingById(meetingId)
            if (resp.isSuccessful) {
                val m = resp.body()
                if (m != null) {
                    meeting = m
                    title = m.title ?: ""
                    address = m.address ?: ""
                    description = m.description ?: ""
                    duration = ((m.duration ?: 0) / 60).toString().takeIf { it != "0" } ?: ""
                    isPaidEntry = (m.ticketPrice ?: 0) > 0
                    ticketPrice = if (isPaidEntry) (m.ticketPrice ?: 0).toString() else ""
                    hasDiscount = (m.discountAmount ?: 0) > 0
                    discountDuration = if (hasDiscount) (m.discountDuration ?: 0).toString() else ""
                    discountAmount = if (hasDiscount) (m.discountAmount ?: 0).toString() else ""
                    discountFromAnnounce = (m.discountModifier ?: 1) >= 0

                    m.date?.let { dateStr ->
                        try {
                            val odt = OffsetDateTime.parse(dateStr)
                            val cal = Calendar.getInstance()
                            cal.timeInMillis = odt.toInstant().toEpochMilli()
                            selectedDateMillis = cal.timeInMillis
                            selectedHour = odt.hour
                            selectedMinute = odt.minute
                        } catch (_: Exception) {}
                    }

                    selectedType = meetingTypes.find { it.id == m.type?.id }
                }
            }
        } catch (_: Exception) {}
        isLoadingMeeting = false
    }

    LaunchedEffect(meetingTypes, meeting) {
        if (selectedType == null && meeting != null) {
            selectedType = meetingTypes.find { it.id == meeting?.type?.id }
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
                Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                    IconButton(onClick = onBack) {
                        Icon(Icons.Outlined.ArrowBackIosNew, "Назад", tint = TextPrimary)
                    }
                    Spacer(Modifier.width(4.dp))
                    Text("Редактировать мероприятие", color = TextPrimary, fontWeight = FontWeight.ExtraBold, fontSize = 18.sp)
                }
            }
        }
    ) { padding ->
        if (isLoadingMeeting) {
            Box(Modifier.fillMaxSize().padding(padding), contentAlignment = Alignment.Center) {
                CircularProgressIndicator(color = Accent)
            }
            return@Scaffold
        }

        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 20.dp)
        ) {
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
                placeholder = "ул. Примерная, 1"
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
                        if (selectedDateMillis != null) calendar.timeInMillis = selectedDateMillis!!
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
                        ).show()
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
                    text = if (selectedHour != null && selectedMinute != null)
                        "%02d:%02d".format(selectedHour, selectedMinute)
                    else "Выберите время",
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
                Text("Платный вход", color = TextPrimary, fontSize = 15.sp)
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
                    Text("Добавить скидку", color = TextPrimary, fontSize = 15.sp)
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
                    Row(horizontalArrangement = Arrangement.spacedBy(12.dp), modifier = Modifier.fillMaxWidth()) {
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
                    (!isPaidEntry || ticketPrice.isNotBlank()) &&
                    (!hasDiscount || (discountDuration.isNotBlank() && discountAmount.isNotBlank()))

            Button(
                onClick = {
                    scope.launch {
                        isSaving = true
                        errorMessage = null
                        try {
                            val calendar = Calendar.getInstance()
                            calendar.timeInMillis = selectedDateMillis!!
                            calendar.set(Calendar.HOUR_OF_DAY, selectedHour!!)
                            calendar.set(Calendar.MINUTE, selectedMinute!!)
                            calendar.set(Calendar.SECOND, 0)
                            calendar.set(Calendar.MILLISECOND, 0)

                            val dateIso = ZonedDateTime.ofInstant(
                                java.time.Instant.ofEpochMilli(calendar.timeInMillis),
                                ZoneId.systemDefault()
                            ).format(DateTimeFormatter.ISO_OFFSET_DATE_TIME)

                            val durationMinutes = (duration.toIntOrNull() ?: 0) * 60
                            val price = if (isPaidEntry) ticketPrice.toIntOrNull() ?: 0 else 0

                            val dto = MeetingUpdateDTO(
                                date = dateIso,
                                address = address,
                                title = title,
                                duration = durationMinutes,
                                typeId = selectedType!!.id,
                                ticketPrice = price,
                                description = description.ifBlank { null },
                                discountDuration = if (hasDiscount) discountDuration.toIntOrNull() ?: 0 else 0,
                                discountAmount = if (hasDiscount) discountAmount.toIntOrNull() ?: 0 else 0,
                                discountModifier = if (hasDiscount) (if (discountFromAnnounce) 1 else -1) else 0
                            )

                            val resp = RetrofitClient.api.updateMeeting(meetingId, dto)
                            if (resp.isSuccessful) {
                                onSaved()
                            } else {
                                errorMessage = "Ошибка сохранения (${resp.code()})"
                            }
                        } catch (e: Exception) {
                            errorMessage = "Ошибка: ${e.localizedMessage}"
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
                    CircularProgressIndicator(modifier = Modifier.size(20.dp), color = TextPrimary, strokeWidth = 2.dp)
                } else {
                    Text("Сохранить изменения", fontWeight = FontWeight.SemiBold, fontSize = 16.sp)
                }
            }

            Spacer(Modifier.height(32.dp))
        }
    }
}
