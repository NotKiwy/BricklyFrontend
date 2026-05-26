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
import androidx.compose.foundation.Image
import androidx.compose.ui.res.painterResource
import com.example.bricklyfrontend.R

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
    Image(
        painter = painterResource(id = R.drawable.brickly_logo),
        contentDescription = "Logo",
        modifier = modifier.size(250.dp)
    )
}
