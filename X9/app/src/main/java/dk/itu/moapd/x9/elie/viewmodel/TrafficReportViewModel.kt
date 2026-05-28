package dk.itu.moapd.x9.elie.viewmodel

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dk.itu.moapd.x9.elie.model.TrafficReport
import dk.itu.moapd.x9.elie.repository.StorageRepository
import dk.itu.moapd.x9.elie.repository.TrafficReportRepository
import dk.itu.moapd.x9.elie.work.ReportReminderScheduler
import kotlinx.coroutines.launch
import android.net.Uri

class TrafficReportViewModel : ViewModel() {

    private val repository = TrafficReportRepository()
    private val storageRepository = StorageRepository()
    private val _reports = MutableLiveData<List<TrafficReport>>(emptyList())
    val reports: LiveData<List<TrafficReport>> = _reports
    private val _createReportResult = MutableLiveData<Result<String>>()
    val createReportResult: LiveData<Result<String>> = _createReportResult

    fun addReport(
        report: TrafficReport,
        onSuccess: () -> Unit,
        onError: (String) -> Unit
    ) {
        repository.addReport(report, onSuccess, onError)
    }

    fun addReportReturningId(
        report: TrafficReport,
        onSuccess: (reportId: String) -> Unit,
        onError: (String) -> Unit
    ) {
        repository.addReportReturningId(report, onSuccess, onError)
    }

    fun attachImageToReport(
        userId: String,
        reportId: String,
        imagePath: String,
        imageUrl: String,
        onSuccess: () -> Unit,
        onError: (String) -> Unit
    ) {
        repository.attachImageToReport(
            userId = userId,
            reportId = reportId,
            imagePath = imagePath,
            imageUrl = imageUrl,
            onSuccess = onSuccess,
            onError = onError
        )
    }

    fun observeReports(onError: (String) -> Unit) {
        repository.listenToReports(
            onDataChanged = { _reports.postValue(it) },
            onError = onError
        )
    }

    fun observeAllReports(onError: (String) -> Unit) {
        repository.listenToAllReports(
            onDataChanged = { _reports.postValue(it) },
            onError = onError
        )
    }

    fun removeAt(index: Int) {
        val current = _reports.value ?: return
        if (index !in current.indices) return
        val userId = repository.currentUserId() ?: return
        repository.deleteReport(userId, current[index].id, onSuccess = {}, onError = {})
    }

    fun currentUserId(): String? {
        return repository.currentUserId()
    }

    fun createReport(
        report: TrafficReport,
        imageUri: Uri?
    ) {
        viewModelScope.launch {
            runCatching {
                val reportId = repository.addReportReturningId(report)
                val userId = repository.currentUserId()
                    ?: throw IllegalStateException("You must be logged in")

                if (imageUri != null) {
                    val (imagePath, imageUrl) = storageRepository.uploadReportImage(
                        userId = userId,
                        reportId = reportId,
                        imageUri = imageUri
                    )
                    repository.attachImageToReport(
                        userId = userId,
                        reportId = reportId,
                        imagePath = imagePath,
                        imageUrl = imageUrl
                    )
                }

                ReportReminderScheduler.schedule(
                    context = dk.itu.moapd.x9.elie.X9Application.instance,
                    userId = userId,
                    reportId = reportId,
                    reportTitle = report.title
                )
                reportId
            }.onSuccess { reportId ->
                _createReportResult.value = Result.success(reportId)
            }.onFailure { exception ->
                _createReportResult.value = Result.failure(exception)
            }
        }
    }

    fun deleteReport(
        userId: String,
        reportId: String,
        onSuccess: () -> Unit,
        onError: (String) -> Unit
    ) {
        repository.deleteReport(userId, reportId, onSuccess, onError)
    }

    fun updateReport(
        reportId: String,
        report: TrafficReport,
        onSuccess: () -> Unit,
        onError: (String) -> Unit
    ) {
        repository.updateReport(reportId, report, onSuccess, onError)
    }

    override fun onCleared() {
        repository.clearListener()
        super.onCleared()
    }
}
