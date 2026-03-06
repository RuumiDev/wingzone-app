package wingzone.zenith.data.repository

import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query
import kotlinx.coroutines.tasks.await
import wingzone.zenith.data.models.Review
import java.util.Date

class FirebaseReviewRepository {
    private val firestore = FirebaseFirestore.getInstance()
    private val reviewsCollection = firestore.collection("reviews")
    private val ordersCollection = firestore.collection("orders")
    
    /**
     * Submit a review for an order
     */
    suspend fun submitReview(
        orderId: String,
        userId: String,
        userName: String,
        rating: Int,
        comment: String,
        menuItemIds: List<String>
    ): Result<String> {
        return try {
            // Create review document
            val reviewData = hashMapOf(
                "orderId" to orderId,
                "userId" to userId,
                "userName" to userName,
                "rating" to rating,
                "comment" to comment,
                "menuItemIds" to menuItemIds,
                "createdAt" to com.google.firebase.Timestamp.now(),
                "isEnabled" to true,
                "moderationStatus" to "pending"
            )
            
            val reviewRef = reviewsCollection.add(reviewData).await()
            
            // Update order to mark it as rated (best-effort — don't fail the review if this fails)
            try {
                ordersCollection.document(orderId).update(
                    mapOf(
                        "ratedAt" to com.google.firebase.Timestamp.now(),
                        "rating" to rating
                    )
                ).await()
            } catch (updateError: Exception) {
                android.util.Log.w("ReviewRepository", "Could not update order rating: ${updateError.message}")
            }
            
            Result.success(reviewRef.id)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
    
    /**
     * Get recent reviews for display on home screen
     */
    suspend fun getRecentReviews(limit: Int = 10): List<Review> {
        return try {
            val snapshot = reviewsCollection
                .whereEqualTo("isEnabled", true)
                .whereEqualTo("moderationStatus", "approved")
                .orderBy("createdAt", Query.Direction.DESCENDING)
                .limit(limit.toLong())
                .get()
                .await()
            
            snapshot.documents.mapNotNull { doc ->
                try {
                    Review(
                        id = doc.id,
                        orderId = doc.getString("orderId") ?: "",
                        userId = doc.getString("userId") ?: "",
                        userName = doc.getString("userName") ?: "",
                        rating = (doc.getLong("rating") ?: 0).toInt(),
                        comment = doc.getString("comment") ?: "",
                        createdAt = (doc.get("createdAt") as? com.google.firebase.Timestamp)?.toDate() ?: Date(),
                        menuItemIds = (doc.get("menuItemIds") as? List<*>)?.mapNotNull { it as? String } ?: emptyList(),
                        isEnabled = doc.getBoolean("isEnabled") ?: true,
                        moderationStatus = doc.getString("moderationStatus") ?: "approved"
                    )
                } catch (e: Exception) {
                    null
                }
            }
        } catch (e: Exception) {
            emptyList()
        }
    }
    
    /**
     * Check if an order has been rated
     */
    suspend fun isOrderRated(orderId: String): Boolean {
        return try {
            val snapshot = reviewsCollection
                .whereEqualTo("orderId", orderId)
                .limit(1)
                .get()
                .await()
            
            !snapshot.isEmpty
        } catch (e: Exception) {
            false
        }
    }
    
    /**
     * Get reviews by user
     */
    suspend fun getUserReviews(userId: String): List<Review> {
        return try {
            val snapshot = reviewsCollection
                .whereEqualTo("userId", userId)
                .orderBy("createdAt", Query.Direction.DESCENDING)
                .get()
                .await()
            
            snapshot.documents.mapNotNull { doc ->
                try {
                    Review(
                        id = doc.id,
                        orderId = doc.getString("orderId") ?: "",
                        userId = doc.getString("userId") ?: "",
                        userName = doc.getString("userName") ?: "",
                        rating = (doc.getLong("rating") ?: 0).toInt(),
                        comment = doc.getString("comment") ?: "",
                        createdAt = (doc.get("createdAt") as? com.google.firebase.Timestamp)?.toDate() ?: Date(),
                        menuItemIds = (doc.get("menuItemIds") as? List<*>)?.mapNotNull { it as? String } ?: emptyList(),
                        isEnabled = doc.getBoolean("isEnabled") ?: true,
                        moderationStatus = doc.getString("moderationStatus") ?: "approved"
                    )
                } catch (e: Exception) {
                    null
                }
            }
        } catch (e: Exception) {
            emptyList()
        }
    }
}
