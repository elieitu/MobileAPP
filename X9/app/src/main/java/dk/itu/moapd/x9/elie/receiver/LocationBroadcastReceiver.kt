package dk.itu.moapd.x9.elie.receiver

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.location.Location
import android.os.Build
import dk.itu.moapd.x9.elie.service.LocationService

class LocationBroadcastReceiver(
    private val onLocationReceived: (Location) -> Unit
) : BroadcastReceiver() {

    override fun onReceive(context: Context?, intent: Intent?) {
        val safeIntent = intent ?: return
        val location = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            safeIntent.getParcelableExtra(LocationService.EXTRA_LOCATION, Location::class.java)
        } else {
            @Suppress("DEPRECATION")
            safeIntent.getParcelableExtra(LocationService.EXTRA_LOCATION)
        }

        if (location != null) {
            onLocationReceived(location)
        }
    }
}
