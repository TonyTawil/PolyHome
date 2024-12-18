package com.antoinetawil.polyhome.Activities

import android.content.Context
import android.content.res.Configuration
import androidx.appcompat.app.AppCompatActivity
import java.util.Locale

open class BaseActivity : AppCompatActivity() {

    override fun attachBaseContext(newBase: Context) {
        val locale = Locale(getLanguagePreference(newBase))
        val config = Configuration(newBase.resources.configuration)
        Locale.setDefault(locale)
        config.setLocale(locale)
        val context = newBase.createConfigurationContext(config)
        super.attachBaseContext(context)
    }

    private fun getLanguagePreference(context: Context): String {
        val sharedPreferences = context.getSharedPreferences("PolyHomePrefs", MODE_PRIVATE)
        return sharedPreferences.getString("LANGUAGE", "en") ?: "en"
    }

    protected fun setLocalePreference(languageCode: String) {
        getSharedPreferences("PolyHomePrefs", MODE_PRIVATE)
                .edit()
                .putString("LANGUAGE", languageCode)
                .apply()
        recreate()
    }
}
