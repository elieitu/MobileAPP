package dk.itu.moapd.x9.elie.model

data class TrafficReport(
    val id: String = "",
    val userId: String = "",
    val createdAt: Long = 0L,
    val updatedAt: Long = 0L,
    val title: String = "",
    val location: String = "",
    val date: String = "",
    val type: String = "",
    val severity: String = "",
    val description: String = "",
    val latitude: Double = 0.0,
    val longitude: Double = 0.0,
    val address: String = "",
    val imageUrl: String = "",
    val imagePath: String = "",
    val isExpired: Boolean = false,
    val expiresAt: Long = 0L,
    val lastConfirmedAt: Long = 0L
)
