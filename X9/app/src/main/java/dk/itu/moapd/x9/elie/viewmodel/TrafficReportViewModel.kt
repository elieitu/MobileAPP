package dk.itu.moapd.x9.elie.viewmodel

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import dk.itu.moapd.x9.elie.model.TrafficReport

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
        val updated = current.toMutableList()
        updated.removeAt(index)
        _reports.value = updated
    }
}
