package com.antoinetawil.polyhome.Activities

import android.content.Context
import android.content.res.Configuration
import androidx.appcompat.app.AppCompatActivity
import java.util.Locale

open class BaseActivity : AppCompatActivity() {

    companion object {
        const val PREFS_NAME = "PolyHomePrefs"
        const val LANGUAGE_KEY = "LANGUAGE"
    }

    override fun attachBaseContext(newBase: Context) {
        val locale = Locale(getLanguagePreference(newBase))
        val config = Configuration(newBase.resources.configuration)
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
}
