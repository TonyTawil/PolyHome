package com.antoinetawil.polyhome.Activities

import android.app.KeyguardManager
import android.content.Context
import android.content.Intent
import android.content.res.Configuration
import android.os.Bundle
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.app.AppCompatDelegate
import com.antoinetawil.polyhome.FingerPrint
import com.antoinetawil.polyhome.R
import java.util.Locale

open class BaseActivity : AppCompatActivity() {

    companion object {
        const val PREFS_NAME = "PolyHomePrefs"
        const val LANGUAGE_KEY = "LANGUAGE"
        const val DARK_MODE_KEY = "DARK_MODE"
        const val SECURITY_TYPE_KEY = "SECURITY_TYPE"
        private var isAuthenticatedWithFingerprint = false
    }

    private val authLauncher =
            registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
                if (result.resultCode == RESULT_OK) {
                    isAuthenticatedWithFingerprint = true
                } else {
                    finish()
                }
            }

    override fun onCreate(savedInstanceState: Bundle?) {
        val isDarkMode =
                getSharedPreferences(PREFS_NAME, MODE_PRIVATE).getBoolean(DARK_MODE_KEY, false)
        AppCompatDelegate.setDefaultNightMode(
                if (isDarkMode) AppCompatDelegate.MODE_NIGHT_YES
                else AppCompatDelegate.MODE_NIGHT_NO
        )

        super.onCreate(savedInstanceState)

        if (!isAuthenticatedWithFingerprint) {
            when (getSecurityType()) {
                "fingerprint" -> {
                    val fingerPrint = FingerPrint(this)
                    fingerPrint.onFingerprintReady = { _ -> isAuthenticatedWithFingerprint = true }
                    fingerPrint.onAuthError = { error -> finish() }
                }
                "none" -> {
                    isAuthenticatedWithFingerprint = true
                }
                "password" -> {
                    val keyguardManager =
                            getSystemService(Context.KEYGUARD_SERVICE) as KeyguardManager
                    if (keyguardManager.isDeviceSecure) {
                        val intent =
                                keyguardManager.createConfirmDeviceCredentialIntent(
                                        getString(R.string.auth_required),
                                        getString(R.string.auth_verify_identity)
                                )
                        if (intent != null) {
                            authLauncher.launch(intent)
                        } else {
                            isAuthenticatedWithFingerprint = true
                        }
                    } else {
                        isAuthenticatedWithFingerprint = true
                    }
                }
            }
        }
    }

    override fun attachBaseContext(newBase: Context) {
        val languageCode =
                newBase.getSharedPreferences(PREFS_NAME, MODE_PRIVATE).getString(LANGUAGE_KEY, "en")
                        ?: "en"

        val locale =
                if (languageCode == "ar") {
                    Locale.Builder().setLanguage("ar").setExtension('u', "nu-latn").build()
                } else {
                    Locale(languageCode)
                }

        Locale.setDefault(locale)
        val config = Configuration(newBase.resources.configuration)
        config.setLocale(locale)

        if (locale.language == "ar") {
            config.setLayoutDirection(Locale("ar"))
        } else {
            config.setLayoutDirection(Locale.getDefault())
        }

        val context = newBase.createConfigurationContext(config)
        super.attachBaseContext(context)
    }

    protected fun getLanguagePreference(context: Context): String {
        val sharedPreferences = context.getSharedPreferences(PREFS_NAME, MODE_PRIVATE)
        return sharedPreferences.getString(LANGUAGE_KEY, "en") ?: "en"
    }

    protected fun setLocalePreference(languageCode: String) {
        // Save the new language preference
        getSharedPreferences(PREFS_NAME, MODE_PRIVATE)
                .edit()
                .putString(LANGUAGE_KEY, languageCode)
                .apply()

        // Restart the app with the new language
        val intent =
                packageManager.getLaunchIntentForPackage(packageName)?.apply {
                    addFlags(Intent.FLAG_ACTIVITY_CLEAR_TASK or Intent.FLAG_ACTIVITY_NEW_TASK)
                }
        startActivity(intent)
        finish()
    }

    protected fun setThemePreference(isDarkMode: Boolean) {
        // Save the preference first
        getSharedPreferences(PREFS_NAME, MODE_PRIVATE)
                .edit()
                .putBoolean(DARK_MODE_KEY, isDarkMode)
                .apply()

        // Then update the theme
        AppCompatDelegate.setDefaultNightMode(
                if (isDarkMode) AppCompatDelegate.MODE_NIGHT_YES
                else AppCompatDelegate.MODE_NIGHT_NO
        )
    }

    protected fun getSecurityType(): String {
        return getSharedPreferences(PREFS_NAME, MODE_PRIVATE)
                .getString(SECURITY_TYPE_KEY, "fingerprint")
                ?: "fingerprint"
    }

    protected fun setSecurityType(type: String) {
        getSharedPreferences(PREFS_NAME, MODE_PRIVATE)
                .edit()
                .putString(SECURITY_TYPE_KEY, type)
                .apply()
    }
}
