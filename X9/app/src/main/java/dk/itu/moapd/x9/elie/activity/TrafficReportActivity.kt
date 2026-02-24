package dk.itu.moapd.x9.elie.activity

import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.widget.ArrayAdapter
import android.widget.Button
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.google.android.material.appbar.MaterialToolbar
import com.google.android.material.textfield.MaterialAutoCompleteTextView
import com.google.android.material.textfield.TextInputEditText
import dk.itu.moapd.x9.elie.R
import dk.itu.moapd.x9.elie.util.UiPickers

class TrafficReportActivity : AppCompatActivity() {

    companion object {
        const val EXTRA_TYPE = "dk.itu.moapd.x9.elie.EXTRA_TYPE"
        const val EXTRA_DESCRIPTION = "dk.itu.moapd.x9.elie.EXTRA_DESCRIPTION"
        const val EXTRA_SEVERITY = "dk.itu.moapd.x9.elie.EXTRA_SEVERITY"
        const val EXTRA_DATE = "dk.itu.moapd.x9.elie.EXTRA_DATE"

        private const val TAG = "X9_TRAFFIC_REPORT"
        private const val STATE_TYPE = "state_type"
        private const val STATE_DESCRIPTION = "state_description"
        private const val STATE_SEVERITY = "state_severity"
        private const val STATE_DATE = "state_date"
    }

    private lateinit var typeInput: TextInputEditText
    private lateinit var dateInput: TextInputEditText
    private lateinit var descriptionInput: TextInputEditText
    private lateinit var severityDropdown: MaterialAutoCompleteTextView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        Log.d(TAG, "onCreate")
        setContentView(R.layout.activity_traffic_report)

        typeInput = findViewById(R.id.inputType)
        dateInput = findViewById(R.id.inputDate)
        descriptionInput = findViewById(R.id.inputDescription)
        severityDropdown = findViewById(R.id.inputSeverity)
        val topAppBar = findViewById<MaterialToolbar>(R.id.topAppBar)

        val submitButton = findViewById<Button>(R.id.btnSubmitReport)

        topAppBar.navigationIcon = getDrawable(androidx.appcompat.R.drawable.abc_ic_ab_back_material)
        topAppBar.navigationContentDescription = getString(R.string.back)
        topAppBar.setNavigationOnClickListener {
            onBackPressedDispatcher.onBackPressed()
        }

        val adapter = ArrayAdapter.createFromResource(
            this,
            R.array.severity_options,
            android.R.layout.simple_list_item_1
        )
        UiPickers.attachDatePicker(this, dateInput)
        severityDropdown.setAdapter(adapter)
        severityDropdown.setOnClickListener {
            severityDropdown.error = null
            severityDropdown.showDropDown()
        }
        severityDropdown.setOnItemClickListener { _, _, _, _ ->
            severityDropdown.error = null
        }

        if (savedInstanceState != null) {
            typeInput.setText(savedInstanceState.getString(STATE_TYPE).orEmpty())
            dateInput.setText(savedInstanceState.getString(STATE_DATE).orEmpty())
            descriptionInput.setText(savedInstanceState.getString(STATE_DESCRIPTION).orEmpty())
            val savedSeverity = savedInstanceState.getString(STATE_SEVERITY).orEmpty()
            if (savedSeverity.isNotEmpty()) severityDropdown.setText(savedSeverity, false)
        }

        submitButton.setOnClickListener {
            val type = typeInput.text?.toString()?.trim().orEmpty()
            val date = dateInput.text?.toString()?.trim().orEmpty()
            val description = descriptionInput.text?.toString()?.trim().orEmpty()
            val severity = severityDropdown.text?.toString()?.trim().orEmpty()

            var valid = true
            if (type.isEmpty()) { typeInput.error = "Required"; valid = false } else typeInput.error = null
            if (date.isEmpty()) { dateInput.error = "Required"; valid = false } else dateInput.error = null
            if (description.isEmpty()) { descriptionInput.error = "Required"; valid = false } else descriptionInput.error = null
            if (severity.isEmpty()) { severityDropdown.error = "Required"; valid = false } else severityDropdown.error = null

            if (!valid) {
                Log.w(TAG, "Invalid submission: typeEmpty=${type.isEmpty()}, dateEmpty=${date.isEmpty()}, descEmpty=${description.isEmpty()}, severityEmpty=${severity.isEmpty()}")
                Toast.makeText(this, "Invalid submission: udfyld alle felter", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            // Summary log (requirement)
            Log.i(TAG, "TrafficReport summary: type='$type', severity='$severity', descriptionLen=${description.length}")

            val data = Intent().apply {
                putExtra(EXTRA_TYPE, type)
                putExtra(EXTRA_DATE, date)
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
        outState.putString(STATE_DATE, dateInput.text?.toString().orEmpty())
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
