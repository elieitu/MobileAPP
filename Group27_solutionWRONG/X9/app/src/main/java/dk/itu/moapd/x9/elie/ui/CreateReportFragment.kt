package dk.itu.moapd.x9.elie.ui

import android.Manifest
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.content.ServiceConnection
import android.content.pm.PackageManager
import android.location.Geocoder
import android.location.Location
import android.net.Uri
import android.os.Bundle
import android.os.IBinder
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ArrayAdapter
import android.widget.Toast
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.content.ContextCompat
import androidx.core.os.bundleOf
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.localbroadcastmanager.content.LocalBroadcastManager
import androidx.navigation.fragment.findNavController
import dk.itu.moapd.x9.elie.R
import dk.itu.moapd.x9.elie.databinding.FragmentCreateReportBinding
import dk.itu.moapd.x9.elie.model.TrafficReport
import dk.itu.moapd.x9.elie.receiver.LocationBroadcastReceiver
import dk.itu.moapd.x9.elie.service.LocationService
import dk.itu.moapd.x9.elie.viewmodel.TrafficReportViewModel
import java.text.DateFormat
import java.util.Date
import java.util.Locale

class CreateReportFragment : Fragment() {

    companion object {
        private const val TAG = "X9_CREATE_REPORT"
    }

    private var _binding: FragmentCreateReportBinding? = null
    private val binding get() = checkNotNull(_binding)
    private val viewModel: TrafficReportViewModel by activityViewModels()
    private var currentLocation: Location? = null
    private var resolvedAddress: String? = null
    private var locationService: LocationService? = null
    private var locationServiceBound = false
    private var selectedImageUri: Uri? = null
    private var pendingCreatedReport: Bundle? = null

    private val locationReceiver = LocationBroadcastReceiver { location ->
        updateLocation(location)
    }

    private val serviceConnection = object : ServiceConnection {
        override fun onServiceConnected(name: ComponentName?, service: IBinder?) {
            val binder = service as? LocationService.LocalBinder ?: return
            locationService = binder.getService()
            locationServiceBound = true
            locationService?.lastKnownLocation?.let(::updateLocation)
            locationService?.subscribeToLocationUpdates()
        }

        override fun onServiceDisconnected(name: ComponentName?) {
            locationService = null
            locationServiceBound = false
        }
    }

    private val permissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { granted ->
        if (granted) {
            bindToLocationService()
        } else if (_binding != null) {
            binding.locationStatusText.text = getString(R.string.location_permission_denied)
        }
    }

    private val imagePickerLauncher = registerForActivityResult(
        ActivityResultContracts.PickVisualMedia()
    ) { uri ->
        if (_binding == null) return@registerForActivityResult
        if (uri == null) {
            binding.imageStatusText.text = getString(R.string.image_pick_cancelled)
            Toast.makeText(
                requireContext(),
                R.string.image_pick_cancelled,
                Toast.LENGTH_SHORT
            ).show()
        } else {
            showSelectedImage(uri)
        }
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentCreateReportBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val typeAdapter = ArrayAdapter(
            requireContext(),
            android.R.layout.simple_list_item_1,
            resources.getStringArray(R.array.report_type_options)
        )
        binding.typeInput.setAdapter(typeAdapter)
        binding.typeInput.setOnClickListener { binding.typeInput.showDropDown() }

        val severityAdapter = ArrayAdapter(
            requireContext(),
            android.R.layout.simple_list_item_1,
            resources.getStringArray(R.array.severity_options)
        )
        binding.severityInput.setAdapter(severityAdapter)
        binding.severityInput.setOnClickListener { binding.severityInput.showDropDown() }

        binding.resolveAddressButton.setOnClickListener {
            val location = currentLocation
            if (location == null) {
                Toast.makeText(
                    requireContext(),
                    R.string.location_not_available,
                    Toast.LENGTH_SHORT
                ).show()
                return@setOnClickListener
            }
            resolveAddress(location)
        }

        binding.selectImageButton.setOnClickListener {
            openImagePicker()
        }

        binding.submitButton.setOnClickListener {
            val type = binding.typeInput.text?.toString()?.trim().orEmpty()
            val description = binding.descriptionInput.text?.toString()?.trim().orEmpty()
            val severity = binding.severityInput.text?.toString()?.trim().orEmpty()
            val location = currentLocation

            val missing = mutableListOf<String>()
            if (type.isEmpty()) missing.add("type")
            if (description.isEmpty()) missing.add("description")
            if (severity.isEmpty()) missing.add("severity")

            if (missing.isNotEmpty()) {
                Log.w(TAG, "Invalid submission: missing ${missing.joinToString(", ")}")
                Toast.makeText(
                    requireContext(),
                    R.string.invalid_submission_fill_everything,
                    Toast.LENGTH_SHORT
                ).show()
                return@setOnClickListener
            }
            if (location == null) {
                Toast.makeText(
                    requireContext(),
                    R.string.location_not_available,
                    Toast.LENGTH_SHORT
                ).show()
                requestLocationPermissionIfNeeded()
                return@setOnClickListener
            }

            submitReport(type, description, severity, location)
        }

        viewModel.createReportResult.observe(viewLifecycleOwner) { result ->
            binding.submitButton.isEnabled = true
            result.onSuccess {
                if (!isAdded) return@onSuccess
                binding.imageStatusText.text = getString(R.string.image_upload_success)
                Toast.makeText(
                    requireContext(),
                    R.string.traffic_report_created,
                    Toast.LENGTH_SHORT
                ).show()
                pendingCreatedReport?.let { createdReport ->
                    findNavController().previousBackStackEntry
                        ?.savedStateHandle
                        ?.set("created_report", createdReport)
                }
                pendingCreatedReport = null
                findNavController().popBackStack()
            }.onFailure { exception ->
                if (!isAdded) return@onFailure
                pendingCreatedReport = null
                val fallbackMessage = if (selectedImageUri != null) {
                    getString(R.string.image_upload_failed)
                } else {
                    getString(R.string.report_create_failed)
                }
                binding.imageStatusText.text = fallbackMessage
                Toast.makeText(
                    requireContext(),
                    exception.message ?: fallbackMessage,
                    Toast.LENGTH_SHORT
                ).show()
            }
        }
    }

    override fun onStart() {
        super.onStart()
        Log.d(TAG, "onStart")
        LocalBroadcastManager.getInstance(requireContext()).registerReceiver(
            locationReceiver,
            IntentFilter(LocationService.ACTION_FOREGROUND_ONLY_LOCATION_BROADCAST)
        )
        requestLocationPermissionIfNeeded()
    }

    override fun onStop() {
        if (locationServiceBound) {
            locationService?.unsubscribeToLocationUpdates()
            requireContext().unbindService(serviceConnection)
            locationServiceBound = false
        }
        LocalBroadcastManager.getInstance(requireContext()).unregisterReceiver(locationReceiver)
        super.onStop()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }

    private fun requestLocationPermissionIfNeeded() {
        val context = context ?: return
        if (
            ContextCompat.checkSelfPermission(
                context,
                Manifest.permission.ACCESS_FINE_LOCATION
            ) == PackageManager.PERMISSION_GRANTED
        ) {
            bindToLocationService()
        } else {
            permissionLauncher.launch(Manifest.permission.ACCESS_FINE_LOCATION)
        }
    }

    private fun bindToLocationService() {
        if (locationServiceBound) {
            locationService?.subscribeToLocationUpdates()
            return
        }
        val context = context ?: return
        context.bindService(
            Intent(context, LocationService::class.java),
            serviceConnection,
            Context.BIND_AUTO_CREATE
        )
    }

    private fun updateLocation(location: Location) {
        currentLocation = location
        resolvedAddress = null
        if (_binding != null) {
            binding.locationStatusText.text = getString(
                R.string.location_status_format,
                location.latitude,
                location.longitude
            )
            binding.addressStatusText.text = getString(R.string.address_waiting)
        }
    }

    private fun openImagePicker() {
        try {
            imagePickerLauncher.launch(
                PickVisualMediaRequest(
                    ActivityResultContracts.PickVisualMedia.ImageOnly
                )
            )
        } catch (exception: Exception) {
            Log.w(TAG, "Image picker failed", exception)
            if (_binding != null) {
                binding.imageStatusText.text = getString(R.string.image_pick_failed)
            }
            Toast.makeText(
                requireContext(),
                R.string.image_pick_failed,
                Toast.LENGTH_SHORT
            ).show()
        }
    }

    private fun showSelectedImage(uri: Uri) {
        selectedImageUri = uri
        binding.imagePreview.setImageURI(uri)
        binding.imagePreview.visibility = View.VISIBLE
        binding.imageStatusText.text = getString(R.string.image_selected)
    }

    private fun submitReport(
        type: String,
        description: String,
        severity: String,
        location: Location
    ) {
        Log.i(
            TAG,
            "TrafficReport summary: type='$type', severity='$severity', descriptionLen=${description.length}"
        )

        val baseReport = TrafficReport(
            title = getString(R.string.fragment_report_title),
            location = resolvedAddress ?: getString(
                R.string.location_label_format,
                location.latitude,
                location.longitude
            ),
            date = DateFormat.getDateTimeInstance().format(Date()),
            type = type,
            severity = severity,
            description = description,
            latitude = location.latitude,
            longitude = location.longitude,
            address = resolvedAddress.orEmpty()
        )

        val imageUri = selectedImageUri
        binding.submitButton.isEnabled = false
        pendingCreatedReport = bundleOf(
            "type" to type,
            "description" to description,
            "severity" to severity
        )
        if (imageUri != null) {
            binding.imageStatusText.text = getString(R.string.image_uploading)
        }
        viewModel.createReport(baseReport, imageUri)
    }

    private fun resolveAddress(location: Location) {
        try {
            val geocoder = Geocoder(requireContext(), Locale.getDefault())
            @Suppress("DEPRECATION")
            val addressLine = geocoder
                .getFromLocation(location.latitude, location.longitude, 1)
                ?.firstOrNull()
                ?.getAddressLine(0)
                .orEmpty()

            resolvedAddress = addressLine.ifBlank { null }
            binding.addressStatusText.text = if (resolvedAddress != null) {
                getString(R.string.address_resolved_format, resolvedAddress)
            } else {
                getString(R.string.address_not_found)
            }
        } catch (exception: Exception) {
            Log.w(TAG, "Reverse geocoding failed", exception)
            resolvedAddress = null
            binding.addressStatusText.text = getString(R.string.address_not_found)
            Toast.makeText(
                requireContext(),
                R.string.address_resolve_failed,
                Toast.LENGTH_SHORT
            ).show()
        }
    }
}
