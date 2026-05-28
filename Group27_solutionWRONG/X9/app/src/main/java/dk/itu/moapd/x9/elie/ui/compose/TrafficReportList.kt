package dk.itu.moapd.x9.elie.ui.compose

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
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
fun TrafficReportList(
    reports: List<IndexedValue<TrafficReport>>,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier,
        verticalArrangement = Arrangement.spacedBy(dimensionResource(R.dimen.spacing_small))
    ) {
        reports.forEach { entry ->
            TrafficReportItem(
                report = entry.value
            )
        }
    }
}

@Composable
fun TrafficReportItem(
    report: TrafficReport,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier.fillMaxWidth()
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(dimensionResource(R.dimen.spacing_medium)),
            verticalArrangement = Arrangement.spacedBy(dimensionResource(R.dimen.spacing_xsmall))
        ) {
            Text(
                text = report.title,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold
            )
            Text(text = report.severity, style = MaterialTheme.typography.labelLarge)
            Text(text = stringResource(R.string.report_meta_summary, report.location, report.date, report.type))
            Text(text = report.description)
        }
    }
}
