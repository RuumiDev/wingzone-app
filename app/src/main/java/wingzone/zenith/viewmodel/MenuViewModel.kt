package wingzone.zenith.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import coil.imageLoader
import coil.request.CachePolicy
import coil.request.ImageRequest
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import wingzone.zenith.data.model.HomeBanner
import wingzone.zenith.data.models.MenuItem
import wingzone.zenith.data.models.Review
import wingzone.zenith.data.repository.AvailabilityRepository
import wingzone.zenith.data.repository.FirebaseBannerRepository
import wingzone.zenith.data.repository.FirebaseMenuRepository
import wingzone.zenith.data.repository.FirebaseReviewRepository
import wingzone.zenith.data.repository.RepositoryProvider

data class MenuCategory(
    val id: String,
    val name: String,
    val iconPath: String,  // SVG file path in assets
    val items: List<MenuItem>
)

sealed class MenuState {
    object Loading : MenuState()
    data class Success(val categories: List<MenuCategory>) : MenuState()
    data class Error(val message: String) : MenuState()
}

class MenuViewModel(application: Application) : AndroidViewModel(application) {
    private val menuRepository: FirebaseMenuRepository =
        RepositoryProvider.menuRepository
    private val availabilityRepository = AvailabilityRepository()
    private val bannerRepository = FirebaseBannerRepository()
    private val reviewRepository = FirebaseReviewRepository()

    private val _menuState = MutableStateFlow<MenuState>(MenuState.Loading)
    val menuState: StateFlow<MenuState> = _menuState.asStateFlow()

    private val _menuItems = MutableStateFlow<List<MenuItem>>(emptyList())
    val menuItems: StateFlow<List<MenuItem>> = _menuItems.asStateFlow()

    // --- Banner state ---
    private val _banners = MutableStateFlow<List<HomeBanner>>(emptyList())
    val banners: StateFlow<List<HomeBanner>> = _banners.asStateFlow()

    private val _areBannersLoaded = MutableStateFlow(false)
    val areBannersLoaded: StateFlow<Boolean> = _areBannersLoaded.asStateFlow()

    // --- Review state ---
    private val _reviews = MutableStateFlow<List<Review>>(emptyList())
    val reviews: StateFlow<List<Review>> = _reviews.asStateFlow()

    init {
        // Menu data
        loadMenuItems()
        listenToMenuChanges()
        listenToAvailabilityChanges()
        // Hoist home-screen data so it is ready before the user navigates
        loadBanners()
        loadReviews()
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
        
        // Sort Wings by displayOrder (just wings first, then with fries)
        val sortedWings = (groupedItems["Wings"] ?: emptyList()).sortedWith(
            compareBy<MenuItem> { it.displayOrder ?: 999 }
                .thenBy { it.name }
        )
        
        val categories = listOf(
            MenuCategory("combos", "Combo Meals", "icons/meal.svg", sortedComboMeals),
            MenuCategory("wings", "Wings", "icons/wings.svg", sortedWings),
            MenuCategory("tenders", "Tenders", "icons/tenders.svg", groupedItems["Tenders"] ?: emptyList()),
            MenuCategory("burgers", "Burgers & Sandwiches", "icons/burger.svg", groupedItems["Burgers & Sandwiches"] ?: emptyList()),
            MenuCategory("local", "Local Favorites", "icons/local-favourite.svg", groupedItems["Local Favorites"] ?: emptyList()),
            MenuCategory("salads", "Salads", "icons/salad.svg", groupedItems["Salads"] ?: emptyList()),
            MenuCategory("sides", "Sides", "icons/sides.svg", groupedItems["Sides"] ?: emptyList()),
            MenuCategory("beverages", "Beverages", "icons/beverage.svg", groupedItems["Beverages"] ?: emptyList())
        ).filter { it.items.isNotEmpty() } // Only show categories with items
        
        _menuState.value = MenuState.Success(categories)
        // Pre-warm Coil singleton cache the moment categories are built
        preloadMenuImages(categories)
    }

    private fun preloadMenuImages(categories: List<MenuCategory>) {
        val app = getApplication<android.app.Application>()
        categories.forEach { category ->
            category.items.forEach { item ->
                if (!item.imageUrl.isNullOrEmpty()) {
                    app.imageLoader.enqueue(
                        ImageRequest.Builder(app)
                            .data(item.imageUrl)
                            .memoryCacheKey(item.id)
                            .memoryCachePolicy(CachePolicy.ENABLED)
                            .diskCachePolicy(CachePolicy.ENABLED)
                            .build()
                    )
                }
            }
        }
    }
    
    fun getMenuItemsByCategory(category: String): List<MenuItem> {
        return _menuItems.value.filter { it.category == category }
    }
    
    fun refreshMenu() {
        loadMenuItems()
    }

    private fun loadBanners() {
        viewModelScope.launch {
            try {
                android.util.Log.d("MenuViewModel", "Fetching banners...")
                val loaded = bannerRepository.getActiveBanners()
                _banners.value = loaded
                _areBannersLoaded.value = true
                android.util.Log.d("MenuViewModel", "Loaded ${loaded.size} banners")
                // Eagerly warm up the Coil singleton cache (same instance as AsyncImage)
                val app = getApplication<android.app.Application>()
                loaded.forEach { banner ->
                    if (banner.imageUrl.isNotEmpty()) {
                        app.imageLoader.enqueue(
                            ImageRequest.Builder(app)
                                .data(banner.imageUrl)
                                .memoryCacheKey(banner.id)
                                .memoryCachePolicy(CachePolicy.ENABLED)
                                .diskCachePolicy(CachePolicy.ENABLED)
                                .build()
                        )
                    }
                }
            } catch (e: Exception) {
                android.util.Log.e("MenuViewModel", "Error loading banners: ${e.message}", e)
                _areBannersLoaded.value = true
            }
        }
    }

    private fun loadReviews() {
        viewModelScope.launch {
            try {
                android.util.Log.d("MenuViewModel", "Fetching reviews...")
                val loaded = reviewRepository.getRecentReviews(limit = 10)
                _reviews.value = loaded
                android.util.Log.d("MenuViewModel", "Loaded ${loaded.size} reviews")
            } catch (e: Exception) {
                android.util.Log.e("MenuViewModel", "Error loading reviews: ${e.message}", e)
            }
        }
    }

    override fun onCleared() {
        super.onCleared()
        availabilityRepository.cleanup()
    }
}
