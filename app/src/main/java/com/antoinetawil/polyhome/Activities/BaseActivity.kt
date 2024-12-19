package com.antoinetawil.polyhome.Activities

import android.content.Context
import android.content.res.Configuration
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.app.AppCompatDelegate
import java.util.Locale

open class BaseActivity : AppCompatActivity() {

    companion object {
        const val PREFS_NAME = "PolyHomePrefs"
        const val LANGUAGE_KEY = "LANGUAGE"
        const val DARK_MODE_KEY = "DARK_MODE"
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        // Set theme before calling super.onCreate
        val isDarkMode =
                getSharedPreferences(PREFS_NAME, MODE_PRIVATE).getBoolean(DARK_MODE_KEY, false)
        AppCompatDelegate.setDefaultNightMode(
                if (isDarkMode) AppCompatDelegate.MODE_NIGHT_YES
                else AppCompatDelegate.MODE_NIGHT_NO
        )

        super.onCreate(savedInstanceState)
    }

    override fun attachBaseContext(newBase: Context) {
        val languageCode = getLanguagePreference(newBase)
        val locale = if (languageCode == "ar") {
            // For Arabic, only force Western Arabic numerals while keeping Arabic text
            Locale.Builder()
                .setLanguage("ar")
                .setExtension('u', "nu-latn") // This only sets numeric type to Latin
                .build()
        } else {
            Locale(languageCode)
        }

        val config = Configuration(newBase.resources.configuration)

        // Support RTL layout for Arabic
        if (locale.language == "ar") {
            config.setLayoutDirection(Locale("ar"))
        } else {
            config.setLayoutDirection(Locale.getDefault())
        }

        Locale.setDefault(locale)
        config.setLocale(locale)
        val context = newBase.createConfigurationContext(config)
        super.attachBaseContext(context)
    }

    protected fun getLanguagePreference(context: Context): String {
        val sharedPreferences = context.getSharedPreferences(PREFS_NAME, MODE_PRIVATE)
        return sharedPreferences.getString(LANGUAGE_KEY, "en") ?: "en"
    }

    protected fun setLocalePreference(languageCode: String) {
        getSharedPreferences(PREFS_NAME, MODE_PRIVATE)
                .edit()
                .putString(LANGUAGE_KEY, languageCode)
                .apply()
        recreate()
    }

    protected fun setThemePreference(isDarkMode: Boolean) {
        AppCompatDelegate.setDefaultNightMode(
                if (isDarkMode) AppCompatDelegate.MODE_NIGHT_YES
                else AppCompatDelegate.MODE_NIGHT_NO
        )
        getSharedPreferences(PREFS_NAME, MODE_PRIVATE)
                .edit()
                .putBoolean(DARK_MODE_KEY, isDarkMode)
                .apply()
    }
}
