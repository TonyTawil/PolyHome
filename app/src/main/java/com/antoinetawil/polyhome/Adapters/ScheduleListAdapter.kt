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
        val context = holder.itemView.context

        // Format time
        holder.timeText.text = schedule.dateTime.split(" ")[1]

        // Format days/date
        val daysText =
                when {
                    schedule.recurringDays.isNotEmpty() -> {
                        context.getString(R.string.every) +
                                " " +
                                schedule.recurringDays.joinToString(", ") {
                                    getDayName(it, context)
                                }
                    }
                    schedule.isSpecificDate -> {
                        schedule.dateTime.split(" ")[0]
                    }
                    else -> ""
                }
        holder.daysText.text = daysText

        // Create summary text - Show house ID and number of commands
        val totalCommands = schedule.commands.size
        holder.commandsSummaryText.text =
                context.getString(
                        R.string.house_tag_with_commands,
                        schedule.houseId,
                        totalCommands,
                        if (totalCommands > 1) "s" else ""
                )

        // Setup commands display
        holder.commandsContainer.removeAllViews()

        // Add command views (remove house ID view since it's now in the summary)
        schedule.commands.forEach { command ->
            val commandView =
                    TextView(context).apply {
                        text =
                                "${formatPeripheralId(command.peripheralType, command.peripheralId, context)}: ${formatCommand(command.command, context)}"
                        setTextColor(context.getColor(R.color.secondary_text))
                        textSize = 14f
                        setPadding(0, 4.dpToPx(context), 0, 4.dpToPx(context))
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

    private fun formatCommand(command: String, context: Context): String {
        return when (command) {
            "TURN ON" -> context.getString(R.string.command_turn_on)
            "TURN OFF" -> context.getString(R.string.command_turn_off)
            "OPEN" -> context.getString(R.string.command_open)
            "CLOSE" -> context.getString(R.string.command_close)
            "STOP" -> context.getString(R.string.command_stop)
            else -> command
        }
    }

    private fun getDayName(day: Int, context: Context): String {
        return when (day) {
            1 -> context.getString(R.string.monday)
            2 -> context.getString(R.string.tuesday)
            3 -> context.getString(R.string.wednesday)
            4 -> context.getString(R.string.thursday)
            5 -> context.getString(R.string.friday)
            6 -> context.getString(R.string.saturday)
            7 -> context.getString(R.string.sunday)
            else -> ""
        }
    }

    private fun Int.dpToPx(context: Context): Int {
        return (this * context.resources.displayMetrics.density).toInt()
    }

    private fun formatPeripheralId(type: String, id: String, context: Context): String {
        // Extract floor and number separately (e.g., "Light_1.1" -> floor "1", number "1")
        val pattern = """.*?(\d+)\.(\d+)""".toRegex()
        val matchResult = pattern.find(id)

        val displayNumber =
                if (matchResult != null) {
                    // If we found floor.number format, reconstruct it
                    val (floor, number) = matchResult.destructured
                    "$floor.$number"
                } else {
                    // If no floor.number format, just use any numbers found
                    id.filter { it.isDigit() }
                }

        return when (type.uppercase()) {
            "LIGHT" -> context.getString(R.string.peripheral_light, displayNumber)
            "SHUTTER" -> context.getString(R.string.peripheral_shutter, displayNumber)
            "GARAGE" -> context.getString(R.string.peripheral_garage, displayNumber)
            else -> id
        }
    }
}

private class ScheduleDiffCallback : DiffUtil.ItemCallback<Schedule>() {
    override fun areItemsTheSame(oldItem: Schedule, newItem: Schedule) = oldItem.id == newItem.id
    override fun areContentsTheSame(oldItem: Schedule, newItem: Schedule) = oldItem == newItem
}
