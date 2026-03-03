package com.example.bricklyfrontend.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.bricklyfrontend.data.AppConfig
import com.example.bricklyfrontend.data.RetrofitClient
import com.example.bricklyfrontend.data.UserCreateDTO
import com.example.bricklyfrontend.data.UserPreferences
import com.example.bricklyfrontend.ui.theme.*
import kotlinx.coroutines.launch
import java.time.ZonedDateTime
import java.time.format.DateTimeFormatter

@Composable
fun RegisterScreen(
    onNavigateToLogin: () -> Unit,
    onRegistered: () -> Unit
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()

    var username by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    var confirmPassword by remember { mutableStateOf("") }
    var passwordVisible by remember { mutableStateOf(false) }
    var confirmVisible by remember { mutableStateOf(false) }
    var isLoading by remember { mutableStateOf(false) }
    var errorMessage by remember { mutableStateOf<String?>(null) }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Background)
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 24.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Spacer(Modifier.height(80.dp))

            Text(
                text = "Создать аккаунт",
                style = MaterialTheme.typography.headlineMedium,
                color = TextPrimary
            )

            Spacer(Modifier.height(8.dp))

            Text(
                text = "Заполните данные для регистрации",
                style = MaterialTheme.typography.bodyMedium,
                color = TextSecondary
            )

            Spacer(Modifier.height(40.dp))

            BricklyTextField(
                value = username,
                onValueChange = { username = it; errorMessage = null },
                label = "Никнейм",
                placeholder = "Введите никнейм"
            )

            Spacer(Modifier.height(16.dp))

            BricklyTextField(
                value = password,
                onValueChange = { password = it; errorMessage = null },
                label = "Пароль",
                placeholder = "Минимум 6 символов",
                keyboardType = KeyboardType.Password,
                isPassword = true,
                passwordVisible = passwordVisible,
                onTogglePassword = { passwordVisible = !passwordVisible }
            )

            Spacer(Modifier.height(16.dp))

            BricklyTextField(
                value = confirmPassword,
                onValueChange = { confirmPassword = it; errorMessage = null },
                label = "Подтвердите пароль",
                placeholder = "Повторите пароль",
                keyboardType = KeyboardType.Password,
                isPassword = true,
                passwordVisible = confirmVisible,
                onTogglePassword = { confirmVisible = !confirmVisible }
            )

            if (errorMessage != null) {
                Spacer(Modifier.height(12.dp))
                Text(
                    text = errorMessage!!,
                    color = ErrorColor,
                    style = MaterialTheme.typography.bodyMedium,
                    textAlign = TextAlign.Center
                )
            }

            Spacer(Modifier.height(32.dp))

            Button(
                onClick = {
                    if (AppConfig.debugMode) {
                        UserPreferences.saveUser(context, 0L, username.ifBlank { "debug" })
                        onRegistered()
                        return@Button
                    }
                    when {
                        username.isBlank() -> errorMessage = "Введите никнейм"
                        password.length < 6 -> errorMessage = "Пароль должен быть не менее 6 символов"
                        password != confirmPassword -> errorMessage = "Пароли не совпадают"
                        else -> {
                            scope.launch {
                                isLoading = true
                                errorMessage = null
                                try {
                                    val now = ZonedDateTime.now()
                                        .format(DateTimeFormatter.ISO_OFFSET_DATE_TIME)
                                    val response = RetrofitClient.api.registerUser(
                                        UserCreateDTO(
                                            username = username.trim(),
                                            password = password,
                                            createdAt = now
                                        )
                                    )
                                    if (response.isSuccessful && response.body() != null) {
                                        val user = response.body()!!
                                        UserPreferences.saveUser(context, user.id, user.username)
                                        onRegistered()
                                    } else {
                                        val code = response.code()
                                        errorMessage = when (code) {
                                            409 -> "Никнейм уже занят. Попробуйте другой."
                                            400 -> "Неверные данные. Проверьте введённые поля."
                                            500 -> "Ошибка сервера. Попробуйте позже."
                                            else -> "Ошибка регистрации (код $code)."
                                        }
                                    }
                                } catch (e: Exception) {
                                    errorMessage = "Нет соединения с сервером"
                                } finally {
                                    isLoading = false
                                }
                            }
                        }
                    }
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(56.dp),
                shape = RoundedCornerShape(16.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = Accent,
                    contentColor = TextPrimary
                ),
                enabled = !isLoading
            ) {
                if (isLoading) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(24.dp),
                        color = TextPrimary,
                        strokeWidth = 2.dp
                    )
                } else {
                    Text(
                        text = "Зарегистрироваться",
                        style = MaterialTheme.typography.labelLarge.copy(fontSize = 16.sp)
                    )
                }
            }

            Spacer(Modifier.height(16.dp))

            TextButton(onClick = onNavigateToLogin) {
                Text(
                    text = "Уже есть аккаунт? ",
                    style = MaterialTheme.typography.bodyMedium,
                    color = TextSecondary
                )
                Text(
                    text = "Войти",
                    style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.SemiBold),
                    color = TextPrimary
                )
            }
        }
    }
}
