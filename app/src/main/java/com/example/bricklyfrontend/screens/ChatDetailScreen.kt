package com.example.bricklyfrontend.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.Send
import androidx.compose.material.icons.outlined.ArrowBackIosNew
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.bricklyfrontend.data.MessageCreateDTO
import com.example.bricklyfrontend.data.MessageDefaultDTO
import com.example.bricklyfrontend.data.RetrofitClient
import com.example.bricklyfrontend.data.UserPreferences
import com.example.bricklyfrontend.ui.theme.*
import kotlinx.coroutines.launch
import java.time.OffsetDateTime
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.Locale

@Composable
fun ChatDetailScreen(
    otherUserId: Long,
    onBack: () -> Unit
) {
    SetStatusBarColor(Accent)

    val context = LocalContext.current
    val myId = remember { UserPreferences.getUserId(context) }
    val scope = rememberCoroutineScope()
    val listState = rememberLazyListState()

    var title by remember { mutableStateOf("Чат") }
    var messages by remember { mutableStateOf<List<MessageDefaultDTO>>(emptyList()) }
    var isLoading by remember { mutableStateOf(true) }
    var errorMessage by remember { mutableStateOf<String?>(null) }
    var input by remember { mutableStateOf("") }
    var isSending by remember { mutableStateOf(false) }

    suspend fun reload() {
        try {
            val resp = RetrofitClient.api.getMessagesByMembers(otherUserId)
            if (resp.isSuccessful) {
                messages = resp.body()?.content.orEmpty().sortedBy { it.date ?: "" }
            } else {
                errorMessage = "Ошибка загрузки (${resp.code()})"
            }
        } catch (e: Exception) {
            errorMessage = "Нет соединения с сервером"
        }
    }

    LaunchedEffect(otherUserId) {
        try {
            val userResp = RetrofitClient.api.getUserById(otherUserId)
            if (userResp.isSuccessful) {
                val u = userResp.body()
                title = u?.name?.takeIf { it.isNotBlank() } ?: u?.username ?: "Чат"
            }
        } catch (_: Exception) {}
        reload()
        isLoading = false
    }

    LaunchedEffect(messages.size) {
        if (messages.isNotEmpty()) listState.animateScrollToItem(messages.size - 1)
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
                IconButton(onClick = onBack, modifier = Modifier.align(Alignment.CenterStart)) {
                    Icon(Icons.Outlined.ArrowBackIosNew, "Назад", tint = TextPrimary)
                }
                Text(
                    text = title,
                    color = TextPrimary,
                    fontWeight = FontWeight.Bold,
                    fontSize = 18.sp,
                    modifier = Modifier.align(Alignment.Center)
                )
            }
        },
        bottomBar = {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(Color.White)
                    .navigationBarsPadding()
                    .imePadding()
                    .padding(horizontal = 12.dp, vertical = 8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                TextField(
                    value = input,
                    onValueChange = { input = it },
                    modifier = Modifier.weight(1f),
                    placeholder = { Text("Сообщение…", color = IconInactive) },
                    shape = RoundedCornerShape(24.dp),
                    keyboardOptions = KeyboardOptions(imeAction = ImeAction.Send),
                    colors = TextFieldDefaults.colors(
                        focusedContainerColor = Background,
                        unfocusedContainerColor = Background,
                        focusedIndicatorColor = Color.Transparent,
                        unfocusedIndicatorColor = Color.Transparent,
                        cursorColor = AccentDark
                    ),
                    maxLines = 4
                )
                Spacer(Modifier.width(8.dp))
                val canSend = input.isNotBlank() && !isSending
                IconButton(
                    onClick = {
                        val text = input.trim()
                        if (text.isEmpty()) return@IconButton
                        isSending = true
                        scope.launch {
                            try {
                                val resp = RetrofitClient.api.sendMessage(MessageCreateDTO(text, otherUserId))
                                if (resp.isSuccessful) {
                                    input = ""
                                    val sent = resp.body()
                                    messages = if (sent != null) messages + sent else messages
                                }
                            } catch (_: Exception) {}
                            isSending = false
                        }
                    },
                    enabled = canSend,
                    modifier = Modifier
                        .size(48.dp)
                        .clip(RoundedCornerShape(24.dp))
                        .background(if (canSend) Accent else Divider)
                ) {
                    Icon(
                        Icons.AutoMirrored.Outlined.Send,
                        contentDescription = "Отправить",
                        tint = if (canSend) TextPrimary else IconInactive,
                        modifier = Modifier.size(20.dp)
                    )
                }
            }
        }
    ) { padding ->
        when {
            isLoading -> Box(Modifier.fillMaxSize().padding(padding), contentAlignment = Alignment.Center) {
                CircularProgressIndicator(color = Accent)
            }

            errorMessage != null && messages.isEmpty() -> Box(
                Modifier.fillMaxSize().padding(padding).padding(32.dp),
                contentAlignment = Alignment.Center
            ) {
                Text(errorMessage!!, color = ErrorColor, textAlign = TextAlign.Center)
            }

            messages.isEmpty() -> Box(
                Modifier.fillMaxSize().padding(padding).padding(32.dp),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    "Начните переписку",
                    color = TextSecondary,
                    fontWeight = FontWeight.SemiBold,
                    fontSize = 16.sp
                )
            }

            else -> LazyColumn(
                state = listState,
                modifier = Modifier.fillMaxSize().padding(padding),
                contentPadding = PaddingValues(horizontal = 16.dp, vertical = 12.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                items(messages, key = { it.id }) { message ->
                    MessageBubble(message = message, isMine = message.author?.id == myId)
                }
            }
        }
    }
}

@Composable
private fun MessageBubble(message: MessageDefaultDTO, isMine: Boolean) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = if (isMine) Arrangement.End else Arrangement.Start
    ) {
        Column(
            modifier = Modifier
                .widthIn(max = 280.dp)
                .clip(
                    RoundedCornerShape(
                        topStart = 18.dp,
                        topEnd = 18.dp,
                        bottomStart = if (isMine) 18.dp else 4.dp,
                        bottomEnd = if (isMine) 4.dp else 18.dp
                    )
                )
                .background(if (isMine) Accent else Color.White)
                .padding(horizontal = 14.dp, vertical = 10.dp)
        ) {
            Text(
                text = message.text ?: "",
                color = TextPrimary,
                fontSize = 15.sp
            )
            message.date?.let {
                Text(
                    text = formatChatTime(it),
                    color = TextSecondary,
                    fontSize = 11.sp,
                    modifier = Modifier.align(Alignment.End)
                )
            }
        }
    }
}

private val timeFormatter: DateTimeFormatter = DateTimeFormatter.ofPattern("HH:mm", Locale("ru"))
private val dayFormatter: DateTimeFormatter = DateTimeFormatter.ofPattern("d MMM", Locale("ru"))

private fun parseChatDate(raw: String): OffsetDateTime? =
    try { OffsetDateTime.parse(raw) } catch (_: Exception) {
        try { OffsetDateTime.parse(raw + "Z") } catch (_: Exception) { null }
    }

fun formatChatTime(raw: String): String =
    parseChatDate(raw)?.atZoneSameInstant(ZoneId.systemDefault())?.format(timeFormatter) ?: ""

fun formatChatTimestamp(raw: String): String {
    val odt = parseChatDate(raw)?.atZoneSameInstant(ZoneId.systemDefault()) ?: return ""
    val now = OffsetDateTime.now().atZoneSameInstant(ZoneId.systemDefault())
    return if (odt.toLocalDate() == now.toLocalDate()) odt.format(timeFormatter)
    else odt.format(dayFormatter)
}
