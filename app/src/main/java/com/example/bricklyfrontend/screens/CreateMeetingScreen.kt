package com.example.bricklyfrontend.screens

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
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
import com.example.bricklyfrontend.data.MeetingCreateDTO
import com.example.bricklyfrontend.data.MeetingTypeDefaultDTO
import com.example.bricklyfrontend.data.RetrofitClient
import com.example.bricklyfrontend.ui.theme.*
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CreateMeetingScreen(
    onBack: () -> Unit
) {
    SetStatusBarColor(Color.White)
    
    val context = LocalContext.current
    val scope = rememberCoroutineScope()

    var date by remember { mutableStateOf("") }
    var address by remember { mutableStateOf("") }
    var description by remember { mutableStateOf("") }
    var ticketPrice by remember { mutableStateOf("") }
    var discountDuration by remember { mutableStateOf("") }
    var discountAmount by remember { mutableStateOf("") }
    var discountModifier by remember { mutableStateOf("") }
    var coverImageUri by remember { mutableStateOf<Uri?>(null) }

    var meetingTypes by remember { mutableStateOf<List<MeetingTypeDefaultDTO>>(emptyList()) }
    var selectedTypeId by remember { mutableIntStateOf(-1) }
    var typeExpanded by remember { mutableStateOf(false) }

    var isSaving by remember { mutableStateOf(false) }
    var errorMessage by remember { mutableStateOf<String?>(null) }
    var successMessage by remember { mutableStateOf<String?>(null) }

    val imagePicker = rememberLauncherForActivityResult(ActivityResultContracts.GetContent()) { uri ->
        uri?.let { coverImageUri = it }
    }

    LaunchedEffect(Unit) {
        try {
            val resp = RetrofitClient.api.getMeetingTypes()
            if (resp.isSuccessful) {
                meetingTypes = resp.body() ?: emptyList()
                if (meetingTypes.isNotEmpty()) selectedTypeId = meetingTypes.first().id
            }
        } catch (_: Exception) {}
    }

    Scaffold(
        containerColor = Background,
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = "Создать сходку",
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
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = Background
                ),
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

            // Обложка
            Text("Обложка", style = MaterialTheme.typography.labelLarge, color = TextPrimary)
            Spacer(Modifier.height(8.dp))
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(180.dp)
                    .clip(RoundedCornerShape(16.dp))
                    .background(CardBackground)
                    .border(1.5.dp, if (coverImageUri != null) Accent else Divider, RoundedCornerShape(16.dp))
                    .clickable { imagePicker.launch("image/*") },
                contentAlignment = Alignment.Center
            ) {
                if (coverImageUri != null) {
                    AsyncImage(
                        model = coverImageUri,
                        contentDescription = null,
                        modifier = Modifier.fillMaxSize().clip(RoundedCornerShape(16.dp)),
                        contentScale = ContentScale.Crop
                    )
                } else {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Icon(Icons.Outlined.AddPhotoAlternate, null, tint = IconInactive, modifier = Modifier.size(40.dp))
                        Spacer(Modifier.height(8.dp))
                        Text("Выбрать обложку", color = TextSecondary, fontSize = 14.sp)
                    }
                }
            }

            Spacer(Modifier.height(20.dp))

            BricklyTextField(
                value = date,
                onValueChange = { date = it },
                label = "Дата и время",
                placeholder = "2026-04-10T18:00:00"
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
                value = description,
                onValueChange = { description = it },
                label = "Описание",
                placeholder = "Описание мероприятия"
            )

            Spacer(Modifier.height(16.dp))

            Text("Тип мероприятия", style = MaterialTheme.typography.labelLarge, color = TextPrimary)
            Spacer(Modifier.height(6.dp))

            ExposedDropdownMenuBox(
                expanded = typeExpanded,
                onExpandedChange = { typeExpanded = it }
            ) {
                OutlinedTextField(
                    value = meetingTypes.find { it.id == selectedTypeId }?.description ?: "Выберите тип",
                    onValueChange = {},
                    readOnly = true,
                    modifier = Modifier.fillMaxWidth().menuAnchor(),
                    trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = typeExpanded) },
                    shape = RoundedCornerShape(14.dp),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = Accent,
                        unfocusedBorderColor = Divider,
                        focusedContainerColor = CardBackground,
                        unfocusedContainerColor = CardBackground
                    ),
                    textStyle = MaterialTheme.typography.bodyLarge.copy(color = TextPrimary)
                )
                ExposedDropdownMenu(expanded = typeExpanded, onDismissRequest = { typeExpanded = false }) {
                    meetingTypes.forEach { type ->
                        DropdownMenuItem(
                            text = { Text(type.description ?: "Тип #${type.id}") },
                            onClick = { selectedTypeId = type.id; typeExpanded = false }
                        )
                    }
                }
            }

            Spacer(Modifier.height(16.dp))

            BricklyTextField(
                value = ticketPrice,
                onValueChange = { ticketPrice = it.filter { c -> c.isDigit() } },
                label = "Цена билета (₽)",
                placeholder = "500",
                keyboardType = KeyboardType.Number
            )

            Spacer(Modifier.height(16.dp))

            Text("Скидка (необязательно)", style = MaterialTheme.typography.labelLarge, color = TextSecondary)
            Spacer(Modifier.height(8.dp))

            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                Box(modifier = Modifier.weight(1f)) {
                    BricklyTextField(
                        value = discountDuration,
                        onValueChange = { discountDuration = it.filter { c -> c.isDigit() } },
                        label = "Дней",
                        placeholder = "0",
                        keyboardType = KeyboardType.Number
                    )
                }
                Box(modifier = Modifier.weight(1f)) {
                    BricklyTextField(
                        value = discountAmount,
                        onValueChange = { discountAmount = it.filter { c -> c.isDigit() } },
                        label = "Размер",
                        placeholder = "0",
                        keyboardType = KeyboardType.Number
                    )
                }
                Box(modifier = Modifier.weight(1f)) {
                    BricklyTextField(
                        value = discountModifier,
                        onValueChange = { discountModifier = it.filter { c -> c.isDigit() } },
                        label = "Модиф.",
                        placeholder = "0",
                        keyboardType = KeyboardType.Number
                    )
                }
            }

            Spacer(Modifier.height(24.dp))

            if (errorMessage != null) {
                Text(errorMessage!!, color = ErrorColor, style = MaterialTheme.typography.bodySmall)
                Spacer(Modifier.height(8.dp))
            }
            if (successMessage != null) {
                Text(successMessage!!, color = AccentDark, style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.SemiBold))
                Spacer(Modifier.height(8.dp))
            }

            Button(
                onClick = {
                    scope.launch {
                        isSaving = true
                        errorMessage = null
                        successMessage = null
                        try {
                            val dto = MeetingCreateDTO(
                                date = date,
                                address = address,
                                typeId = selectedTypeId,
                                ticketPrice = ticketPrice.toIntOrNull() ?: 0,
                                description = description.ifBlank { null },
                                discountDuration = discountDuration.toIntOrNull(),
                                discountAmount = discountAmount.toIntOrNull(),
                                discountModifier = discountModifier.toIntOrNull()
                            )
                            val response = RetrofitClient.api.createMeeting(dto)
                            if (response.isSuccessful) {
                                successMessage = "Сходка создана!"
                                date = ""; address = ""; description = ""
                                ticketPrice = ""; discountDuration = ""
                                discountAmount = ""; discountModifier = ""
                                coverImageUri = null
                            } else {
                                errorMessage = "Ошибка создания (${response.code()})"
                            }
                        } catch (e: Exception) {
                            errorMessage = "Нет соединения с сервером"
                        }
                        isSaving = false
                    }
                },
                enabled = !isSaving && date.isNotBlank() && address.isNotBlank() && selectedTypeId != -1,
                modifier = Modifier.fillMaxWidth().height(56.dp),
                shape = RoundedCornerShape(14.dp),
                colors = ButtonDefaults.buttonColors(containerColor = Accent, contentColor = TextPrimary)
            ) {
                if (isSaving) {
                    CircularProgressIndicator(modifier = Modifier.size(18.dp), color = TextPrimary, strokeWidth = 2.dp)
                } else {
                    Text("Создать", fontWeight = FontWeight.SemiBold, fontSize = 16.sp)
                }
            }

            Spacer(Modifier.height(32.dp))
        }
    }
}
