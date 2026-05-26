package com.example.bricklyfrontend.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.bricklyfrontend.ui.theme.*
import androidx.compose.ui.graphics.Color
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Remove
import java.time.OffsetDateTime
import java.time.format.DateTimeFormatter
import java.util.Locale

@Composable
fun ListingCartScreen(
    onBack: () -> Unit
) {
    val items = ListingCartState.items

    Scaffold(
        containerColor = Background,
        topBar = {
            Surface(
                color = Accent,
                shape = RoundedCornerShape(bottomStart = 28.dp, bottomEnd = 28.dp),
                modifier = Modifier
                    .fillMaxWidth()
            ) {
                Box(modifier = Modifier.statusBarsPadding().padding(horizontal = 8.dp, vertical = 12.dp)) {
                    IconButton(onClick = onBack, modifier = Modifier.align(Alignment.CenterStart)) {
                        Icon(Icons.Outlined.ArrowBackIosNew, "Назад", tint = TextPrimary)
                    }
                    Text(
                        text = "Корзина",
                        color = Color(0xFF1A1A1A),
                        fontWeight = FontWeight.Bold,
                        fontSize = 18.sp,
                        modifier = Modifier.align(Alignment.Center)
                    )
                }
            }
        }
    ) { padding ->
        if (items.isEmpty()) {
            Box(Modifier.fillMaxSize().padding(padding), contentAlignment = Alignment.Center) {
                Text("Корзина пуста", style = MaterialTheme.typography.bodyLarge, color = TextPrimary)
            }
        } else {
            Column(modifier = Modifier.fillMaxSize().padding(padding)) {
                LazyColumn(modifier = Modifier.weight(1f)) {
                    items(items, key = { it.listingId }) { item ->
                        Card(modifier = Modifier.fillMaxWidth().padding(12.dp), shape = RoundedCornerShape(16.dp)) {
                            Row(modifier = Modifier.padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(item.title, style = MaterialTheme.typography.bodyLarge.copy(fontWeight = FontWeight.SemiBold), color = TextPrimary)
                                    Spacer(Modifier.height(4.dp))
                                    Text("${item.quantity} × ${item.unitPrice} ₽", style = MaterialTheme.typography.bodySmall, color = TextSecondary)
                                }
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    IconButton(onClick = { ListingCartState.decrementQuantity(item.listingId) }) {
                                        Icon(Icons.Outlined.Remove, null)
                                    }
                                    Text("${item.quantity}", modifier = Modifier.padding(horizontal = 8.dp))
                                    IconButton(onClick = { ListingCartState.incrementQuantity(item.listingId) }) {
                                        Icon(Icons.Outlined.Add, null)
                                    }
                                    IconButton(onClick = { ListingCartState.removeItem(item.listingId) }) {
                                        Icon(Icons.Outlined.Close, null, tint = ErrorColor)
                                    }
                                }
                            }
                        }
                    }
                }

                Card(modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(topStart = 20.dp, topEnd = 20.dp), colors = CardDefaults.cardColors(containerColor = CardBackground)) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Text("Итого: ${ListingCartState.totalPrice()} ₽", style = MaterialTheme.typography.titleMedium, color = TextPrimary)
                        Spacer(Modifier.height(8.dp))
                        Button(onClick = {}, modifier = Modifier.fillMaxWidth()) { Text("Оформить заказ") }
                    }
                }
            }
        }
    }
}