package dk.itu.moapd.x9.elie.repository

import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.DatabaseReference
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.Query
import com.google.firebase.database.ValueEventListener
import dk.itu.moapd.x9.elie.model.TrafficReport
import dk.itu.moapd.x9.elie.util.FirebaseConfig
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException

class TrafficReportRepository(
    private val auth: FirebaseAuth = FirebaseAuth.getInstance(),
    private val database: FirebaseDatabase =
        FirebaseDatabase.getInstance(FirebaseConfig.DATABASE_URL)
) {

    companion object {
        private const val PATH_REPORTS = "reports"
        private const val ERROR_NOT_LOGGED_IN = "You must be logged in"
        private const val ERROR_PERMISSION_DENIED = "Permission denied"
        private const val ERROR_NO_INTERNET = "No internet connection"
        private const val ERROR_UNKNOWN = "Unknown database error"
        private const val TWO_HOURS_IN_MILLIS = 2 * 60 * 60 * 1000L
    }

    private val reportsRef: DatabaseReference = database.getReference(PATH_REPORTS)
    private var reportsListener: ValueEventListener? = null

    init {
        reportsRef.keepSynced(true)
    }

    fun currentUserId(): String? = auth.currentUser?.uid

    fun reportsQuery(userId: String): Query {
        return userReportsRef(userId).orderByChild("createdAt")
    }

    fun addReport(
        report: TrafficReport,
        onSuccess: () -> Unit,
        onError: (String) -> Unit
    ) {
        addReportReturningId(
            report = report,
            onSuccess = { onSuccess() },
            onError = onError
        )
    }

    fun addReportReturningId(
        report: TrafficReport,
        onSuccess: (reportId: String) -> Unit,
        onError: (String) -> Unit
    ) {
        val userId = currentUserId() ?: run {
            onError(ERROR_NOT_LOGGED_IN)
            return
        }
        val reportId = userReportsRef(userId).push().key ?: run {
            onError(ERROR_UNKNOWN)
            return
        }
        val now = System.currentTimeMillis()
        val reportWithMetadata = report.copy(
            id = reportId,
            userId = userId,
            createdAt = if (report.createdAt == 0L) now else report.createdAt,
            updatedAt = now,
            isExpired = false,
            expiresAt = now + TWO_HOURS_IN_MILLIS,
            lastConfirmedAt = 0L
        )

        userReportsRef(userId)
            .child(reportId)
            .setValue(reportWithMetadata)
            .addOnSuccessListener { onSuccess(reportId) }
            .addOnFailureListener { exception ->
                onError(mapExceptionToMessage(exception.message))
            }
    }

    suspend fun addReportReturningId(report: TrafficReport): String {
        val userId = currentUserId() ?: throw IllegalStateException(ERROR_NOT_LOGGED_IN)
        val reportId = userReportsRef(userId).push().key ?: throw IllegalStateException(ERROR_UNKNOWN)
        val now = System.currentTimeMillis()
        val reportWithMetadata = report.copy(
            id = reportId,
            userId = userId,
            createdAt = if (report.createdAt == 0L) now else report.createdAt,
            updatedAt = now,
            isExpired = false,
            expiresAt = now + TWO_HOURS_IN_MILLIS,
            lastConfirmedAt = 0L
        )

        userReportsRef(userId)
            .child(reportId)
            .setValue(reportWithMetadata)
            .await()

        return reportId
    }

    fun attachImageToReport(
        userId: String,
        reportId: String,
        imagePath: String,
        imageUrl: String,
        onSuccess: () -> Unit,
        onError: (String) -> Unit
    ) {
        if (userId.isBlank() || reportId.isBlank()) {
            onError(ERROR_UNKNOWN)
            return
        }

        val updates = mapOf<String, Any?>(
            "imagePath" to imagePath,
            "imageUrl" to imageUrl,
            "updatedAt" to System.currentTimeMillis()
        )

        userReportsRef(userId)
            .child(reportId)
            .updateChildren(updates)
            .addOnSuccessListener { onSuccess() }
            .addOnFailureListener { exception ->
                onError(mapExceptionToMessage(exception.message))
            }
    }

    suspend fun attachImageToReport(
        userId: String,
        reportId: String,
        imagePath: String,
        imageUrl: String
    ) {
        if (userId.isBlank() || reportId.isBlank()) {
            throw IllegalStateException(ERROR_UNKNOWN)
        }

        val updates = mapOf<String, Any?>(
            "imagePath" to imagePath,
            "imageUrl" to imageUrl,
            "updatedAt" to System.currentTimeMillis()
        )

        userReportsRef(userId)
            .child(reportId)
            .updateChildren(updates)
            .await()
    }

    fun listenToReports(
        onDataChanged: (List<TrafficReport>) -> Unit,
        onError: (String) -> Unit
    ) {
        val userId = currentUserId() ?: run {
            onError(ERROR_NOT_LOGGED_IN)
            return
        }

        reportsListener?.let { userReportsRef(userId).removeEventListener(it) }

        val listener = object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                val reports = snapshot.children.mapNotNull { child ->
                    child.getValue(TrafficReport::class.java)
                }.filterNot { it.isExpired }
                onDataChanged(reports)
            }

            override fun onCancelled(error: DatabaseError) {
                onError(mapDatabaseErrorToMessage(error))
            }
        }

        reportsListener = listener
        reportsQuery(userId).addValueEventListener(listener)
    }

    fun listenToAllReports(
        onDataChanged: (List<TrafficReport>) -> Unit,
        onError: (String) -> Unit
    ) {
        reportsListener?.let { reportsRef.removeEventListener(it) }

        val listener = object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                val reports = buildList {
                    snapshot.children.forEach { child ->
                        val directReport = child.getValue(TrafficReport::class.java)
                        if (directReport?.id?.isNotBlank() == true) {
                            add(directReport)
                        } else {
                            child.children.mapNotNullTo(this) { nestedChild ->
                                nestedChild.getValue(TrafficReport::class.java)
                            }
                        }
                    }
                }.filterNot { it.isExpired }
                    .sortedByDescending { it.createdAt }

                onDataChanged(reports)
            }

            override fun onCancelled(error: DatabaseError) {
                onError(mapDatabaseErrorToMessage(error))
            }
        }

        reportsListener = listener
        reportsRef.addValueEventListener(listener)
    }

    fun updateReport(
        reportId: String,
        report: TrafficReport,
        onSuccess: () -> Unit,
        onError: (String) -> Unit
    ) {
        val userId = currentUserId() ?: run {
            onError(ERROR_NOT_LOGGED_IN)
            return
        }
        if (reportId.isBlank()) {
            onError(ERROR_UNKNOWN)
            return
        }

        val now = System.currentTimeMillis()
        val updatedReport = report.copy(
            id = reportId,
            userId = userId,
            createdAt = if (report.createdAt == 0L) now else report.createdAt,
            updatedAt = now
        )

        userReportsRef(userId)
            .child(reportId)
            .setValue(updatedReport)
            .addOnSuccessListener { onSuccess() }
            .addOnFailureListener { exception ->
                onError(mapExceptionToMessage(exception.message))
            }
    }

    fun updateReportRelevance(
        userId: String,
        reportId: String,
        stillRelevant: Boolean,
        onSuccess: () -> Unit,
        onError: (String) -> Unit
    ) {
        if (userId.isBlank() || reportId.isBlank()) {
            onError(ERROR_UNKNOWN)
            return
        }

        val now = System.currentTimeMillis()
        val updates: Map<String, Any> = if (stillRelevant) {
            mapOf(
                "isExpired" to false,
                "lastConfirmedAt" to now,
                "updatedAt" to now,
                "expiresAt" to now + TWO_HOURS_IN_MILLIS
            )
        } else {
            mapOf(
                "isExpired" to true,
                "updatedAt" to now
            )
        }

        userReportsRef(userId)
            .child(reportId)
            .updateChildren(updates)
            .addOnSuccessListener { onSuccess() }
            .addOnFailureListener { exception ->
                onError(mapExceptionToMessage(exception.message))
            }
    }

    fun deleteReport(
        userId: String,
        reportId: String,
        onSuccess: () -> Unit,
        onError: (String) -> Unit
    ) {
        if (userId.isBlank()) {
            onError(ERROR_NOT_LOGGED_IN)
            return
        }
        if (reportId.isBlank()) {
            onError(ERROR_UNKNOWN)
            return
        }

        userReportsRef(userId)
            .child(reportId)
            .removeValue()
            .addOnSuccessListener { onSuccess() }
            .addOnFailureListener { exception ->
                onError(mapExceptionToMessage(exception.message))
            }
    }

    fun clearListener() {
        reportsListener?.let { listener ->
            reportsRef.removeEventListener(listener)
            currentUserId()?.let { userReportsRef(it).removeEventListener(listener) }
            reportsListener = null
        }
    }

    private fun userReportsRef(userId: String): DatabaseReference = reportsRef.child(userId)

    private fun mapExceptionToMessage(message: String?): String {
        val normalized = message.orEmpty()
        return when {
            normalized.contains("permission", ignoreCase = true) ||
                normalized.contains("denied", ignoreCase = true) ->
                ERROR_PERMISSION_DENIED
            normalized.contains("network", ignoreCase = true) ||
                normalized.contains("unavailable", ignoreCase = true) ||
                normalized.contains("timeout", ignoreCase = true) ->
                ERROR_NO_INTERNET
            else -> ERROR_UNKNOWN
        }
    }

    private fun mapDatabaseErrorToMessage(error: DatabaseError): String {
        return when (error.code) {
            DatabaseError.PERMISSION_DENIED -> ERROR_PERMISSION_DENIED
            DatabaseError.DISCONNECTED,
            DatabaseError.NETWORK_ERROR,
            DatabaseError.UNAVAILABLE -> ERROR_NO_INTERNET
            else -> ERROR_UNKNOWN
        }
    }

    private suspend fun <T> com.google.android.gms.tasks.Task<T>.await(): T =
        suspendCancellableCoroutine { continuation ->
            addOnSuccessListener { result -> continuation.resume(result) }
            addOnFailureListener { exception -> continuation.resumeWithException(exception) }
            addOnCanceledListener { continuation.cancel() }
        }
}
