package dk.itu.moapd.x9.elie

import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.widget.ArrayAdapter
import android.widget.AutoCompleteTextView
import android.widget.Button
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.google.android.material.textfield.TextInputEditText

class TrafficReportActivity : AppCompatActivity() {

    companion object {
        const val EXTRA_TYPE = "dk.itu.moapd.x9.elie.EXTRA_TYPE"
        const val EXTRA_DESCRIPTION = "dk.itu.moapd.x9.elie.EXTRA_DESCRIPTION"
        const val EXTRA_SEVERITY = "dk.itu.moapd.x9.elie.EXTRA_SEVERITY"

        private const val TAG = "X9_TRAFFIC_REPORT"
        private const val STATE_TYPE = "state_type"
        private const val STATE_DESCRIPTION = "state_description"
        private const val STATE_SEVERITY = "state_severity"
    }

    private lateinit var typeInput: TextInputEditText
    private lateinit var descriptionInput: TextInputEditText
    private lateinit var severityDropdown: AutoCompleteTextView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        Log.d(TAG, "onCreate")
        setContentView(R.layout.activity_traffic_report)

        typeInput = findViewById(R.id.inputType)
        descriptionInput = findViewById(R.id.inputDescription)
        severityDropdown = findViewById(R.id.inputSeverity)

        val submitButton = findViewById<Button>(R.id.btnSubmitReport)

        val adapter = ArrayAdapter.createFromResource(
            this,
            R.array.severity_options,
            android.R.layout.simple_list_item_1
        )
        severityDropdown.setAdapter(adapter)
        severityDropdown.setOnClickListener { severityDropdown.showDropDown() }

        if (savedInstanceState != null) {
            typeInput.setText(savedInstanceState.getString(STATE_TYPE).orEmpty())
            descriptionInput.setText(savedInstanceState.getString(STATE_DESCRIPTION).orEmpty())
            val savedSeverity = savedInstanceState.getString(STATE_SEVERITY).orEmpty()
            if (savedSeverity.isNotEmpty()) severityDropdown.setText(savedSeverity, false)
        }

        submitButton.setOnClickListener {
            val type = typeInput.text?.toString()?.trim().orEmpty()
            val description = descriptionInput.text?.toString()?.trim().orEmpty()
            val severity = severityDropdown.text?.toString()?.trim().orEmpty()

            var valid = true
            if (type.isEmpty()) { typeInput.error = "Required"; valid = false } else typeInput.error = null
            if (description.isEmpty()) { descriptionInput.error = "Required"; valid = false } else descriptionInput.error = null
            if (severity.isEmpty()) { severityDropdown.error = "Required"; valid = false } else severityDropdown.error = null

            if (!valid) {
                Log.w(TAG, "Invalid submission: typeEmpty=${type.isEmpty()}, descEmpty=${description.isEmpty()}, severityEmpty=${severity.isEmpty()}")
                Toast.makeText(this, "Invalid submission: udfyld alle felter", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            // Summary log (requirement)
            Log.i(TAG, "TrafficReport summary: type='$type', severity='$severity', descriptionLen=${description.length}")

            val data = Intent().apply {
                putExtra(EXTRA_TYPE, type)
                putExtra(EXTRA_DESCRIPTION, description)
                putExtra(EXTRA_SEVERITY, severity)
            }

            setResult(RESULT_OK, data)
            finish()
        }
    }

    override fun onSaveInstanceState(outState: Bundle) {
        super.onSaveInstanceState(outState)
        Log.d(TAG, "onSaveInstanceState")
        outState.putString(STATE_TYPE, typeInput.text?.toString().orEmpty())
        outState.putString(STATE_DESCRIPTION, descriptionInput.text?.toString().orEmpty())
        outState.putString(STATE_SEVERITY, severityDropdown.text?.toString().orEmpty())
    }

    override fun onStart() { super.onStart(); Log.d(TAG, "onStart") }
    override fun onResume() { super.onResume(); Log.d(TAG, "onResume") }
    override fun onPause() { super.onPause(); Log.d(TAG, "onPause") }
    override fun onStop() { super.onStop(); Log.d(TAG, "onStop") }
    override fun onDestroy() { super.onDestroy(); Log.d(TAG, "onDestroy") }
    override fun onRestart() { super.onRestart(); Log.d(TAG, "onRestart") }
}