package dk.itu.moapd.x9.elie.activity

import android.app.DatePickerDialog
import android.app.Activity
import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.weight
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenu
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.livedata.observeAsState
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import dk.itu.moapd.x9.elie.TrafficReport
import dk.itu.moapd.x9.elie.viewmodel.TrafficReportViewModel
import java.util.Calendar
import java.util.Locale

class MainActivity : ComponentActivity() {

    companion object {
        private const val TAG = "X9_MAIN_COMPOSE"
    }

    private val viewModel: TrafficReportViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            MaterialTheme {
                MainScreen(viewModel = viewModel)
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun MainScreen(viewModel: TrafficReportViewModel) {
    val context = LocalContext.current
    val reports by viewModel.reports.observeAsState(emptyList())
    val reportTypes = remember { context.resources.getStringArray(dk.itu.moapd.x9.elie.R.array.report_type_options).toList() }

    var title by remember { mutableStateOf("") }
    var location by remember { mutableStateOf("") }
    var date by remember { mutableStateOf("") }
    var type by remember { mutableStateOf("") }
    var description by remember { mutableStateOf("") }
    var expandedType by remember { mutableStateOf(false) }

    val launcher = rememberLauncherForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
        if (result.resultCode != Activity.RESULT_OK) return@rememberLauncherForActivityResult
        val data = result.data ?: return@rememberLauncherForActivityResult
        type = data.getStringExtra(TrafficReportActivity.EXTRA_TYPE).orEmpty()
        description = data.getStringExtra(TrafficReportActivity.EXTRA_DESCRIPTION).orEmpty()
        val severity = data.getStringExtra(TrafficReportActivity.EXTRA_SEVERITY).orEmpty()
        if (severity.isNotEmpty()) {
            Toast.makeText(context, "Report received: $severity", Toast.LENGTH_SHORT).show()
            Log.i(TAG, "Returned report: type='$type', severity='$severity'")
        }
    }

    LaunchedEffect(Unit) {
        viewModel.addMockDataIfEmpty()
    }

    val openDatePicker = {
        val cal = Calendar.getInstance()
        DatePickerDialog(
            context,
            { _, y, m, d ->
                val mm = String.format(Locale.US, "%02d", m + 1)
                val dd = String.format(Locale.US, "%02d", d)
                date = "$y-$mm-$dd"
            },
            cal.get(Calendar.YEAR),
            cal.get(Calendar.MONTH),
            cal.get(Calendar.DAY_OF_MONTH)
        ).show()
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        Text(
            text = "X9 Traffic",
            style = MaterialTheme.typography.headlineSmall,
            fontWeight = FontWeight.Bold
        )

        OutlinedTextField(
            value = title,
            onValueChange = { title = it },
            label = { Text("Report title") },
            modifier = Modifier.fillMaxWidth(),
            singleLine = true
        )
        OutlinedTextField(
            value = location,
            onValueChange = { location = it },
            label = { Text("Location") },
            modifier = Modifier.fillMaxWidth(),
            singleLine = true
        )
        OutlinedTextField(
            value = date,
            onValueChange = {},
            modifier = Modifier.fillMaxWidth(),
            label = { Text("Report date") },
            readOnly = true,
            singleLine = true
        )
        Button(onClick = openDatePicker, modifier = Modifier.fillMaxWidth()) {
            Text("Pick date")
        }

        ExposedDropdownMenuBox(
            expanded = expandedType,
            onExpandedChange = { expandedType = it }
        ) {
            OutlinedTextField(
                value = type,
                onValueChange = {},
                readOnly = true,
                modifier = Modifier
                    .menuAnchor()
                    .fillMaxWidth(),
                label = { Text("Report type") },
                trailingIcon = {
                    ExposedDropdownMenuDefaults.TrailingIcon(expanded = expandedType)
                }
            )
            ExposedDropdownMenu(
                expanded = expandedType,
                onDismissRequest = { expandedType = false }
            ) {
                reportTypes.forEach { option ->
                    DropdownMenuItem(
                        text = { Text(option) },
                        onClick = {
                            type = option
                            expandedType = false
                        }
                    )
                }
            }
        }

        OutlinedTextField(
            value = description,
            onValueChange = { description = it },
            label = { Text("Description") },
            modifier = Modifier
                .fillMaxWidth()
                .height(100.dp)
        )

        Button(
            onClick = {
                launcher.launch(Intent(context, TrafficReportActivity::class.java))
            },
            modifier = Modifier.fillMaxWidth()
        ) {
            Text("Open traffic report screen")
        }

        LazyColumn(
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth(),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            itemsIndexed(reports) { index, report ->
                ReportCard(
                    report = report,
                    onDelete = { viewModel.removeAt(index) }
                )
            }
        }

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Button(
                modifier = Modifier.weight(1f),
                onClick = {
                    saveReport(
                        viewModel = viewModel,
                        context = context,
                        title = title,
                        location = location,
                        date = date,
                        type = type,
                        description = description,
                        severity = "Minor"
                    )
                }
            ) { Text("Minor") }
            Button(
                modifier = Modifier.weight(1f),
                onClick = {
                    saveReport(
                        viewModel = viewModel,
                        context = context,
                        title = title,
                        location = location,
                        date = date,
                        type = type,
                        description = description,
                        severity = "Moderate"
                    )
                }
            ) { Text("Moderate") }
            Button(
                modifier = Modifier.weight(1f),
                onClick = {
                    saveReport(
                        viewModel = viewModel,
                        context = context,
                        title = title,
                        location = location,
                        date = date,
                        type = type,
                        description = description,
                        severity = "Major"
                    )
                }
            ) { Text("Major") }
        }
    }
}

private fun saveReport(
    viewModel: TrafficReportViewModel,
    context: android.content.Context,
    title: String,
    location: String,
    date: String,
    type: String,
    description: String,
    severity: String
) {
    if (
        title.isBlank() ||
        location.isBlank() ||
        date.isBlank() ||
        type.isBlank() ||
        description.isBlank()
    ) {
        Toast.makeText(context, "Udfyld alle felter", Toast.LENGTH_SHORT).show()
        Log.w("X9_MAIN_COMPOSE", "Invalid submission: empty field")
        return
    }

    viewModel.addReport(
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
    Log.i("X9_MAIN_COMPOSE", "Saved report: '$title' $severity")
}

@Composable
private fun ReportCard(report: TrafficReport, onDelete: () -> Unit) {
    Card(modifier = Modifier.fillMaxWidth()) {
        Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(text = report.title, fontWeight = FontWeight.SemiBold)
                Text(text = report.severity)
            }
            Text(text = report.type, style = MaterialTheme.typography.bodySmall)
            Text(text = "${report.location} | ${report.date}", style = MaterialTheme.typography.bodySmall)
            Text(text = report.description, style = MaterialTheme.typography.bodyMedium)
            Button(
                onClick = onDelete,
                modifier = Modifier.width(100.dp)
            ) {
                Text("Delete")
            }
        }
    }
}
