package com.example.bricklyfrontend.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.ArrowBackIosNew
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
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
    val scope = rememberCoroutineScope()

    var date by remember { mutableStateOf("") }
    var address by remember { mutableStateOf("") }
    var description by remember { mutableStateOf("") }
    var ticketPrice by remember { mutableStateOf("") }
    var discountDuration by remember { mutableStateOf("") }
    var discountAmount by remember { mutableStateOf("") }
    var discountModifier by remember { mutableStateOf("") }

    var meetingTypes by remember { mutableStateOf<List<MeetingTypeDefaultDTO>>(emptyList()) }
    var selectedTypeId by remember { mutableIntStateOf(-1) }
    var typeExpanded by remember { mutableStateOf(false) }

    var isSaving by remember { mutableStateOf(false) }
    var errorMessage by remember { mutableStateOf<String?>(null) }
    var successMessage by remember { mutableStateOf<String?>(null) }

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
                        "Создать сходку",
                        style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold),
                        color = TextPrimary
                    )
                },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.Outlined.ArrowBackIosNew, "Назад", tint = TextPrimary)
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = Background)
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
            Spacer(Modifier.height(8.dp))

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

            Text(
                "Тип мероприятия",
                style = MaterialTheme.typography.labelLarge,
                color = TextPrimary
            )
            Spacer(Modifier.height(6.dp))

            ExposedDropdownMenuBox(
                expanded = typeExpanded,
                onExpandedChange = { typeExpanded = it }
            ) {
                OutlinedTextField(
                    value = meetingTypes.find { it.id == selectedTypeId }?.description ?: "Выберите тип",
                    onValueChange = {},
                    readOnly = true,
                    modifier = Modifier.fillMaxWidth(),
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
                ExposedDropdownMenu(
                    expanded = typeExpanded,
                    onDismissRequest = { typeExpanded = false }
                ) {
                    meetingTypes.forEach { type ->
                        DropdownMenuItem(
                            text = { Text(type.description ?: "Тип #${type.id}") },
                            onClick = {
                                selectedTypeId = type.id
                                typeExpanded = false
                            }
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

            BricklyTextField(
                value = discountDuration,
                onValueChange = { discountDuration = it.filter { c -> c.isDigit() } },
                label = "Длительность скидки (дней)",
                placeholder = "0",
                keyboardType = KeyboardType.Number
            )

            Spacer(Modifier.height(16.dp))

            BricklyTextField(
                value = discountAmount,
                onValueChange = { discountAmount = it.filter { c -> c.isDigit() } },
                label = "Размер скидки",
                placeholder = "0",
                keyboardType = KeyboardType.Number
            )

            Spacer(Modifier.height(16.dp))

            BricklyTextField(
                value = discountModifier,
                onValueChange = { discountModifier = it.filter { c -> c.isDigit() } },
                label = "Модификатор скидки",
                placeholder = "0",
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
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(14.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = Accent,
                    contentColor = TextPrimary
                )
            ) {
                if (isSaving) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(18.dp),
                        color = TextPrimary,
                        strokeWidth = 2.dp
                    )
                } else {
                    Text("Создать", fontWeight = FontWeight.SemiBold)
                }
            }

            Spacer(Modifier.height(24.dp))
        }
    }
}
