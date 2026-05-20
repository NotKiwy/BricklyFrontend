package com.example.bricklyfrontend.screens

import android.widget.Toast
import androidx.compose.foundation.background
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
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.bricklyfrontend.data.RetrofitClient
import com.example.bricklyfrontend.data.UserPreferences
import com.example.bricklyfrontend.data.UserUpdateDTO
import com.example.bricklyfrontend.ui.theme.*
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EditProfileScreen(
    onBack: () -> Unit,
    onSaved: () -> Unit = {}
) {
    SetStatusBarColor(Color.White)
    
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val userId = remember { UserPreferences.getUserId(context) }

    var name by remember { mutableStateOf("") }
    var email by remember { mutableStateOf("") }
    var city by remember { mutableStateOf("") }

    var isLoading by remember { mutableStateOf(true) }
    var isSaving by remember { mutableStateOf(false) }
    var errorMessage by remember { mutableStateOf<String?>(null) }

    LaunchedEffect(userId) {
        try {
            val response = RetrofitClient.api.getUserById(userId)
            if (response.isSuccessful) {
                val user = response.body()
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
                        text = "Изменить профиль",
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
                Spacer(Modifier.height(16.dp))

                BricklyTextField(
                    value = name,
                    onValueChange = { name = it; errorMessage = null },
                    label = "Имя",
                    placeholder = "Введите имя"
                )

                Spacer(Modifier.height(16.dp))

                BricklyTextField(
                    value = email,
                    onValueChange = { email = it; errorMessage = null },
                    label = "Email",
                    placeholder = "Введите email"
                )

                Spacer(Modifier.height(16.dp))

                BricklyTextField(
                    value = city,
                    onValueChange = { city = it; errorMessage = null },
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

                Button(
                    onClick = {
                        scope.launch {
                            isSaving = true
                            errorMessage = null
                            try {
                                val dto = UserUpdateDTO(
                                    name = name.ifBlank { null },
                                    email = email.ifBlank { null },
                                    city = city.ifBlank { null }
                                )
                                val response = RetrofitClient.api.updateUser(userId, dto)
                                if (response.isSuccessful) {
                                    Toast.makeText(
                                        context,
                                        "Профиль успешно изменен",
                                        Toast.LENGTH_SHORT
                                    ).show()
                                    onSaved()
                                } else {
                                    errorMessage = "Ошибка обновления (${response.code()})"
                                    isSaving = false
                                }
                            } catch (e: Exception) {
                                errorMessage = "Нет соединения с сервером"
                                isSaving = false
                            }
                        }
                    },
                    enabled = !isSaving,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(56.dp),
                    shape = RoundedCornerShape(14.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = Accent,
                        contentColor = TextPrimary
                    )
                ) {
                    if (isSaving) {
                        CircularProgressIndicator(modifier = Modifier.size(18.dp), color = TextPrimary, strokeWidth = 2.dp)
                    } else {
                        Text("Сохранить", fontWeight = FontWeight.SemiBold)
                    }
                }

                Spacer(Modifier.height(24.dp))
            }
        }
    }
}
