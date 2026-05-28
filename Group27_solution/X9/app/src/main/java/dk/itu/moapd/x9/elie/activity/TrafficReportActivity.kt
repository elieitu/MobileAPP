package dk.itu.moapd.x9.elie.activity

import android.app.Activity
import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenu
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp

class TrafficReportActivity : ComponentActivity() {

    companion object {
        const val EXTRA_TYPE = "dk.itu.moapd.x9.elie.EXTRA_TYPE"
        const val EXTRA_DESCRIPTION = "dk.itu.moapd.x9.elie.EXTRA_DESCRIPTION"
        const val EXTRA_SEVERITY = "dk.itu.moapd.x9.elie.EXTRA_SEVERITY"

        private const val TAG = "X9_TRAFFIC_REPORT"
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        Log.d(TAG, "onCreate")
        setContent {
            MaterialTheme {
                TrafficReportScreen(
                    onSubmit = { type, description, severity ->
                        val data = Intent().apply {
                            putExtra(EXTRA_TYPE, type)
                            putExtra(EXTRA_DESCRIPTION, description)
                            putExtra(EXTRA_SEVERITY, severity)
                        }
                        setResult(Activity.RESULT_OK, data)
                        finish()
                    }
                )
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun TrafficReportScreen(onSubmit: (String, String, String) -> Unit) {
    val context = LocalContext.current
    val severityOptions = context.resources.getStringArray(dk.itu.moapd.x9.elie.R.array.severity_options).toList()

    var type by rememberSaveable { mutableStateOf("") }
    var description by rememberSaveable { mutableStateOf("") }
    var severity by rememberSaveable { mutableStateOf("") }
    var expanded by rememberSaveable { mutableStateOf(false) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Text(
            text = "Create traffic report",
            style = MaterialTheme.typography.headlineSmall,
            fontWeight = FontWeight.Bold
        )

        OutlinedTextField(
            value = type,
            onValueChange = { type = it },
            modifier = Modifier.fillMaxWidth(),
            label = { Text("Report type") },
            singleLine = true
        )

        OutlinedTextField(
            value = description,
            onValueChange = { description = it },
            modifier = Modifier.fillMaxWidth(),
            label = { Text("Description") }
        )

        ExposedDropdownMenuBox(
            expanded = expanded,
            onExpandedChange = { expanded = it }
        ) {
            OutlinedTextField(
                value = severity,
                onValueChange = {},
                readOnly = true,
                label = { Text("Report severity") },
                modifier = Modifier
                    .menuAnchor()
                    .fillMaxWidth(),
                trailingIcon = {
                    ExposedDropdownMenuDefaults.TrailingIcon(expanded = expanded)
                }
            )
            ExposedDropdownMenu(
                expanded = expanded,
                onDismissRequest = { expanded = false }
            ) {
                severityOptions.forEach { option ->
                    DropdownMenuItem(
                        text = { Text(option) },
                        onClick = {
                            severity = option
                            expanded = false
                        }
                    )
                }
            }
        }

        Button(
            onClick = {
                val safeType = type.trim()
                val safeDescription = description.trim()
                val safeSeverity = severity.trim()
                if (safeType.isEmpty() || safeDescription.isEmpty() || safeSeverity.isEmpty()) {
                    Log.w(TAG, "Invalid submission: typeEmpty=${safeType.isEmpty()}, descEmpty=${safeDescription.isEmpty()}, severityEmpty=${safeSeverity.isEmpty()}")
                    Toast.makeText(context, "Invalid submission: udfyld alle felter", Toast.LENGTH_SHORT).show()
                    return@Button
                }
                Log.i(TAG, "TrafficReport summary: type='$safeType', severity='$safeSeverity', descriptionLen=${safeDescription.length}")
                onSubmit(safeType, safeDescription, safeSeverity)
            },
            modifier = Modifier.fillMaxWidth()
        ) {
            Text("Submit")
        }
    }
}
