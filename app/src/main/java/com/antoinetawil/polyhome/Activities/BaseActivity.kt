package com.antoinetawil.polyhome.Activities

import android.Manifest
import android.app.KeyguardManager
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.content.res.Configuration
import android.os.Build
import android.os.Bundle
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.app.AppCompatDelegate
import androidx.core.content.ContextCompat
import com.antoinetawil.polyhome.Utils.FingerPrint
import com.antoinetawil.polyhome.R
import java.util.Locale

open class BaseActivity : AppCompatActivity() {

    companion object {
        const val PREFS_NAME = "PolyHomePrefs"
        const val LANGUAGE_KEY = "LANGUAGE"
        const val DARK_MODE_KEY = "DARK_MODE"
        const val SECURITY_TYPE_KEY = "SECURITY_TYPE"
        const val FIRST_LAUNCH_KEY = "FIRST_LAUNCH"
        private var isAuthenticatedWithFingerprint = false
    }

    private val notificationPermissionLauncher =
            registerForActivityResult(ActivityResultContracts.RequestPermission()) { isGranted ->
                getSharedPreferences(PREFS_NAME, MODE_PRIVATE)
                        .edit()
                        .putBoolean(FIRST_LAUNCH_KEY, false)
                        .apply()
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
        super.onCreate(savedInstanceState)
        loadLocale()
        loadTheme()

        val isFirstLaunch =
                getSharedPreferences(PREFS_NAME, MODE_PRIVATE).getBoolean(FIRST_LAUNCH_KEY, true)

        if (isFirstLaunch && Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            when {
                ContextCompat.checkSelfPermission(this, Manifest.permission.POST_NOTIFICATIONS) ==
                        PackageManager.PERMISSION_GRANTED -> {
                    getSharedPreferences(PREFS_NAME, MODE_PRIVATE)
                            .edit()
                            .putBoolean(FIRST_LAUNCH_KEY, false)
                            .apply()
                }
                else -> {
                    notificationPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
                }
            }
        }

        if (!isAuthenticatedWithFingerprint && isUserLoggedIn()) {
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
        } else {
            isAuthenticatedWithFingerprint = true
        }
    }

    private fun loadLocale() {
        val languageCode = getLanguagePreference(this)
        val locale = Locale(languageCode)
        Locale.setDefault(locale)
        val config = Configuration()
        config.setLocale(locale)
        baseContext.createConfigurationContext(config)
        resources.updateConfiguration(config, resources.displayMetrics)
    }

    private fun loadTheme() {
        val isDarkMode =
                getSharedPreferences(PREFS_NAME, MODE_PRIVATE).getBoolean(DARK_MODE_KEY, false)
        AppCompatDelegate.setDefaultNightMode(
                if (isDarkMode) AppCompatDelegate.MODE_NIGHT_YES
                else AppCompatDelegate.MODE_NIGHT_NO
        )
    }

    private fun isUserLoggedIn(): Boolean {
        val sharedPreferences = getSharedPreferences(PREFS_NAME, MODE_PRIVATE)
        return sharedPreferences.getString("auth_token", null) != null
    }

    protected fun getLanguagePreference(context: Context): String {
        return context.getSharedPreferences(PREFS_NAME, MODE_PRIVATE).getString(LANGUAGE_KEY, "en")
                ?: "en"
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

    override fun attachBaseContext(newBase: Context) {
        val languageCode = getLanguagePreference(newBase)
        val locale = Locale(languageCode)
        val config = Configuration(newBase.resources.configuration)
        Locale.setDefault(locale)
        config.setLocale(locale)
        val context = newBase.createConfigurationContext(config)
        super.attachBaseContext(context)
    }

    protected fun setLocalePreference(languageCode: String) {
        getSharedPreferences(PREFS_NAME, MODE_PRIVATE)
                .edit()
                .putString(LANGUAGE_KEY, languageCode)
                .apply()

        val intent =
                packageManager.getLaunchIntentForPackage(packageName)?.apply {
                    addFlags(Intent.FLAG_ACTIVITY_CLEAR_TASK or Intent.FLAG_ACTIVITY_NEW_TASK)
                }
        startActivity(intent)
        finish()
    }

    protected fun setThemePreference(isDarkMode: Boolean) {
        getSharedPreferences(PREFS_NAME, MODE_PRIVATE)
                .edit()
                .putBoolean(DARK_MODE_KEY, isDarkMode)
                .apply()

        AppCompatDelegate.setDefaultNightMode(
                if (isDarkMode) AppCompatDelegate.MODE_NIGHT_YES
                else AppCompatDelegate.MODE_NIGHT_NO
        )
        recreate()
    }
}
