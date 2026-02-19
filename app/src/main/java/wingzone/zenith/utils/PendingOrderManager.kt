package wingzone.zenith.utils

import android.content.Context
import org.json.JSONObject
import wingzone.zenith.data.models.Cart
import wingzone.zenith.data.models.CartItem
import wingzone.zenith.data.models.EntreeCustomization
import wingzone.zenith.data.models.FriesExchange
import java.util.UUID

object PendingOrderManager {
    private const val PREFS_NAME = "pending_orders"
    private const val KEY_PREFIX = "pending_order_"
    
    /**
     * Store a pending order and return its unique ID
     */
    fun storePendingOrder(
        context: Context,
        userId: String,
        userName: String,
        userEmail: String? = null,
        cart: Cart,
        paymentMethod: String,
        paymentType: String,
        phoneNumber: String?,
        orderType: String?,
        location: String?,
        lobbyId: String? = null,
        paymentUrl: String? = null
    ): String {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        val pendingOrderId = UUID.randomUUID().toString()
        
        val orderData = JSONObject().apply {
            put("userId", userId)
            put("userName", userName)
            put("userEmail", userEmail ?: "")
            put("cart", serializeCart(cart))
            put("paymentMethod", paymentMethod)
            put("paymentType", paymentType)
            put("phoneNumber", phoneNumber ?: "")
            put("orderType", orderType ?: "")
            put("location", location ?: "")
            put("lobbyId", lobbyId ?: "")
            put("paymentUrl", paymentUrl ?: "")
            put("timestamp", System.currentTimeMillis())
        }
        
        prefs.edit()
            .putString(KEY_PREFIX + pendingOrderId, orderData.toString())
            .apply()
        
        android.util.Log.d("PendingOrderManager", "Stored pending order: $pendingOrderId")
        return pendingOrderId
    }
    
    /**
     * Update payment URL for a pending order
     */
    fun updatePaymentUrl(context: Context, pendingOrderId: String, paymentUrl: String) {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        val existing = getPendingOrder(context, pendingOrderId)
        
        if (existing != null) {
            existing.put("paymentUrl", paymentUrl)
            prefs.edit()
                .putString(KEY_PREFIX + pendingOrderId, existing.toString())
                .apply()
            android.util.Log.d("PendingOrderManager", "Updated payment URL for: $pendingOrderId")
        }
    }
    
    /**
     * Retrieve a pending order by its ID
     */
    fun getPendingOrder(context: Context, pendingOrderId: String): JSONObject? {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        val json = prefs.getString(KEY_PREFIX + pendingOrderId, null) ?: return null
        
        return try {
            JSONObject(json)
        } catch (e: Exception) {
            android.util.Log.e("PendingOrderManager", "Failed to parse pending order", e)
            null
        }
    }
    
    /**
     * Delete a pending order after it's been processed
     */
    fun deletePendingOrder(context: Context, pendingOrderId: String) {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        prefs.edit()
            .remove(KEY_PREFIX + pendingOrderId)
            .apply()
        android.util.Log.d("PendingOrderManager", "Deleted pending order: $pendingOrderId")
    }
    
    /**
     * Clean up old pending orders (older than 24 hours)
     */
    fun cleanupOldOrders(context: Context) {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        val editor = prefs.edit()
        val now = System.currentTimeMillis()
        val dayInMillis = 24 * 60 * 60 * 1000
        
        prefs.all.forEach { (key, value) ->
            if (key.startsWith(KEY_PREFIX) && value is String) {
                try {
                    val data = JSONObject(value)
                    val timestamp = data.getLong("timestamp")
                    
                    if (now - timestamp > dayInMillis) {
                        editor.remove(key)
                        android.util.Log.d("PendingOrderManager", "Cleaned up old order: $key")
                    }
                } catch (e: Exception) {
                    android.util.Log.e("PendingOrderManager", "Error parsing order: $key", e)
                }
            }
        }
        
        editor.apply()
    }
    
    /**
     * Get payment URL by creating ToyyibPay bill
     * This makes an HTTP request to create the bill and returns the payment URL
     */
    suspend fun getPaymentUrl(context: Context, pendingOrderId: String): Result<String> {
        return try {
            val pendingOrder = getPendingOrder(context, pendingOrderId)
                ?: return Result.failure(Exception("Pending order not found"))
            
            // Check if payment URL already exists
            val existingUrl = pendingOrder.optString("paymentUrl", null)
            if (!existingUrl.isNullOrEmpty()) {
                return Result.success(existingUrl)
            }
            
            // Extract order details
            val userId = pendingOrder.getString("userId")
            val userName = pendingOrder.getString("userName")
            val userEmail = pendingOrder.optString("userEmail", "").ifEmpty { "$userId@wingzone.app" }
            val phoneNumber = if (pendingOrder.has("phoneNumber") && !pendingOrder.isNull("phoneNumber"))
                pendingOrder.getString("phoneNumber") else ""
            
            // Get cart total
            val cartJson = pendingOrder.getString("cart")
            val cart = JSONObject(cartJson)
            val totalAmount = cart.getDouble("total")
            
            // Create ToyyibPay bill
            val paymentRepository = wingzone.zenith.data.repository.PaymentRepository()
            val result = paymentRepository.createToyyibPayBill(
                orderId = pendingOrderId,
                customerName = userName,
                customerEmail = userEmail,
                totalAmount = totalAmount,
                customerPhone = phoneNumber.ifEmpty { null }
            )
            
            result.onSuccess { paymentUrl ->
                // Store payment URL in pending order for future reference
                updatePaymentUrl(context, pendingOrderId, paymentUrl)
            }
            
            result
        } catch (e: Exception) {
            android.util.Log.e("PendingOrderManager", "Error creating payment URL", e)
            Result.failure(e)
        }
    }
    
    private fun serializeCart(cart: Cart): String {
        val json = JSONObject().apply {
            put("subtotal", cart.subtotal)
            put("tax", cart.tax)
            put("total", cart.total)
            put("totalItems", cart.totalItems)
            put("taxRate", cart.taxRate)
            // Note: Cart items will need to be stored separately with full customization details
        }
        return json.toString()
    }
    
    fun deserializeCart(json: String): Cart {
        val obj = JSONObject(json)
        // Note: Cart with empty items list - actual items should be stored separately
        // This just preserves the totals for reference
        return Cart(
            items = emptyList(),
            taxRate = obj.optDouble("taxRate", 0.0)
        )
    }
}
