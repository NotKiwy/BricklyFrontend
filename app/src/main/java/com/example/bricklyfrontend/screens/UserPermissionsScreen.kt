package com.example.bricklyfrontend.screens

import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.bricklyfrontend.data.*
import com.example.bricklyfrontend.ui.theme.*
import kotlinx.coroutines.launch

@Composable
fun UserPermissionsScreen(
    onBack: () -> Unit,
    onNavigate: (String) -> Unit = {}
) {
    SetStatusBarColor(Accent)

    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val keyboardController = LocalSoftwareKeyboardController.current
    val focusManager = LocalFocusManager.current

    var searchQuery by remember { mutableStateOf("") }
    var isSearching by remember { mutableStateOf(false) }
    var searchError by remember { mutableStateOf<String?>(null) }
    var searchResults by remember { mutableStateOf<List<UserDefaultDTO>>(emptyList()) }
    var selectedUser by remember { mutableStateOf<UserDefaultDTO?>(null) }

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
            searchResults = emptyList()
            selectedUser = null
            checkedRoles = emptySet()
            try {
                val resp = RetrofitClient.api.getUsersByUsernameContaining(q)
                if (resp.isSuccessful) {
                    val list = resp.body()?.content ?: emptyList()
                    if (list.isEmpty()) searchError = "Пользователи не найдены"
                    else searchResults = list
                } else {
                    searchError = "Ошибка поиска (${resp.code()})"
                }
            } catch (_: Exception) {
                searchError = "Нет соединения"
            }
            isSearching = false
        }
    }

    fun selectUser(user: UserDefaultDTO) {
        selectedUser = user
        checkedRoles = user.authorities?.map { it.authority }?.toSet() ?: emptySet()
        searchResults = emptyList()
    }

    fun save() {
        val user = selectedUser ?: return
        scope.launch {
            isSaving = true
            try {
                val resp = RetrofitClient.api.changeUserAuthorities(
                    user.id,
                    UserAuthoritiesPatchDTO(checkedRoles.toList())
                )
                isSaving = false
                if (resp.isSuccessful) {
                    Toast.makeText(context, "Роли сохранены", Toast.LENGTH_SHORT).show()
                    resp.body()?.let {
                        selectedUser = it
                        checkedRoles = it.authorities?.map { a -> a.authority }?.toSet() ?: checkedRoles
                    }
                } else {
                    Toast.makeText(context, "Ошибка сохранения (${resp.code()})", Toast.LENGTH_SHORT).show()
                }
            } catch (_: Exception) {
                isSaving = false
                Toast.makeText(context, "Нет соединения", Toast.LENGTH_SHORT).show()
            }
        }
    }

    Scaffold(
        containerColor = Background,
        bottomBar = {
            BricklyBottomBar(currentRoute = "profile", onNavigate = onNavigate)
        },
        topBar = {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(bottomStart = 28.dp, bottomEnd = 28.dp))
                    .background(Accent)
                    .statusBarsPadding()
                    .padding(horizontal = 20.dp)
                    .padding(top = 16.dp, bottom = 20.dp)
            ) {
                Column {
                    Box(modifier = Modifier.fillMaxWidth()) {
                        IconButton(
                            onClick = onBack,
                            modifier = Modifier.align(Alignment.CenterStart).offset(x = (-12).dp)
                        ) {
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
                    Spacer(Modifier.height(14.dp))
                    OutlinedTextField(
                        value = searchQuery,
                        onValueChange = { searchQuery = it },
                        modifier = Modifier.fillMaxWidth(),
                        placeholder = { Text("Имя пользователя...", color = TextSecondary, style = MaterialTheme.typography.bodyLarge) },
                        leadingIcon = { Icon(Icons.Outlined.Search, null, tint = TextSecondary, modifier = Modifier.size(20.dp)) },
                        trailingIcon = {
                            when {
                                isSearching -> CircularProgressIndicator(modifier = Modifier.size(20.dp), color = Accent, strokeWidth = 2.dp)
                                searchQuery.isNotEmpty() -> IconButton(onClick = {
                                    searchQuery = ""
                                    keyboardController?.hide()
                                    focusManager.clearFocus()
                                }) {
                                    Icon(Icons.Outlined.Close, null, tint = TextSecondary, modifier = Modifier.size(18.dp))
                                }
                            }
                        },
                        shape = RoundedCornerShape(16.dp),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = Color.Transparent,
                            unfocusedBorderColor = Color.Transparent,
                            focusedContainerColor = Color.White,
                            unfocusedContainerColor = Color.White.copy(alpha = 0.9f),
                            cursorColor = TextPrimary
                        ),
                        singleLine = true,
                        textStyle = MaterialTheme.typography.bodyLarge.copy(color = TextPrimary),
                        keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
                        keyboardActions = KeyboardActions(onSearch = { search() })
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

            if (searchError != null) {
                Text(
                    searchError!!,
                    color = ErrorColor,
                    fontSize = 14.sp,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.fillMaxWidth()
                )
            }

            if (searchResults.isNotEmpty()) {
                Text(
                    "РЕЗУЛЬТАТЫ",
                    fontSize = 11.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = TextPrimary.copy(alpha = 0.5f),
                    letterSpacing = 0.8.sp
                )
                Spacer(Modifier.height(8.dp))
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(20.dp),
                    colors = CardDefaults.cardColors(containerColor = CardBackground),
                    elevation = CardDefaults.cardElevation(0.dp)
                ) {
                    Column {
                        searchResults.forEachIndexed { index, user ->
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable { selectUser(user) }
                                    .padding(horizontal = 16.dp, vertical = 14.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(Icons.Outlined.Person, null, tint = IconInactive, modifier = Modifier.size(20.dp))
                                Spacer(Modifier.width(12.dp))
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(user.username, fontSize = 15.sp, fontWeight = FontWeight.SemiBold, color = TextPrimary)
                                    if (!user.name.isNullOrBlank()) {
                                        Text(user.name, fontSize = 12.sp, color = TextSecondary)
                                    }
                                }
                                Icon(Icons.Outlined.ChevronRight, null, tint = IconInactive, modifier = Modifier.size(20.dp))
                            }
                            if (index < searchResults.lastIndex) {
                                HorizontalDivider(modifier = Modifier.padding(horizontal = 16.dp), color = Divider, thickness = 1.dp)
                            }
                        }
                    }
                }
            }

            val user = selectedUser
            if (user != null) {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(20.dp),
                    colors = CardDefaults.cardColors(containerColor = CardBackground),
                    elevation = CardDefaults.cardElevation(0.dp)
                ) {
                    Row(
                        modifier = Modifier.padding(16.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(Icons.Outlined.Person, null, tint = Accent, modifier = Modifier.size(20.dp))
                        Spacer(Modifier.width(10.dp))
                        Column {
                            Text(user.username, fontSize = 16.sp, fontWeight = FontWeight.Bold, color = TextPrimary)
                            if (!user.name.isNullOrBlank()) {
                                Text(user.name, fontSize = 12.sp, color = TextSecondary)
                            }
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
                                val isChecked = role.authority in checkedRoles
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .clickable {
                                            checkedRoles = if (isChecked) checkedRoles - role.authority else checkedRoles + role.authority
                                        }
                                        .padding(horizontal = 16.dp, vertical = 4.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Checkbox(
                                        checked = isChecked,
                                        onCheckedChange = { checked ->
                                            checkedRoles = if (checked) checkedRoles + role.authority else checkedRoles - role.authority
                                        },
                                        colors = CheckboxDefaults.colors(
                                            checkedColor = Accent,
                                            checkmarkColor = TextPrimary
                                        )
                                    )
                                    Spacer(Modifier.width(8.dp))
                                    Text(
                                        role.authority.removePrefix("ROLE_"),
                                        fontSize = 15.sp,
                                        fontWeight = FontWeight.Medium,
                                        color = TextPrimary
                                    )
                                }
                                if (index < allRoles.lastIndex) {
                                    HorizontalDivider(modifier = Modifier.padding(horizontal = 16.dp), color = Divider, thickness = 1.dp)
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
                            Text("Сохранить роли", fontWeight = FontWeight.SemiBold)
                        }
                    }
                }
            }

            Spacer(Modifier.height(32.dp))
        }
    }
}
