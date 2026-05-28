package dk.itu.moapd.x9.elie.ui

import android.Manifest
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.content.ServiceConnection
import android.content.pm.PackageManager
import android.location.Location
import android.os.Bundle
import android.os.IBinder
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.localbroadcastmanager.content.LocalBroadcastManager
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.ItemTouchHelper
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.firebase.ui.auth.AuthUI
import com.firebase.ui.database.FirebaseRecyclerOptions
import com.google.firebase.auth.FirebaseAuth
import dk.itu.moapd.x9.elie.R
import dk.itu.moapd.x9.elie.databinding.FragmentMainBinding
import dk.itu.moapd.x9.elie.model.TrafficReport
import dk.itu.moapd.x9.elie.repository.TrafficReportRepository
import dk.itu.moapd.x9.elie.receiver.LocationBroadcastReceiver
import dk.itu.moapd.x9.elie.service.LocationService
import dk.itu.moapd.x9.elie.util.SwipeToDeleteCallback
import dk.itu.moapd.x9.elie.util.UiPickers
import dk.itu.moapd.x9.elie.viewmodel.TrafficReportViewModel
import dk.itu.moapd.x9.elie.work.ReportReminderScheduler

class MainFragment : Fragment() {

    companion object {
        private const val TAG = "X9_MAIN_FRAGMENT"
        private const val CREATED_REPORT_KEY = "created_report"
        private const val REPORT_TYPE_KEY = "type"
        private const val REPORT_DESCRIPTION_KEY = "description"
        private const val REPORT_SEVERITY_KEY = "severity"
    }

    private var _binding: FragmentMainBinding? = null
    private val binding get() = checkNotNull(_binding)
    private lateinit var reportAdapter: TrafficReportAdapter
    private val viewModel: TrafficReportViewModel by activityViewModels()
    private var currentLocation: Location? = null
    private var locationService: LocationService? = null
    private var locationServiceBound = false
    private var reportsUiConfigured = false
    private val reportRepository = TrafficReportRepository()

    private val locationReceiver = LocationBroadcastReceiver { location ->
        currentLocation = location
    }

    private val serviceConnection = object : ServiceConnection {
        override fun onServiceConnected(name: ComponentName?, service: IBinder?) {
            val binder = service as? LocationService.LocalBinder ?: return
            locationService = binder.getService()
            locationServiceBound = true
            currentLocation = locationService?.lastKnownLocation
            locationService?.subscribeToLocationUpdates()
        }

        override fun onServiceDisconnected(name: ComponentName?) {
            locationService = null
            locationServiceBound = false
        }
    }

    private val locationPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { granted ->
        if (granted) {
            bindToLocationService()
        }
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentMainBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        UiPickers.attachDatePicker(requireContext(), binding.reportDateInput)
        UiPickers.attachDropdown(
            requireContext(),
            binding.reportTypeInput,
            resources.getStringArray(R.array.report_type_options).toList()
        )

        binding.buttonSignIn.setOnClickListener {
            (requireActivity() as? MainActivity)?.launchSignInFlow()
        }
        binding.buttonSignOut.setOnClickListener {
            signOut()
        }

        val user = FirebaseAuth.getInstance().currentUser
        updateUiForAuthState(user)
        if (user != null) {
            reportsUiConfigured = true
            val query = reportRepository.reportsQuery(user.uid)

            val options = FirebaseRecyclerOptions.Builder<TrafficReport>()
                .setQuery(query, TrafficReport::class.java)
                .setLifecycleOwner(viewLifecycleOwner)
                .build()

            reportAdapter = TrafficReportAdapter(options) { report ->
                startActivity(ReportDetailsActivity.newIntent(requireContext(), report))
            }
            binding.recyclerView.layoutManager = LinearLayoutManager(requireContext())
            binding.recyclerView.adapter = reportAdapter
            binding.recyclerView.isNestedScrollingEnabled = false
            binding.recyclerView.setHasFixedSize(true)

            val swipeHandler = object : SwipeToDeleteCallback() {
                override fun onSwiped(viewHolder: RecyclerView.ViewHolder, direction: Int) {
                    val userId = viewModel.currentUserId()
                    if (userId == null) {
                        reportAdapter.notifyItemChanged(viewHolder.bindingAdapterPosition)
                        Toast.makeText(
                            requireContext(),
                            "Du skal være logget ind",
                            Toast.LENGTH_SHORT
                        ).show()
                        return
                    }

                    val position = viewHolder.bindingAdapterPosition
                    if (position == RecyclerView.NO_POSITION) {
                        return
                    }

                    val reportId = reportAdapter.getRef(position).key.orEmpty()
                    if (reportId.isBlank()) {
                        reportAdapter.notifyItemChanged(position)
                        return
                    }

                    viewModel.deleteReport(
                        userId = userId,
                        reportId = reportId,
                        onSuccess = {
                            if (!isAdded) return@deleteReport
                            Toast.makeText(
                                requireContext(),
                                "Report slettet",
                                Toast.LENGTH_SHORT
                            ).show()
                        },
                        onError = { message ->
                            if (!isAdded) return@deleteReport
                            Toast.makeText(requireContext(), message, Toast.LENGTH_SHORT).show()
                            reportAdapter.notifyItemChanged(position)
                        }
                    )
                }
            }
            ItemTouchHelper(swipeHandler).attachToRecyclerView(binding.recyclerView)
        } else {
            Log.w(TAG, "No authenticated user; report list is unavailable")
            binding.recyclerView.visibility = View.GONE
            Toast.makeText(
                requireContext(),
                "Du skal være logget ind for at se Firebase-rapporter.",
                Toast.LENGTH_SHORT
            ).show()
        }

        binding.btnOpenReport.setOnClickListener {
            findNavController().navigate(R.id.action_mainFragment_to_createReportFragment)
        }
        binding.btnOpenComposeDashboard.setOnClickListener {
            findNavController().navigate(R.id.action_mainFragment_to_composeDashboardFragment)
        }
        binding.btnOpenMap.setOnClickListener {
            findNavController().navigate(R.id.action_mainFragment_to_mapFragment)
        }
        binding.buttonMinor.setOnClickListener { validateAndProcess(getString(R.string.severity_minor)) }
        binding.buttonModerate.setOnClickListener { validateAndProcess(getString(R.string.severity_moderate)) }
        binding.buttonMajor.setOnClickListener { validateAndProcess(getString(R.string.severity_major)) }

        findNavController().currentBackStackEntry
            ?.savedStateHandle
            ?.getLiveData<Bundle>(CREATED_REPORT_KEY)
            ?.observe(viewLifecycleOwner) { result ->
                val returnedType = result.getString(REPORT_TYPE_KEY).orEmpty()
                val returnedDescription = result.getString(REPORT_DESCRIPTION_KEY).orEmpty()
                val returnedSeverity = result.getString(REPORT_SEVERITY_KEY).orEmpty()
                if (returnedType.isEmpty() && returnedDescription.isEmpty() && returnedSeverity.isEmpty()) {
                    return@observe
                }

                binding.reportTypeInput.setText(returnedType, false)
                binding.reportDescriptionInput.setText(returnedDescription)
                Toast.makeText(
                    requireContext(),
                    getString(R.string.report_received_message, returnedSeverity),
                    Toast.LENGTH_SHORT
                ).show()
                findNavController().currentBackStackEntry
                    ?.savedStateHandle
                    ?.remove<Bundle>(CREATED_REPORT_KEY)
            }
    }

    private fun validateAndProcess(severity: String) {
        if (FirebaseAuth.getInstance().currentUser == null) {
            Toast.makeText(requireContext(), R.string.auth_status_signed_out, Toast.LENGTH_SHORT).show()
            updateUiForAuthState(null)
            return
        }

        val title = binding.reportTitleInput.text?.toString()?.trim().orEmpty()
        val location = binding.reportLocationInput.text?.toString()?.trim().orEmpty()
        val date = binding.reportDateInput.text?.toString()?.trim().orEmpty()
        val type = binding.reportTypeInput.text?.toString()?.trim().orEmpty()
        val description = binding.reportDescriptionInput.text?.toString()?.trim().orEmpty()
        val currentGpsLocation = currentLocation

        if (title.isEmpty() || location.isEmpty() || date.isEmpty() || type.isEmpty() || description.isEmpty()) {
            Toast.makeText(requireContext(), R.string.fill_all_fields, Toast.LENGTH_SHORT).show()
            Log.w(TAG, "Invalid submission: empty field")
            return
        }
        if (currentGpsLocation == null) {
            Toast.makeText(requireContext(), R.string.location_not_available, Toast.LENGTH_SHORT).show()
            requestLocationPermissionIfNeeded()
            return
        }

        val report = TrafficReport(
                title = title,
                location = location,
                date = date,
                type = type,
                severity = severity,
                description = description,
                latitude = currentGpsLocation.latitude,
                longitude = currentGpsLocation.longitude
            )

        viewModel.addReportReturningId(
            report,
            onSuccess = { reportId ->
                if (!isAdded) return@addReportReturningId
                val userId = viewModel.currentUserId()
                if (!userId.isNullOrBlank()) {
                    ReportReminderScheduler.schedule(
                        context = requireContext(),
                        userId = userId,
                        reportId = reportId,
                        reportTitle = report.title
                    )
                }
                Toast.makeText(
                    requireContext(),
                    getString(R.string.saved_severity_message, severity),
                    Toast.LENGTH_SHORT
                ).show()
            },
            onError = { message ->
                if (!isAdded) return@addReportReturningId
                Toast.makeText(requireContext(), message, Toast.LENGTH_SHORT).show()
            }
        )
    }

    override fun onStart() {
        super.onStart()
        LocalBroadcastManager.getInstance(requireContext()).registerReceiver(
            locationReceiver,
            IntentFilter(LocationService.ACTION_FOREGROUND_ONLY_LOCATION_BROADCAST)
        )
        requestLocationPermissionIfNeeded()
    }

    override fun onResume() {
        super.onResume()
        Log.d(TAG, "onResume")
        updateUiForAuthState(FirebaseAuth.getInstance().currentUser)
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
        if (::reportAdapter.isInitialized) {
            reportAdapter.stopListening()
        }
        _binding = null
    }

    private fun signOut() {
        AuthUI.getInstance()
            .signOut(requireContext())
            .addOnCompleteListener {
                FirebaseAuth.getInstance().signOut()

                val intent = Intent(requireContext(), MainActivity::class.java).apply {
                    flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
                }
                startActivity(intent)
                requireActivity().finish()
            }
    }

    private fun updateUiForAuthState(user: com.google.firebase.auth.FirebaseUser?) {
        val signedIn = user != null
        binding.textAuthStatus.visibility = if (signedIn) View.GONE else View.VISIBLE
        binding.buttonSignIn.visibility = if (signedIn) View.GONE else View.VISIBLE
        binding.signedInContentGroup.visibility = if (signedIn) View.VISIBLE else View.GONE
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
            locationPermissionLauncher.launch(Manifest.permission.ACCESS_FINE_LOCATION)
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
}
