package dk.itu.moapd.x9.elie.ui

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.compose.runtime.getValue
import androidx.compose.runtime.livedata.observeAsState
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.navigation.fragment.findNavController
import dk.itu.moapd.x9.elie.R
import dk.itu.moapd.x9.elie.ui.compose.MainScreen
import dk.itu.moapd.x9.elie.ui.compose.X9Theme
import dk.itu.moapd.x9.elie.viewmodel.TrafficReportViewModel

class ComposeDashboardFragment : Fragment() {

    private val viewModel: TrafficReportViewModel by activityViewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        viewModel.observeReports { message ->
            if (isAdded) {
                Toast.makeText(requireContext(), message, Toast.LENGTH_SHORT).show()
            }
        }
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        return androidx.compose.ui.platform.ComposeView(requireContext()).apply {
            setContent {
                X9Theme {
                    val reports by viewModel.reports.observeAsState(emptyList())
                    MainScreen(
                        reports = reports,
                        onOpenCreateReport = {
                            findNavController().navigate(R.id.action_composeDashboardFragment_to_createReportFragment)
                        }
                    )
                }
            }
        }
    }
}
