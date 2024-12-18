package com.antoinetawil.polyhome.Utils

import android.content.SharedPreferences
import android.content.res.Configuration
import android.os.Bundle
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.app.AppCompatDelegate
import com.antoinetawil.polyhome.FingerPrint
import java.util.*

open class BaseActivity : AppCompatActivity() {

    private lateinit var sharedPreferences: SharedPreferences
    private lateinit var notificationPermissionHelper: NotificationPermissionHelper
    private var fingerPrint: FingerPrint? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        sharedPreferences = getSharedPreferences("PolyHomePrefs", MODE_PRIVATE)
        notificationPermissionHelper = NotificationPermissionHelper(this)

        // Apply theme and locale before calling super.onCreate()
        applyThemeFromPreferences()
        applyLocale()

        super.onCreate(savedInstanceState)

        // Check notification permission
        notificationPermissionHelper.checkNotificationPermission()

        // Initialize fingerprint if not already verified
        if (!isDeviceVerified()) {
            initializeFingerprint()
        }
    }

    private fun isDeviceVerified(): Boolean {
        return sharedPreferences.getString("VISITOR_ID", null) != null
    }

    private fun showLoading() {
        // Implement loading UI if needed
    }

    private fun hideLoading() {
        // Hide loading UI if needed
    }

    private fun initializeFingerprint() {
        showLoading()
        fingerPrint =
                FingerPrint(this).apply {
                    onFingerprintReady = { visitorId ->
                        hideLoading()
                        handleFingerprintReady(visitorId)
                    }
                    onAuthError = { error ->
                        hideLoading()
                        Toast.makeText(this@BaseActivity, error, Toast.LENGTH_LONG).show()
                        // Handle failed authentication (e.g., close app or restrict access)
                        handleAuthenticationError()
                    }
                }
    }

    private fun handleFingerprintReady(visitorId: String) {
        runOnUiThread {
            // Store the visitor ID for future reference
            sharedPreferences.edit().putString("VISITOR_ID", visitorId).apply()

            // You can override this in child activities if needed
            Toast.makeText(this, "Device verified", Toast.LENGTH_SHORT).show()
        }
    }

    private fun handleAuthenticationError() {
        // You might want to finish() the activity or show a dialog
        // depending on your security requirements
        finish()
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
