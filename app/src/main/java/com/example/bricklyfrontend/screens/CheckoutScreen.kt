package com.example.bricklyfrontend.screens

import android.app.Activity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.ArrowBackIosNew
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.bricklyfrontend.BuildConfig
import com.example.bricklyfrontend.data.RetrofitClient
import com.example.bricklyfrontend.data.TopUpRequestDTO
import com.example.bricklyfrontend.data.UserPreferences
import com.example.bricklyfrontend.ui.theme.*
import kotlinx.coroutines.launch
import ru.yoomoney.sdk.kassa.payments.Checkout
import ru.yoomoney.sdk.kassa.payments.checkoutParameters.Amount
import ru.yoomoney.sdk.kassa.payments.checkoutParameters.PaymentMethodType
import ru.yoomoney.sdk.kassa.payments.checkoutParameters.PaymentParameters
import ru.yoomoney.sdk.kassa.payments.checkoutParameters.SavePaymentMethod
import java.math.BigDecimal
import java.util.Currency
import java.util.Locale

@Composable
fun CheckoutScreen(
    totalPrice: Int,
    onBack: () -> Unit,
    onPaymentSuccess: () -> Unit = {}
) {
    SetStatusBarColor(Accent)
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val userId = remember { UserPreferences.getUserId(context) }

    var selectedTab by remember { mutableIntStateOf(0) }

    var pickupAddress by remember { mutableStateOf("") }

    var deliveryStreet by remember { mutableStateOf("") }
    var deliveryHouse by remember { mutableStateOf("") }
    var deliveryEntrance by remember { mutableStateOf("") }
    var deliveryApt by remember { mutableStateOf("") }
    var deliveryFloor by remember { mutableStateOf("") }
    var deliveryCode by remember { mutableStateOf("") }
    var deliveryComment by remember { mutableStateOf("") }

    var errorMessage by remember { mutableStateOf<String?>(null) }
    var isProcessing by remember { mutableStateOf(false) }
    var pendingPaymentId by remember { mutableStateOf<String?>(null) }

    val threeDsLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.StartActivityForResult()
    ) { result ->
        val yooId = pendingPaymentId
        when (result.resultCode) {
            Activity.RESULT_OK -> {
                if (yooId != null) {
                    scope.launch {
                        try {
                            val resp = RetrofitClient.api.getPaymentByYooId(yooId)
                            if (resp.isSuccessful) {
                                val body = resp.body()
                                when {
                                    body?.status == "succeeded" -> {
                                        isProcessing = false
                                        onPaymentSuccess()
                                    }
                                    !body?.cancellationReason.isNullOrBlank() -> {
                                        errorMessage = "Платёж отклонён: ${body?.cancellationReason}"
                                        isProcessing = false
                                    }
                                    else -> {
                                        errorMessage = "Платёж не подтверждён"
                                        isProcessing = false
                                    }
                                }
                            } else {
                                errorMessage = "Ошибка проверки статуса (${resp.code()})"
                                isProcessing = false
                            }
                        } catch (_: Exception) {
                            errorMessage = "Нет соединения с сервером"
                            isProcessing = false
                        }
                    }
                } else {
                    isProcessing = false
                }
            }
            Activity.RESULT_CANCELED -> {
                errorMessage = "Оплата отменена"
                isProcessing = false
            }
            Checkout.RESULT_ERROR -> {
                errorMessage = "Ошибка проведения 3D Secure"
                isProcessing = false
            }
        }
    }

    val tokenizeLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.StartActivityForResult()
    ) { result ->
        when (result.resultCode) {
            Activity.RESULT_OK -> {
                val data = result.data
                if (data != null) {
                    val tokenResult = Checkout.createTokenizationResult(data)
                    isProcessing = true
                    errorMessage = null
                    scope.launch {
                        try {
                            val resp = RetrofitClient.api.payForCart(
                                TopUpRequestDTO(
                                    userId = userId,
                                    amount = "$totalPrice.00",
                                    paymentToken = tokenResult.paymentToken
                                )
                            )
                            if (resp.isSuccessful) {
                                val body = resp.body()
                                when {
                                    body?.status == "succeeded" -> {
                                        isProcessing = false
                                        onPaymentSuccess()
                                    }
                                    body?.status == "pending" && !body.confirmationUrl.isNullOrBlank() -> {
                                        pendingPaymentId = body.paymentId
                                        val intent = Checkout.createConfirmationIntent(
                                            context,
                                            body.confirmationUrl,
                                            tokenResult.paymentMethodType,
                                            BuildConfig.YOOKASSA_CLIENT_KEY,
                                            BuildConfig.YOOKASSA_SHOP_ID
                                        )
                                        threeDsLauncher.launch(intent)
                                    }
                                    !body?.cancellationReason.isNullOrBlank() -> {
                                        errorMessage = "Платёж отклонён: ${body?.cancellationReason}"
                                        isProcessing = false
                                    }
                                    body?.status == "canceled" -> {
                                        errorMessage = "Платёж отменён"
                                        isProcessing = false
                                    }
                                    else -> {
                                        errorMessage = "Ошибка обработки платежа"
                                        isProcessing = false
                                    }
                                }
                            } else {
                                errorMessage = "Ошибка сервера (${resp.code()})"
                                isProcessing = false
                            }
                        } catch (_: Exception) {
                            errorMessage = "Нет соединения с сервером"
                            isProcessing = false
                        }
                    }
                }
            }
            Activity.RESULT_CANCELED -> {}
        }
    }

    fun validateAndPay() {
        if (selectedTab == 0 && pickupAddress.isBlank()) {
            errorMessage = "Укажите адрес пункта выдачи"
            return
        }
        if (selectedTab == 1 && (deliveryStreet.isBlank() || deliveryHouse.isBlank())) {
            errorMessage = "Укажите улицу и номер дома"
            return
        }
        errorMessage = null

        val subtitle = if (selectedTab == 0) {
            "Самовывоз: $pickupAddress"
        } else {
            "Доставка: $deliveryStreet, д. $deliveryHouse"
        }

        val paymentParameters = PaymentParameters(
            amount = Amount(BigDecimal.valueOf(totalPrice.toLong()), Currency.getInstance("RUB")),
            title = "Заказ Brickly",
            subtitle = subtitle,
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
                    Icon(Icons.Outlined.ArrowBackIosNew, "Назад", tint = TextPrimary)
                }
                Text(
                    text = "Оформление заказа",
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
                .verticalScroll(rememberScrollState())
        ) {
            Spacer(Modifier.height(16.dp))

            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = CardBackground),
                elevation = CardDefaults.cardElevation(0.dp)
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(4.dp),
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    listOf("Самовывоз", "Доставка").forEachIndexed { index, label ->
                        val selected = selectedTab == index
                        Button(
                            onClick = { selectedTab = index; errorMessage = null },
                            modifier = Modifier.weight(1f).height(44.dp),
                            shape = RoundedCornerShape(12.dp),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = if (selected) Accent else Color.Transparent,
                                contentColor = TextPrimary
                            ),
                            elevation = ButtonDefaults.buttonElevation(0.dp)
                        ) {
                            Text(
                                text = label,
                                fontWeight = if (selected) FontWeight.SemiBold else FontWeight.Normal,
                                fontSize = 14.sp
                            )
                        }
                    }
                }
            }

            Spacer(Modifier.height(20.dp))

            if (selectedTab == 0) {
                FieldSectionLabel("Пункт выдачи")
                Card(
                    modifier = Modifier.fillMaxWidth().padding(horizontal = 20.dp),
                    shape = RoundedCornerShape(20.dp),
                    colors = CardDefaults.cardColors(containerColor = CardBackground),
                    elevation = CardDefaults.cardElevation(0.dp)
                ) {
                    Column(modifier = Modifier.padding(20.dp)) {
                        CheckoutField(
                            value = pickupAddress,
                            onValueChange = { pickupAddress = it },
                            label = "Адрес пункта выдачи",
                            modifier = Modifier.fillMaxWidth()
                        )
                    }
                }
            } else {
                FieldSectionLabel("Адрес доставки")
                Card(
                    modifier = Modifier.fillMaxWidth().padding(horizontal = 20.dp),
                    shape = RoundedCornerShape(20.dp),
                    colors = CardDefaults.cardColors(containerColor = CardBackground),
                    elevation = CardDefaults.cardElevation(0.dp)
                ) {
                    Column(modifier = Modifier.padding(20.dp)) {
                        CheckoutField(
                            value = deliveryStreet,
                            onValueChange = { deliveryStreet = it },
                            label = "Улица",
                            modifier = Modifier.fillMaxWidth()
                        )
                        Spacer(Modifier.height(14.dp))
                        Row(
                            Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            CheckoutField(
                                value = deliveryHouse,
                                onValueChange = { deliveryHouse = it },
                                label = "Дом",
                                modifier = Modifier.weight(1f)
                            )
                            CheckoutField(
                                value = deliveryEntrance,
                                onValueChange = { deliveryEntrance = it },
                                label = "Подъезд",
                                modifier = Modifier.weight(1f)
                            )
                        }
                        Spacer(Modifier.height(14.dp))
                        Row(
                            Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            CheckoutField(
                                value = deliveryApt,
                                onValueChange = { deliveryApt = it },
                                label = "Квартира",
                                modifier = Modifier.weight(1f)
                            )
                            CheckoutField(
                                value = deliveryFloor,
                                onValueChange = { deliveryFloor = it },
                                label = "Этаж",
                                modifier = Modifier.weight(1f)
                            )
                        }
                        Spacer(Modifier.height(14.dp))
                        CheckoutField(
                            value = deliveryCode,
                            onValueChange = { deliveryCode = it },
                            label = "Код домофона",
                            modifier = Modifier.fillMaxWidth()
                        )
                        Spacer(Modifier.height(14.dp))
                        CheckoutField(
                            value = deliveryComment,
                            onValueChange = { deliveryComment = it },
                            label = "Комментарий для курьера",
                            modifier = Modifier.fillMaxWidth()
                        )
                    }
                }
            }

            if (errorMessage != null) {
                Spacer(Modifier.height(10.dp))
                Text(
                    text = errorMessage!!,
                    color = ErrorColor,
                    fontSize = 13.sp,
                    modifier = Modifier.padding(horizontal = 24.dp)
                )
            }

            Spacer(Modifier.height(20.dp))

            FieldSectionLabel("Итого")
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp),
                shape = RoundedCornerShape(20.dp),
                colors = CardDefaults.cardColors(containerColor = CardBackground),
                elevation = CardDefaults.cardElevation(0.dp)
            ) {
                Column(modifier = Modifier.padding(20.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text("К оплате", color = TextSecondary, fontSize = 14.sp)
                        Text(
                            "$totalPrice ₽",
                            fontWeight = FontWeight.ExtraBold,
                            fontSize = 24.sp,
                            color = TextPrimary
                        )
                    }
                    Spacer(Modifier.height(16.dp))
                    Button(
                        onClick = { if (!isProcessing) validateAndPay() },
                        enabled = !isProcessing,
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(56.dp),
                        shape = RoundedCornerShape(16.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = Accent,
                            contentColor = TextPrimary
                        )
                    ) {
                        if (isProcessing) {
                            CircularProgressIndicator(
                                modifier = Modifier.size(22.dp),
                                color = TextPrimary,
                                strokeWidth = 2.5.dp
                            )
                        } else {
                            Text(
                                "Оплатить $totalPrice ₽",
                                fontWeight = FontWeight.Bold,
                                fontSize = 16.sp
                            )
                        }
                    }
                }
            }

            Spacer(Modifier.height(32.dp))
        }
    }
}

@Composable
private fun FieldSectionLabel(text: String) {
    Text(
        text = text.uppercase(Locale.getDefault()),
        fontSize = 11.sp,
        fontWeight = FontWeight.SemiBold,
        color = TextPrimary.copy(alpha = 0.5f),
        letterSpacing = 0.8.sp,
        modifier = Modifier
            .padding(horizontal = 32.dp)
            .padding(bottom = 8.dp)
    )
}

@Composable
private fun CheckoutField(
    value: String,
    onValueChange: (String) -> Unit,
    label: String,
    modifier: Modifier = Modifier
) {
    Column(modifier = modifier) {
        Text(
            text = label.uppercase(Locale.getDefault()),
            fontSize = 10.sp,
            fontWeight = FontWeight.SemiBold,
            color = TextPrimary.copy(alpha = 0.5f),
            letterSpacing = 0.6.sp
        )
        Spacer(Modifier.height(4.dp))
        OutlinedTextField(
            value = value,
            onValueChange = onValueChange,
            modifier = Modifier.fillMaxWidth(),
            singleLine = true,
            shape = RoundedCornerShape(14.dp),
            colors = OutlinedTextFieldDefaults.colors(
                focusedBorderColor = Accent,
                unfocusedBorderColor = Divider,
                focusedContainerColor = Color.White,
                unfocusedContainerColor = Color.White,
                cursorColor = Accent
            ),
            textStyle = MaterialTheme.typography.bodyLarge.copy(color = TextPrimary)
        )
    }
}
