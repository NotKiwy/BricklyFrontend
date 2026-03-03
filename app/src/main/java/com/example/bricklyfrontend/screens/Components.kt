package com.example.bricklyfrontend.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.input.*
import androidx.compose.ui.unit.dp
import com.example.bricklyfrontend.ui.theme.*

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
                    color = TextSecondary
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

data class BottomNavItem(
    val route: String,
    val icon: ImageVector,
    val label: String
)

@Composable
fun BricklyBottomBar(currentRoute: String, onNavigate: (String) -> Unit = {}) {
    val items = listOf(
        BottomNavItem("meetings", Icons.Outlined.Language, "Сходки"),
        BottomNavItem("home", Icons.Outlined.Home, "Главная"),
        BottomNavItem("cart", Icons.Outlined.ShoppingBag, "Корзина"),
        BottomNavItem("profile", Icons.Outlined.Person, "Профиль"),
    )

    val accentColor = Accent

    NavigationBar(
        containerColor = Color.White,
        tonalElevation = 0.dp
    ) {
        items.forEach { item ->
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
