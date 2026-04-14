package com.example.bricklyfrontend.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.*
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
import com.example.bricklyfrontend.data.MeetingDefaultDTO
import com.example.bricklyfrontend.data.RetrofitClient
import com.example.bricklyfrontend.ui.theme.*
import kotlinx.coroutines.launch
import java.time.OffsetDateTime
import java.time.format.DateTimeFormatter
import java.util.Locale

private enum class SortMode(val label: String) {
    DATE_ASC("По дате \u2191"),
    DATE_DESC("По дате \u2193"),
    PRICE_ASC("Сначала дешёвые"),
    PRICE_DESC("Сначала дорогие"),
    FREE_FIRST("Бесплатные")
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CatalogScreen(
    onNavigateToMeetings: () -> Unit = {},
    onNavigateToProfile: () -> Unit = {},
    onNavigateToCart: () -> Unit = {},
    onNavigateToMeetingDetail: (Long) -> Unit = {}
) {
    val scope = rememberCoroutineScope()

    var meetings by remember { mutableStateOf<List<MeetingDefaultDTO>>(emptyList()) }
    var isLoading by remember { mutableStateOf(true) }
    var errorMessage by remember { mutableStateOf<String?>(null) }
    var searchQuery by remember { mutableStateOf("") }
    var sortMode by remember { mutableStateOf(SortMode.DATE_ASC) }

    fun loadMeetings() {
        scope.launch {
            isLoading = true
            errorMessage = null
            try {
                val response = RetrofitClient.api.getAllMeetings()
                if (response.isSuccessful) {
                    meetings = response.body() ?: emptyList()
                } else {
                    errorMessage = "Ошибка загрузки (${response.code()})"
                }
            } catch (e: Exception) {
                errorMessage = "Нет соединения с сервером"
            }
            isLoading = false
        }
    }

    LaunchedEffect(Unit) { loadMeetings() }

    val filtered = if (searchQuery.isBlank()) meetings
    else meetings.filter {
        it.address?.contains(searchQuery, ignoreCase = true) == true ||
                it.description?.contains(searchQuery, ignoreCase = true) == true ||
                it.type?.description?.contains(searchQuery, ignoreCase = true) == true
    }

    val sorted = when (sortMode) {
        SortMode.DATE_ASC -> filtered.sortedWith(compareBy(nullsLast()) { parseDateSafeCatalog(it.date) })
        SortMode.DATE_DESC -> filtered.sortedWith(compareByDescending(nullsLast()) { parseDateSafeCatalog(it.date) })
        SortMode.PRICE_ASC -> filtered.sortedBy { it.ticketPrice ?: 0 }
        SortMode.PRICE_DESC -> filtered.sortedByDescending { it.ticketPrice ?: 0 }
        SortMode.FREE_FIRST -> filtered.sortedBy { if (it.ticketPrice == null || it.ticketPrice == 0) 0 else 1 }
    }

    Scaffold(
        containerColor = Background,
        bottomBar = {
            BricklyBottomBar(currentRoute = "home", onNavigate = { route ->
                when (route) {
                    "meetings" -> onNavigateToMeetings()
                    "profile" -> onNavigateToProfile()
                    "cart" -> onNavigateToCart()
                }
            })
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
        ) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(bottomStart = 28.dp, bottomEnd = 28.dp))
                    .background(Accent)
                    .padding(horizontal = 20.dp)
                    .padding(top = 56.dp, bottom = 20.dp)
            ) {
                OutlinedTextField(
                    value = searchQuery,
                    onValueChange = { searchQuery = it },
                    modifier = Modifier.fillMaxWidth(),
                    placeholder = {
                        Text(
                            "Искать...",
                            color = TextSecondary,
                            style = MaterialTheme.typography.bodyLarge
                        )
                    },
                    leadingIcon = {
                        Icon(
                            imageVector = Icons.Outlined.Search,
                            contentDescription = null,
                            tint = TextSecondary,
                            modifier = Modifier.size(20.dp)
                        )
                    },
                    shape = RoundedCornerShape(16.dp),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = Color.Transparent,
                        unfocusedBorderColor = Color.Transparent,
                        focusedContainerColor = Color.White,
                        unfocusedContainerColor = Color.White.copy(alpha = 0.92f),
                        cursorColor = TextPrimary
                    ),
                    singleLine = true,
                    textStyle = MaterialTheme.typography.bodyLarge.copy(color = TextPrimary)
                )
            }

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .horizontalScroll(rememberScrollState())
                    .padding(horizontal = 16.dp, vertical = 12.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                SortMode.entries.forEach { mode ->
                    FilterChip(
                        selected = sortMode == mode,
                        onClick = { sortMode = mode },
                        label = {
                            Text(
                                mode.label,
                                style = MaterialTheme.typography.bodySmall.copy(
                                    fontWeight = if (sortMode == mode) FontWeight.Bold else FontWeight.Normal
                                )
                            )
                        },
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = Accent,
                            selectedLabelColor = TextPrimary,
                            containerColor = CardBackground,
                            labelColor = TextSecondary
                        ),
                        shape = RoundedCornerShape(12.dp),
                        border = FilterChipDefaults.filterChipBorder(
                            borderColor = Divider,
                            selectedBorderColor = Accent,
                            enabled = true,
                            selected = sortMode == mode
                        )
                    )
                }
            }

            when {
                isLoading -> {
                    Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        CircularProgressIndicator(color = Accent)
                    }
                }

                errorMessage != null -> {
                    Box(Modifier.fillMaxSize().padding(32.dp), contentAlignment = Alignment.Center) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Text(errorMessage!!, color = ErrorColor)
                            Spacer(Modifier.height(8.dp))
                            Button(
                                onClick = { loadMeetings() },
                                colors = ButtonDefaults.buttonColors(containerColor = Accent)
                            ) { Text("Повторить", color = TextPrimary) }
                        }
                    }
                }

                sorted.isEmpty() -> {
                    Box(Modifier.fillMaxSize().padding(32.dp), contentAlignment = Alignment.Center) {
                        Text(
                            "Ничего не найдено",
                            style = MaterialTheme.typography.bodyLarge,
                            color = TextSecondary
                        )
                    }
                }

                else -> {
                    LazyVerticalGrid(
                        columns = GridCells.Fixed(2),
                        contentPadding = PaddingValues(horizontal = 16.dp, vertical = 4.dp),
                        horizontalArrangement = Arrangement.spacedBy(12.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        items(sorted) { meeting ->
                            CatalogCard(
                                meeting = meeting,
                                onClick = { onNavigateToMeetingDetail(meeting.id) }
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun CatalogCard(meeting: MeetingDefaultDTO, onClick: () -> Unit) {
    val dateFormatted = meeting.date?.let {
        try {
            val dt = parseDateSafeCatalog(it)
            dt?.let { d ->
                val fmt = DateTimeFormatter.ofPattern("d MMM", Locale("ru"))
                d.format(fmt)
            }
        } catch (e: Exception) { null }
    }

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .aspectRatio(0.75f)
            .clickable(onClick = onClick),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = CardBackground),
        elevation = CardDefaults.cardElevation(0.dp)
    ) {
        Column {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f)
                    .background(Accent.copy(alpha = 0.08f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    Icons.Outlined.Event,
                    null,
                    tint = Accent.copy(alpha = 0.5f),
                    modifier = Modifier.size(36.dp)
                )
            }

            Column(modifier = Modifier.padding(12.dp)) {
                Text(
                    text = meeting.type?.description
                        ?: meeting.description?.take(20)
                        ?: "Мероприятие",
                    style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.SemiBold),
                    color = TextPrimary,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Spacer(Modifier.height(4.dp))

                if (dateFormatted != null) {
                    Text(
                        dateFormatted,
                        style = MaterialTheme.typography.bodySmall,
                        color = TextSecondary
                    )
                }

                Text(
                    text = if (meeting.ticketPrice != null && meeting.ticketPrice > 0)
                        "${meeting.ticketPrice} \u20BD"
                    else "Бесплатно",
                    style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.Bold),
                    color = TextPrimary
                )
            }
        }
    }
}

private fun parseDateSafeCatalog(dateStr: String?): OffsetDateTime? {
    if (dateStr.isNullOrBlank()) return null
    return try {
        OffsetDateTime.parse(dateStr)
    } catch (e: Exception) {
        try {
            val local = java.time.LocalDateTime.parse(dateStr)
            local.atOffset(java.time.ZoneOffset.UTC)
        } catch (e2: Exception) { null }
    }
}
