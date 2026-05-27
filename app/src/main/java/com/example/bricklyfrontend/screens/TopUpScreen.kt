package com.example.bricklyfrontend.screens

import android.app.Activity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.ArrowBackIosNew
import androidx.compose.material.icons.outlined.AccountBalanceWallet
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.bricklyfrontend.BuildConfig
import com.example.bricklyfrontend.ui.theme.*
import ru.yoomoney.sdk.kassa.payments.Checkout
import ru.yoomoney.sdk.kassa.payments.checkoutParameters.Amount
import ru.yoomoney.sdk.kassa.payments.checkoutParameters.PaymentMethodType
import ru.yoomoney.sdk.kassa.payments.checkoutParameters.PaymentParameters
import ru.yoomoney.sdk.kassa.payments.checkoutParameters.SavePaymentMethod
import java.math.BigDecimal
import java.util.Currency
import java.util.Locale

@Composable
fun TopUpScreen(
    onBack: () -> Unit,
    onSuccess: (Int) -> Unit = {}
) {
    SetStatusBarColor(Accent)
    val context = LocalContext.current

    var amountInput by remember { mutableStateOf("") }
    var errorMessage by remember { mutableStateOf<String?>(null) }

    val presets = listOf(500, 1000, 2000, 5000)

    val tokenizeLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.StartActivityForResult()
    ) { result ->
        when (result.resultCode) {
            Activity.RESULT_OK -> {
                val data = result.data
                if (data != null) {
                    val tokenResult = Checkout.createTokenizationResult(data)
                    android.util.Log.d("BRICKLY_PAYMENT", "token=${tokenResult.paymentToken} method=${tokenResult.paymentMethodType}")
                    onSuccess(amountInput.toIntOrNull() ?: 0)
                }
            }
            Activity.RESULT_CANCELED -> {}
        }
    }

    fun pay() {
        val amount = amountInput.toIntOrNull()
        if (amount == null || amount <= 0) {
            errorMessage = "Введите сумму пополнения"
            return
        }
        if (amount < 10) {
            errorMessage = "Минимальная сумма — 10 ₽"
            return
        }
        errorMessage = null

        val paymentParameters = PaymentParameters(
            amount = Amount(BigDecimal.valueOf(amount.toLong()), Currency.getInstance("RUB")),
            title = "Пополнение баланса Brickly",
            subtitle = "$amount ₽",
            clientApplicationKey = BuildConfig.YOOKASSA_CLIENT_KEY,
            shopId = BuildConfig.YOOKASSA_SHOP_ID,
            savePaymentMethod = SavePaymentMethod.OFF,
            paymentMethodTypes = setOf(PaymentMethodType.BANK_CARD, PaymentMethodType.SBP)
        )

        val ruConfig = android.content.res.Configuration(context.resources.configuration)
            .also { it.setLocale(Locale("ru")) }
        val ruContext = context.createConfigurationContext(ruConfig)
        val intent = Checkout.createTokenizeIntent(ruContext, paymentParameters)
        tokenizeLauncher.launch(intent)
    }

    Scaffold(
        containerColor = Background,
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
                    Icon(Icons.Outlined.ArrowBackIosNew, null, tint = TextPrimary)
                }
                Text(
                    "Пополнение баланса",
                    color = TextPrimary,
                    fontWeight = FontWeight.Bold,
                    fontSize = 18.sp,
                    modifier = Modifier.align(Alignment.Center)
                )
            }
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(horizontal = 20.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Spacer(Modifier.height(32.dp))

            Box(
                modifier = Modifier
                    .size(80.dp)
                    .clip(RoundedCornerShape(24.dp))
                    .background(Accent.copy(alpha = 0.15f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(Icons.Outlined.AccountBalanceWallet, null, tint = Accent, modifier = Modifier.size(36.dp))
            }

            Spacer(Modifier.height(20.dp))

            Text("Сколько хотите пополнить?", fontSize = 18.sp, fontWeight = FontWeight.Bold, color = TextPrimary)
            Spacer(Modifier.height(4.dp))
            Text("Средства зачислятся мгновенно", fontSize = 13.sp, color = TextSecondary)

            Spacer(Modifier.height(24.dp))

            Column(modifier = Modifier.fillMaxWidth()) {
                Text(
                    "СУММА",
                    fontSize = 10.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = TextPrimary.copy(alpha = 0.5f),
                    letterSpacing = 0.6.sp
                )
                Spacer(Modifier.height(4.dp))
                OutlinedTextField(
                    value = amountInput,
                    onValueChange = { amountInput = it; errorMessage = null },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true,
                    suffix = { Text("₽", color = TextSecondary, fontSize = 18.sp) },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    shape = RoundedCornerShape(14.dp),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = Accent,
                        unfocusedBorderColor = Divider,
                        focusedContainerColor = Color.White,
                        unfocusedContainerColor = Color.White,
                        cursorColor = Accent
                    ),
                    textStyle = MaterialTheme.typography.headlineSmall.copy(
                        fontWeight = FontWeight.ExtraBold,
                        color = TextPrimary
                    )
                )
            }

            Spacer(Modifier.height(12.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                presets.forEach { preset ->
                    val selected = amountInput == preset.toString()
                    OutlinedButton(
                        onClick = { amountInput = preset.toString(); errorMessage = null },
                        modifier = Modifier.height(40.dp),
                        shape = RoundedCornerShape(12.dp),
                        contentPadding = PaddingValues(horizontal = 16.dp),
                        colors = ButtonDefaults.outlinedButtonColors(
                            containerColor = if (selected) Accent else Color.Transparent,
                            contentColor = TextPrimary
                        ),
                        border = ButtonDefaults.outlinedButtonBorder(enabled = true).copy(
                            width = if (selected) 0.dp else 1.dp
                        )
                    ) {
                        Text(
                            "$preset ₽",
                            fontSize = 13.sp,
                            fontWeight = if (selected) FontWeight.SemiBold else FontWeight.Normal
                        )
                    }
                }
            }

            if (errorMessage != null) {
                Spacer(Modifier.height(10.dp))
                Text(errorMessage!!, color = ErrorColor, fontSize = 13.sp)
            }

            Spacer(Modifier.weight(1f))

            Button(
                onClick = { pay() },
                modifier = Modifier.fillMaxWidth().height(56.dp),
                shape = RoundedCornerShape(16.dp),
                colors = ButtonDefaults.buttonColors(containerColor = Accent, contentColor = TextPrimary),
                enabled = amountInput.isNotBlank()
            ) {
                Text(
                    if (amountInput.isNotBlank() && amountInput.toIntOrNull() != null)
                        "Пополнить ${amountInput.toIntOrNull()} ₽"
                    else "Пополнить",
                    fontWeight = FontWeight.Bold,
                    fontSize = 16.sp
                )
            }

            Spacer(Modifier.height(32.dp))
        }
    }
}
