package com.antoinetawil.polyhome.Activities

import android.os.Bundle
import android.view.View
import android.widget.AdapterView
import android.widget.ArrayAdapter
import android.widget.Spinner
import android.widget.Switch
import androidx.appcompat.app.AppCompatDelegate
import androidx.drawerlayout.widget.DrawerLayout
import com.antoinetawil.polyhome.R
import com.antoinetawil.polyhome.Utils.HeaderUtils

class SettingsActivity : BaseActivity() {

    private lateinit var drawerLayout: DrawerLayout
    private lateinit var themeSwitch: Switch
    private lateinit var languageSpinner: Spinner
    private lateinit var securitySpinner: Spinner

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_settings)

        drawerLayout = findViewById(R.id.drawer_layout)
        HeaderUtils.setupHeaderWithDrawer(this, drawerLayout)

        themeSwitch = findViewById(R.id.themeSwitch)
        languageSpinner = findViewById(R.id.languageSpinner)
        securitySpinner = findViewById(R.id.securitySpinner)

        themeSwitch.isChecked = isDarkModeEnabled()
        themeSwitch.setOnCheckedChangeListener { _, isChecked -> onThemeChanged(isChecked) }

        setupLanguageSpinner()
        setupSecuritySpinner()
    }

    private fun isDarkModeEnabled(): Boolean {
        val nightMode = AppCompatDelegate.getDefaultNightMode()
        return nightMode == AppCompatDelegate.MODE_NIGHT_YES
    }

    private fun onThemeChanged(isDarkMode: Boolean) {
        setThemePreference(isDarkMode)
    }

    private fun getCurrentLanguage(): String {
        return getLanguagePreference(this)
    }

    private fun setupLanguageSpinner() {
        val languageSpinner = findViewById<Spinner>(R.id.languageSpinner)
        val languages = resources.getStringArray(R.array.language_options)
        val adapter = ArrayAdapter(this, android.R.layout.simple_spinner_item, languages)
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        languageSpinner.adapter = adapter

        // Set current selection
        val currentLanguage = getCurrentLanguage()
        val position =
                when (currentLanguage) {
                    "fr" -> 1
                    "es" -> 2
                    "ar" -> 3
                    "ko" -> 4
                    else -> 0
                }
        languageSpinner.setSelection(position)

        languageSpinner.onItemSelectedListener =
                object : AdapterView.OnItemSelectedListener {
                    override fun onItemSelected(
                            parent: AdapterView<*>?,
                            view: View?,
                            pos: Int,
                            id: Long
                    ) {
                        val newLocale =
                                when (pos) {
                                    1 -> "fr"
                                    2 -> "es"
                                    3 -> "ar"
                                    4 -> "ko"
                                    else -> "en"
                                }
                        val currentLanguage = getCurrentLanguage()
                        if (newLocale != currentLanguage) {
                            setLocalePreference(newLocale)
                            // Recreate the activity to apply the new locale
                            recreate()
                        }
                    }

                    override fun onNothingSelected(parent: AdapterView<*>?) {}
                }
    }

    private fun setupSecuritySpinner() {
        val securityOptions =
                arrayOf(
                        getString(R.string.auth_option_fingerprint),
                        getString(R.string.auth_option_password),
                        getString(R.string.auth_option_none)
                )
        val adapter = ArrayAdapter(this, android.R.layout.simple_spinner_item, securityOptions)
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        securitySpinner.adapter = adapter

        // Set current selection
        val currentSecurity = getSecurityType()
        val position =
                when (currentSecurity) {
                    "password" -> 1
                    "none" -> 2
                    else -> 0 // fingerprint is default
                }
        securitySpinner.setSelection(position)

        securitySpinner.onItemSelectedListener =
                object : AdapterView.OnItemSelectedListener {
                    override fun onItemSelected(
                            parent: AdapterView<*>?,
                            view: View?,
                            pos: Int,
                            id: Long
                    ) {
                        val newSecurityType =
                                when (pos) {
                                    1 -> "password"
                                    2 -> "none"
                                    else -> "fingerprint"
                                }
                        if (newSecurityType != currentSecurity) {
                            setSecurityType(newSecurityType)
                        }
                    }

                    override fun onNothingSelected(parent: AdapterView<*>?) {}
                }
    }
}
