package com.antoinetawil.polyhome.Activities

import android.content.Intent
import android.os.Bundle
import android.view.View
import android.widget.TextView
import android.widget.Toast
import androidx.drawerlayout.widget.DrawerLayout
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.antoinetawil.polyhome.Adapters.ScheduleListAdapter
import com.antoinetawil.polyhome.Database.DatabaseHelper
import com.antoinetawil.polyhome.Models.Schedule
import com.antoinetawil.polyhome.R
import com.antoinetawil.polyhome.Activities.BaseActivity
import com.antoinetawil.polyhome.Utils.HeaderUtils
import com.google.android.material.floatingactionbutton.FloatingActionButton
import java.text.SimpleDateFormat
import java.util.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class SchedulesListActivity : BaseActivity() {
    private lateinit var drawerLayout: DrawerLayout
    private lateinit var schedulesRecyclerView: RecyclerView
    private lateinit var addScheduleButton: FloatingActionButton
    private lateinit var emptyStateText: TextView
    private lateinit var dbHelper: DatabaseHelper
    private lateinit var adapter: ScheduleListAdapter

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_schedules_list)

        drawerLayout = findViewById(R.id.drawer_layout)
        schedulesRecyclerView = findViewById(R.id.schedulesRecyclerView)
        addScheduleButton = findViewById(R.id.addScheduleButton)
        emptyStateText = findViewById(R.id.emptyStateText)

        HeaderUtils.setupHeaderWithDrawer(this, drawerLayout)
        dbHelper = DatabaseHelper(this)
        setupRecyclerView()
        loadSchedules()

        addScheduleButton.setOnClickListener {
            startActivity(Intent(this, SchedulesActivity::class.java))
        }

        // Initially show empty state
        showEmptyState()
    }

    override fun onResume() {
        super.onResume()
        loadSchedules() // Refresh the list when returning to this activity
    }

    private fun setupRecyclerView() {
        adapter = ScheduleListAdapter(
            onDeleteClick = { schedule -> deleteSchedule(schedule) },
            onScheduleClick = { schedule -> editSchedule(schedule) }
        )
        schedulesRecyclerView.layoutManager = LinearLayoutManager(this)
        schedulesRecyclerView.adapter = adapter
    }

    private fun loadSchedules() {
        lifecycleScope.launch(Dispatchers.IO) {
            val schedules = dbHelper.getAllSchedules()
            withContext(Dispatchers.Main) {
                if (schedules.isEmpty()) {
                    showEmptyState()
                } else {
                    hideEmptyState()
                    adapter.submitList(schedules)
                }
            }
        }
    }

    private fun deleteSchedule(schedule: Schedule) {
        lifecycleScope.launch(Dispatchers.IO) {
            dbHelper.deleteSchedule(schedule.id)
            loadSchedules() // Reload the list after deletion
        }
    }

    private fun editSchedule(schedule: Schedule) {
        val intent = Intent(this, SchedulesActivity::class.java).apply {
            putExtra("EDIT_MODE", true)
            putExtra("SCHEDULE_ID", schedule.id)
        }
        startActivity(intent)
    }

    private fun showEmptyState() {
        schedulesRecyclerView.visibility = View.GONE
        emptyStateText.visibility = View.VISIBLE
    }

    private fun hideEmptyState() {
        schedulesRecyclerView.visibility = View.VISIBLE
        emptyStateText.visibility = View.GONE
    }

    fun updateScheduleEnabled(scheduleId: Long, isEnabled: Boolean) {
        lifecycleScope.launch(Dispatchers.IO) {
            val schedule = dbHelper.getSchedule(scheduleId)
            if (schedule != null) {
                if (isEnabled) {
                    // Calculate time until next occurrence
                    val nextOccurrence = calculateNextOccurrence(schedule)
                    val timeDiff = nextOccurrence - System.currentTimeMillis()

                    val hours = timeDiff / (1000 * 60 * 60)
                    val minutes = (timeDiff % (1000 * 60 * 60)) / (1000 * 60)

                    withContext(Dispatchers.Main) {
                        val message =
                                when {
                                    hours > 0 ->
                                            getString(
                                                    R.string.schedule_will_run_hours_minutes,
                                                    hours,
                                                    minutes
                                            )
                                    minutes > 0 ->
                                            getString(R.string.schedule_will_run_minutes, minutes)
                                    else -> getString(R.string.schedule_will_run_soon)
                                }
                        Toast.makeText(this@SchedulesListActivity, message, Toast.LENGTH_LONG)
                                .show()
                    }
                }

                // Update the schedule as before
                if (schedule.recurringDays.isEmpty() && !schedule.isSpecificDate) {
                    val scheduleTime =
                            SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.getDefault())
                                    .parse(schedule.dateTime)
                                    ?.time
                                    ?: return@launch

                    if (scheduleTime < System.currentTimeMillis()) {
                        val calendar = Calendar.getInstance()
                        val timeParts = schedule.dateTime.split(" ")[1].split(":")
                        val hour = timeParts[0].toInt()
                        val minute = timeParts[1].toInt()

                        calendar.set(Calendar.HOUR_OF_DAY, hour)
                        calendar.set(Calendar.MINUTE, minute)

                        if (calendar.timeInMillis <= System.currentTimeMillis()) {
                            calendar.add(Calendar.DAY_OF_MONTH, 1)
                        }

                        val newDateTime =
                                SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.getDefault())
                                        .format(calendar.time)
                        dbHelper.updateScheduleDateTime(scheduleId, newDateTime)
                    }
                }

                dbHelper.updateScheduleEnabled(scheduleId, isEnabled)
                loadSchedules()
            }
        }
    }

    private fun calculateNextOccurrence(schedule: Schedule): Long {
        val calendar = Calendar.getInstance()
        val timeParts = schedule.dateTime.split(" ")[1].split(":")
        val hour = timeParts[0].toInt()
        val minute = timeParts[1].toInt()

        when {
            schedule.recurringDays.isNotEmpty() -> {
                // Find next recurring day
                val currentDay = calendar.get(Calendar.DAY_OF_WEEK)
                val nextDay =
                        schedule.recurringDays
                                .map {
                                    if (it == 7) 1 else it + 1
                                } // Convert our day format to Calendar format
                                .sortedBy { if (it > currentDay) it else it + 7 }
                                .first()

                val daysToAdd =
                        if (nextDay > currentDay) nextDay - currentDay
                        else 7 - (currentDay - nextDay)
                calendar.add(Calendar.DAY_OF_MONTH, daysToAdd)
            }
            schedule.isSpecificDate -> {
                // Use the specific date
                val dateParts = schedule.dateTime.split(" ")[0].split("-")
                calendar.set(dateParts[0].toInt(), dateParts[1].toInt() - 1, dateParts[2].toInt())
            }
            else -> {
                // Use today or tomorrow
                if (hour < calendar.get(Calendar.HOUR_OF_DAY) ||
                                (hour == calendar.get(Calendar.HOUR_OF_DAY) &&
                                        minute <= calendar.get(Calendar.MINUTE))
                ) {
                    calendar.add(Calendar.DAY_OF_MONTH, 1)
                }
            }
        }

        calendar.set(Calendar.HOUR_OF_DAY, hour)
        calendar.set(Calendar.MINUTE, minute)
        calendar.set(Calendar.SECOND, 0)
        calendar.set(Calendar.MILLISECOND, 0)

        return calendar.timeInMillis
    }
}
