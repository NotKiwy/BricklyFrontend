package com.example.bricklyfrontend.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.ChatBubbleOutline
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.platform.LocalContext
import com.example.bricklyfrontend.data.MessageDefaultDTO
import com.example.bricklyfrontend.data.RetrofitClient
import com.example.bricklyfrontend.data.UserPreferences
import com.example.bricklyfrontend.data.UserShortDTO
import com.example.bricklyfrontend.ui.theme.*

data class ChatPreview(
    val otherUser: UserShortDTO,
    val lastMessage: String,
    val date: String?
)

@Composable
fun ChatsScreen(
    onNavigateToMeetings: () -> Unit = {},
    onNavigateToHome: () -> Unit = {},
    onNavigateToProfile: () -> Unit = {},
    onNavigateToBrickognize: () -> Unit = {},
    onOpenChat: (Long) -> Unit = {}
) {
    SetStatusBarColor(Accent)

    val context = LocalContext.current
    val myId = remember { UserPreferences.getUserId(context) }

    var chats by remember { mutableStateOf<List<ChatPreview>>(emptyList()) }
    var isLoading by remember { mutableStateOf(true) }
    var errorMessage by remember { mutableStateOf<String?>(null) }

    LaunchedEffect(Unit) {
        try {
            val resp = RetrofitClient.api.getMessagesLastActivity()
            if (resp.isSuccessful) {
                chats = buildChatPreviews(resp.body()?.content.orEmpty(), myId)
            } else {
                errorMessage = "Ошибка загрузки (${resp.code()})"
            }
        } catch (e: Exception) {
            errorMessage = "Нет соединения с сервером"
        }
        isLoading = false
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
                    .padding(horizontal = 20.dp)
                    .padding(top = 18.dp, bottom = 20.dp)
            ) {
                Text(
                    text = "Чаты",
                    color = TextPrimary,
                    fontWeight = FontWeight.ExtraBold,
                    fontSize = 24.sp,
                    modifier = Modifier.align(Alignment.CenterStart)
                )
            }
        },
        bottomBar = {
            BricklyBottomBar(currentRoute = "chats", onNavigate = { route ->
                when (route) {
                    "meetings" -> onNavigateToMeetings()
                    "home" -> onNavigateToHome()
                    "profile" -> onNavigateToProfile()
                    "brickognize" -> onNavigateToBrickognize()
                }
            }, onScanClick = onNavigateToBrickognize)
        }
    ) { padding ->
        when {
            isLoading -> Box(Modifier.fillMaxSize().padding(padding), contentAlignment = Alignment.Center) {
                CircularProgressIndicator(color = Accent)
            }

            errorMessage != null -> Box(
                Modifier.fillMaxSize().padding(padding).padding(32.dp),
                contentAlignment = Alignment.Center
            ) {
                Text(errorMessage!!, color = ErrorColor)
            }

            chats.isEmpty() -> Column(
                Modifier.fillMaxSize().padding(padding).padding(32.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center
            ) {
                Icon(Icons.Outlined.ChatBubbleOutline, null, tint = IconInactive, modifier = Modifier.size(56.dp))
                Spacer(Modifier.height(16.dp))
                Text(
                    "У вас пока нет сообщений",
                    color = TextSecondary,
                    fontWeight = FontWeight.SemiBold,
                    fontSize = 16.sp
                )
            }

            else -> LazyColumn(
                modifier = Modifier.fillMaxSize().padding(padding),
                contentPadding = PaddingValues(vertical = 12.dp)
            ) {
                items(chats, key = { it.otherUser.id }) { chat ->
                    ChatRow(chat = chat, onClick = { onOpenChat(chat.otherUser.id) })
                }
            }
        }
    }
}

@Composable
private fun ChatRow(chat: ChatPreview, onClick: () -> Unit) {
    val title = chat.otherUser.name?.takeIf { it.isNotBlank() } ?: chat.otherUser.username
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(horizontal = 20.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .size(52.dp)
                .clip(CircleShape)
                .background(Accent),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = title.take(1).uppercase(),
                fontSize = 22.sp,
                fontWeight = FontWeight.Bold,
                color = TextPrimary
            )
        }
        Spacer(Modifier.width(14.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = title,
                fontWeight = FontWeight.Bold,
                fontSize = 16.sp,
                color = TextPrimary,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            Spacer(Modifier.height(2.dp))
            Text(
                text = chat.lastMessage,
                fontSize = 14.sp,
                color = TextSecondary,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
        chat.date?.let {
            Spacer(Modifier.width(8.dp))
            Text(
                text = formatChatTimestamp(it),
                fontSize = 12.sp,
                color = IconInactive
            )
        }
    }
}

private fun buildChatPreviews(messages: List<MessageDefaultDTO>, myId: Long): List<ChatPreview> {
    val sorted = messages.sortedByDescending { it.date ?: "" }
    val seen = LinkedHashMap<Long, ChatPreview>()
    for (m in sorted) {
        val other = when {
            m.author != null && m.author.id != myId -> m.author
            m.target != null && m.target.id != myId -> m.target
            else -> null
        } ?: continue
        if (!seen.containsKey(other.id)) {
            seen[other.id] = ChatPreview(
                otherUser = other,
                lastMessage = m.text ?: "",
                date = m.date
            )
        }
    }
    return seen.values.toList()
}
