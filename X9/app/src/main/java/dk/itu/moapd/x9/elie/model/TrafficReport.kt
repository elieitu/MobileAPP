package dk.itu.moapd.x9.elie.model

data class TrafficReport(
    val title: String,
    val location: String,
    val date: String,
    val type: String,
    val severity: String,
    val description: String
)
