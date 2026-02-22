package dk.itu.moapd.x9.elie.adapter

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.chip.Chip
import dk.itu.moapd.x9.elie.R
import dk.itu.moapd.x9.elie.TrafficReport

class TrafficReportAdapter(
    private val reports: MutableList<TrafficReport>
) : RecyclerView.Adapter<TrafficReportAdapter.ReportViewHolder>() {

    class ReportViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val title: TextView = itemView.findViewById(R.id.textTitle)
        val type: TextView = itemView.findViewById(R.id.textType)
        val meta: TextView = itemView.findViewById(R.id.textMeta)
        val description: TextView = itemView.findViewById(R.id.textDescription)
        val severity: Chip = itemView.findViewById(R.id.chipSeverity)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ReportViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.row_item_report, parent, false)
        return ReportViewHolder(view)
    }

    override fun onBindViewHolder(holder: ReportViewHolder, position: Int) {
        val report = reports[position]

        holder.title.text = report.title
        holder.type.text = report.type
        holder.description.text = report.description
        holder.severity.text = report.severity
        holder.meta.text = "${report.location} | ${report.date}"
    }

    override fun getItemCount(): Int = reports.size

    fun addReport(report: TrafficReport) {
        reports.add(0, report)
        notifyItemInserted(0)
    }
}