package com.example.bricklyfrontend.screens

import android.app.Activity
import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.*
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.example.bricklyfrontend.ui.theme.*
import java.io.ByteArrayOutputStream

@Composable
fun SetStatusBarColor(color: Color) {
    val view = LocalView.current
    if (!view.isInEditMode) {
        SideEffect {
            val window = (view.context as Activity).window
            window.statusBarColor = color.toArgb()
        }
    }
}

@Composable
fun BricklyTextField(
    value: String,
    onValueChange: (String) -> Unit,
    label: String,
    placeholder: String,
    keyboardType: KeyboardType = KeyboardType.Text,
    isPassword: Boolean = false,
    passwordVisible: Boolean = false,
    onTogglePassword: (() -> Unit)? = null
) {
    val visualTransformation = if (isPassword && !passwordVisible)
        PasswordVisualTransformation() else VisualTransformation.None

    Column(modifier = Modifier.fillMaxWidth()) {
        Text(
            text = label,
            style = MaterialTheme.typography.labelLarge,
            color = TextPrimary
        )
        Spacer(Modifier.height(6.dp))
        OutlinedTextField(
            value = value,
            onValueChange = onValueChange,
            modifier = Modifier.fillMaxWidth(),
            placeholder = {
                Text(
                    text = placeholder,
                    style = MaterialTheme.typography.bodyLarge,
                    color = TextSecondary.copy(alpha = 0.4f)
                )
            },
            visualTransformation = visualTransformation,
            keyboardOptions = KeyboardOptions(keyboardType = keyboardType),
            trailingIcon = if (isPassword && onTogglePassword != null) {
                {
                    IconButton(onClick = onTogglePassword) {
                        Icon(
                            imageVector = if (passwordVisible) Icons.Default.VisibilityOff
                            else Icons.Default.Visibility,
                            contentDescription = null,
                            tint = TextSecondary
                        )
                    }
                }
            } else null,
            shape = RoundedCornerShape(14.dp),
            colors = OutlinedTextFieldDefaults.colors(
                focusedBorderColor = Accent,
                unfocusedBorderColor = Divider,
                focusedContainerColor = Color.White,
                unfocusedContainerColor = Color.White,
                cursorColor = Accent
            ),
            singleLine = true,
            textStyle = MaterialTheme.typography.bodyLarge.copy(color = TextPrimary)
        )
    }
}

@Composable
fun EmptyStateBox(
    icon: ImageVector,
    title: String,
    subtitle: String,
    onRefresh: () -> Unit,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier.padding(32.dp),
        contentAlignment = Alignment.Center
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Box(
                modifier = Modifier
                    .size(80.dp)
                    .clip(RoundedCornerShape(24.dp))
                    .background(Accent.copy(alpha = 0.15f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(icon, null, tint = Accent, modifier = Modifier.size(36.dp))
            }
            Spacer(Modifier.height(16.dp))
            androidx.compose.material3.Text(
                title,
                style = androidx.compose.material3.MaterialTheme.typography.bodyLarge.copy(fontWeight = FontWeight.Bold),
                color = TextPrimary
            )
            Spacer(Modifier.height(6.dp))
            androidx.compose.material3.Text(
                subtitle,
                style = androidx.compose.material3.MaterialTheme.typography.bodyMedium,
                color = TextSecondary,
                textAlign = TextAlign.Center
            )
            Spacer(Modifier.height(16.dp))
            androidx.compose.material3.TextButton(onClick = onRefresh) {
                androidx.compose.material3.Text("Обновить", color = TextSecondary)
            }
        }
    }
}

data class BottomNavItem(
    val route: String,
    val icon: ImageVector,
    val label: String
)

@Composable
fun BricklyBottomBar(
    currentRoute: String,
    onNavigate: (String) -> Unit = {},
    onScanClick: (() -> Unit)? = null
) {
    val items = listOf(
        BottomNavItem("meetings", Icons.Outlined.Language, "Сходки"),
        BottomNavItem("home", Icons.Outlined.Storefront, "Маркет"),
        BottomNavItem("scan", Icons.Outlined.DocumentScanner, "Сканер"),
        BottomNavItem("chats", Icons.Outlined.Chat, "Чаты"),
        BottomNavItem("profile", Icons.Outlined.Person, "Профиль"),
    )

    val accentColor = Accent

    NavigationBar(
        containerColor = Color.White,
        tonalElevation = 0.dp
    ) {
        items.forEach { item ->
            if (item.route == "scan") {
                NavigationBarItem(
                    selected = currentRoute.startsWith("brickognize"),
                    onClick = { onScanClick?.invoke() ?: onNavigate("brickognize") },
                    icon = {
                        Box(
                            modifier = Modifier
                                .size(44.dp)
                                .clip(CircleShape)
                                .background(Accent),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Outlined.DocumentScanner,
                                contentDescription = "Сканер",
                                tint = Color.White,
                                modifier = Modifier.size(22.dp)
                            )
                        }
                    },
                    label = {
                        Text(
                            text = "Сканер",
                            style = MaterialTheme.typography.labelSmall,
                            color = if (currentRoute == "brickognize") TextPrimary else IconInactive
                        )
                    },
                    colors = NavigationBarItemDefaults.colors(
                        selectedIconColor = TextPrimary,
                        selectedTextColor = TextPrimary,
                        unselectedIconColor = IconInactive,
                        unselectedTextColor = IconInactive,
                        indicatorColor = Color.Transparent
                    )
                )
            } else {
                val selected = currentRoute == item.route
                NavigationBarItem(
                    selected = selected,
                    onClick = { onNavigate(item.route) },
                    icon = {
                        Icon(
                            imageVector = item.icon,
                            contentDescription = item.label,
                            modifier = Modifier.size(24.dp)
                        )
                    },
                    label = {
                        Text(
                            text = item.label,
                            style = MaterialTheme.typography.labelSmall
                        )
                    },
                    modifier = if (selected) {
                        Modifier.drawBehind {
                            val strokeWidth = 3.dp.toPx()
                            val y = size.height - strokeWidth / 2
                            val paddingPx = 16.dp.toPx()
                            drawLine(
                                color = accentColor,
                                start = Offset(paddingPx, y),
                                end = Offset(size.width - paddingPx, y),
                                strokeWidth = strokeWidth,
                                cap = StrokeCap.Round
                            )
                        }
                    } else Modifier,
                    colors = NavigationBarItemDefaults.colors(
                        selectedIconColor = TextPrimary,
                        selectedTextColor = TextPrimary,
                        unselectedIconColor = IconInactive,
                        unselectedTextColor = IconInactive,
                        indicatorColor = Color.Transparent
                    )
                )
            }
        }
    }
}

fun compressImageBytes(context: Context, uri: Uri): ByteArray {
    val inputStream = context.contentResolver.openInputStream(uri)
        ?: throw IllegalArgumentException("Cannot open URI")
    val bitmap = BitmapFactory.decodeStream(inputStream)
    inputStream.close()
    var quality = 90
    val baos = ByteArrayOutputStream()
    do {
        baos.reset()
        bitmap.compress(Bitmap.CompressFormat.JPEG, quality, baos)
        quality -= 10
    } while (baos.size() > 1_000_000 && quality > 10)
    bitmap.recycle()
    return baos.toByteArray()
}
