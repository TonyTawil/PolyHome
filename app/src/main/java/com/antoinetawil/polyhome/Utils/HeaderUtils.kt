package com.antoinetawil.polyhome.Utils

import android.content.Context
import android.content.Intent
import android.view.Gravity
import android.view.View
import androidx.appcompat.app.AppCompatActivity
import androidx.drawerlayout.widget.DrawerLayout
import com.antoinetawil.polyhome.Activities.HouseListActivity
import com.antoinetawil.polyhome.Activities.LoginActivity
import com.antoinetawil.polyhome.Activities.NotificationsActivity
import com.antoinetawil.polyhome.Activities.SchedulesListActivity
import com.antoinetawil.polyhome.Activities.SettingsActivity
import com.antoinetawil.polyhome.Activities.StatisticsActivity
import com.antoinetawil.polyhome.R
import com.google.android.material.navigation.NavigationView

object HeaderUtils {

    fun setupHeaderWithDrawer(activity: AppCompatActivity, drawerLayout: DrawerLayout) {
        val menuButton: View? = activity.findViewById(R.id.menuButton)
        menuButton?.setOnClickListener { drawerLayout.openDrawer(Gravity.START) }

        setupDrawerMenu(activity, drawerLayout)
    }

    private fun setupDrawerMenu(activity: AppCompatActivity, drawerLayout: DrawerLayout) {
        val navigationView: NavigationView = drawerLayout.findViewById(R.id.navigation_view)
        navigationView.setNavigationItemSelectedListener { menuItem ->
            when (menuItem.itemId) {
                R.id.menu_houses -> {
                    val intent = Intent(activity, HouseListActivity::class.java)
                    activity.startActivity(intent)
                }
                R.id.menu_notifications -> {
                    val intent = Intent(activity, NotificationsActivity::class.java)
                    activity.startActivity(intent)
                }
                R.id.menu_settings -> {
                    val intent = Intent(activity, SettingsActivity::class.java)
                    activity.startActivity(intent)
                }
                R.id.menu_schedules -> {
                    val intent = Intent(activity, SchedulesListActivity::class.java)
                    activity.startActivity(intent)
                }
                R.id.menu_statistics -> {
                    val intent = Intent(activity, StatisticsActivity::class.java)
                    activity.startActivity(intent)
                }
                R.id.menu_sign_out -> {
                    clearAuthToken(activity)
                    val intent = Intent(activity, LoginActivity::class.java)
                    intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
                    activity.startActivity(intent)
                }
            }
            drawerLayout.closeDrawer(Gravity.START)
            true
        }
    }

    private fun clearAuthToken(context: Context) {
        val sharedPreferences = context.getSharedPreferences("PolyHomePrefs", Context.MODE_PRIVATE)
        val editor = sharedPreferences.edit()
        editor.remove("auth_token")
        editor.apply()
    }
}
