package dk.itu.moapd.x9.elie.fragment

import android.content.Context
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ArrayAdapter
import android.widget.AutoCompleteTextView
import android.widget.Button
import android.widget.Toast
import androidx.fragment.app.Fragment
import com.google.android.material.textfield.TextInputEditText
import dk.itu.moapd.x9.elie.R

class CreateReportFragment : Fragment() {

    interface Callbacks {
        fun onReportCreated(type: String, description: String, severity: String)
    }

    companion object {
        private const val TAG = "X9_CREATE_REPORT"
        fun newInstance(): CreateReportFragment = CreateReportFragment()
    }

    private var callbacks: Callbacks? = null

    override fun onAttach(context: Context) {
        super.onAttach(context)
        callbacks = context as? Callbacks
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        return inflater.inflate(R.layout.fragment_create_report, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val typeInput = view.findViewById<AutoCompleteTextView>(R.id.typeInput)
        val descriptionInput = view.findViewById<TextInputEditText>(R.id.descriptionInput)
        val severityInput = view.findViewById<AutoCompleteTextView>(R.id.severityInput)
        val submitBtn = view.findViewById<Button>(R.id.submitButton)

        val typeAdapter = ArrayAdapter(
            requireContext(),
            android.R.layout.simple_list_item_1,
            resources.getStringArray(R.array.report_type_options)
        )
        typeInput.setAdapter(typeAdapter)
        typeInput.setOnClickListener { typeInput.showDropDown() }

        val severityAdapter = ArrayAdapter(
            requireContext(),
            android.R.layout.simple_list_item_1,
            resources.getStringArray(R.array.severity_options)
        )
        severityInput.setAdapter(severityAdapter)
        severityInput.setOnClickListener { severityInput.showDropDown() }

        submitBtn.setOnClickListener {
            val type = typeInput.text?.toString()?.trim().orEmpty()
            val description = descriptionInput.text?.toString()?.trim().orEmpty()
            val severity = severityInput.text?.toString()?.trim().orEmpty()

            val missing = mutableListOf<String>()
            if (type.isEmpty()) missing.add("type")
            if (description.isEmpty()) missing.add("description")
            if (severity.isEmpty()) missing.add("severity")

            if (missing.isNotEmpty()) {
                Log.w(TAG, "Invalid submission: missing ${missing.joinToString(", ")}")
                Toast.makeText(requireContext(), "Invalid submission: udfyld alle felter", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            Log.i(TAG, "TrafficReport summary: type='$type', severity='$severity', descriptionLen=${description.length}")
            Toast.makeText(requireContext(), "Report created: $severity", Toast.LENGTH_SHORT).show()
            Toast.makeText(requireContext(), "Traffic report created", Toast.LENGTH_SHORT).show()

            callbacks?.onReportCreated(type, description, severity)
        }
    }

    override fun onDetach() {
        super.onDetach()
        callbacks = null
    }
}