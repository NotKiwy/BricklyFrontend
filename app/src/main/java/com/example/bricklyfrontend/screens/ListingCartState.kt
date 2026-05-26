package com.example.bricklyfrontend.screens

import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import com.example.bricklyfrontend.data.CartItemCreateDTO
import com.example.bricklyfrontend.data.RetrofitClient

data class ListingCartItem(
    val listingId: Long,
    val title: String,
    val unitPrice: Int,
    val quantity: Int,
    val maxQuantity: Int
)

object ListingCartState {
    var items by mutableStateOf<List<ListingCartItem>>(emptyList())

    fun addItem(item: ListingCartItem, userId: Long? = null, onError: ((String) -> Unit)? = null) {
        val existing = items.find { it.listingId == item.listingId }
        items = if (existing != null) {
            items.map {
                if (it.listingId == item.listingId) {
                    val newQty = (it.quantity + item.quantity).coerceAtMost(it.maxQuantity)
                    it.copy(quantity = newQty)
                } else it
            }
        } else {
            items + item.copy(quantity = item.quantity.coerceAtMost(item.maxQuantity))
        }
    }

    suspend fun addItemWithApi(item: ListingCartItem, userId: Long, onError: ((String) -> Unit)? = null) {
        addItem(item)
        try {
            RetrofitClient.api.addCartItem(
                CartItemCreateDTO(
                    userId = userId,
                    itemType = "L",
                    itemId = item.listingId,
                    quantity = item.quantity
                )
            )
        } catch (e: Exception) {
            onError?.invoke(e.message ?: "Ошибка синхронизации корзины")
        }
    }

    fun updateQuantity(listingId: Long, quantity: Int) {
        if (quantity <= 0) {
            removeItem(listingId)
        } else {
            items = items.map { if (it.listingId == listingId) it.copy(quantity = quantity.coerceAtMost(it.maxQuantity)) else it }
        }
    }

    fun incrementQuantity(listingId: Long) {
        items = items.map { 
            if (it.listingId == listingId) {
                val newQty = (it.quantity + 1).coerceAtMost(it.maxQuantity)
                it.copy(quantity = newQty)
            } else it 
        }
    }

    fun decrementQuantity(listingId: Long) {
        val item = items.find { it.listingId == listingId }
        if (item != null) {
            if (item.quantity <= 1) {
                removeItem(listingId)
            } else {
                items = items.map { 
                    if (it.listingId == listingId) it.copy(quantity = it.quantity - 1) 
                    else it 
                }
            }
        }
    }

    fun removeItem(listingId: Long) { items = items.filter { it.listingId != listingId } }

    fun totalPrice(): Int = items.sumOf { it.unitPrice * it.quantity }
    fun totalCount(): Int = items.sumOf { it.quantity }
}
