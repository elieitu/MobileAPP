package dk.itu.moapd.x9.elie.ui.compose

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.dimensionResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import dk.itu.moapd.x9.elie.R
import dk.itu.moapd.x9.elie.model.TrafficReport

@Composable
fun MainScreen(
    reports: List<TrafficReport>,
    onOpenCreateReport: () -> Unit
) {
    val allFilter = stringResource(R.string.filter_all)
    val spacingSmall = dimensionResource(R.dimen.spacing_small)
    val spacingMedium = dimensionResource(R.dimen.spacing_medium)
    val spacingLarge = dimensionResource(R.dimen.spacing_large)
    val filters = listOf(
        allFilter,
        stringResource(R.string.severity_minor),
        stringResource(R.string.severity_moderate),
        stringResource(R.string.severity_major)
    )

    val filteredReports = reports.withIndex().toList()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(spacingLarge),
        verticalArrangement = Arrangement.spacedBy(spacingMedium)
    ) {
        Text(
            text = stringResource(R.string.compose_dashboard_title),
            style = MaterialTheme.typography.headlineSmall,
            fontWeight = FontWeight.Bold
        )

        Text(
            text = stringResource(R.string.compose_dashboard_summary),
            style = MaterialTheme.typography.bodyLarge
        )

        Button(
            onClick = onOpenCreateReport,
            modifier = Modifier.fillMaxWidth()
        ) {
            Text(stringResource(R.string.open_create_report))
        }

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(spacingSmall)
        ) {
            DashboardStatCard(
                label = stringResource(R.string.total_reports_label),
                value = reports.size.toString(),
                modifier = Modifier.weight(1f)
            )
            DashboardStatCard(
                label = stringResource(R.string.latest_report_label),
                value = reports.lastOrNull()?.type ?: stringResource(R.string.no_reports_label),
                modifier = Modifier.weight(1f)
            )
            DashboardStatCard(
                label = stringResource(R.string.active_filters_label),
                value = filters.size.toString(),
                modifier = Modifier.weight(1f)
            )
        }

        Text(
            text = stringResource(R.string.reports_heading),
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.SemiBold
        )

        Text(
            text = stringResource(R.string.showing_reports, filteredReports.size, reports.size),
            style = MaterialTheme.typography.bodyMedium
        )

        TrafficReportList(
            reports = filteredReports,
            modifier = Modifier.fillMaxWidth()
        )
    }
}

@Composable
private fun DashboardStatCard(
    label: String,
    value: String,
    modifier: Modifier = Modifier
) {
    Card(modifier = modifier) {
        Column(
            modifier = Modifier.padding(dimensionResource(R.dimen.spacing_medium)),
            verticalArrangement = Arrangement.spacedBy(dimensionResource(R.dimen.spacing_xsmall))
        ) {
            Text(
                text = label,
                style = MaterialTheme.typography.labelMedium
            )
            Text(
                text = value,
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold
            )
        }
    }
}
