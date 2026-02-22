package dk.itu.moapd.x9.elie

import android.content.Context
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModelProvider
import androidx.recyclerview.widget.ItemTouchHelper
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.textfield.MaterialAutoCompleteTextView
import com.google.android.material.textfield.TextInputEditText

class MainFragment : Fragment() {

    interface Callbacks {
        fun onOpenCreateReport()
    }

    companion object {
        private const val TAG = "X9_MAIN_FRAGMENT"
        fun newInstance() = MainFragment()
    }

    private var callbacks: Callbacks? = null

    private lateinit var titleInput: TextInputEditText
    private lateinit var locationInput: TextInputEditText
    private lateinit var dateInput: TextInputEditText
    private lateinit var typeInput: MaterialAutoCompleteTextView
    private lateinit var descriptionInput: TextInputEditText

    private lateinit var recyclerView: RecyclerView
    private lateinit var reportAdapter: TrafficReportAdapter

    private lateinit var viewModel: TrafficReportViewModel

    private val reports = mutableListOf<TrafficReport>()

    override fun onAttach(context: Context) {
        super.onAttach(context)
        callbacks = context as? Callbacks
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        return inflater.inflate(R.layout.fragment_main, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        viewModel = ViewModelProvider(requireActivity())[TrafficReportViewModel::class.java]

        bindViews(view)
        setupPickers()
        setupRecyclerView()
        observeReports()
        setupButtons(view)

        viewModel.addMockDataIfEmpty()
    }

    private fun bindViews(view: View) {
        titleInput = view.findViewById(R.id.reportTitleInput)
        locationInput = view.findViewById(R.id.reportLocationInput)
        dateInput = view.findViewById(R.id.reportDateInput)
        typeInput = view.findViewById(R.id.reportTypeInput)
        descriptionInput = view.findViewById(R.id.reportDescriptionInput)

        recyclerView = view.findViewById(R.id.recycler_view)
    }

    private fun setupPickers() {
        UiPickers.attachDatePicker(requireContext(), dateInput)

        val reportTypes = resources.getStringArray(R.array.report_type_options).toList()
        UiPickers.attachDropdown(requireContext(), typeInput, reportTypes)
    }

    private fun setupRecyclerView() {
        reportAdapter = TrafficReportAdapter(reports)
        recyclerView.layoutManager = LinearLayoutManager(requireContext())
        recyclerView.adapter = reportAdapter

        val swipeHandler = object : ItemTouchHelper.SimpleCallback(
            0,
            ItemTouchHelper.LEFT or ItemTouchHelper.RIGHT
        ) {
            override fun onMove(
                recyclerView: RecyclerView,
                viewHolder: RecyclerView.ViewHolder,
                target: RecyclerView.ViewHolder
            ): Boolean = false

            override fun onSwiped(viewHolder: RecyclerView.ViewHolder, direction: Int) {
                val position = viewHolder.bindingAdapterPosition
                if (position == RecyclerView.NO_POSITION) return
                viewModel.removeAt(position)
            }
        }
        recyclerView.setHasFixedSize(true)
        ItemTouchHelper(swipeHandler).attachToRecyclerView(recyclerView)
    }

    private fun observeReports() {
        viewModel.reports.observe(viewLifecycleOwner) { newList ->
            reports.clear()
            reports.addAll(newList)
            reportAdapter.notifyDataSetChanged()
        }
    }

    private fun setupButtons(view: View) {
        val openBtn = view.findViewById<Button>(R.id.btnOpenReport)
        openBtn.setOnClickListener { callbacks?.onOpenCreateReport() }

        val minorBtn = view.findViewById<Button>(R.id.buttonMinor)
        val moderateBtn = view.findViewById<Button>(R.id.buttonModerate)
        val majorBtn = view.findViewById<Button>(R.id.buttonMajor)

        minorBtn.setOnClickListener { validateAndProcess("Minor") }
        moderateBtn.setOnClickListener { validateAndProcess("Moderate") }
        majorBtn.setOnClickListener { validateAndProcess("Major") }
    }

    private fun validateAndProcess(severity: String) {
        val title = titleInput.text?.toString()?.trim().orEmpty()
        val location = locationInput.text?.toString()?.trim().orEmpty()
        val date = dateInput.text?.toString()?.trim().orEmpty()
        val type = typeInput.text?.toString()?.trim().orEmpty()
        val description = descriptionInput.text?.toString()?.trim().orEmpty()

        if (title.isEmpty() || location.isEmpty() || date.isEmpty() || type.isEmpty() || description.isEmpty()) {
            Toast.makeText(requireContext(), "Udfyld alle felter", Toast.LENGTH_SHORT).show()
            Log.w(TAG, "Invalid submission: empty field")
            return
        }

        val report = TrafficReport(
            title = title,
            location = location,
            date = date,
            type = type,
            severity = severity,
            description = description
        )

        viewModel.addReport(report)

        Toast.makeText(requireContext(), "Saved: $severity", Toast.LENGTH_SHORT).show()
        Log.i(TAG, "Saved report: '$title' $severity")
    }

    fun applyReturnedReport(type: String, description: String, severity: String) {
        typeInput.setText(type, false)
        descriptionInput.setText(description)

        Toast.makeText(requireContext(), "Report received: $severity", Toast.LENGTH_SHORT).show()
        Log.i(TAG, "Returned report: type='$type', severity='$severity'")
    }

    override fun onDetach() {
        super.onDetach()
        callbacks = null
    }


}