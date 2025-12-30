package wingzone.zenith.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import wingzone.zenith.data.models.*
import wingzone.zenith.data.repository.CartRepository
import wingzone.zenith.data.repository.RepositoryProvider
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch

class CartViewModel(private val cartRepository: CartRepository = RepositoryProvider.getCartRepository()) : ViewModel() {
    
    val cart: StateFlow<Cart> = cartRepository.cart
    
    fun addItem(menuItem: MenuItem, quantity: Int, customization: EntreeCustomization? = null) {
        viewModelScope.launch {
            val cartItem = CartItem(
                menuItem = menuItem,
                quantity = quantity,
                customization = customization
            )
            cartRepository.addItem(cartItem)
        }
    }
    
    fun removeItem(itemId: String) {
        viewModelScope.launch {
            cartRepository.removeItem(itemId)
        }
    }
    
    fun updateQuantity(itemId: String, newQuantity: Int) {
        viewModelScope.launch {
            cartRepository.updateItemQuantity(itemId, newQuantity)
        }
    }
    
    fun clearCart() {
        viewModelScope.launch {
            cartRepository.clearCart()
        }
    }
    
    fun getItemCount(): Int = cartRepository.getItemCount()
    
    fun getTotal(): Double = cartRepository.getTotal()
}
