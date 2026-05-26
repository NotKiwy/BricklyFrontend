package com.example.bricklyfrontend.screens

import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.example.bricklyfrontend.ui.theme.CardBackground

@Composable
fun shimmerBrush(): Brush {
    val transition = rememberInfiniteTransition(label = "shimmer")
    val x by transition.animateFloat(
        initialValue = 0f,
        targetValue = 1400f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 900, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "shimmer"
    )
    return Brush.linearGradient(
        colors = listOf(Color(0xFFE4E4E4), Color(0xFFF6F6F6), Color(0xFFE4E4E4)),
        start = Offset(x - 500f, 0f),
        end = Offset(x, 0f)
    )
}

// --- Listing grid skeleton (CatalogScreen) ---

@Composable
fun ListingGridSkeleton(modifier: Modifier = Modifier) {
    val brush = shimmerBrush()
    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp, vertical = 16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        repeat(3) {
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                ListingCardSkeleton(Modifier.weight(1f), brush)
                ListingCardSkeleton(Modifier.weight(1f), brush)
            }
        }
    }
}

@Composable
private fun ListingCardSkeleton(modifier: Modifier, brush: Brush) {
    Card(
        modifier = modifier,
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = CardBackground),
        elevation = CardDefaults.cardElevation(0.dp)
    ) {
        Column {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .aspectRatio(1f)
                    .background(brush)
            )
            Column(modifier = Modifier.padding(12.dp)) {
                Box(
                    Modifier
                        .fillMaxWidth(0.55f)
                        .height(14.dp)
                        .clip(RoundedCornerShape(6.dp))
                        .background(brush)
                )
                Spacer(Modifier.height(8.dp))
                Box(
                    Modifier
                        .fillMaxWidth(0.4f)
                        .height(18.dp)
                        .clip(RoundedCornerShape(6.dp))
                        .background(brush)
                )
                Spacer(Modifier.height(6.dp))
                Box(
                    Modifier
                        .fillMaxWidth(0.6f)
                        .height(10.dp)
                        .clip(RoundedCornerShape(6.dp))
                        .background(brush)
                )
            }
        }
    }
}

// --- Meeting list skeleton (MeetingsScreen) ---

@Composable
fun MeetingListSkeleton() {
    val brush = shimmerBrush()
    Column(modifier = Modifier.fillMaxWidth()) {
        Box(
            Modifier
                .padding(horizontal = 20.dp)
                .width(180.dp)
                .height(22.dp)
                .clip(RoundedCornerShape(8.dp))
                .background(brush)
        )
        Spacer(Modifier.height(14.dp))
        repeat(3) {
            MeetingCardSkeleton(brush)
            Spacer(Modifier.height(12.dp))
        }
    }
}

@Composable
private fun MeetingCardSkeleton(brush: Brush) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 20.dp),
        shape = RoundedCornerShape(24.dp),
        colors = CardDefaults.cardColors(containerColor = CardBackground),
        elevation = CardDefaults.cardElevation(0.dp)
    ) {
        Column {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .aspectRatio(16f / 9f)
                    .background(brush)
            )
            Column(modifier = Modifier.padding(horizontal = 16.dp, vertical = 14.dp)) {
                Box(
                    Modifier
                        .fillMaxWidth(0.65f)
                        .height(16.dp)
                        .clip(RoundedCornerShape(6.dp))
                        .background(brush)
                )
                Spacer(Modifier.height(10.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Box(
                        Modifier
                            .width(100.dp)
                            .height(28.dp)
                            .clip(RoundedCornerShape(8.dp))
                            .background(brush)
                    )
                    Box(
                        Modifier
                            .width(80.dp)
                            .height(28.dp)
                            .clip(RoundedCornerShape(8.dp))
                            .background(brush)
                    )
                }
            }
        }
    }
}

// --- Detail page skeleton (ListingDetail / SetDetail / MinifigDetail / MeetingDetail) ---

@Composable
fun DetailPageSkeleton() {
    val brush = shimmerBrush()
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(top = 16.dp)
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(300.dp)
                .padding(horizontal = 20.dp)
                .clip(RoundedCornerShape(24.dp))
                .background(brush)
        )
        Spacer(Modifier.height(16.dp))
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp),
            shape = RoundedCornerShape(24.dp),
            colors = CardDefaults.cardColors(containerColor = CardBackground),
            elevation = CardDefaults.cardElevation(0.dp)
        ) {
            Column(modifier = Modifier.padding(20.dp)) {
                Box(
                    Modifier
                        .fillMaxWidth(0.6f)
                        .height(22.dp)
                        .clip(RoundedCornerShape(8.dp))
                        .background(brush)
                )
                Spacer(Modifier.height(20.dp))
                repeat(3) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Box(
                            Modifier
                                .size(36.dp)
                                .clip(RoundedCornerShape(10.dp))
                                .background(brush)
                        )
                        Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                            Box(
                                Modifier
                                    .width(60.dp)
                                    .height(10.dp)
                                    .clip(RoundedCornerShape(4.dp))
                                    .background(brush)
                            )
                            Box(
                                Modifier
                                    .width(120.dp)
                                    .height(14.dp)
                                    .clip(RoundedCornerShape(4.dp))
                                    .background(brush)
                            )
                        }
                    }
                    if (it < 2) Spacer(Modifier.height(14.dp))
                }
            }
        }
        Spacer(Modifier.height(12.dp))
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(80.dp)
                .padding(horizontal = 20.dp)
                .clip(RoundedCornerShape(24.dp))
                .background(brush)
        )
    }
}

// --- Feedback list skeleton (FeedbacksScreen) ---

@Composable
fun FeedbackListSkeleton() {
    val brush = shimmerBrush()
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 20.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        repeat(4) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(90.dp)
                    .clip(RoundedCornerShape(20.dp))
                    .background(brush)
            )
        }
    }
}

// --- Profile name skeleton ---

@Composable
fun ProfileNameSkeleton() {
    val brush = shimmerBrush()
    Box(
        modifier = Modifier
            .width(130.dp)
            .height(22.dp)
            .clip(RoundedCornerShape(8.dp))
            .background(brush)
    )
}
