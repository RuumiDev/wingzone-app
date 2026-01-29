package wingzone.zenith.data.models

import java.util.Date
import java.util.UUID

// User model
data class User(
    val id: String = UUID.randomUUID().toString(),
    val email: String,
    val name: String,
    val phoneNumber: String? = null,
    val profileImageUrl: String? = null,
    val wzBalance: Double = 0.0,
    val wzPoints: Int = 0,
    val isPhoneVerified: Boolean = false,
    val isEmailVerified: Boolean = false
)

// Auth state
sealed class AuthState {
    object Unauthenticated : AuthState()
    object Loading : AuthState()
    data class Authenticated(val user: User) : AuthState()
    data class Error(val message: String) : AuthState()
}

// Group Order models
data class GroupMember(
    val userId: String,
    val name: String,
    val profileImageUrl: String? = null,
    val isHost: Boolean = false,
    val cartItems: List<CartItem> = emptyList(),
    val hasPaid: Boolean = false
) {
    val memberTotal: Double
        get() = cartItems.sumOf { it.subtotal }
}

data class GroupOrder(
    val id: String = UUID.randomUUID().toString(),
    val code: String,
    val hostId: String,
    val members: List<GroupMember> = emptyList(),
    val maxMembers: Int = 8,
    val status: GroupOrderStatus = GroupOrderStatus.OPEN,
    val createdAt: Date = Date(),
    val expiresAt: Date,
    val deliveryAddress: String? = null,
    val specialInstructions: String? = null,
    val orderType: String? = null,
    val location: String? = null,
    val paymentMethod: String? = null
) {
    val totalAmount: Double
        get() = members.sumOf { it.memberTotal }
    
    val totalItems: Int
        get() = members.sumOf { member -> member.cartItems.sumOf { it.quantity } }
    
    val isFull: Boolean
        get() = members.size >= maxMembers
        
    val isExpired: Boolean
        get() = Date().after(expiresAt)
    
    val allMembersPaid: Boolean
        get() = members.isNotEmpty() && members.all { it.hasPaid || it.cartItems.isEmpty() }
    
    val canFinalize: Boolean
        get() = allMembersPaid && members.any { it.cartItems.isNotEmpty() } && status == GroupOrderStatus.ORDERING
}

enum class GroupOrderStatus {
    OPEN,       // Members can join
    FULL,       // Max members reached
    ORDERING,   // Members are adding items and paying
    CONFIRMED,  // Host has finalized, sent to kitchen
    COMPLETED,  // Order ready/delivered
    CANCELLED   // Order cancelled
}
