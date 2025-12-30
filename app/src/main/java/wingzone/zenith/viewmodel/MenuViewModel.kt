package wingzone.zenith.viewmodel

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import wingzone.zenith.data.models.MenuItem
import wingzone.zenith.data.repository.FirebaseMenuRepository
import wingzone.zenith.data.repository.RepositoryProvider
import wingzone.zenith.data.repository.AvailabilityRepository

data class MenuCategory(
    val id: String,
    val name: String,
    val icon: ImageVector,
    val items: List<MenuItem>
)

sealed class MenuState {
    object Loading : MenuState()
    data class Success(val categories: List<MenuCategory>) : MenuState()
    data class Error(val message: String) : MenuState()
}

class MenuViewModel : ViewModel() {
    private val menuRepository: FirebaseMenuRepository = 
        RepositoryProvider.menuRepository
    private val availabilityRepository = AvailabilityRepository()
    
    private val _menuState = MutableStateFlow<MenuState>(MenuState.Loading)
    val menuState: StateFlow<MenuState> = _menuState.asStateFlow()
    
    private val _menuItems = MutableStateFlow<List<MenuItem>>(emptyList())
    val menuItems: StateFlow<List<MenuItem>> = _menuItems.asStateFlow()
    
    init {
        // Load menu items from Firebase
        loadMenuItems()
        // Listen to real-time updates
        listenToMenuChanges()
        // Listen to availability changes
        listenToAvailabilityChanges()
    }
    
    private fun loadMenuItems() {
        viewModelScope.launch {
            try {
                _menuState.value = MenuState.Loading
                val items = menuRepository.getMenuItems()
                _menuItems.value = items
                updateCategories(items)
            } catch (e: Exception) {
                _menuState.value = MenuState.Error(e.message ?: "Failed to load menu")
            }
        }
    }
    
    private fun listenToMenuChanges() {
        viewModelScope.launch {
            menuRepository.listenToMenuItems()
                .catch { e ->
                    _menuState.value = MenuState.Error(e.message ?: "Connection error")
                }
                .collect { items ->
                    _menuItems.value = items
                    updateCategories(items)
                }
        }
    }
    
    private fun listenToAvailabilityChanges() {
        viewModelScope.launch {
            availabilityRepository.availability
                .collect { _ ->
                    // When availability changes, update categories to filter items
                    updateCategories(_menuItems.value)
                }
        }
    }
    
    private fun updateCategories(items: List<MenuItem>) {
        val availabilitySettings = availabilityRepository.availability.value
        
        // Group items by category
        val groupedItems = items.groupBy { it.category }
        
        // Sort Combo Meals by entree number (Entree 1, Entree 2, ..., Entree 12)
        val sortedComboMeals = (groupedItems["Combo Meals"] ?: emptyList()).sortedBy { item ->
            val match = Regex("Entree (\\d+)").find(item.name)
            match?.groupValues?.get(1)?.toIntOrNull() ?: 999
        }
        
        // Filter beverages based on availability
        val availableBeverages = (groupedItems["Beverages"] ?: emptyList()).filter { beverage ->
            availabilitySettings.beverages.contains(beverage.name)
        }
        
        val categories = listOf(
            MenuCategory("combos", "Combo Meals", Icons.Default.Star, sortedComboMeals),
            MenuCategory("wings", "Wings", Icons.Default.ShoppingCart, groupedItems["Wings"] ?: emptyList()),
            MenuCategory("tenders", "Tenders", Icons.Default.ShoppingCart, groupedItems["Tenders"] ?: emptyList()),
            MenuCategory("burgers", "Burgers & Sandwiches", Icons.Default.ShoppingCart, groupedItems["Burgers & Sandwiches"] ?: emptyList()),
            MenuCategory("local", "Local Favorites", Icons.Default.Home, groupedItems["Local Favorites"] ?: emptyList()),
            MenuCategory("salads", "Salads", Icons.Default.ShoppingCart, groupedItems["Salads"] ?: emptyList()),
            MenuCategory("sides", "Sides", Icons.Default.ShoppingCart, groupedItems["Sides"] ?: emptyList()),
            MenuCategory("beverages", "Beverages", Icons.Default.ShoppingCart, availableBeverages)
        ).filter { it.items.isNotEmpty() } // Only show categories with items
        
        _menuState.value = MenuState.Success(categories)
    }
    
    fun getMenuItemsByCategory(category: String): List<MenuItem> {
        return _menuItems.value.filter { it.category == category }
    }
    
    fun refreshMenu() {
        loadMenuItems()
    }
    
    override fun onCleared() {
        super.onCleared()
        availabilityRepository.cleanup()
    }
}
