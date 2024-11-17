package com.antoinetawil.polyhome.Activities

import android.content.Intent
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import androidx.biometric.BiometricManager
import androidx.biometric.BiometricPrompt
import androidx.core.content.ContextCompat
import com.antoinetawil.polyhome.Utils.LockManager

abstract class BaseActivity : AppCompatActivity() {

    companion object {
        private var isAppBackgrounded = false
        private var hasAuthenticatedSinceLaunch = false
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        if (!hasAuthenticatedSinceLaunch && LockManager.isLocked(this)) {
            authenticateUser()
        }
    }

    override fun onResume() {
        super.onResume()
        if (isAppBackgrounded && LockManager.isLocked(this) && !hasAuthenticatedSinceLaunch) {
            authenticateUser()
        }
        isAppBackgrounded = false
    }

    override fun onPause() {
        super.onPause()
        if (!isChangingConfigurations && !isFinishing) {
            isAppBackgrounded = true
            LockManager.setLocked(this, true)
        }
    }

    private fun authenticateUser() {
        val executor = ContextCompat.getMainExecutor(this)
        val biometricPrompt = BiometricPrompt(this, executor, object : BiometricPrompt.AuthenticationCallback() {
            override fun onAuthenticationSucceeded(result: BiometricPrompt.AuthenticationResult) {
                super.onAuthenticationSucceeded(result)
                LockManager.setLocked(this@BaseActivity, false)
                hasAuthenticatedSinceLaunch = true
            }

            override fun onAuthenticationFailed() {
                super.onAuthenticationFailed()
                finishAffinity() // Close the app on failed authentication
            }

            override fun onAuthenticationError(errorCode: Int, errString: CharSequence) {
                super.onAuthenticationError(errorCode, errString)
                finishAffinity()
            }
        })

        val promptInfo = BiometricPrompt.PromptInfo.Builder()
            .setTitle("Authentication Required")
            .setSubtitle("Authenticate to access your account")
            .setAllowedAuthenticators(BiometricManager.Authenticators.BIOMETRIC_STRONG or BiometricManager.Authenticators.DEVICE_CREDENTIAL)
            .build()

        biometricPrompt.authenticate(promptInfo)
    }
}
