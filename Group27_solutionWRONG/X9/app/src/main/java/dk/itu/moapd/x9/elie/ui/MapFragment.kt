package dk.itu.moapd.x9.elie.ui

import android.Manifest
import android.content.pm.PackageManager
import android.os.Bundle
import android.view.View
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import com.google.android.gms.location.LocationServices
import com.google.android.gms.maps.CameraUpdateFactory
import com.google.android.gms.maps.GoogleMap
import com.google.android.gms.maps.OnMapReadyCallback
import com.google.android.gms.maps.SupportMapFragment
import com.google.android.gms.maps.model.LatLng
import com.google.android.gms.maps.model.MarkerOptions
import dk.itu.moapd.x9.elie.R
import dk.itu.moapd.x9.elie.model.TrafficReport
import dk.itu.moapd.x9.elie.viewmodel.TrafficReportViewModel

class MapFragment : Fragment(R.layout.fragment_map), OnMapReadyCallback {

    private val viewModel: TrafficReportViewModel by activityViewModels()
    private var googleMap: GoogleMap? = null
    private var latestReports: List<TrafficReport> = emptyList()

    private val locationPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { granted ->
        if (granted) {
            enableMyLocation()
        } else if (isAdded) {
            Toast.makeText(
                requireContext(),
                R.string.location_permission_denied_message,
                Toast.LENGTH_LONG
            ).show()
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        viewModel.observeReports { message ->
            if (isAdded) {
                Toast.makeText(requireContext(), message, Toast.LENGTH_SHORT).show()
            }
        }
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val mapsApiKey = requireContext().applicationInfo.metaData
            ?.getString("com.google.android.geo.API_KEY")
            .orEmpty()
        if (mapsApiKey.isBlank()) {
            Toast.makeText(requireContext(), R.string.map_missing_api_key, Toast.LENGTH_LONG).show()
        }

        val mapChildFragment = childFragmentManager.findFragmentById(R.id.map_container)
            as? SupportMapFragment
        mapChildFragment?.getMapAsync(this)

        viewModel.reports.observe(viewLifecycleOwner) { reports ->
            latestReports = reports
            showMarkers(reports)
        }
    }

    override fun onMapReady(map: GoogleMap) {
        googleMap = map.apply {
            uiSettings.isZoomControlsEnabled = true
            uiSettings.isCompassEnabled = true
            mapType = GoogleMap.MAP_TYPE_NORMAL
        }
        enableMyLocation()
        setupMarkerClick()
        loadReportsAndShowMarkers()
    }

    private fun loadReportsAndShowMarkers() {
        showMarkers(latestReports)
    }

    private fun showMarkers(reports: List<TrafficReport>) {
        val map = googleMap ?: return
        map.clear()

        val reportsWithCoordinates = reports.filter { it.hasCoordinates() }
        if (reportsWithCoordinates.isEmpty()) {
            map.moveCamera(CameraUpdateFactory.newLatLngZoom(DEFAULT_LOCATION, DEFAULT_ZOOM))
            if (isAdded) {
                Toast.makeText(requireContext(), R.string.map_no_reports, Toast.LENGTH_SHORT).show()
            }
            return
        }

        reportsWithCoordinates.forEach { report ->
            val position = LatLng(report.latitude, report.longitude)
            map.addMarker(
                MarkerOptions()
                    .position(position)
                    .title(report.type.ifBlank { getString(R.string.map_default_marker_title) })
                    .snippet(
                        buildString {
                            append(report.title.ifBlank { getString(R.string.map_untitled_report) })
                            if (report.description.isNotBlank()) {
                                append("\n")
                                append(report.description)
                            }
                        }
                    )
            )?.tag = report
        }

        val firstReport = reportsWithCoordinates.first()
        map.animateCamera(
            CameraUpdateFactory.newLatLngZoom(
                LatLng(firstReport.latitude, firstReport.longitude),
                12f
            )
        )
    }

    private fun enableMyLocation() {
        val context = context ?: return
        val map = googleMap ?: return
        val hasFineLocation = ContextCompat.checkSelfPermission(
            context,
            Manifest.permission.ACCESS_FINE_LOCATION
        ) == PackageManager.PERMISSION_GRANTED
        val hasCoarseLocation = ContextCompat.checkSelfPermission(
            context,
            Manifest.permission.ACCESS_COARSE_LOCATION
        ) == PackageManager.PERMISSION_GRANTED

        if (!hasFineLocation && !hasCoarseLocation) {
            locationPermissionLauncher.launch(Manifest.permission.ACCESS_FINE_LOCATION)
            return
        }

        map.isMyLocationEnabled = true
        val fusedLocationClient = LocationServices.getFusedLocationProviderClient(requireActivity())
        fusedLocationClient.lastLocation.addOnSuccessListener { location ->
            if (location != null && latestReports.isEmpty()) {
                map.animateCamera(
                    CameraUpdateFactory.newLatLngZoom(
                        LatLng(location.latitude, location.longitude),
                        13f
                    )
                )
            }
        }
    }

    private fun setupMarkerClick() {
        val map = googleMap ?: return

        map.setOnMarkerClickListener { marker ->
            marker.showInfoWindow()
            false
        }

        map.setOnInfoWindowClickListener { marker ->
            val report = marker.tag as? TrafficReport ?: return@setOnInfoWindowClickListener
            startActivity(ReportDetailsActivity.newIntent(requireContext(), report))
        }
    }

    private fun TrafficReport.hasCoordinates(): Boolean {
        return latitude != 0.0 || longitude != 0.0
    }

    companion object {
        private val DEFAULT_LOCATION = LatLng(55.6761, 12.5683)
        private const val DEFAULT_ZOOM = 10f
    }
}
