package com.example.gymapplication

import android.graphics.Color
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import java.text.SimpleDateFormat
import java.util.*

class CheckInAdapter(
    private val checkInList: List<CheckIn>
) : RecyclerView.Adapter<CheckInAdapter.CheckInViewHolder>() {

    inner class CheckInViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val tvDay: TextView     = itemView.findViewById(R.id.tvCheckInDay)
        val tvDate: TextView    = itemView.findViewById(R.id.textCheckInDate)
        val tvTime: TextView    = itemView.findViewById(R.id.textCheckInTime)
        val tvBadge: TextView   = itemView.findViewById(R.id.tvCheckInBadge)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): CheckInViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_checkin, parent, false)
        return CheckInViewHolder(view)
    }

    override fun onBindViewHolder(holder: CheckInViewHolder, position: Int) {
        val item = checkInList[position]

        // Format date nicely
        try {
            val sdf = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
            val date = sdf.parse(item.date)
            if (date != null) {
                holder.tvDay.text  = SimpleDateFormat("EEE", Locale.getDefault()).format(date)
                holder.tvDate.text = SimpleDateFormat("dd MMM yyyy", Locale.getDefault()).format(date)
            }
        } catch (_: Exception) {
            holder.tvDay.text  = ""
            holder.tvDate.text = item.date
        }

        holder.tvTime.text = if (item.time.isNotBlank()) "at ${item.time}" else ""

        // Badge: today = green, else blue
        val today = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(Date())
        if (item.date == today) {
            holder.tvBadge.text = "Today"
            holder.tvBadge.setBackgroundColor(Color.parseColor("#E8F5E9"))
            holder.tvBadge.setTextColor(Color.parseColor("#2E7D32"))
        } else {
            holder.tvBadge.text = "Visited"
            holder.tvBadge.setBackgroundColor(Color.parseColor("#E8EAF6"))
            holder.tvBadge.setTextColor(Color.parseColor("#3949AB"))
        }
    }

    override fun getItemCount(): Int = checkInList.size
}