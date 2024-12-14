package com.antoinetawil.polyhome.Activities

import android.content.Intent
import android.os.Bundle
import android.view.View
import android.widget.TextView
import androidx.drawerlayout.widget.DrawerLayout
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.antoinetawil.polyhome.Adapters.ScheduleListAdapter
import com.antoinetawil.polyhome.Database.DatabaseHelper
import com.antoinetawil.polyhome.Models.Schedule
import com.antoinetawil.polyhome.R
import com.antoinetawil.polyhome.Utils.BaseActivity
import com.antoinetawil.polyhome.Utils.HeaderUtils
import com.google.android.material.floatingactionbutton.FloatingActionButton
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

    private fun setupRecyclerView() {
        adapter = ScheduleListAdapter { schedule -> deleteSchedule(schedule) }
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

    private fun showEmptyState() {
        schedulesRecyclerView.visibility = View.GONE
        emptyStateText.visibility = View.VISIBLE
    }

    private fun hideEmptyState() {
        schedulesRecyclerView.visibility = View.VISIBLE
        emptyStateText.visibility = View.GONE
    }
}
