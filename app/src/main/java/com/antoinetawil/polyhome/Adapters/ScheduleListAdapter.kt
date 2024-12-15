package com.antoinetawil.polyhome.Adapters

import android.content.Context
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageButton
import android.widget.LinearLayout
import android.widget.TextView
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.antoinetawil.polyhome.Activities.SchedulesListActivity
import com.antoinetawil.polyhome.Models.Schedule
import com.antoinetawil.polyhome.R
import com.google.android.material.switchmaterial.SwitchMaterial

class ScheduleListAdapter(private val onDeleteClick: (Schedule) -> Unit) :
        ListAdapter<Schedule, ScheduleListAdapter.ScheduleViewHolder>(ScheduleDiffCallback()) {

    class ScheduleViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val timeText: TextView = view.findViewById(R.id.timeText)
        val daysText: TextView = view.findViewById(R.id.daysText)
        val commandsSummaryText: TextView = view.findViewById(R.id.commandsSummaryText)
        val commandsContainer: LinearLayout = view.findViewById(R.id.commandsContainer)
        val expandButton: ImageButton = view.findViewById(R.id.expandButton)
        val deleteButton: ImageButton = view.findViewById(R.id.deleteButton)
        val scheduleSwitch: SwitchMaterial = view.findViewById(R.id.scheduleSwitch)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ScheduleViewHolder {
        val view =
                LayoutInflater.from(parent.context)
                        .inflate(R.layout.schedule_list_item, parent, false)
        return ScheduleViewHolder(view)
    }

    override fun onBindViewHolder(holder: ScheduleViewHolder, position: Int) {
        val schedule = getItem(position)

        // Format time
        holder.timeText.text = schedule.dateTime.split(" ")[1]

        // Format days/date
        val daysText =
                when {
                    schedule.recurringDays.isNotEmpty() -> {
                        "Every " + schedule.recurringDays.joinToString(", ") { getDayName(it) }
                    }
                    schedule.isSpecificDate -> {
                        schedule.dateTime.split(" ")[0]
                    }
                    else -> ""
                }
        holder.daysText.text = daysText

        // Create summary text
        val totalCommands = schedule.commands.size
        val firstCommand = schedule.commands.first()
        holder.commandsSummaryText.text =
                if (totalCommands > 1) {
                    "${formatCommand(firstCommand.command)} and ${totalCommands - 1} more commands"
                } else {
                    formatCommand(firstCommand.command)
                }

        // Setup commands display
        holder.commandsContainer.removeAllViews()
        schedule.commands.forEach { command ->
            val commandView =
                    TextView(holder.itemView.context).apply {
                        text = "${command.peripheralId}: ${formatCommand(command.command)}"
                        setTextColor(holder.itemView.context.getColor(R.color.secondary_text))
                        textSize = 14f
                        setPadding(
                                0,
                                4.dpToPx(holder.itemView.context),
                                0,
                                4.dpToPx(holder.itemView.context)
                        )
                    }
            holder.commandsContainer.addView(commandView)
        }

        // Setup expand/collapse functionality
        holder.expandButton.setOnClickListener {
            val isExpanded = holder.commandsContainer.visibility == View.VISIBLE
            holder.commandsContainer.visibility = if (isExpanded) View.GONE else View.VISIBLE
            holder.expandButton.rotation = if (isExpanded) 0f else 180f
        }

        holder.deleteButton.setOnClickListener { onDeleteClick(schedule) }

        // Setup switch
        holder.scheduleSwitch.isChecked = schedule.isEnabled
        holder.scheduleSwitch.setOnCheckedChangeListener { _, isChecked ->
            (holder.itemView.context as? SchedulesListActivity)?.let { activity ->
                activity.updateScheduleEnabled(schedule.id, isChecked)
            }
        }
    }

    private fun formatCommand(command: String): String {
        return when (command) {
            "TURN ON" -> "Turn On"
            "TURN OFF" -> "Turn Off"
            "OPEN" -> "Open"
            "CLOSE" -> "Close"
            else -> command
        }
    }

    private fun getDayName(day: Int): String {
        return when (day) {
            1 -> "Monday"
            2 -> "Tuesday"
            3 -> "Wednesday"
            4 -> "Thursday"
            5 -> "Friday"
            6 -> "Saturday"
            7 -> "Sunday"
            else -> ""
        }
    }

    private fun Int.dpToPx(context: Context): Int {
        return (this * context.resources.displayMetrics.density).toInt()
    }
}

private class ScheduleDiffCallback : DiffUtil.ItemCallback<Schedule>() {
    override fun areItemsTheSame(oldItem: Schedule, newItem: Schedule) = oldItem.id == newItem.id
    override fun areContentsTheSame(oldItem: Schedule, newItem: Schedule) = oldItem == newItem
}
