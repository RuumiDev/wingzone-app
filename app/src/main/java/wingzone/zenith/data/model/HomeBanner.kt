package wingzone.zenith.data.model

data class HomeBanner(
    val id: String = "",
    val title: String = "",
    val subtitle: String = "",
    val description: String = "",
    val imageUrl: String = "",
    val backgroundColor: String = "#C8102E",
    val accentColor: String = "#FF6B35",
    val order: Int = 0,
    val enabled: Boolean = true
)
