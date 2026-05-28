package dk.itu.moapd.x9.elie.receiver

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.widget.Toast
import androidx.core.app.NotificationManagerCompat
import dk.itu.moapd.x9.elie.R
import dk.itu.moapd.x9.elie.repository.TrafficReportRepository
import dk.itu.moapd.x9.elie.work.ReportRelevanceWorker
import dk.itu.moapd.x9.elie.work.ReportReminderScheduler

class ReportRelevanceActionReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent) {
        val userId = intent.getStringExtra(ReportRelevanceWorker.KEY_USER_ID).orEmpty()
        val reportId = intent.getStringExtra(ReportRelevanceWorker.KEY_REPORT_ID).orEmpty()
        val reportTitle = intent.getStringExtra(ReportRelevanceWorker.KEY_REPORT_TITLE).orEmpty()

        if (userId.isBlank() || reportId.isBlank()) {
            return
        }

        NotificationManagerCompat.from(context).cancel(reportId.hashCode())

        val repository = TrafficReportRepository()
        when (intent.action) {
            ACTION_STILL_RELEVANT -> {
                repository.updateReportRelevance(
                    userId = userId,
                    reportId = reportId,
                    stillRelevant = true,
                    onSuccess = {
                        ReportReminderScheduler.schedule(
                            context = context,
                            userId = userId,
                            reportId = reportId,
                            reportTitle = reportTitle
                        )
                        Toast.makeText(
                            context,
                            R.string.report_kept_active,
                            Toast.LENGTH_SHORT
                        ).show()
                    },
                    onError = { showError(context) }
                )
            }
            ACTION_NO_LONGER_RELEVANT -> {
                repository.updateReportRelevance(
                    userId = userId,
                    reportId = reportId,
                    stillRelevant = false,
                    onSuccess = {
                        ReportReminderScheduler.cancel(context, reportId)
                        Toast.makeText(
                            context,
                            R.string.report_marked_expired,
                            Toast.LENGTH_SHORT
                        ).show()
                    },
                    onError = { showError(context) }
                )
            }
        }
    }

    private fun showError(context: Context) {
        Toast.makeText(
            context,
            R.string.report_relevance_update_failed,
            Toast.LENGTH_SHORT
        ).show()
    }

    companion object {
        const val ACTION_STILL_RELEVANT = "dk.itu.moapd.x9.elie.ACTION_STILL_RELEVANT"
        const val ACTION_NO_LONGER_RELEVANT = "dk.itu.moapd.x9.elie.ACTION_NO_LONGER_RELEVANT"
    }
}
