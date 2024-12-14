package com.antoinetawil.polyhome.Adapters

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageButton
import android.widget.TextView
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.antoinetawil.polyhome.Models.Schedule
import com.antoinetawil.polyhome.R

class ScheduleListAdapter(private val onDeleteClick: (Schedule) -> Unit) :
        ListAdapter<Schedule, ScheduleListAdapter.ScheduleViewHolder>(ScheduleDiffCallback()) {

    class ScheduleViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val dateTimeText: TextView = view.findViewById(R.id.dateTimeText)
        val peripheralText: TextView = view.findViewById(R.id.peripheralText)
        val commandText: TextView = view.findViewById(R.id.commandText)
        val deleteButton: ImageButton = view.findViewById(R.id.deleteButton)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ScheduleViewHolder {
        val view =
                LayoutInflater.from(parent.context)
                        .inflate(R.layout.schedule_list_item, parent, false)
        return ScheduleViewHolder(view)
    }

    override fun onBindViewHolder(holder: ScheduleViewHolder, position: Int) {
        val schedule = getItem(position)
        holder.dateTimeText.text = schedule.dateTime
        holder.peripheralText.text = schedule.peripheralId
        holder.commandText.text = schedule.command
        holder.deleteButton.setOnClickListener { onDeleteClick(schedule) }
    }
}

private class ScheduleDiffCallback : DiffUtil.ItemCallback<Schedule>() {
    override fun areItemsTheSame(oldItem: Schedule, newItem: Schedule) = oldItem.id == newItem.id
    override fun areContentsTheSame(oldItem: Schedule, newItem: Schedule) = oldItem == newItem
}
