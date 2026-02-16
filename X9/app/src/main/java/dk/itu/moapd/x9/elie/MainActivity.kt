package dk.itu.moapd.x9.elie

import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.widget.Button
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import com.google.android.material.textfield.TextInputEditText
import android.widget.AutoCompleteTextView

class MainActivity : AppCompatActivity() {

    companion object {
        private const val TAG = "X9_LIFECYCLE_MAIN"
    }

    private val createReportLauncher =
        registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->

            if (result.resultCode != RESULT_OK) return@registerForActivityResult
            val data = result.data ?: return@registerForActivityResult

            val type = data.getStringExtra(TrafficReportActivity.EXTRA_TYPE).orEmpty()
            val description = data.getStringExtra(TrafficReportActivity.EXTRA_DESCRIPTION).orEmpty()
            val severity = data.getStringExtra(TrafficReportActivity.EXTRA_SEVERITY).orEmpty()

            Log.d(TAG, "Returned report type=$type severity=$severity")
            Toast.makeText(this, "Report received $severity", Toast.LENGTH_SHORT).show()

            val typeInput = findViewById<AutoCompleteTextView>(R.id.reportTypeInput)
            val descriptionInput = findViewById<TextInputEditText>(R.id.reportDescriptionInput)

            typeInput.setText(type, false)
            descriptionInput.setText(description)
        }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        Log.d(TAG, "onCreate")

        enableEdgeToEdge()
        setContentView(R.layout.activity_main)

        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }

        val openReportButton = findViewById<Button>(R.id.btnOpenReport)
        openReportButton.setOnClickListener {
            Log.d(TAG, "Open report clicked")
            val intent = Intent(this, TrafficReportActivity::class.java)
            createReportLauncher.launch(intent)
        }
    }

    override fun onStart() { super.onStart(); Log.d(TAG, "onStart") }
    override fun onResume() { super.onResume(); Log.d(TAG, "onResume") }
    override fun onPause() { super.onPause(); Log.d(TAG, "onPause") }
    override fun onStop() { super.onStop(); Log.d(TAG, "onStop") }
    override fun onDestroy() { super.onDestroy(); Log.d(TAG, "onDestroy") }
    override fun onRestart() { super.onRestart(); Log.d(TAG, "onRestart") }
}
