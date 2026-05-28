package dk.itu.moapd.x9.elie.work

import android.content.Context
import androidx.work.Data
import androidx.work.ExistingWorkPolicy
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkManager
import java.util.concurrent.TimeUnit

object ReportReminderScheduler {

    fun schedule(
        context: Context,
        userId: String,
        reportId: String,
        reportTitle: String
    ) {
        val inputData = Data.Builder()
            .putString(ReportRelevanceWorker.KEY_USER_ID, userId)
            .putString(ReportRelevanceWorker.KEY_REPORT_ID, reportId)
            .putString(ReportRelevanceWorker.KEY_REPORT_TITLE, reportTitle)
            .build()

        val request = OneTimeWorkRequestBuilder<ReportRelevanceWorker>()
            .setInitialDelay(2, TimeUnit.HOURS)
            .setInputData(inputData)
            .addTag("report_relevance_$reportId")
            .build()

        WorkManager.getInstance(context).enqueueUniqueWork(
            "report_relevance_$reportId",
            ExistingWorkPolicy.REPLACE,
            request
        )
    }

    fun cancel(context: Context, reportId: String) {
        WorkManager.getInstance(context).cancelUniqueWork("report_relevance_$reportId")
    }
}
