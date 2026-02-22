package dk.itu.moapd.x9.elie.adapter

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import dk.itu.moapd.x9.elie.R
import dk.itu.moapd.x9.elie.data.TrafficReport

class TrafficReportAdapter(
    private val items: MutableList<TrafficReport>
) : RecyclerView.Adapter<TrafficReportAdapter.ReportViewHolder>() {

    class ReportViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val title: TextView = view.findViewById(R.id.itemTitle)
        val meta: TextView = view.findViewById(R.id.itemMeta)
        val description: TextView = view.findViewById(R.id.itemDescription)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ReportViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.row_item_report, parent, false)
        return ReportViewHolder(view)
    }

    override fun onBindViewHolder(holder: ReportViewHolder, position: Int) {
        val report = items[position]
        holder.title.text = report.title
        holder.meta.text = "${report.location} | ${report.date} | ${report.type} | ${report.severity}"
        holder.description.text = report.description
    }

    override fun getItemCount(): Int = items.size
}