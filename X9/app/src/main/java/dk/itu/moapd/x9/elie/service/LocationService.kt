package dk.itu.moapd.x9.elie.service

import android.app.Service
import android.content.Intent
import android.location.Location
import android.os.Binder
import android.os.IBinder
import android.os.Looper
import androidx.localbroadcastmanager.content.LocalBroadcastManager
import com.google.android.gms.location.FusedLocationProviderClient
import com.google.android.gms.location.LocationCallback
import com.google.android.gms.location.LocationRequest
import com.google.android.gms.location.LocationResult
import com.google.android.gms.location.LocationServices
import com.google.android.gms.location.Priority
import com.google.android.gms.tasks.CancellationTokenSource

class LocationService : Service() {

    companion object {
        const val ACTION_LOCATION_BROADCAST =
            "dk.itu.moapd.x9.elie.ACTION_LOCATION_BROADCAST"
        const val ACTION_FOREGROUND_ONLY_LOCATION_BROADCAST =
            ACTION_LOCATION_BROADCAST
        const val EXTRA_LOCATION =
            "dk.itu.moapd.x9.elie.EXTRA_LOCATION"
        private const val UPDATE_INTERVAL_MILLIS = 10_000L
        private const val MIN_UPDATE_INTERVAL_MILLIS = 5_000L
        private const val MAX_UPDATE_DELAY_MILLIS = 15_000L
    }

    inner class LocalBinder : Binder() {
        fun getService(): LocationService = this@LocationService
    }

    private val binder = LocalBinder()
    private lateinit var fusedLocationProviderClient: FusedLocationProviderClient
    private lateinit var locationCallback: LocationCallback
    private var currentLocationTokenSource: CancellationTokenSource? = null
    private var subscribedToLocationUpdates = false

    var lastKnownLocation: Location? = null
        private set

    override fun onBind(intent: Intent?): IBinder = binder

    override fun onCreate() {
        super.onCreate()
        fusedLocationProviderClient = LocationServices.getFusedLocationProviderClient(this)
        locationCallback = object : LocationCallback() {
            override fun onLocationResult(locationResult: LocationResult) {
                val currentLocation = locationResult.lastLocation ?: return
                broadcastLocation(currentLocation)
            }
        }
    }

    fun subscribeToLocationUpdates() {
        if (subscribedToLocationUpdates) {
            return
        }

        val locationRequest = LocationRequest.Builder(
            Priority.PRIORITY_BALANCED_POWER_ACCURACY,
            UPDATE_INTERVAL_MILLIS
        )
            .setMinUpdateIntervalMillis(MIN_UPDATE_INTERVAL_MILLIS)
            .setMaxUpdateDelayMillis(MAX_UPDATE_DELAY_MILLIS)
            .setWaitForAccurateLocation(false)
            .build()

        try {
            fusedLocationProviderClient.lastLocation.addOnSuccessListener { location ->
                if (location != null) {
                    broadcastLocation(location)
                }
            }

            currentLocationTokenSource?.cancel()
            currentLocationTokenSource = CancellationTokenSource()
            fusedLocationProviderClient
                .getCurrentLocation(
                    Priority.PRIORITY_HIGH_ACCURACY,
                    currentLocationTokenSource?.token
                )
                .addOnSuccessListener { location ->
                    if (location != null) {
                        broadcastLocation(location)
                    }
                }

            fusedLocationProviderClient.requestLocationUpdates(
                locationRequest,
                locationCallback,
                Looper.getMainLooper()
            )
            subscribedToLocationUpdates = true
        } catch (_: SecurityException) {
        }
    }

    fun unsubscribeToLocationUpdates() {
        try {
            currentLocationTokenSource?.cancel()
            currentLocationTokenSource = null
            fusedLocationProviderClient.removeLocationUpdates(locationCallback)
            subscribedToLocationUpdates = false
        } catch (_: SecurityException) {
        }
    }

    override fun onDestroy() {
        unsubscribeToLocationUpdates()
        super.onDestroy()
    }

    private fun broadcastLocation(location: Location) {
        lastKnownLocation = location
        val broadcastIntent = Intent(ACTION_LOCATION_BROADCAST).apply {
            putExtra(EXTRA_LOCATION, location)
        }
        LocalBroadcastManager.getInstance(applicationContext).sendBroadcast(broadcastIntent)
    }
}
