package dk.itu.moapd.x9.elie.ui

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.firebase.ui.database.FirebaseRecyclerAdapter
import com.firebase.ui.database.FirebaseRecyclerOptions
import dk.itu.moapd.x9.elie.R
import dk.itu.moapd.x9.elie.databinding.RowItemReportBinding
import dk.itu.moapd.x9.elie.model.TrafficReport

class TrafficReportAdapter(
    options: FirebaseRecyclerOptions<TrafficReport>,
    private val onReportSelected: (TrafficReport) -> Unit
) : FirebaseRecyclerAdapter<TrafficReport, TrafficReportAdapter.ReportViewHolder>(options) {

    class ReportViewHolder(
        private val binding: RowItemReportBinding
    ) : RecyclerView.ViewHolder(binding.root) {
        fun bind(report: TrafficReport, onClick: (TrafficReport) -> Unit) {
            binding.itemTitle.text = report.title
            binding.itemMeta.text = binding.root.context.getString(
                R.string.report_meta_details,
                report.location,
                report.date,
                report.type,
                report.severity
            )
            binding.itemDescription.text = report.description
            binding.root.setOnClickListener {
                onClick(report)
            }
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ReportViewHolder {
        val binding = RowItemReportBinding.inflate(
            LayoutInflater.from(parent.context),
            parent,
            false
        )
        return ReportViewHolder(binding)
    }

    override fun onBindViewHolder(holder: ReportViewHolder, position: Int, model: TrafficReport) {
        holder.bind(model, onReportSelected)
    }
}
