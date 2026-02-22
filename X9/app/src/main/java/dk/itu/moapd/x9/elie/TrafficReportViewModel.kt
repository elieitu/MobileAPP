package dk.itu.moapd.x9.elie

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel

class TrafficReportViewModel : ViewModel() {

    private val _reports = MutableLiveData<List<TrafficReport>>(emptyList())
    val reports: LiveData<List<TrafficReport>> = _reports

    fun addReport(report: TrafficReport) {
        val current = _reports.value ?: emptyList()
        _reports.value = current + report
    }

    fun removeAt(index: Int) {
        val current = _reports.value ?: return
        if (index !in current.indices) return
        _reports.value = current.toMutableList().also { it.removeAt(index) }
    }

    fun addMockDataIfEmpty() {
        val current = _reports.value ?: emptyList()
        if (current.isNotEmpty()) return

        _reports.value = listOf(
            TrafficReport(
                title = "Speed camera",
                location = "Lyngby",
                date = "2026-02-21",
                type = "Camera",
                severity = "Minor",
                description = "Near the intersection"
            ),
            TrafficReport(
                title = "Heavy traffic",
                location = "Nørrebro",
                date = "2026-02-21",
                type = "Traffic",
                severity = "Moderate",
                description = "Slow moving cars"
            )
        )
    }
}