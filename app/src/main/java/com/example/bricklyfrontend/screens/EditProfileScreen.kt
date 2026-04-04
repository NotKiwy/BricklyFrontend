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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.bricklyfrontend.data.RetrofitClient
import com.example.bricklyfrontend.data.UserPreferences
import com.example.bricklyfrontend.data.UserUpdateDTO
import com.example.bricklyfrontend.ui.theme.*
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EditProfileScreen(
    onBack: () -> Unit
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val userId = remember { UserPreferences.getUserId(context) }

    var username by remember { mutableStateOf("") }
    var name by remember { mutableStateOf("") }
    var email by remember { mutableStateOf("") }
    var city by remember { mutableStateOf("") }

    var isLoading by remember { mutableStateOf(true) }
    var isSaving by remember { mutableStateOf(false) }
    var errorMessage by remember { mutableStateOf<String?>(null) }
    var successMessage by remember { mutableStateOf<String?>(null) }

    LaunchedEffect(userId) {
        try {
            val response = RetrofitClient.api.getUserById(userId)
            if (response.isSuccessful) {
                val user = response.body()
                username = user?.username ?: ""
                name = user?.name ?: ""
                email = user?.email ?: ""
                city = user?.city ?: ""
            }
        } catch (_: Exception) {
            errorMessage = "Не удалось загрузить данные профиля"
        }
        isLoading = false
    }

    Scaffold(
        containerColor = Background,
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        "Изменить профиль",
                        style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold),
                        color = TextPrimary
                    )
                },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(
                            imageVector = Icons.Outlined.ArrowBackIosNew,
                            contentDescription = "Назад",
                            tint = TextPrimary
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = Background)
            )
        }
    ) { padding ->
        if (isLoading) {
            Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                CircularProgressIndicator(color = Accent)
            }
        } else {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding)
                    .verticalScroll(rememberScrollState())
                    .padding(horizontal = 20.dp)
            ) {
                Spacer(Modifier.height(8.dp))

                BricklyTextField(
                    value = name,
                    onValueChange = { name = it; successMessage = null },
                    label = "Имя",
                    placeholder = "Введите имя"
                )

                Spacer(Modifier.height(16.dp))

                BricklyTextField(
                    value = email,
                    onValueChange = { email = it; successMessage = null },
                    label = "Email",
                    placeholder = "Введите email"
                )

                Spacer(Modifier.height(16.dp))

                BricklyTextField(
                    value = city,
                    onValueChange = { city = it; successMessage = null },
                    label = "Город",
                    placeholder = "Введите город"
                )

                Spacer(Modifier.height(24.dp))

                if (errorMessage != null) {
                    Text(
                        errorMessage!!,
                        color = ErrorColor,
                        style = MaterialTheme.typography.bodySmall
                    )
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
                                val dto = UserUpdateDTO(
                                    username = username.ifBlank { null },
                                    name = name.ifBlank { null },
                                    email = email.ifBlank { null },
                                    city = city.ifBlank { null }
                                )
                                val response = RetrofitClient.api.updateUser(userId, dto)
                                if (response.isSuccessful) {
                                    successMessage = "Профиль обновлён"
                                } else {
                                    errorMessage = "Ошибка обновления (${response.code()})"
                                }
                            } catch (e: Exception) {
                                errorMessage = "Нет соединения с сервером"
                            }
                            isSaving = false
                        }
                    },
                    enabled = !isSaving,
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
                        Text("Сохранить", fontWeight = FontWeight.SemiBold)
                    }
                }

                Spacer(Modifier.height(24.dp))
            }
        }
    }
}
