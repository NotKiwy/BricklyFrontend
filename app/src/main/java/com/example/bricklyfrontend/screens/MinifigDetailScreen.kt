package com.example.bricklyfrontend.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.example.bricklyfrontend.data.MinifigDTO
import com.example.bricklyfrontend.data.RetrofitClient
import com.example.bricklyfrontend.ui.theme.*

@Composable
fun MinifigDetailScreen(
    blId: String,
    onBack: () -> Unit,
    onNavigateToMeetings: () -> Unit = {},
    onNavigateToProfile: () -> Unit = {},
    onNavigateToCart: () -> Unit = {},
    onNavigateToBrickognize: () -> Unit = {},
    onNavigateToListings: (String) -> Unit = {}
) {
    SetStatusBarColor(Accent)

    var minifig by remember { mutableStateOf<MinifigDTO?>(null) }
    var isLoading by remember { mutableStateOf(true) }
    var errorMessage by remember { mutableStateOf<String?>(null) }

    LaunchedEffect(blId) {
        try {
            val response = RetrofitClient.api.getMinifigByBlId(blId)
            if (response.isSuccessful) {
                minifig = response.body()?.firstOrNull()
                if (minifig == null) errorMessage = "Минифигурка не найдена"
            } else {
                errorMessage = "Ошибка загрузки (${response.code()})"
            }
        } catch (e: Exception) {
            errorMessage = "Нет соединения с сервером"
        }
        isLoading = false
    }

    Scaffold(
        containerColor = Background,
        bottomBar = {
            BricklyBottomBar(currentRoute = "home", onNavigate = { route ->
                when (route) {
                    "profile" -> onNavigateToProfile()
                    "cart" -> onNavigateToCart()
                    "meetings" -> onNavigateToMeetings()
                    "brickognize" -> onNavigateToBrickognize()
                }
            }, onScanClick = onNavigateToBrickognize)
        },
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
                    text = "Информация о минифигурке",
                    color = Color(0xFF1A1A1A),
                    fontWeight = FontWeight.Bold,
                    fontSize = 18.sp,
                    modifier = Modifier.align(Alignment.Center)
                )
            }
        }
    ) { padding ->
        when {
            isLoading -> DetailPageSkeleton()

            errorMessage != null -> Box(
                Modifier.fillMaxSize().padding(32.dp),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    errorMessage!!,
                    color = ErrorColor,
                    textAlign = TextAlign.Center
                )
            }

            minifig != null -> {
                val m = minifig!!
                val imageUrl = m.blMinifig?.imageUrl?.takeIf { it.isNotBlank() }
                    ?: m.imageUrl?.takeIf { it.isNotBlank() }

                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(padding)
                        .verticalScroll(rememberScrollState())
                ) {
                    Spacer(Modifier.height(16.dp))

                    if (!imageUrl.isNullOrBlank()) {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 20.dp)
                                .height(260.dp)
                                .clip(RoundedCornerShape(24.dp))
                                .background(Color.White)
                        ) {
                            AsyncImage(
                                model = imageUrl,
                                contentDescription = m.name,
                                modifier = Modifier
                                    .fillMaxSize()
                                    .padding(8.dp)           // внутренние отступы
                                    .clip(RoundedCornerShape(16.dp)),
                                contentScale = ContentScale.Fit
                            )
                        }
                    } else {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(260.dp)
                                .padding(horizontal = 20.dp)
                                .clip(RoundedCornerShape(24.dp))
                                .background(Accent.copy(alpha = 0.12f)),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                Icons.Outlined.Image,
                                null,
                                tint = Accent,
                                modifier = Modifier.size(64.dp)
                            )
                        }
                    }

                    Spacer(Modifier.height(20.dp))

                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 20.dp),
                        shape = RoundedCornerShape(24.dp),
                        colors = CardDefaults.cardColors(containerColor = CardBackground),
                        elevation = CardDefaults.cardElevation(0.dp)
                    ) {
                        Column(modifier = Modifier.padding(20.dp)) {
                            Text(
                                text = m.blMinifig?.name ?: m.name ?: m.id,
                                style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.ExtraBold),
                                color = TextPrimary
                            )

                            Spacer(Modifier.height(16.dp))

                            MinifigInfoRow(label = "BrickLink ID", value = m.blMinifig?.id ?: blId)
                            Spacer(Modifier.height(10.dp))

                            MinifigInfoRow(label = "Rebrickable ID", value = m.id)
                            Spacer(Modifier.height(10.dp))

                            MinifigInfoRow(
                                label = "Категория",
                                value = m.blMinifig?.categoryName ?: "Не указана"
                            )
                            Spacer(Modifier.height(10.dp))

                            MinifigInfoRow(
                                label = "Количество деталей",
                                value = m.numParts?.toString() ?: "Не указано"
                            )
                        }
                    }

                    Spacer(Modifier.height(16.dp))

                    Button(
                        onClick = { onNavigateToListings(m.blMinifig?.id ?: blId) },
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 20.dp)
                            .height(52.dp),
                        shape = RoundedCornerShape(16.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = Accent, contentColor = TextPrimary)
                    ) {
                        Icon(Icons.Outlined.Storefront, null, modifier = Modifier.size(18.dp))
                        Spacer(Modifier.width(8.dp))
                        Text("Просмотреть объявления", fontWeight = FontWeight.SemiBold)
                    }

                    Spacer(Modifier.height(32.dp))
                }
            }
        }
    }
}

@Composable
private fun MinifigInfoRow(label: String, value: String) {
    Column {
        Text(
            label,
            style = MaterialTheme.typography.labelSmall,
            color = TextSecondary
        )
        Text(
            value,
            style = MaterialTheme.typography.bodyLarge.copy(fontWeight = FontWeight.SemiBold),
            color = TextPrimary
        )
    }
}
