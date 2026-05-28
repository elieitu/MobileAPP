package dk.itu.moapd.x9.elie.work

import android.Manifest
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import dk.itu.moapd.x9.elie.R
import dk.itu.moapd.x9.elie.X9Application
import dk.itu.moapd.x9.elie.receiver.ReportRelevanceActionReceiver

class ReportRelevanceWorker(
    private val context: Context,
    params: WorkerParameters
) : CoroutineWorker(context, params) {

    override suspend fun doWork(): Result {
        val userId = inputData.getString(KEY_USER_ID) ?: return Result.failure()
        val reportId = inputData.getString(KEY_REPORT_ID) ?: return Result.failure()
        val reportTitle = inputData.getString(KEY_REPORT_TITLE) ?: "Traffic report"

        val stillRelevantIntent = Intent(context, ReportRelevanceActionReceiver::class.java).apply {
            action = ReportRelevanceActionReceiver.ACTION_STILL_RELEVANT
            putExtra(KEY_USER_ID, userId)
            putExtra(KEY_REPORT_ID, reportId)
            putExtra(KEY_REPORT_TITLE, reportTitle)
        }

        val noLongerRelevantIntent = Intent(context, ReportRelevanceActionReceiver::class.java).apply {
            action = ReportRelevanceActionReceiver.ACTION_NO_LONGER_RELEVANT
            putExtra(KEY_USER_ID, userId)
            putExtra(KEY_REPORT_ID, reportId)
            putExtra(KEY_REPORT_TITLE, reportTitle)
        }

        val stillRelevantPendingIntent = PendingIntent.getBroadcast(
            context,
            reportId.hashCode(),
            stillRelevantIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val noLongerRelevantPendingIntent = PendingIntent.getBroadcast(
            context,
            reportId.hashCode() + 1,
            noLongerRelevantIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val notification = NotificationCompat.Builder(
            context,
            X9Application.REPORT_RELEVANCE_CHANNEL_ID
        )
            .setSmallIcon(R.drawable.baseline_notifications_24)
            .setContentTitle(context.getString(R.string.report_relevance_question))
            .setContentText(reportTitle)
            .setPriority(NotificationCompat.PRIORITY_DEFAULT)
            .setAutoCancel(true)
            .addAction(
                0,
                context.getString(R.string.report_still_relevant),
                stillRelevantPendingIntent
            )
            .addAction(
                0,
                context.getString(R.string.report_no_longer_relevant),
                noLongerRelevantPendingIntent
            )
            .build()

        val hasPermission = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            ContextCompat.checkSelfPermission(
                context,
                Manifest.permission.POST_NOTIFICATIONS
            ) == PackageManager.PERMISSION_GRANTED
        } else {
            true
        }

        if (hasPermission) {
            NotificationManagerCompat.from(context).notify(reportId.hashCode(), notification)
        }

        return Result.success()
    }

    companion object {
        const val KEY_USER_ID = "user_id"
        const val KEY_REPORT_ID = "report_id"
        const val KEY_REPORT_TITLE = "report_title"
    }
}
