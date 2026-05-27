package com.example.bricklyfrontend.screens

import androidx.compose.animation.core.*
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Check
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.bricklyfrontend.ui.theme.*
import kotlin.math.sin
import kotlin.random.Random

private data class ConfettiParticle(
    val xFraction: Float,
    val color: Color,
    val width: Float,
    val height: Float,
    val startProgress: Float,
    val fallDuration: Float,
    val rotationTotal: Float,
    val xWobble: Float
)

private val confettiColors = listOf(
    Color(0xFFFDDD0C),
    Color(0xFFFF6B6B),
    Color(0xFF4ECDC4),
    Color(0xFF45B7D1),
    Color(0xFF96CEB4),
    Color(0xFFFF8B94),
    Color(0xFFA8E6CF),
    Color(0xFFFFD93D),
    Color(0xFF6C5CE7),
    Color(0xFFFF7675),
    Color(0xFF00B894),
    Color(0xFFE17055)
)

private val particles = List(60) {
    ConfettiParticle(
        xFraction = Random.nextFloat(),
        color = confettiColors[Random.nextInt(confettiColors.size)],
        width = Random.nextFloat() * 14f + 12f,
        height = Random.nextFloat() * 8f + 7f,
        startProgress = Random.nextFloat() * 0.35f,
        fallDuration = Random.nextFloat() * 0.3f + 0.55f,
        rotationTotal = Random.nextFloat() * 540f + 180f,
        xWobble = Random.nextFloat() * 50f - 25f
    )
}

@Composable
fun PaymentSuccessScreen(
    totalPrice: Int,
    onGoHome: () -> Unit,
    title: String = "Оплата прошла успешно!",
    subtitle: String = "Ваш заказ оформлен",
    buttonLabel: String = "На главную"
) {
    SetStatusBarColor(Background)

    val confettiProgress = remember { Animatable(0f) }

    LaunchedEffect(Unit) {
        confettiProgress.animateTo(
            targetValue = 1f,
            animationSpec = tween(durationMillis = 3800, easing = LinearEasing)
        )
    }

    var checkVisible by remember { mutableStateOf(false) }
    val checkScale by animateFloatAsState(
        targetValue = if (checkVisible) 1f else 0f,
        animationSpec = spring(dampingRatio = 0.45f, stiffness = 220f),
        label = "checkScale"
    )

    LaunchedEffect(Unit) { checkVisible = true }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Background)
    ) {
        val progress = confettiProgress.value

        Canvas(modifier = Modifier.fillMaxSize()) {
            val w = size.width
            val h = size.height

            particles.forEach { p ->
                val particleProgress = ((progress - p.startProgress) / p.fallDuration)
                    .coerceIn(0f, 1f)

                if (particleProgress <= 0f) return@forEach

                val y = -24f + particleProgress * (h + 48f)
                val x = p.xFraction * w + sin(particleProgress * Math.PI.toFloat() * 3f) * p.xWobble
                val rotation = particleProgress * p.rotationTotal

                rotate(rotation, pivot = Offset(x, y)) {
                    drawRect(
                        color = p.color,
                        topLeft = Offset(x - p.width / 2f, y - p.height / 2f),
                        size = Size(p.width, p.height)
                    )
                }
            }
        }

        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(32.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Box(
                modifier = Modifier
                    .size(100.dp)
                    .scale(checkScale)
                    .clip(CircleShape)
                    .background(Accent),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    Icons.Outlined.Check,
                    contentDescription = null,
                    tint = TextPrimary,
                    modifier = Modifier.size(52.dp)
                )
            }

            Spacer(Modifier.height(28.dp))

            Text(
                title,
                fontSize = 24.sp,
                fontWeight = FontWeight.ExtraBold,
                color = TextPrimary
            )

            Spacer(Modifier.height(8.dp))

            Text(
                subtitle,
                fontSize = 15.sp,
                color = TextSecondary
            )

            Spacer(Modifier.height(6.dp))

            Text(
                "$totalPrice ₽",
                fontSize = 32.sp,
                fontWeight = FontWeight.ExtraBold,
                color = TextPrimary
            )

            Spacer(Modifier.height(40.dp))

            Button(
                onClick = onGoHome,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(56.dp),
                shape = RoundedCornerShape(16.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = TextPrimary,
                    contentColor = Accent
                )
            ) {
                Text(buttonLabel, fontWeight = FontWeight.Bold, fontSize = 16.sp)
            }
        }
    }
}
