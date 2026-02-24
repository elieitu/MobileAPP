package dk.itu.moapd.x9.elie.activity

import android.content.Intent
import android.os.Bundle
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.viewModels
import androidx.compose.runtime.getValue
import androidx.compose.runtime.livedata.observeAsState
import dk.itu.moapd.x9.elie.model.TrafficReport
import dk.itu.moapd.x9.elie.ui.screen.MainScreen
import dk.itu.moapd.x9.elie.ui.theme.X9Theme
import dk.itu.moapd.x9.elie.viewmodel.TrafficReportViewModel
import java.time.LocalDate

class MainActivity : ComponentActivity() {

    private val viewModel: TrafficReportViewModel by viewModels()
    private val createReportLauncher = registerForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) { result ->
        val data = result.data ?: return@registerForActivityResult
        if (result.resultCode != RESULT_OK) return@registerForActivityResult

        val type = data.getStringExtra(TrafficReportActivity.EXTRA_TYPE).orEmpty().trim()
        val description = data.getStringExtra(TrafficReportActivity.EXTRA_DESCRIPTION).orEmpty().trim()
        val severity = data.getStringExtra(TrafficReportActivity.EXTRA_SEVERITY).orEmpty().trim()
        if (type.isEmpty() || description.isEmpty() || severity.isEmpty()) return@registerForActivityResult

        viewModel.addReport(
            TrafficReport(
                title = "XML report",
                location = "From XML screen",
                date = LocalDate.now().toString(),
                type = type,
                severity = severity,
                description = description
            )
        )
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        viewModel.addMockDataIfEmpty()

        setContent {
            X9Theme {
                val reports by viewModel.reports.observeAsState(emptyList())
                MainScreen(
                    reports = reports,
                    onOpenCreateReport = {
                        createReportLauncher.launch(Intent(this, TrafficReportActivity::class.java))
                    },
                    onOpenFragmentFlow = {
                        startActivity(Intent(this, FragmentHostActivity::class.java))
                    },
                    onAddReport = { report ->
                        viewModel.addReport(report)
                    },
                    onRemoveReport = { index ->
                        viewModel.removeAt(index)
                    }
                )
            }
        }
    }
}
