package com.example.bricklyfrontend.screens

import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import com.example.bricklyfrontend.data.CartItemCreateDTO
import com.example.bricklyfrontend.data.CartItemUpdateDTO
import com.example.bricklyfrontend.data.RetrofitClient

data class ListingCartItem(
    val listingId: Long,
    val title: String,
    val unitPrice: Int,
    val quantity: Int,
    val maxQuantity: Int,
    val serverCartItemId: Long? = null
)

object ListingCartState {
    var items by mutableStateOf<List<ListingCartItem>>(emptyList())

    suspend fun addItemWithApi(item: ListingCartItem, userId: Long) {
        val existing = items.find { it.listingId == item.listingId }
        if (existing != null && existing.serverCartItemId != null) {
            val newQty = (existing.quantity + item.quantity).coerceAtMost(existing.maxQuantity)
            items = items.map {
                if (it.listingId == item.listingId) it.copy(quantity = newQty) else it
            }
            try {
                RetrofitClient.api.updateCartItem(existing.serverCartItemId, CartItemUpdateDTO(newQty))
            } catch (_: Exception) {}
        } else if (existing != null) {
            val newQty = (existing.quantity + item.quantity).coerceAtMost(existing.maxQuantity)
            items = items.map {
                if (it.listingId == item.listingId) it.copy(quantity = newQty) else it
            }
        } else {
            items = items + item.copy(quantity = item.quantity.coerceAtMost(item.maxQuantity))
            try {
                val resp = RetrofitClient.api.addCartItem(
                    CartItemCreateDTO(userId = userId, itemType = "L", itemId = item.listingId, quantity = item.quantity)
                )
                if (resp.isSuccessful) {
                    val serverId = resp.body()?.id
                    if (serverId != null) {
                        items = items.map {
                            if (it.listingId == item.listingId) it.copy(serverCartItemId = serverId) else it
                        }
                    }
                }
            } catch (_: Exception) {}
        }
    }

    fun incrementQuantity(listingId: Long) {
        items = items.map {
            if (it.listingId == listingId) it.copy(quantity = (it.quantity + 1).coerceAtMost(it.maxQuantity)) else it
        }
    }

    fun decrementQuantity(listingId: Long) {
        val item = items.find { it.listingId == listingId } ?: return
        if (item.quantity <= 1) removeItem(listingId)
        else items = items.map { if (it.listingId == listingId) it.copy(quantity = it.quantity - 1) else it }
    }

    fun removeItem(listingId: Long) { items = items.filter { it.listingId != listingId } }

    suspend fun incrementQuantityWithApi(listingId: Long) {
        val item = items.find { it.listingId == listingId } ?: return
        if (item.quantity >= item.maxQuantity) return
        val newQty = item.quantity + 1
        items = items.map { if (it.listingId == listingId) it.copy(quantity = newQty) else it }
        val serverId = item.serverCartItemId ?: return
        try { RetrofitClient.api.updateCartItem(serverId, CartItemUpdateDTO(newQty)) } catch (_: Exception) {}
    }

    suspend fun decrementQuantityWithApi(listingId: Long) {
        val item = items.find { it.listingId == listingId } ?: return
        if (item.quantity <= 1) {
            items = items.filter { it.listingId != listingId }
            val serverId = item.serverCartItemId ?: return
            try { RetrofitClient.api.deleteCartItem(serverId) } catch (_: Exception) {}
            return
        }
        val newQty = item.quantity - 1
        items = items.map { if (it.listingId == listingId) it.copy(quantity = newQty) else it }
        val serverId = item.serverCartItemId ?: return
        try { RetrofitClient.api.updateCartItem(serverId, CartItemUpdateDTO(newQty)) } catch (_: Exception) {}
    }

    fun totalPrice(): Int = items.sumOf { it.unitPrice * it.quantity }
    fun totalCount(): Int = items.sumOf { it.quantity }
}
