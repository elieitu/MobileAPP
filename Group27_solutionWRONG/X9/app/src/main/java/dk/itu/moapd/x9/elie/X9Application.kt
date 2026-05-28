package dk.itu.moapd.x9.elie

import android.app.Application
import android.app.NotificationChannel
import android.app.NotificationManager
import android.os.Build
import com.google.firebase.database.FirebaseDatabase
import dk.itu.moapd.x9.elie.util.FirebaseConfig

class X9Application : Application() {

    override fun onCreate() {
        super.onCreate()
        instance = this
        FirebaseConfig.init(this)
        FirebaseDatabase.getInstance(FirebaseConfig.DATABASE_URL).setPersistenceEnabled(true)
        createNotificationChannel()
    }

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                REPORT_RELEVANCE_CHANNEL_ID,
                "Report relevance reminders",
                NotificationManager.IMPORTANCE_DEFAULT
            ).apply {
                description = "Asks whether a traffic report is still relevant"
            }

            val manager = getSystemService(NotificationManager::class.java)
            manager.createNotificationChannel(channel)
        }
    }

    companion object {
        const val REPORT_RELEVANCE_CHANNEL_ID = "report_relevance_channel"
        lateinit var instance: X9Application
            private set
    }
}
