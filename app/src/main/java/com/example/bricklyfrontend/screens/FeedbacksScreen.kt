package com.example.bricklyfrontend.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.outlined.ArrowBackIosNew
import androidx.compose.material.icons.outlined.StarBorder
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.bricklyfrontend.data.FeedbackCreateDTO
import com.example.bricklyfrontend.data.FeedbackDefaultDTO
import com.example.bricklyfrontend.data.RetrofitClient
import com.example.bricklyfrontend.data.UserPreferences
import com.example.bricklyfrontend.data.UserShortDTO
import com.example.bricklyfrontend.ui.theme.*
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FeedbacksScreen(
    targetUserId: Long,
    onBack: () -> Unit
) {
    SetStatusBarColor(Color.White)
    
    val context = LocalContext.current
    val scope = rememberCoroutineScope()

    val currentUserId = remember { UserPreferences.getUserId(context) }
    val currentUsername = remember { UserPreferences.getUsername(context) }

    var feedbacks by remember { mutableStateOf<List<FeedbackDefaultDTO>>(emptyList()) }
    var isLoading by remember { mutableStateOf(true) }
    var errorMessage by remember { mutableStateOf<String?>(null) }

    var showDialog by remember { mutableStateOf(false) }
    var dialogRate by remember { mutableIntStateOf(5) }
    var dialogComment by remember { mutableStateOf("") }
    var isSubmitting by remember { mutableStateOf(false) }
    var submitError by remember { mutableStateOf<String?>(null) }

    fun loadFeedbacks() {
        scope.launch {
            isLoading = true
            errorMessage = null
            try {
                val response = RetrofitClient.api.getFeedbacksByTargetId(targetUserId)
                if (response.isSuccessful) {
                    feedbacks = response.body() ?: emptyList()
                } else {
                    errorMessage = "Не удалось загрузить отзывы (${response.code()})"
                }
            } catch (e: Exception) {
                errorMessage = "Нет соединения с сервером"
            } finally {
                isLoading = false
            }
        }
    }

    LaunchedEffect(targetUserId) { loadFeedbacks() }

    val averageRating = if (feedbacks.isEmpty()) null
    else feedbacks.map { it.rate }.average()

    Scaffold(
        containerColor = Background,
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        "Отзывы",
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
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = Background
                ),
                actions = {
                    if (currentUserId != targetUserId) {
                        TextButton(onClick = { showDialog = true }) {
                            Text(
                                "Оставить",
                                color = Accent,
                                style = MaterialTheme.typography.bodyMedium.copy(
                                    fontWeight = FontWeight.SemiBold
                                )
                            )
                        }
                    }
                }
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
        ) {
            if (!isLoading && feedbacks.isNotEmpty() && averageRating != null) {
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 20.dp, vertical = 8.dp),
                    shape = RoundedCornerShape(20.dp),
                    colors = CardDefaults.cardColors(containerColor = CardBackground),
                    border = androidx.compose.foundation.BorderStroke(1.5.dp, Accent),
                    elevation = CardDefaults.cardElevation(0.dp)
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(20.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Text(
                                text = String.format("%.1f", averageRating),
                                style = MaterialTheme.typography.displaySmall.copy(
                                    fontWeight = FontWeight.ExtraBold,
                                    fontSize = 48.sp
                                ),
                                color = TextPrimary
                            )
                            StarsRow(rating = averageRating.toFloat(), size = 18.dp)
                        }

                        Spacer(Modifier.width(24.dp))

                        Column(modifier = Modifier.weight(1f)) {
                            for (star in 5 downTo 1) {
                                val count = feedbacks.count { it.rate == star }
                                val fraction = count.toFloat() / feedbacks.size
                                RatingBar(star = star, fraction = fraction, count = count)
                                if (star > 1) Spacer(Modifier.height(4.dp))
                            }
                        }
                    }
                }

                Spacer(Modifier.height(4.dp))

                Text(
                    text = "${feedbacks.size} ${pluralFeedbacks(feedbacks.size)}",
                    style = MaterialTheme.typography.bodyMedium,
                    color = TextSecondary,
                    modifier = Modifier.padding(horizontal = 20.dp)
                )

                Spacer(Modifier.height(12.dp))
            }

            when {
                isLoading -> {
                    Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        CircularProgressIndicator(color = Accent)
                    }
                }

                errorMessage != null -> {
                    Box(
                        Modifier
                            .fillMaxSize()
                            .padding(32.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Text(
                                text = errorMessage!!,
                                color = ErrorColor,
                                textAlign = TextAlign.Center
                            )
                            Spacer(Modifier.height(16.dp))
                            Button(
                                onClick = { loadFeedbacks() },
                                colors = ButtonDefaults.buttonColors(containerColor = Accent)
                            ) {
                                Text("Повторить", color = TextPrimary)
                            }
                        }
                    }
                }

                feedbacks.isEmpty() -> {
                    Box(
                        Modifier
                            .fillMaxSize()
                            .padding(32.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Text(
                                "Пока нет отзывов",
                                style = MaterialTheme.typography.bodyLarge.copy(
                                    fontWeight = FontWeight.SemiBold
                                ),
                                color = TextPrimary
                            )
                            Spacer(Modifier.height(6.dp))
                            Text(
                                "Будьте первым, кто оставит отзыв",
                                style = MaterialTheme.typography.bodyMedium,
                                color = TextSecondary,
                                textAlign = TextAlign.Center
                            )
                        }
                    }
                }

                else -> {
                    LazyColumn(
                        contentPadding = PaddingValues(vertical = 8.dp)
                    ) {
                        items(feedbacks, key = { it.id }) { feedback ->
                            FeedbackCard(feedback = feedback)
                        }
                    }
                }
            }
        }
    }

    if (showDialog) {
        AlertDialog(
            onDismissRequest = {
                if (!isSubmitting) {
                    showDialog = false
                    submitError = null
                    dialogComment = ""
                    dialogRate = 5
                }
            },
            containerColor = CardBackground,
            shape = RoundedCornerShape(24.dp),
            title = {
                Text(
                    "Оставить отзыв",
                    style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold),
                    color = TextPrimary
                )
            },
            text = {
                Column {
                    Text(
                        "Оценка",
                        style = MaterialTheme.typography.labelLarge,
                        color = TextPrimary
                    )
                    Spacer(Modifier.height(8.dp))

                    Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        for (star in 1..5) {
                            IconButton(
                                onClick = { dialogRate = star },
                                modifier = Modifier.size(36.dp)
                            ) {
                                Icon(
                                    imageVector = if (star <= dialogRate) Icons.Filled.Star
                                    else Icons.Outlined.StarBorder,
                                    contentDescription = "$star звезд",
                                    tint = if (star <= dialogRate) Accent else IconInactive,
                                    modifier = Modifier.size(28.dp)
                                )
                            }
                        }
                    }

                    Spacer(Modifier.height(16.dp))

                    Text(
                        "Комментарий (необязательно)",
                        style = MaterialTheme.typography.labelLarge,
                        color = TextPrimary
                    )
                    Spacer(Modifier.height(8.dp))

                    OutlinedTextField(
                        value = dialogComment,
                        onValueChange = { dialogComment = it; submitError = null },
                        modifier = Modifier
                            .fillMaxWidth()
                            .heightIn(min = 100.dp),
                        placeholder = {
                            Text(
                                "Напишите что-нибудь…",
                                color = TextSecondary,
                                style = MaterialTheme.typography.bodyMedium
                            )
                        },
                        shape = RoundedCornerShape(14.dp),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = Accent,
                            unfocusedBorderColor = Divider,
                            focusedContainerColor = Background,
                            unfocusedContainerColor = Background,
                            cursorColor = Accent
                        ),
                        textStyle = MaterialTheme.typography.bodyMedium.copy(color = TextPrimary),
                        maxLines = 5
                    )

                    if (submitError != null) {
                        Spacer(Modifier.height(8.dp))
                        Text(
                            submitError!!,
                            color = ErrorColor,
                            style = MaterialTheme.typography.bodySmall
                        )
                    }
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        scope.launch {
                            isSubmitting = true
                            submitError = null
                            try {
                                val dto = FeedbackCreateDTO(
                                    target_id = targetUserId,
                                    author = UserShortDTO(
                                        id = currentUserId,
                                        username = currentUsername,
                                        name = null
                                    ),
                                    rate = dialogRate,
                                    comment = dialogComment.ifBlank { null }
                                )
                                val response = RetrofitClient.api.createFeedback(dto)
                                if (response.isSuccessful) {
                                    showDialog = false
                                    dialogComment = ""
                                    dialogRate = 5
                                    loadFeedbacks()
                                } else {
                                    submitError = "Не удалось отправить отзыв (${response.code()})"
                                }
                            } catch (e: Exception) {
                                submitError = "Нет соединения с сервером"
                            } finally {
                                isSubmitting = false
                            }
                        }
                    },
                    enabled = !isSubmitting,
                    shape = RoundedCornerShape(14.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = Accent,
                        contentColor = TextPrimary
                    )
                ) {
                    if (isSubmitting) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(18.dp),
                            color = TextPrimary,
                            strokeWidth = 2.dp
                        )
                    } else {
                        Text("Отправить", fontWeight = FontWeight.SemiBold)
                    }
                }
            },
            dismissButton = {
                TextButton(
                    onClick = {
                        showDialog = false
                        submitError = null
                        dialogComment = ""
                        dialogRate = 5
                    },
                    enabled = !isSubmitting
                ) {
                    Text("Отмена", color = TextSecondary)
                }
            }
        )
    }
}

@Composable
private fun FeedbackCard(feedback: FeedbackDefaultDTO) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 20.dp, vertical = 12.dp)
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Box(
                modifier = Modifier
                    .size(44.dp)
                    .clip(CircleShape)
                    .background(Accent.copy(alpha = 0.25f)),
                contentAlignment = Alignment.Center
            ) {
                val initial = (feedback.author?.name ?: feedback.author?.username ?: "?")
                    .firstOrNull()?.uppercaseChar() ?: '?'
                Text(
                    text = initial.toString(),
                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                    color = TextPrimary
                )
            }

            Spacer(Modifier.width(12.dp))

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = feedback.author?.name?.takeIf { it.isNotBlank() }
                        ?: feedback.author?.username
                        ?: "Аноним",
                    style = MaterialTheme.typography.bodyLarge.copy(fontWeight = FontWeight.SemiBold),
                    color = TextPrimary
                )
                if (!feedback.author?.name.isNullOrBlank()) {
                    Text(
                        text = "@${feedback.author?.username}",
                        style = MaterialTheme.typography.bodySmall,
                        color = TextSecondary
                    )
                }
            }

            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    imageVector = Icons.Filled.Star,
                    contentDescription = null,
                    tint = Accent,
                    modifier = Modifier.size(16.dp)
                )
                Spacer(Modifier.width(4.dp))
                Text(
                    text = "${feedback.rate}",
                    style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold),
                    color = TextPrimary
                )
            }
        }

        if (!feedback.comment.isNullOrBlank()) {
            Spacer(Modifier.height(10.dp))
            Text(
                text = feedback.comment,
                style = MaterialTheme.typography.bodyMedium,
                color = TextPrimary,
                lineHeight = 20.sp
            )
        }

        Spacer(Modifier.height(12.dp))
        HorizontalDivider(color = Divider, thickness = 1.dp)
    }
}

@Composable
private fun StarsRow(rating: Float, size: androidx.compose.ui.unit.Dp) {
    Row(horizontalArrangement = Arrangement.spacedBy(2.dp)) {
        for (star in 1..5) {
            Icon(
                imageVector = if (star <= rating) Icons.Filled.Star else Icons.Outlined.StarBorder,
                contentDescription = null,
                tint = if (star <= rating) Accent else IconInactive,
                modifier = Modifier.size(size)
            )
        }
    }
}

@Composable
private fun RatingBar(star: Int, fraction: Float, count: Int) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(6.dp)
    ) {
        Text(
            text = "$star",
            style = MaterialTheme.typography.bodySmall,
            color = TextSecondary,
            modifier = Modifier.width(10.dp),
            textAlign = TextAlign.End
        )
        Icon(
            imageVector = Icons.Filled.Star,
            contentDescription = null,
            tint = Accent,
            modifier = Modifier.size(12.dp)
        )
        Box(
            modifier = Modifier
                .weight(1f)
                .height(6.dp)
                .clip(RoundedCornerShape(3.dp))
                .background(Divider)
        ) {
            if (fraction > 0f) {
                Box(
                    modifier = Modifier
                        .fillMaxHeight()
                        .fillMaxWidth(fraction)
                        .clip(RoundedCornerShape(3.dp))
                        .background(Accent)
                )
            }
        }
        Text(
            text = "$count",
            style = MaterialTheme.typography.bodySmall,
            color = TextSecondary,
            modifier = Modifier.width(16.dp),
            textAlign = TextAlign.End
        )
    }
}

private fun pluralFeedbacks(n: Int): String = when {
    n % 100 in 11..19 -> "отзывов"
    n % 10 == 1 -> "отзыв"
    n % 10 in 2..4 -> "отзыва"
    else -> "отзывов"
}
