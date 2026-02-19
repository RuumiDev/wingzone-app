package wingzone.zenith.data.repository

import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.tasks.await
import wingzone.zenith.data.model.HomeBanner

class FirebaseBannerRepository {
    private val db = FirebaseFirestore.getInstance()
    private val bannersCollection = db.collection("homeBanners")

    suspend fun getActiveBanners(): List<HomeBanner> {
        return try {
            android.util.Log.d("BannerRepository", "Fetching banners from Firestore...")
            val snapshot = bannersCollection
                .whereEqualTo("enabled", true)
                .get()
                .await()
            
            android.util.Log.d("BannerRepository", "Found ${snapshot.documents.size} banner documents")
            
            snapshot.documents.mapNotNull { doc ->
                try {
                    val banner = HomeBanner(
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
                    android.util.Log.d("BannerRepository", "Loaded banner: ${banner.title}")
                    banner
                } catch (e: Exception) {
                    android.util.Log.e("BannerRepository", "Error parsing banner: ${e.message}")
                    null
                }
            }.sortedBy { it.order }
        } catch (e: Exception) {
            android.util.Log.e("BannerRepository", "Error fetching banners: ${e.message}", e)
            emptyList()
        }
    }
}
