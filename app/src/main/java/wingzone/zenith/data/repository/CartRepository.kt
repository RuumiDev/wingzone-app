package wingzone.zenith.data.repository

import wingzone.zenith.data.models.*
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

class CartRepository {
    private val _cart = MutableStateFlow(Cart())
    val cart: StateFlow<Cart> = _cart.asStateFlow()
    
    fun addItem(cartItem: CartItem) {
        val currentItems = _cart.value.items.toMutableList()
        
        // Check if same item with same customization exists
        val existingItemIndex = currentItems.indexOfFirst { 
            it.menuItem.id == cartItem.menuItem.id && 
            it.customization == cartItem.customization 
        }
        
        if (existingItemIndex != -1) {
            // Update quantity of existing item
            val existingItem = currentItems[existingItemIndex]
            currentItems[existingItemIndex] = existingItem.copy(
                quantity = existingItem.quantity + cartItem.quantity
            )
        } else {
            // Add new item
            currentItems.add(cartItem)
        }
        
        _cart.value = Cart(items = currentItems)
    }
    
        

    fun removeItem(itemId: String) {
        val currentItems = _cart.value.items.toMutableList()
        currentItems.removeAll { it.id == itemId }
        _cart.value = Cart(items = currentItems)
    }
    
    fun updateItemQuantity(itemId: String, newQuantity: Int) {
        if (newQuantity <= 0) {
            removeItem(itemId)
            return
        }
        
        val currentItems = _cart.value.items.toMutableList()
        val itemIndex = currentItems.indexOfFirst { it.id == itemId }
        
        if (itemIndex != -1) {
            currentItems[itemIndex] = currentItems[itemIndex].copy(quantity = newQuantity)
            _cart.value = Cart(items = currentItems)
        }
    }
    
    
    fun clearCart() {
        _cart.value = Cart()
    }
    
    fun getItemCount(): Int = _cart.value.totalItems
    
    fun getTotal(): Double = _cart.value.total
}
