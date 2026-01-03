package wingzone.zenith.data.models

import java.util.UUID

// Enums for customization options
enum class BoneType(val displayName: String) {
    ORIGINAL("Original"),
    BONELESS("Boneless");
    
    companion object {
        fun fromDisplayName(name: String): BoneType? {
            return values().find { it.displayName.equals(name, ignoreCase = true) }
        }
    }
}

enum class Flavor(val displayName: String) {
    BUFFALO_WING("Buffalo Wing"),
    SRIRACHA_HOT_CHILLI("Sriracha Hot Chilli"),
    SOUL_OF_SEOUL("Soul of Seoul"),
    GARLIC_PARM("Garlic Parm"),
    MAMBO_SAUCE("Mambo Sauce"),
    SWEET_SAMURAI("Sweet Samurai"),
    HONEY_Q("Honey Q"),
    BLACKENED_VOODOO("Blackened Voodoo"),
    LEMON_PEPPER("Lemon Pepper"),
    LOUISIANA_SMOKED("Louisiana Smoked"),
    SPICY_ALABAMA("Spicy Alabama"),
    TOKYO_DRAGON("Tokyo Dragon"),
    THAI_CHILI("Thai Chili"),
    SWEET_BOMBOM("Sweet Bombom"),
    SMOKIN_Q("Smokin Q");
    
    companion object {
        fun fromDisplayName(name: String): Flavor? {
            return values().find { it.displayName.equals(name, ignoreCase = true) }
        }
    }
}

enum class DippingSauce(val displayName: String) {
    RANCH("Ranch"),
    BLEU_CHEESE("Bleu Cheese"),
    NONE("None");
    
    companion object {
        fun fromDisplayName(name: String): DippingSauce? {
            return values().find { it.displayName.equals(name, ignoreCase = true) }
        }
    }
}

enum class Drink(val displayName: String) {
    COCA_COLA("Coca-Cola"),
    COKE_ZERO("Coke Zero"),
    SPRITE("Sprite"),
    ICED_LEMON_TEA("Iced Lemon Tea"),
    ORANGE_JUICE("Orange Juice"),
    NONE("None");
    
    companion object {
        fun fromDisplayName(name: String): Drink? {
            return values().find { it.displayName.equals(name, ignoreCase = true) }
        }
    }
}

enum class FriesExchange(val displayName: String, val regularPrice: Double, val jumboPrice: Double?) {
    PREMIUM_WEDGE_FRIES("Premium Wedge Fries", 0.0, 8.0),
    KETTLE_CHIPS("Kettle Chips", 0.0, 8.0),
    SMILEY_FRIES("Smiley Fries", 0.0, null),
    RICE_WITH_GRILLED_VEGE("Rice with Grilled Vege", 0.0, null),
    FLAVOR_RUB_FRIES("Flavor Rub Fries", 5.0, 10.0),
    SWEET_POTATO_FRIES("Sweet Potato Fries", 5.0, 12.0),
    MOZZARELLA_STIX("Mozzarella Stix", 11.0, null),
    CAESAR_SALAD("Caesar Salad", 14.0, null),
    GARDEN_SALAD("Garden Salad", 14.0, null);
    
    companion object {
        fun fromDisplayName(name: String): FriesExchange? {
            return values().find { it.displayName == name }
        }
    }
}

// Menu item data class
data class MenuItem(
    val id: String = UUID.randomUUID().toString(),
    val name: String,
    val description: String,
    val price: Double,
    val category: String,
    val imageUrl: String? = null,
    val displayOrder: Int? = null, // For sorting items within categories
    val kitchenIngredients: KitchenIngredients? = null, // Structured raw materials
    val requiresCustomization: Boolean = false, // True for entrees
    val customizationOptions: CustomizationOptions? = null,
    val isAvailable: Boolean = true // Track availability status
)

data class KitchenIngredient(
    val type: String, // "wings", "fries", "bread", "tenders", "salad", etc.
    val quantity: Int,
    val requiresSelection: Boolean = false // true for wings (bone type selection)
)

data class KitchenIngredients(
    val ingredients: List<KitchenIngredient> = emptyList()
)

data class CustomizationOptions(
    val requiresFlavor: Boolean = false,
    val requiresBeverage: Boolean = false,
    val requiresDippingSauce: Boolean = false,
    val requiresBoneType: Boolean = false,
    val allowFriesExchange: Boolean = false,
    val availableFlavors: List<String> = emptyList(),
    val availableBeverages: List<String> = emptyList(),
    val availableDippingSauces: List<String> = emptyList(),
    val availableBoneTypes: List<String> = emptyList(),
    val friesExchanges: List<FriesExchangeOption> = emptyList()
)

data class FriesExchangeOption(
    val name: String,
    val regularPrice: Double,
    val jumboPrice: Double?,
    val selectedSize: String = "regular", // "regular" or "jumbo"
    val selectedFlavor: String? = null // For Flavor Rub Fries (Blackened Voodoo or Lemon Pepper)
)

// Cart item customization
data class EntreeCustomization(
    val flavor: Flavor,
    val dippingSauce: DippingSauce,
    val drink: Drink,
    val boneType: BoneType? = null,
    val friesExchange: FriesExchangeOption? = null,
    val saladType: String? = null // "Garden Salad" or "Caesar Salad"
)

// Cart item
data class CartItem(
    val id: String = UUID.randomUUID().toString(),
    val menuItem: MenuItem,
    val quantity: Int,
    val customization: EntreeCustomization? = null,
    val specialInstructions: String? = null
) {
    val subtotal: Double
        get() {
            val basePrice = menuItem.price * quantity
            val sideExchangeCost = customization?.friesExchange?.let { exchange ->
                val pricePerUnit = when (exchange.selectedSize) {
                    "jumbo" -> exchange.jumboPrice ?: exchange.regularPrice
                    else -> exchange.regularPrice
                }
                pricePerUnit * quantity
            } ?: 0.0
            return basePrice + sideExchangeCost
        }
}

// App Settings (configurable by admin)
data class AppSettings(
    val taxRate: Double = 0.0, // No tax
    val deliveryFee: Double = 0.0,
    val minimumOrderAmount: Double = 0.0
)

// Cart
data class Cart(
    val items: List<CartItem> = emptyList(),
    val taxRate: Double = 0.0 // No tax
) {
    val totalItems: Int
        get() = items.sumOf { it.quantity }
    
    val subtotal: Double
        get() = items.sumOf { it.subtotal }
    
    val tax: Double
        get() = 0.0 // No tax
    
    val total: Double
        get() = subtotal // Total equals subtotal (no tax)
}
