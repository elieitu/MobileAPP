package dk.itu.moapd.x9.elie.ui.screen

import android.app.DatePickerDialog
import android.widget.Toast
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.background
import androidx.compose.material3.Button
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.runtime.Composable
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringArrayResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import dk.itu.moapd.x9.elie.R
import dk.itu.moapd.x9.elie.model.TrafficReport
import dk.itu.moapd.x9.elie.ui.component.TrafficReportList
import java.util.Calendar
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MainScreen(
    reports: List<TrafficReport>,
    onOpenCreateReport: () -> Unit,
    onAddReport: (TrafficReport) -> Unit,
    onRemoveReport: (Int) -> Unit
) {
    val context = LocalContext.current
    val reportTypes = stringArrayResource(R.array.report_type_options).toList()

    var title by rememberSaveable { mutableStateOf("") }
    var location by rememberSaveable { mutableStateOf("") }
    var date by rememberSaveable { mutableStateOf("") }
    var type by rememberSaveable { mutableStateOf("") }
    var description by rememberSaveable { mutableStateOf("") }
    var typeExpanded by remember { mutableStateOf(false) }
    var selectedSeverityFilter by rememberSaveable { mutableStateOf("All") }

    val filteredReports by remember(reports, selectedSeverityFilter) {
        derivedStateOf {
            reports.withIndex().filter { indexed ->
                selectedSeverityFilter == "All" || indexed.value.severity == selectedSeverityFilter
            }
        }
    }

    fun submitWithSeverity(severity: String) {
        if (title.isBlank() || location.isBlank() || date.isBlank() || type.isBlank() || description.isBlank()) {
            Toast.makeText(context, "Udfyld alle felter", Toast.LENGTH_SHORT).show()
            return
        }

        onAddReport(
            TrafficReport(
                title = title.trim(),
                location = location.trim(),
                date = date.trim(),
                type = type.trim(),
                severity = severity,
                description = description.trim()
            )
        )

        Toast.makeText(context, "Saved: $severity", Toast.LENGTH_SHORT).show()
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Text(
            text = stringResource(R.string.app_name),
            style = MaterialTheme.typography.headlineSmall,
            fontWeight = FontWeight.Bold
        )

        OutlinedTextField(
            value = title,
            onValueChange = { title = it },
            label = { Text(stringResource(R.string.report_title_label)) },
            modifier = Modifier.fillMaxWidth()
        )

        OutlinedTextField(
            value = location,
            onValueChange = { location = it },
            label = { Text(stringResource(R.string.report_location_label)) },
            modifier = Modifier.fillMaxWidth()
        )

        OutlinedTextField(
            value = date,
            onValueChange = {},
            readOnly = true,
            label = { Text(stringResource(R.string.report_date_label)) },
            modifier = Modifier
                .fillMaxWidth()
                .clickable {
                    val cal = Calendar.getInstance()
                    DatePickerDialog(
                        context,
                        { _, y, m, d -> date = String.format(Locale.US, "%04d-%02d-%02d", y, m + 1, d) },
                        cal.get(Calendar.YEAR),
                        cal.get(Calendar.MONTH),
                        cal.get(Calendar.DAY_OF_MONTH)
                    ).show()
                }
        )

        ExposedDropdownMenuBox(
            expanded = typeExpanded,
            onExpandedChange = { typeExpanded = !typeExpanded }
        ) {
            OutlinedTextField(
                value = type,
                onValueChange = {},
                readOnly = true,
                label = { Text(stringResource(R.string.report_type_label)) },
                trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = typeExpanded) },
                modifier = Modifier
                    .menuAnchor()
                    .fillMaxWidth()
            )

            DropdownMenu(
                expanded = typeExpanded,
                onDismissRequest = { typeExpanded = false }
            ) {
                reportTypes.forEach { option ->
                    DropdownMenuItem(
                        text = { Text(option) },
                        onClick = {
                            type = option
                            typeExpanded = false
                        }
                    )
                }
            }
        }

        OutlinedTextField(
            value = description,
            onValueChange = { description = it },
            label = { Text(stringResource(R.string.report_description_label)) },
            modifier = Modifier.fillMaxWidth(),
            minLines = 3
        )

        Button(
            onClick = onOpenCreateReport,
            modifier = Modifier.fillMaxWidth()
        ) {
            Text(stringResource(R.string.open_traffic_report_screen))
        }

        Text(
            text = "Reports",
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.SemiBold
        )

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            listOf("All", "Minor", "Moderate", "Major").forEach { filter ->
                val selected = selectedSeverityFilter == filter
                val buttonModifier = Modifier.weight(1f)

                if (selected) {
                    Button(
                        onClick = { selectedSeverityFilter = filter },
                        modifier = buttonModifier
                    ) { Text(filter) }
                } else {
                    OutlinedButton(
                        onClick = { selectedSeverityFilter = filter },
                        modifier = buttonModifier
                    ) { Text(filter) }
                }
            }
        }

        Text(
            text = "Showing ${filteredReports.size} of ${reports.size}",
            style = MaterialTheme.typography.bodyMedium
        )

        TrafficReportList(
            reports = filteredReports,
            onRemoveReport = onRemoveReport,
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth()
        )

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Button(
                onClick = { submitWithSeverity("Minor") },
                modifier = Modifier.weight(1f)
            ) {
                Text(stringResource(R.string.severity_minor))
            }

            Button(
                onClick = { submitWithSeverity("Moderate") },
                modifier = Modifier.weight(1f)
            ) {
                Text(stringResource(R.string.severity_moderate))
            }

            Button(
                onClick = { submitWithSeverity("Major") },
                modifier = Modifier.weight(1f)
            ) {
                Text(stringResource(R.string.severity_major))
            }
        }
    }
}
