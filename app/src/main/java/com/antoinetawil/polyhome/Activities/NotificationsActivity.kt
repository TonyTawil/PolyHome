package com.antoinetawil.polyhome.Activities

import android.os.Bundle
import android.view.View
import android.widget.Button
import android.widget.TextView
import android.widget.Toast
import androidx.drawerlayout.widget.DrawerLayout
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.antoinetawil.polyhome.Adapters.NotificationListAdapter
import com.antoinetawil.polyhome.Database.DatabaseHelper
import com.antoinetawil.polyhome.R
import com.antoinetawil.polyhome.Utils.HeaderUtils
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class NotificationsActivity : BaseActivity() {

    private lateinit var drawerLayout: DrawerLayout
    private lateinit var recyclerView: RecyclerView
    private lateinit var emptyStateText: TextView
    private lateinit var adapter: NotificationListAdapter
    private lateinit var dbHelper: DatabaseHelper
    private lateinit var clearButton: Button

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_notifications)

        drawerLayout = findViewById(R.id.drawer_layout)
        recyclerView = findViewById(R.id.notificationsRecyclerView)
        emptyStateText = findViewById(R.id.emptyStateText)
        clearButton = findViewById(R.id.clearButton)

        HeaderUtils.setupHeaderWithDrawer(this, drawerLayout)

        dbHelper = DatabaseHelper(this)
        setupRecyclerView()
        setupClearButton()
        loadNotifications()
    }

    private fun setupRecyclerView() {
        adapter = NotificationListAdapter()
        recyclerView.layoutManager = LinearLayoutManager(this)
        recyclerView.adapter = adapter
    }

    private fun setupClearButton() {
        clearButton.setOnClickListener {
            lifecycleScope.launch(Dispatchers.IO) {
                dbHelper.clearAllNotifications()
                withContext(Dispatchers.Main) { loadNotifications() }
            }
        }
    }

    private fun loadNotifications() {
        lifecycleScope.launch(Dispatchers.IO) {
            try {
                val notifications = dbHelper.getAllNotifications()

                withContext(Dispatchers.Main) {
                    if (notifications.isEmpty()) {
                        recyclerView.visibility = View.GONE
                        emptyStateText.visibility = View.VISIBLE
                    } else {
                        recyclerView.visibility = View.VISIBLE
                        emptyStateText.visibility = View.GONE
                        adapter.submitList(notifications)
                    }
                }
            } catch (e: Exception) {
                withContext(Dispatchers.Main) {
                    recyclerView.visibility = View.GONE
                    emptyStateText.visibility = View.VISIBLE
                    Toast.makeText(
                                    this@NotificationsActivity,
                                    getString(R.string.failed_to_load_notifications),
                                    Toast.LENGTH_SHORT
                            )
                            .show()
                }
            }
        }
    }

    override fun onResume() {
        super.onResume()
        loadNotifications()
    }
}
