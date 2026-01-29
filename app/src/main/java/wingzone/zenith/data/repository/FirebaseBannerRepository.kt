package wingzone.zenith.data.repository

import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.tasks.await
import wingzone.zenith.data.model.HomeBanner

class FirebaseBannerRepository {
    private val db = FirebaseFirestore.getInstance()
    private val bannersCollection = db.collection("homeBanners")

    suspend fun getActiveBanners(): List<HomeBanner> {
        return try {
            val snapshot = bannersCollection
                .whereEqualTo("enabled", true)
                .get()
                .await()
            
            snapshot.documents.mapNotNull { doc ->
                try {
                    HomeBanner(
                        id = doc.id,
                        title = doc.getString("title") ?: "",
                        subtitle = doc.getString("subtitle") ?: "",
                        description = doc.getString("description") ?: "",
                        imageUrl = doc.getString("imageUrl") ?: "",
                        backgroundColor = doc.getString("backgroundColor") ?: "#C8102E",
                        accentColor = doc.getString("accentColor") ?: "#FF6B35",
                        order = doc.getLong("order")?.toInt() ?: 0,
                        enabled = doc.getBoolean("enabled") ?: true
                    )
                } catch (e: Exception) {
                    null
                }
            }.sortedBy { it.order }
        } catch (e: Exception) {
            emptyList()
        }
    }
}
