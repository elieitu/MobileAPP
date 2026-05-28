package dk.itu.moapd.x9.elie.ui

import android.Manifest
import android.app.Activity
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.content.ServiceConnection
import android.content.pm.PackageManager
import android.location.Location
import android.os.Build
import android.os.Bundle
import android.os.IBinder
import android.text.format.DateFormat
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.localbroadcastmanager.content.LocalBroadcastManager
import androidx.navigation.NavController
import androidx.navigation.fragment.NavHostFragment
import com.firebase.ui.auth.AuthUI
import com.firebase.ui.auth.FirebaseAuthUIActivityResultContract
import com.firebase.ui.auth.data.model.FirebaseAuthUIAuthenticationResult
import com.google.firebase.auth.FirebaseAuth
import dk.itu.moapd.x9.elie.R
import dk.itu.moapd.x9.elie.databinding.ActivityMainBinding
import dk.itu.moapd.x9.elie.receiver.LocationBroadcastReceiver
import dk.itu.moapd.x9.elie.service.LocationService
import java.util.Date

class MainActivity : AppCompatActivity() {

    private lateinit var binding: ActivityMainBinding
    private lateinit var navController: NavController
    private val auth: FirebaseAuth by lazy { FirebaseAuth.getInstance() }
    private var locationService: LocationService? = null
    private var locationServiceBound = false

    private val signInLauncher = registerForActivityResult(
        FirebaseAuthUIActivityResultContract()
    ) { result ->
        onSignInResult(result)
    }

    private val permissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { granted ->
        if (granted) {
            locationService?.subscribeToLocationUpdates()
        } else {
            Toast.makeText(this, R.string.location_permission_denied, Toast.LENGTH_SHORT).show()
        }
        updatePermissionButtonState()
    }

    private val notificationPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { }

    private val locationReceiver = LocationBroadcastReceiver { location ->
        updateLocationUi(location)
    }

    private val serviceConnection = object : ServiceConnection {
        override fun onServiceConnected(name: ComponentName?, service: IBinder?) {
            val binder = service as? LocationService.LocalBinder ?: return
            locationService = binder.getService()
            locationServiceBound = true
            locationService?.lastKnownLocation?.let(::updateLocationUi)

            if (hasLocationPermission()) {
                locationService?.subscribeToLocationUpdates()
            }
        }

        override fun onServiceDisconnected(name: ComponentName?) {
            locationService = null
            locationServiceBound = false
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        requestNotificationPermissionIfNeeded()
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        val navHostFragment = supportFragmentManager
            .findFragmentById(R.id.nav_host_fragment) as NavHostFragment
        navController = navHostFragment.navController

        binding.buttonRequestPermission.setOnClickListener {
            if (hasLocationPermission()) {
                locationService?.subscribeToLocationUpdates()
            } else {
                permissionLauncher.launch(Manifest.permission.ACCESS_FINE_LOCATION)
            }
        }
        updatePermissionButtonState()

        if (auth.currentUser == null) {
            launchSignInFlow()
        }
    }

    override fun onStart() {
        super.onStart()
        LocalBroadcastManager.getInstance(this).registerReceiver(
            locationReceiver,
            IntentFilter(LocationService.ACTION_LOCATION_BROADCAST)
        )
        bindService(
            Intent(this, LocationService::class.java),
            serviceConnection,
            Context.BIND_AUTO_CREATE
        )
    }

    override fun onStop() {
        if (locationServiceBound) {
            locationService?.unsubscribeToLocationUpdates()
            unbindService(serviceConnection)
            locationServiceBound = false
        }
        LocalBroadcastManager.getInstance(this).unregisterReceiver(locationReceiver)
        super.onStop()
    }

    fun launchSignInFlow() {
        val providers = listOf(
            AuthUI.IdpConfig.EmailBuilder().build()
        )

        val signInIntent = AuthUI.getInstance()
            .createSignInIntentBuilder()
            .setAvailableProviders(providers)
            .setTheme(R.style.Theme_X9_Auth)
            .build()

        signInLauncher.launch(signInIntent)
    }

    private fun onSignInResult(result: FirebaseAuthUIAuthenticationResult) {
        if (result.resultCode == Activity.RESULT_OK && auth.currentUser != null) {
            Toast.makeText(this, "Logget ind i Firebase.", Toast.LENGTH_SHORT).show()
            recreate()
            return
        }

        Toast.makeText(
            this,
            "Login er påkrævet for at bruge Firebase i appen.",
            Toast.LENGTH_LONG
        ).show()
    }

    private fun hasLocationPermission(): Boolean {
        return ContextCompat.checkSelfPermission(
            this,
            Manifest.permission.ACCESS_FINE_LOCATION
        ) == PackageManager.PERMISSION_GRANTED
    }

    private fun requestNotificationPermissionIfNeeded() {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU) return

        val granted = ContextCompat.checkSelfPermission(
            this,
            Manifest.permission.POST_NOTIFICATIONS
        ) == PackageManager.PERMISSION_GRANTED

        if (!granted) {
            notificationPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
        }
    }

    private fun updatePermissionButtonState() {
        binding.buttonRequestPermission.text = if (hasLocationPermission()) {
            getString(R.string.refresh_location_updates)
        } else {
            getString(R.string.grant_location_permission)
        }
    }

    private fun updateLocationUi(location: Location) {
        binding.textViewLatitude.text = getString(
            R.string.location_latitude_value,
            location.latitude
        )
        binding.textViewLongitude.text = getString(
            R.string.location_longitude_value,
            location.longitude
        )
        binding.textViewAltitude.text = getString(
            R.string.location_altitude_value,
            location.altitude
        )
        binding.textViewSpeed.text = getString(
            R.string.location_speed_value,
            location.speed
        )
        binding.textViewTime.text = getString(
            R.string.location_time_value,
            DateFormat.format("yyyy-MM-dd HH:mm:ss", Date(location.time))
        )
    }
}
