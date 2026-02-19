package wingzone.zenith.data.models

import java.util.Date

/**
 * Review data model for order ratings and feedback
 */
data class Review(
    val id: String = "",
    val orderId: String = "",
    val userId: String = "",
    val userName: String = "",
    val rating: Int = 0, // 1-5 stars
    val comment: String = "",
    val createdAt: Date = Date(),
    val menuItemIds: List<String> = emptyList(), // Items in the reviewed order
    val isEnabled: Boolean = true, // Admin can disable reviews
    val moderationStatus: String = "approved" // approved, pending, rejected
)
