package com.antoinetawil.polyhome.Activities

import android.os.Bundle
import android.view.View
import android.widget.AdapterView
import android.widget.ArrayAdapter
import android.widget.Spinner
import android.widget.Switch
import android.widget.TextView
import androidx.appcompat.app.AppCompatDelegate
import androidx.drawerlayout.widget.DrawerLayout
import com.antoinetawil.polyhome.R
import com.antoinetawil.polyhome.Utils.HeaderUtils

class SettingsActivity : BaseActivity() {

    private lateinit var drawerLayout: DrawerLayout
    private lateinit var themeSwitch: Switch
    private lateinit var themeModeText: TextView
    private lateinit var languageSpinner: Spinner
    private lateinit var securitySpinner: Spinner

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_settings)

        drawerLayout = findViewById(R.id.drawer_layout)
        HeaderUtils.setupHeaderWithDrawer(this, drawerLayout)

        themeSwitch = findViewById(R.id.themeSwitch)
        themeModeText = findViewById(R.id.themeModeText)
        languageSpinner = findViewById(R.id.languageSpinner)
        securitySpinner = findViewById(R.id.securitySpinner)

        val isDark = isDarkModeEnabled()
        themeSwitch.isChecked = isDark
        updateThemeSwitchText(isDark)
        themeSwitch.setOnCheckedChangeListener { _, isChecked -> onThemeChanged(isChecked) }

        setupLanguageSpinner()
        setupSecuritySpinner()
    }

    private fun updateThemeSwitchText(isDark: Boolean) {
        themeModeText.text =
                getString(if (isDark) R.string.enable_light_mode else R.string.enable_dark_mode)
    }

    private fun isDarkModeEnabled(): Boolean {
        val nightMode = AppCompatDelegate.getDefaultNightMode()
        return nightMode == AppCompatDelegate.MODE_NIGHT_YES
    }

    private fun onThemeChanged(isDarkMode: Boolean) {
        themeSwitch.setOnCheckedChangeListener(null)

        setThemePreference(isDarkMode)
        updateThemeSwitchText(isDarkMode)

        themeSwitch.postDelayed(
                {
                    themeSwitch.setOnCheckedChangeListener { _, isChecked ->
                        onThemeChanged(isChecked)
                    }
                },
                1000
        )
    }

    private fun getCurrentLanguage(): String {
        return getLanguagePreference(this)
    }

    private fun setupLanguageSpinner() {
        val languages = arrayOf("English", "Français", "Español", "한국어", "العربية")
        val languageCodes = arrayOf("en", "fr", "es", "ko", "ar")
        val adapter = ArrayAdapter(this, android.R.layout.simple_spinner_item, languages)
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        languageSpinner.adapter = adapter

        val currentLanguage = getCurrentLanguage()
        val index = languageCodes.indexOf(currentLanguage)
        if (index != -1) {
            languageSpinner.setSelection(index)
        }

        languageSpinner.onItemSelectedListener =
                object : AdapterView.OnItemSelectedListener {
                    override fun onItemSelected(
                            parent: AdapterView<*>?,
                            view: View?,
                            position: Int,
                            id: Long
                    ) {
                        val selectedLanguageCode = languageCodes[position]
                        if (selectedLanguageCode != currentLanguage) {
                            setLocalePreference(selectedLanguageCode)
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

        val currentSecurity = getSecurityType()
        val position =
                when (currentSecurity) {
                    "password" -> 1
                    "none" -> 2
                    else -> 0
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
