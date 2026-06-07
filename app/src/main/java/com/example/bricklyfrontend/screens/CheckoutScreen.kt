package com.example.bricklyfrontend.screens

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.ArrowBackIosNew
import androidx.compose.material.icons.outlined.ConfirmationNumber
import androidx.compose.material.icons.outlined.MyLocation
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
import com.example.bricklyfrontend.data.*
import com.example.bricklyfrontend.ui.theme.*
import kotlinx.coroutines.launch
import java.time.OffsetDateTime
import java.time.format.DateTimeFormatter
import java.util.Locale
import android.Manifest
import android.annotation.SuppressLint
import android.content.Context
import android.content.pm.PackageManager
import android.location.Location
import android.location.LocationManager
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.content.ContextCompat
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.osmdroid.config.Configuration
import org.osmdroid.events.MapEventsReceiver
import org.osmdroid.tileprovider.tilesource.TileSourceFactory
import org.osmdroid.util.GeoPoint
import org.osmdroid.views.MapView
import org.osmdroid.views.overlay.MapEventsOverlay
import org.osmdroid.views.overlay.Marker
import java.io.File

private enum class CheckoutStep {
    TICKETS_CONFIRMATION,
    SHIPPING_FORM
}

@Composable
fun CheckoutScreen(
    onBack: () -> Unit,
    onPaymentSuccess: () -> Unit = {},
    onNavigateToTopUp: (Int) -> Unit = {}
) {
    SetStatusBarColor(Accent)
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val userId = remember { UserPreferences.getUserId(context) }

    var userBalance by remember { mutableIntStateOf(0) }
    var currentStep by remember { mutableStateOf<CheckoutStep?>(null) }
    var cartItems by remember { mutableStateOf<List<CartItemDefaultDTO>>(emptyList()) }
    var ticketItems by remember { mutableStateOf<List<CartItemDefaultDTO>>(emptyList()) }
    var listingItems by remember { mutableStateOf<List<CartItemDefaultDTO>>(emptyList()) }
    var ticketsTotal by remember { mutableIntStateOf(0) }
    var listingsTotal by remember { mutableIntStateOf(0) }

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

    LaunchedEffect(Unit) {
        try {
            val userResp = RetrofitClient.api.getUserById(userId)
            if (userResp.isSuccessful) {
                userBalance = userResp.body()?.balance ?: 0
            }

            val cartResp = RetrofitClient.api.getCartItemsByUserId(userId)
            if (cartResp.isSuccessful) {
                cartItems = cartResp.body() ?: emptyList()
                ticketItems = cartItems.filter { it.itemType == "M" }
                listingItems = cartItems.filter { it.itemType == "L" }

                ticketsTotal = 0
                for (item in ticketItems) {
                    val meetingResp = RetrofitClient.api.getMeetingById(item.itemId?.toLong() ?: continue)
                    if (meetingResp.isSuccessful) {
                        ticketsTotal += (meetingResp.body()?.ticketPrice ?: 0) * (item.quantity ?: 1)
                    }
                }

                listingsTotal = 0
                for (item in listingItems) {
                    val listingResp = RetrofitClient.api.getListingById(item.itemId?.toLong() ?: continue)
                    if (listingResp.isSuccessful) {
                        listingsTotal += (listingResp.body()?.price ?: 0) * (item.quantity ?: 1)
                    }
                }

                currentStep = if (ticketItems.isNotEmpty()) {
                    CheckoutStep.TICKETS_CONFIRMATION
                } else if (listingItems.isNotEmpty()) {
                    CheckoutStep.SHIPPING_FORM
                } else {
                    null
                }
            }
        } catch (_: Exception) {}
    }

    suspend fun createTicketsForItems() {
        for (item in ticketItems) {
            val meetingId = item.itemId?.toLong() ?: continue
            val quantity = item.quantity ?: 1

            val meetingResp = RetrofitClient.api.getMeetingById(meetingId)
            val ticketPrice = if (meetingResp.isSuccessful) {
                meetingResp.body()?.ticketPrice ?: 0
            } else 0

            repeat(quantity) {
                try {
                    RetrofitClient.api.createTicket(
                        TicketCreateDTO(
                            userId = userId,
                            meetingId = meetingId,
                            pricePaid = ticketPrice,
                            state = 0
                        )
                    )
                } catch (_: Exception) {}
            }
        }
    }

    suspend fun createOrderForListings() {
        if (listingItems.isEmpty()) return

        val orderItems = mutableListOf<OrderItemCreateDTO>()
        for (item in listingItems) {
            val listingId = item.itemId?.toLong() ?: continue
            val quantity = item.quantity ?: 1

            val listingResp = RetrofitClient.api.getListingById(listingId)
            val price = if (listingResp.isSuccessful) {
                listingResp.body()?.price ?: 0
            } else 0

            orderItems.add(
                OrderItemCreateDTO(
                    status = "on_confirmation",
                    price = price,
                    quantity = quantity,
                    listingId = listingId
                )
            )
        }

        val shippingMethod = if (selectedTab == 0) "shipping" else "delivery"
        val shippingAddress = if (selectedTab == 0) {
            pickupAddress
        } else {
            buildString {
                append(deliveryStreet)
                append(", д. ")
                append(deliveryHouse)
                if (deliveryEntrance.isNotBlank()) append(", подъезд $deliveryEntrance")
                if (deliveryApt.isNotBlank()) append(", кв. $deliveryApt")
                if (deliveryFloor.isNotBlank()) append(", этаж $deliveryFloor")
                if (deliveryCode.isNotBlank()) append(", код $deliveryCode")
                if (deliveryComment.isNotBlank()) append(" ($deliveryComment)")
            }
        }

        try {
            RetrofitClient.api.createOrder(
                OrderCreateDTO(
                    shippingMethod = shippingMethod,
                    shippingAddress = shippingAddress,
                    createdAt = OffsetDateTime.now().format(DateTimeFormatter.ISO_OFFSET_DATE_TIME),
                    userId = userId,
                    orderItems = orderItems
                )
            )
        } catch (_: Exception) {}
    }

    fun confirmTicketsPurchase() {
        errorMessage = null
        val totalToPay = ticketsTotal

        if (userBalance >= totalToPay) {
            isProcessing = true
            scope.launch {
                try {
                    createTicketsForItems()
                    isProcessing = false

                    if (listingItems.isNotEmpty()) {
                        currentStep = CheckoutStep.SHIPPING_FORM
                    } else {
                        onPaymentSuccess()
                    }
                } catch (_: Exception) {
                    errorMessage = "Ошибка создания билетов"
                    isProcessing = false
                }
            }
        } else {
            val deficit = totalToPay - userBalance
            onNavigateToTopUp(deficit)
        }
    }

    fun confirmOrderPurchase() {
        if (selectedTab == 0 && pickupAddress.isBlank()) {
            errorMessage = "Укажите адрес пункта выдачи"
            return
        }
        if (selectedTab == 1 && (deliveryStreet.isBlank() || deliveryHouse.isBlank())) {
            errorMessage = "Укажите улицу и номер дома"
            return
        }

        errorMessage = null
        val totalToPay = listingsTotal

        if (userBalance >= totalToPay) {
            isProcessing = true
            scope.launch {
                try {
                    createOrderForListings()
                    isProcessing = false
                    onPaymentSuccess()
                } catch (_: Exception) {
                    errorMessage = "Ошибка создания заказа"
                    isProcessing = false
                }
            }
        } else {
            val deficit = totalToPay - userBalance
            onNavigateToTopUp(deficit)
        }
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
                    .padding(horizontal = 8.dp, vertical = 8.dp)
            ) {
                IconButton(onClick = onBack, modifier = Modifier.align(Alignment.CenterStart)) {
                    Icon(Icons.Outlined.ArrowBackIosNew, "Назад", tint = TextPrimary)
                }
                Text(
                    text = when (currentStep) {
                        CheckoutStep.TICKETS_CONFIRMATION -> "Подтверждение покупки"
                        CheckoutStep.SHIPPING_FORM -> "Оформление заказа"
                        else -> "Оформление"
                    },
                    color = TextPrimary,
                    fontWeight = FontWeight.Bold,
                    fontSize = 18.sp,
                    modifier = Modifier.align(Alignment.Center)
                )
            }
        }
    ) { padding ->
        when (currentStep) {
            CheckoutStep.TICKETS_CONFIRMATION -> TicketsConfirmationContent(
                padding = padding,
                ticketItems = ticketItems,
                ticketsTotal = ticketsTotal,
                userBalance = userBalance,
                errorMessage = errorMessage,
                isProcessing = isProcessing,
                onConfirm = { confirmTicketsPurchase() }
            )
            CheckoutStep.SHIPPING_FORM -> ShippingFormContent(
                padding = padding,
                selectedTab = selectedTab,
                onTabChange = { selectedTab = it; errorMessage = null },
                pickupAddress = pickupAddress,
                onPickupAddressChange = { pickupAddress = it },
                deliveryStreet = deliveryStreet,
                onDeliveryStreetChange = { deliveryStreet = it },
                deliveryHouse = deliveryHouse,
                onDeliveryHouseChange = { deliveryHouse = it },
                deliveryEntrance = deliveryEntrance,
                onDeliveryEntranceChange = { deliveryEntrance = it },
                deliveryApt = deliveryApt,
                onDeliveryAptChange = { deliveryApt = it },
                deliveryFloor = deliveryFloor,
                onDeliveryFloorChange = { deliveryFloor = it },
                deliveryCode = deliveryCode,
                onDeliveryCodeChange = { deliveryCode = it },
                deliveryComment = deliveryComment,
                onDeliveryCommentChange = { deliveryComment = it },
                errorMessage = errorMessage,
                isProcessing = isProcessing,
                listingsTotal = listingsTotal,
                userBalance = userBalance,
                onConfirm = { confirmOrderPurchase() }
            )
            else -> Box(
                Modifier.fillMaxSize().padding(padding),
                contentAlignment = Alignment.Center
            ) {
                CircularProgressIndicator(color = Accent)
            }
        }
    }
}

@Composable
private fun TicketsConfirmationContent(
    padding: PaddingValues,
    ticketItems: List<CartItemDefaultDTO>,
    ticketsTotal: Int,
    userBalance: Int,
    errorMessage: String?,
    isProcessing: Boolean,
    onConfirm: () -> Unit
) {
    val scope = rememberCoroutineScope()
    val context = LocalContext.current
    val userId = UserPreferences.getUserId(context)
    var ticketDetails by remember { mutableStateOf<List<Pair<MeetingDefaultDTO, Int>>>(emptyList()) }
    var isLoading by remember { mutableStateOf(true) }

    LaunchedEffect(ticketItems) {
        try {
            val details = mutableListOf<Pair<MeetingDefaultDTO, Int>>()
            for (item in ticketItems) {
                val meetingResp = RetrofitClient.api.getMeetingById(item.itemId?.toLong() ?: continue)
                if (meetingResp.isSuccessful) {
                    val meeting = meetingResp.body()
                    if (meeting != null) {
                        val quantity = item.quantity ?: 1
                        details.add(meeting to quantity)
                    }
                }
            }
            ticketDetails = details
        } catch (_: Exception) {}
        isLoading = false
    }

    if (isLoading) {
        Box(
            Modifier.fillMaxSize().padding(padding),
            contentAlignment = Alignment.Center
        ) {
            CircularProgressIndicator(color = Accent)
        }
    } else {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .verticalScroll(rememberScrollState())
        ) {
            Spacer(Modifier.height(16.dp))

            Card(
                modifier = Modifier.fillMaxWidth().padding(horizontal = 20.dp),
                shape = RoundedCornerShape(24.dp),
                colors = CardDefaults.cardColors(containerColor = CardBackground),
                elevation = CardDefaults.cardElevation(0.dp)
            ) {
                Column(modifier = Modifier.padding(20.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            Icons.Outlined.ConfirmationNumber,
                            null,
                            tint = Accent,
                            modifier = Modifier.size(28.dp)
                        )
                        Spacer(Modifier.width(12.dp))
                        Text(
                            "Билеты на мероприятия",
                            style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold),
                            color = TextPrimary
                        )
                    }

                    Spacer(Modifier.height(16.dp))

                    ticketDetails.forEach { (meeting, quantity) ->
                        Row(
                            modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    meeting.title ?: meeting.type?.description ?: "Мероприятие",
                                    fontSize = 15.sp,
                                    fontWeight = FontWeight.SemiBold,
                                    color = TextPrimary
                                )
                                Spacer(Modifier.height(2.dp))
                                Text(
                                    "×$quantity",
                                    fontSize = 13.sp,
                                    color = TextSecondary
                                )
                            }
                            Text(
                                "${(meeting.ticketPrice ?: 0) * quantity} ₽",
                                fontSize = 16.sp,
                                fontWeight = FontWeight.Bold,
                                color = TextPrimary
                            )
                        }
                        if (ticketDetails.last() != (meeting to quantity)) {
                            HorizontalDivider(color = Divider, modifier = Modifier.padding(vertical = 4.dp))
                        }
                    }
                }
            }

            Spacer(Modifier.height(16.dp))

            Card(
                modifier = Modifier.fillMaxWidth().padding(horizontal = 20.dp),
                shape = RoundedCornerShape(20.dp),
                colors = CardDefaults.cardColors(containerColor = CardBackground),
                elevation = CardDefaults.cardElevation(0.dp)
            ) {
                Column(modifier = Modifier.padding(20.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text("Ваш баланс", fontSize = 14.sp, color = TextSecondary)
                        Text(
                            "$userBalance ₽",
                            fontSize = 16.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = TextPrimary
                        )
                    }
                    Spacer(Modifier.height(8.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text("К оплате", fontSize = 14.sp, color = TextPrimary)
                        Text(
                            "$ticketsTotal ₽",
                            fontSize = 20.sp,
                            fontWeight = FontWeight.ExtraBold,
                            color = TextPrimary
                        )
                    }
                }
            }

            if (errorMessage != null) {
                Spacer(Modifier.height(10.dp))
                Text(
                    text = errorMessage,
                    color = ErrorColor,
                    fontSize = 13.sp,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.fillMaxWidth().padding(horizontal = 24.dp)
                )
            }

            Spacer(Modifier.height(16.dp))

            Button(
                onClick = { if (!isProcessing) onConfirm() },
                enabled = !isProcessing && userBalance >= ticketsTotal,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp)
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
                        "Подтвердить покупку",
                        fontWeight = FontWeight.Bold,
                        fontSize = 16.sp
                    )
                }
            }

            if (userBalance < ticketsTotal) {
                Spacer(Modifier.height(12.dp))
                Text(
                    "Недостаточно средств. Пополните баланс в профиле.",
                    fontSize = 13.sp,
                    color = TextSecondary,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.fillMaxWidth().padding(horizontal = 24.dp)
                )
            }

            Spacer(Modifier.height(32.dp))
        }
    }
}

@Composable
private fun ShippingFormContent(
    padding: PaddingValues,
    selectedTab: Int,
    onTabChange: (Int) -> Unit,
    pickupAddress: String,
    onPickupAddressChange: (String) -> Unit,
    deliveryStreet: String,
    onDeliveryStreetChange: (String) -> Unit,
    deliveryHouse: String,
    onDeliveryHouseChange: (String) -> Unit,
    deliveryEntrance: String,
    onDeliveryEntranceChange: (String) -> Unit,
    deliveryApt: String,
    onDeliveryAptChange: (String) -> Unit,
    deliveryFloor: String,
    onDeliveryFloorChange: (String) -> Unit,
    deliveryCode: String,
    onDeliveryCodeChange: (String) -> Unit,
    deliveryComment: String,
    onDeliveryCommentChange: (String) -> Unit,
    errorMessage: String?,
    isProcessing: Boolean,
    listingsTotal: Int,
    userBalance: Int,
    onConfirm: () -> Unit
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()

    var parentScrollEnabled by remember { mutableStateOf(true) }
    val scrollState = rememberScrollState()

    var selectedGeoPoint by remember { mutableStateOf<GeoPoint?>(null) }
    var userGeoPoint by remember { mutableStateOf<GeoPoint?>(null) }
    var locationError by remember { mutableStateOf<String?>(null) }
    var locationGranted by remember { mutableStateOf<Boolean?>(null) }

    val permLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { perms ->
        val granted = perms[Manifest.permission.ACCESS_FINE_LOCATION] == true ||
                perms[Manifest.permission.ACCESS_COARSE_LOCATION] == true
        if (granted) locationGranted = true
        else locationError = "Геолокация недоступна. Выберите точку на карте вручную."
    }

    LaunchedEffect(Unit) {
        val fine = ContextCompat.checkSelfPermission(context, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED
        val coarse = ContextCompat.checkSelfPermission(context, Manifest.permission.ACCESS_COARSE_LOCATION) == PackageManager.PERMISSION_GRANTED
        if (fine || coarse) locationGranted = true
        else permLauncher.launch(arrayOf(Manifest.permission.ACCESS_FINE_LOCATION, Manifest.permission.ACCESS_COARSE_LOCATION))
    }

    LaunchedEffect(locationGranted) {
        if (locationGranted != true) return@LaunchedEffect
        val loc = withContext(Dispatchers.IO) { fetchUserLocation(context) }
        if (loc != null) {
            val gp = GeoPoint(loc.latitude, loc.longitude)
            selectedGeoPoint = gp
            userGeoPoint = gp
            val result = reverseGeocode(loc.latitude, loc.longitude)
            if (result != null) {
                if (selectedTab == 0) {
                    onPickupAddressChange(listOfNotNull(
                        result.road.takeIf { it.isNotBlank() },
                        result.houseNumber.takeIf { it.isNotBlank() },
                        result.city.takeIf { it.isNotBlank() }
                    ).joinToString(", "))
                } else {
                    if (result.road.isNotBlank()) onDeliveryStreetChange(result.road)
                    if (result.houseNumber.isNotBlank()) onDeliveryHouseChange(result.houseNumber)
                }
            }
        } else {
            locationError = "Не удалось определить местоположение. Выберите точку на карте."
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(padding)
            .verticalScroll(scrollState, enabled = parentScrollEnabled)
    ) {
        Spacer(Modifier.height(16.dp))

        Card(
            modifier = Modifier.fillMaxWidth().padding(horizontal = 20.dp),
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(containerColor = CardBackground),
            elevation = CardDefaults.cardElevation(0.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth().padding(4.dp),
                horizontalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                listOf("Самовывоз", "Доставка").forEachIndexed { index, label ->
                    val selected = selectedTab == index
                    Button(
                        onClick = { onTabChange(index) },
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

        Spacer(Modifier.height(16.dp))

        FieldSectionLabel("Адрес на карте")

        if (locationError != null) {
            Text(
                text = locationError!!,
                color = TextSecondary,
                fontSize = 12.sp,
                modifier = Modifier.padding(horizontal = 32.dp).padding(bottom = 6.dp)
            )
        }

        Box(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp)
                .height(240.dp)
                .clip(RoundedCornerShape(20.dp))
        ) {
            CheckoutInteractiveMap(
                modifier = Modifier.fillMaxSize(),
                selectedGeoPoint = selectedGeoPoint,
                onTouchStart = { parentScrollEnabled = false },
                onTouchEnd = { parentScrollEnabled = true },
                onLocationSelected = { gp ->
                    selectedGeoPoint = gp
                    scope.launch {
                        val result = reverseGeocode(gp.latitude, gp.longitude)
                        if (result != null) {
                            locationError = null
                            if (selectedTab == 0) {
                                onPickupAddressChange(listOfNotNull(
                                    result.road.takeIf { it.isNotBlank() },
                                    result.houseNumber.takeIf { it.isNotBlank() },
                                    result.city.takeIf { it.isNotBlank() }
                                ).joinToString(", "))
                            } else {
                                if (result.road.isNotBlank()) onDeliveryStreetChange(result.road)
                                if (result.houseNumber.isNotBlank()) onDeliveryHouseChange(result.houseNumber)
                            }
                        }
                    }
                }
            )
            if (userGeoPoint != null) {
                FloatingActionButton(
                    onClick = { selectedGeoPoint = userGeoPoint },
                    modifier = Modifier
                        .align(Alignment.BottomEnd)
                        .padding(10.dp)
                        .size(40.dp),
                    containerColor = Color.White,
                    contentColor = Accent,
                    elevation = FloatingActionButtonDefaults.elevation(4.dp, 4.dp)
                ) {
                    Icon(Icons.Outlined.MyLocation, null, modifier = Modifier.size(20.dp))
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
                        onValueChange = onPickupAddressChange,
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
                        onValueChange = onDeliveryStreetChange,
                        label = "Улица",
                        modifier = Modifier.fillMaxWidth()
                    )
                    Spacer(Modifier.height(14.dp))
                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                        CheckoutField(value = deliveryHouse, onValueChange = onDeliveryHouseChange, label = "Дом", modifier = Modifier.weight(1f))
                        CheckoutField(value = deliveryEntrance, onValueChange = onDeliveryEntranceChange, label = "Подъезд", modifier = Modifier.weight(1f))
                    }
                    Spacer(Modifier.height(14.dp))
                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                        CheckoutField(value = deliveryApt, onValueChange = onDeliveryAptChange, label = "Квартира", modifier = Modifier.weight(1f))
                        CheckoutField(value = deliveryFloor, onValueChange = onDeliveryFloorChange, label = "Этаж", modifier = Modifier.weight(1f))
                    }
                    Spacer(Modifier.height(14.dp))
                    CheckoutField(value = deliveryCode, onValueChange = onDeliveryCodeChange, label = "Код домофона", modifier = Modifier.fillMaxWidth())
                    Spacer(Modifier.height(14.dp))
                    CheckoutField(value = deliveryComment, onValueChange = onDeliveryCommentChange, label = "Комментарий для курьера", modifier = Modifier.fillMaxWidth())
                }
            }
        }

        if (errorMessage != null) {
            Spacer(Modifier.height(10.dp))
            Text(text = errorMessage, color = ErrorColor, fontSize = 13.sp, modifier = Modifier.padding(horizontal = 24.dp))
        }

        Spacer(Modifier.height(20.dp))

        FieldSectionLabel("Итого")
        Card(
            modifier = Modifier.fillMaxWidth().padding(horizontal = 20.dp),
            shape = RoundedCornerShape(20.dp),
            colors = CardDefaults.cardColors(containerColor = CardBackground),
            elevation = CardDefaults.cardElevation(0.dp)
        ) {
            Column(modifier = Modifier.padding(20.dp)) {
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                    Text("Ваш баланс", fontSize = 14.sp, color = TextSecondary)
                    Text("$userBalance ₽", fontSize = 16.sp, fontWeight = FontWeight.SemiBold, color = TextPrimary)
                }
                Spacer(Modifier.height(8.dp))
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                    Text("К оплате", color = TextSecondary, fontSize = 14.sp)
                    Text("$listingsTotal ₽", fontWeight = FontWeight.ExtraBold, fontSize = 24.sp, color = TextPrimary)
                }
                Spacer(Modifier.height(16.dp))
                Button(
                    onClick = { if (!isProcessing) onConfirm() },
                    enabled = !isProcessing && userBalance >= listingsTotal,
                    modifier = Modifier.fillMaxWidth().height(56.dp),
                    shape = RoundedCornerShape(16.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = Accent, contentColor = TextPrimary)
                ) {
                    if (isProcessing) {
                        CircularProgressIndicator(modifier = Modifier.size(22.dp), color = TextPrimary, strokeWidth = 2.5.dp)
                    } else {
                        Text("Оформить заказ", fontWeight = FontWeight.Bold, fontSize = 16.sp)
                    }
                }
            }
        }

        if (userBalance < listingsTotal) {
            Spacer(Modifier.height(12.dp))
            Text(
                "Недостаточно средств. Пополните баланс в профиле.",
                fontSize = 13.sp,
                color = TextSecondary,
                textAlign = TextAlign.Center,
                modifier = Modifier.fillMaxWidth().padding(horizontal = 24.dp)
            )
        }

        Spacer(Modifier.height(32.dp))
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

@Composable
private fun CheckoutInteractiveMap(
    modifier: Modifier = Modifier,
    selectedGeoPoint: GeoPoint?,
    onTouchStart: () -> Unit = {},
    onTouchEnd: () -> Unit = {},
    onLocationSelected: (GeoPoint) -> Unit
) {
    val context = LocalContext.current

    val mapView = remember {
        Configuration.getInstance().apply {
            userAgentValue = context.packageName
            osmdroidBasePath = File(context.cacheDir, "osmdroid")
            osmdroidTileCache = File(context.cacheDir, "osmdroid/tiles")
        }
        MapView(context).apply {
            setTileSource(TileSourceFactory.MAPNIK)
            setMultiTouchControls(true)
            controller.setZoom(14.0)
            controller.setCenter(GeoPoint(55.7558, 37.6173))
        }
    }

    val onLocationSelectedState = rememberUpdatedState(onLocationSelected)

    val eventsOverlay = remember {
        MapEventsOverlay(object : MapEventsReceiver {
            override fun singleTapConfirmedHelper(p: GeoPoint): Boolean {
                onLocationSelectedState.value(p)
                return true
            }
            override fun longPressHelper(p: GeoPoint): Boolean = false
        })
    }

    DisposableEffect(mapView) {
        mapView.overlays.add(eventsOverlay)
        onDispose { mapView.onDetach() }
    }

    LaunchedEffect(selectedGeoPoint) {
        val point = selectedGeoPoint ?: return@LaunchedEffect
        mapView.overlays.remove(eventsOverlay)
        mapView.overlays.removeAll { it is Marker }
        val marker = Marker(mapView).apply {
            position = point
            setAnchor(Marker.ANCHOR_CENTER, Marker.ANCHOR_BOTTOM)
        }
        mapView.overlays.add(marker)
        mapView.overlays.add(eventsOverlay)
        mapView.controller.animateTo(point)
        mapView.invalidate()
    }

    AndroidView(
        factory = { mapView },
        modifier = modifier,
        update = { view ->
            view.setOnTouchListener { _, event ->
                when (event.action) {
                    android.view.MotionEvent.ACTION_DOWN -> onTouchStart()
                    android.view.MotionEvent.ACTION_UP,
                    android.view.MotionEvent.ACTION_CANCEL -> onTouchEnd()
                }
                false
            }
        }
    )
}

private data class ReverseGeoResult(
    val road: String,
    val houseNumber: String,
    val city: String
)

private suspend fun reverseGeocode(lat: Double, lon: Double): ReverseGeoResult? = withContext(Dispatchers.IO) {
    try {
        val client = okhttp3.OkHttpClient()
        val request = okhttp3.Request.Builder()
            .url("https://nominatim.openstreetmap.org/reverse?lat=$lat&lon=$lon&format=json&accept-language=ru")
            .header("User-Agent", "BricklyApp/1.0")
            .build()
        val resp = client.newCall(request).execute()
        val body = resp.body?.string() ?: return@withContext null
        val obj = org.json.JSONObject(body)
        val addr = obj.optJSONObject("address") ?: return@withContext null
        ReverseGeoResult(
            road = addr.optString("road").ifBlank { addr.optString("pedestrian") }.ifBlank { addr.optString("street") },
            houseNumber = addr.optString("house_number"),
            city = addr.optString("city").ifBlank { addr.optString("town") }.ifBlank { addr.optString("village") }
        )
    } catch (_: Exception) { null }
}

@SuppressLint("MissingPermission")
private fun fetchUserLocation(context: Context): Location? {
    val lm = context.getSystemService(Context.LOCATION_SERVICE) as LocationManager
    return listOf(LocationManager.GPS_PROVIDER, LocationManager.NETWORK_PROVIDER, LocationManager.PASSIVE_PROVIDER)
        .firstNotNullOfOrNull { provider ->
            try { lm.getLastKnownLocation(provider) } catch (_: Exception) { null }
        }
}
