package com.example.thesis_saas_mobile_app.adapters

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.example.thesis_saas_mobile_app.R
import java.text.SimpleDateFormat
import java.util.*

data class Alert(
    val alertId: String,
    val machineName: String,
    val machineId: String,
    val alertIssueDate: String
)

class AlertsAdapter(private val dataSet: MutableList<Alert>) :
    RecyclerView.Adapter<AlertsAdapter.ViewHolder>() {

    val dateFormat = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSS'Z'", Locale.getDefault())
    var onItemClick: ((Alert) -> Unit)? = null

    inner class ViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val eventTv: TextView = view.findViewById(R.id.eventTV)
        val issuePlacedTV: TextView = view.findViewById(R.id.issuePlacedTV)

        init {
            itemView.setOnClickListener {
                onItemClick?.invoke(dataSet[adapterPosition])
            }
        }
    }

    override fun onCreateViewHolder(viewGroup: ViewGroup, viewType: Int): ViewHolder {
        val view = LayoutInflater.from(viewGroup.context)
            .inflate(R.layout.alerts_item, viewGroup, false)
        return ViewHolder(view)
    }

    override fun onBindViewHolder(viewHolder: ViewHolder, position: Int) {
        dateFormat.timeZone = TimeZone.getTimeZone("UTC")
        val alert = dataSet[position]
        val issuePlaced = dateFormat.parse(alert.alertIssueDate) ?: Date()
        val diffInMillis = Date().time - issuePlaced.time
        val diffInMinutes = diffInMillis / (1000 * 60)
        val diffInHours = diffInMillis / (1000 * 60 * 60)

        viewHolder.eventTv.text = "Issue on ${alert.machineName}"
        viewHolder.issuePlacedTV.text = "Issue placed: ${
            when {
                diffInHours > 0 -> "$diffInHours hour(s)"
                diffInMinutes > 0 -> "$diffInMinutes minute(s)"
                else -> "less than a minute"
            }
        } ago"
    }

    override fun getItemCount() = dataSet.size

    fun add(newAlerts: List<Alert>) {
        dataSet.clear()
        dataSet.addAll(newAlerts)
        notifyDataSetChanged()
    }
}
