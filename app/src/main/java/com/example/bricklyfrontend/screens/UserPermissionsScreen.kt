package com.example.bricklyfrontend.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.bricklyfrontend.data.*
import com.example.bricklyfrontend.ui.theme.*
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun UserPermissionsScreen(onBack: () -> Unit) {
    SetStatusBarColor(Accent)

    val scope = rememberCoroutineScope()
    val snackbarHostState = remember { SnackbarHostState() }

    var searchQuery by remember { mutableStateOf("") }
    var isSearching by remember { mutableStateOf(false) }
    var searchError by remember { mutableStateOf<String?>(null) }
    var foundUser by remember { mutableStateOf<UserDefaultDTO?>(null) }

    var allRoles by remember { mutableStateOf<List<AuthorityDefaultDTO>>(emptyList()) }
    var rolesLoading by remember { mutableStateOf(true) }

    var checkedRoles by remember { mutableStateOf<Set<String>>(emptySet()) }
    var isSaving by remember { mutableStateOf(false) }

    LaunchedEffect(Unit) {
        try {
            val resp = RetrofitClient.api.getAllAuthorities()
            if (resp.isSuccessful) allRoles = resp.body() ?: emptyList()
        } catch (_: Exception) {}
        rolesLoading = false
    }

    fun search() {
        val q = searchQuery.trim()
        if (q.isBlank()) return
        scope.launch {
            isSearching = true
            searchError = null
            foundUser = null
            checkedRoles = emptySet()
            try {
                val resp = RetrofitClient.api.getUsersByUsernameContaining(q)
                if (resp.isSuccessful) {
                    val user = resp.body()?.content?.firstOrNull()
                    if (user != null) {
                        foundUser = user
                        checkedRoles = user.authorities?.map { it.authority }?.toSet() ?: emptySet()
                    } else {
                        searchError = "Пользователь не найден"
                    }
                } else {
                    searchError = "Ошибка поиска (${resp.code()})"
                }
            } catch (_: Exception) {
                searchError = "Нет соединения"
            }
            isSearching = false
        }
    }

    fun save() {
        val user = foundUser ?: return
        scope.launch {
            isSaving = true
            try {
                val resp = RetrofitClient.api.changeUserAuthorities(
                    user.id,
                    UserAuthoritiesPatchDTO(checkedRoles.toList())
                )
                if (resp.isSuccessful) {
                    snackbarHostState.showSnackbar("Роли сохранены")
                    foundUser = resp.body()
                    checkedRoles = resp.body()?.authorities?.map { it.authority }?.toSet() ?: checkedRoles
                } else {
                    snackbarHostState.showSnackbar("Ошибка сохранения (${resp.code()})")
                }
            } catch (_: Exception) {
                snackbarHostState.showSnackbar("Нет соединения")
            }
            isSaving = false
        }
    }

    Scaffold(
        containerColor = Background,
        snackbarHost = { SnackbarHost(snackbarHostState) },
        topBar = {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(bottomStart = 28.dp, bottomEnd = 28.dp))
                    .background(Accent)
                    .statusBarsPadding()
                    .padding(horizontal = 8.dp, vertical = 12.dp)
            ) {
                IconButton(onClick = onBack, modifier = Modifier.align(Alignment.CenterStart)) {
                    Icon(Icons.Outlined.ArrowBackIosNew, "Назад", tint = TextPrimary)
                }
                Text(
                    "Управление ролями",
                    color = Color(0xFF1A1A1A),
                    fontWeight = FontWeight.Bold,
                    fontSize = 18.sp,
                    modifier = Modifier.align(Alignment.Center)
                )
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
            Spacer(Modifier.height(20.dp))

            OutlinedTextField(
                value = searchQuery,
                onValueChange = { searchQuery = it },
                label = { Text("Имя пользователя") },
                leadingIcon = { Icon(Icons.Outlined.Search, null, tint = IconInactive) },
                trailingIcon = {
                    if (isSearching) {
                        CircularProgressIndicator(modifier = Modifier.size(20.dp), color = Accent, strokeWidth = 2.dp)
                    }
                },
                singleLine = true,
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = Accent,
                    unfocusedBorderColor = Divider,
                    focusedLabelColor = Accent
                ),
                keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
                keyboardActions = KeyboardActions(onSearch = { search() })
            )

            Spacer(Modifier.height(12.dp))

            Button(
                onClick = { search() },
                enabled = searchQuery.isNotBlank() && !isSearching,
                modifier = Modifier.fillMaxWidth().height(50.dp),
                shape = RoundedCornerShape(14.dp),
                colors = ButtonDefaults.buttonColors(containerColor = Accent, contentColor = TextPrimary)
            ) {
                Text("Найти пользователя", fontWeight = FontWeight.SemiBold)
            }

            if (searchError != null) {
                Spacer(Modifier.height(16.dp))
                Text(
                    searchError!!,
                    color = ErrorColor,
                    fontSize = 14.sp,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.fillMaxWidth()
                )
            }

            val user = foundUser
            if (user != null) {
                Spacer(Modifier.height(20.dp))

                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(20.dp),
                    colors = CardDefaults.cardColors(containerColor = CardBackground),
                    elevation = CardDefaults.cardElevation(0.dp)
                ) {
                    Column(modifier = Modifier.padding(20.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Outlined.Person, null, tint = Accent, modifier = Modifier.size(20.dp))
                            Spacer(Modifier.width(8.dp))
                            Text(
                                user.username,
                                fontSize = 17.sp,
                                fontWeight = FontWeight.Bold,
                                color = TextPrimary
                            )
                        }
                        if (!user.name.isNullOrBlank()) {
                            Spacer(Modifier.height(4.dp))
                            Text(user.name, fontSize = 13.sp, color = TextSecondary)
                        }
                    }
                }

                Spacer(Modifier.height(16.dp))

                Text(
                    "РОЛИ",
                    fontSize = 11.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = TextPrimary.copy(alpha = 0.5f),
                    letterSpacing = 0.8.sp
                )

                Spacer(Modifier.height(8.dp))

                if (rolesLoading) {
                    Box(Modifier.fillMaxWidth().padding(16.dp), contentAlignment = Alignment.Center) {
                        CircularProgressIndicator(color = Accent)
                    }
                } else {
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(20.dp),
                        colors = CardDefaults.cardColors(containerColor = CardBackground),
                        elevation = CardDefaults.cardElevation(0.dp)
                    ) {
                        Column {
                            allRoles.forEachIndexed { index, role ->
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(horizontal = 16.dp, vertical = 4.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Checkbox(
                                        checked = role.authority in checkedRoles,
                                        onCheckedChange = { checked ->
                                            checkedRoles = if (checked) {
                                                checkedRoles + role.authority
                                            } else {
                                                checkedRoles - role.authority
                                            }
                                        },
                                        colors = CheckboxDefaults.colors(
                                            checkedColor = Accent,
                                            checkmarkColor = TextPrimary
                                        )
                                    )
                                    Spacer(Modifier.width(8.dp))
                                    Text(
                                        role.authority,
                                        fontSize = 15.sp,
                                        fontWeight = FontWeight.Medium,
                                        color = TextPrimary
                                    )
                                }
                                if (index < allRoles.lastIndex) {
                                    HorizontalDivider(
                                        modifier = Modifier.padding(horizontal = 16.dp),
                                        color = Divider,
                                        thickness = 1.dp
                                    )
                                }
                            }
                        }
                    }

                    Spacer(Modifier.height(16.dp))

                    Button(
                        onClick = { save() },
                        enabled = !isSaving,
                        modifier = Modifier.fillMaxWidth().height(52.dp),
                        shape = RoundedCornerShape(14.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = Accent, contentColor = TextPrimary)
                    ) {
                        if (isSaving) {
                            CircularProgressIndicator(modifier = Modifier.size(18.dp), color = TextPrimary, strokeWidth = 2.dp)
                        } else {
                            Icon(Icons.Outlined.Save, null, modifier = Modifier.size(18.dp))
                            Spacer(Modifier.width(8.dp))
                            Text("Сохранить роли", fontWeight = FontWeight.SemiBold)
                        }
                    }
                }
            }

            Spacer(Modifier.height(32.dp))
        }
    }
}
