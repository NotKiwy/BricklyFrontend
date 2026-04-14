package com.example.bricklyfrontend.screens

import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.scale
import androidx.compose.ui.unit.dp
import com.example.bricklyfrontend.ui.theme.Accent
import kotlinx.coroutines.delay
import androidx.compose.ui.graphics.Color
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.ui.draw.clip
import androidx.compose.material3.Text
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp
import com.example.bricklyfrontend.ui.theme.TextPrimary

@Composable
fun SplashScreen(onFinished: () -> Unit) {
    val scale = remember { Animatable(0.6f) }

    LaunchedEffect(Unit) {
        scale.animateTo(
            targetValue = 1f,
            animationSpec = spring(
                dampingRatio = Spring.DampingRatioMediumBouncy,
                stiffness = Spring.StiffnessMedium
            )
        )
        delay(300L)
        onFinished()
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Accent),
        contentAlignment = Alignment.Center
    ) {
        BricklyMascot(modifier = Modifier.scale(scale.value))
    }
}

@Composable
fun BricklyMascot(modifier: Modifier = Modifier) {
    Box(
        modifier = modifier.size(120.dp),
        contentAlignment = Alignment.Center
    ) {
        Box(
            modifier = Modifier
                .width(100.dp)
                .height(80.dp)
                .clip(androidx.compose.foundation.shape.RoundedCornerShape(18.dp))
                .background(Color(0xFF1A1A1A)),
            contentAlignment = Alignment.Center
        ) {
            Row(
                modifier = Modifier
                    .padding(bottom = 8.dp)
                    .fillMaxWidth(),
                horizontalArrangement = Arrangement.Center,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Spacer(Modifier.width(12.dp))
                Box(
                    modifier = Modifier
                        .size(16.dp)
                        .clip(CircleShape)
                        .background(Accent)
                )
                Spacer(Modifier.width(20.dp))
                Box(
                    modifier = Modifier
                        .size(16.dp)
                        .clip(CircleShape)
                        .background(Accent)
                )
                Spacer(Modifier.width(12.dp))
            }
        }

        Row(
            modifier = Modifier
                .align(Alignment.TopCenter)
                .offset(y = (-12).dp),
            horizontalArrangement = Arrangement.spacedBy(18.dp)
        ) {
            repeat(3) {
                Box(
                    modifier = Modifier
                        .size(width = 22.dp, height = 14.dp)
                        .clip(androidx.compose.foundation.shape.RoundedCornerShape(topStart = 8.dp, topEnd = 8.dp, bottomStart = 4.dp, bottomEnd = 4.dp))
                        .background(Color(0xFF1A1A1A))
                )
            }
        }
    }
}
