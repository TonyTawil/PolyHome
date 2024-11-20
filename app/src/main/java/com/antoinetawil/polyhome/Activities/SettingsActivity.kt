package com.antoinetawil.polyhome.Activities

import android.content.SharedPreferences
import android.os.Bundle
import android.widget.Switch
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.app.AppCompatDelegate
import androidx.drawerlayout.widget.DrawerLayout
import com.antoinetawil.polyhome.R
import com.antoinetawil.polyhome.Utils.HeaderUtils

class SettingsActivity : AppCompatActivity() {

    private lateinit var drawerLayout: DrawerLayout
    private lateinit var themeSwitch: Switch
    private lateinit var sharedPreferences: SharedPreferences

    override fun onCreate(savedInstanceState: Bundle?) {
        // Apply the theme before the activity is created
        applyThemeFromPreferences()

        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_settings)

        drawerLayout = findViewById(R.id.drawer_layout)
        HeaderUtils.setupHeaderWithDrawer(this, drawerLayout)

        themeSwitch = findViewById(R.id.themeSwitch)
        sharedPreferences = getSharedPreferences("PolyHomePrefs", MODE_PRIVATE)

        // Set the initial state of the switch based on the current theme
        themeSwitch.isChecked = isDarkModeEnabled()

        // Handle theme toggle
        themeSwitch.setOnCheckedChangeListener { _, isChecked ->
            toggleTheme(isChecked)
        }
    }

    private fun toggleTheme(enableDarkMode: Boolean) {
        // Save the preference
        sharedPreferences.edit()
            .putBoolean("DARK_MODE", enableDarkMode)
            .apply()

        // Update theme dynamically
        AppCompatDelegate.setDefaultNightMode(
            if (enableDarkMode) AppCompatDelegate.MODE_NIGHT_YES
            else AppCompatDelegate.MODE_NIGHT_NO
        )
    }

    private fun isDarkModeEnabled(): Boolean {
        val nightMode = AppCompatDelegate.getDefaultNightMode()
        return when (nightMode) {
            AppCompatDelegate.MODE_NIGHT_YES -> true
            AppCompatDelegate.MODE_NIGHT_NO -> false
            else -> sharedPreferences.getBoolean("DARK_MODE", false)
        }
    }

    private fun applyThemeFromPreferences() {
        val sharedPreferences = getSharedPreferences("PolyHomePrefs", MODE_PRIVATE)
        val isDarkMode = sharedPreferences.getBoolean("DARK_MODE", false)

        AppCompatDelegate.setDefaultNightMode(
            if (isDarkMode) AppCompatDelegate.MODE_NIGHT_YES
            else AppCompatDelegate.MODE_NIGHT_NO
        )
    }
}
