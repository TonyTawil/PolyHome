package com.antoinetawil.polyhome.Utils

import android.content.SharedPreferences
import android.content.res.Configuration
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.app.AppCompatDelegate
import java.util.*

open class BaseActivity : AppCompatActivity() {

    private lateinit var sharedPreferences: SharedPreferences
    private lateinit var notificationPermissionHelper: NotificationPermissionHelper

    override fun onCreate(savedInstanceState: Bundle?) {
        sharedPreferences = getSharedPreferences("PolyHomePrefs", MODE_PRIVATE)
        notificationPermissionHelper = NotificationPermissionHelper(this)

        // Apply theme and locale before calling super.onCreate()
        applyThemeFromPreferences()
        applyLocale()

        super.onCreate(savedInstanceState)

        // Check notification permission
        notificationPermissionHelper.checkNotificationPermission()
    }

    private fun applyThemeFromPreferences() {
        val isDarkMode = sharedPreferences.getBoolean("DARK_MODE", false)
        AppCompatDelegate.setDefaultNightMode(
                if (isDarkMode) AppCompatDelegate.MODE_NIGHT_YES
                else AppCompatDelegate.MODE_NIGHT_NO
        )
    }

    private fun applyLocale() {
        val language = sharedPreferences.getString("LANGUAGE", "en") ?: "en"
        val locale = Locale(language)
        Locale.setDefault(locale)

        val config = Configuration()
        config.setLocale(locale)

        baseContext.resources.updateConfiguration(config, baseContext.resources.displayMetrics)
    }

    protected fun setThemePreference(enableDarkMode: Boolean) {
        sharedPreferences.edit().putBoolean("DARK_MODE", enableDarkMode).apply()

        AppCompatDelegate.setDefaultNightMode(
                if (enableDarkMode) AppCompatDelegate.MODE_NIGHT_YES
                else AppCompatDelegate.MODE_NIGHT_NO
        )
    }

    protected fun setLocalePreference(language: String) {
        sharedPreferences.edit().putString("LANGUAGE", language).apply()

        val locale = Locale(language)
        Locale.setDefault(locale)

        val config = Configuration()
        config.setLocale(locale)

        baseContext.resources.updateConfiguration(config, baseContext.resources.displayMetrics)

        recreate()
    }
}
