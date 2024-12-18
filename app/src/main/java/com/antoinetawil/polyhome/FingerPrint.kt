package com.antoinetawil.polyhome

import androidx.biometric.BiometricManager
import androidx.biometric.BiometricPrompt
import androidx.core.content.ContextCompat
import androidx.fragment.app.FragmentActivity
import java.util.UUID

class FingerPrint(private val activity: FragmentActivity) {
    private lateinit var biometricPrompt: BiometricPrompt
    private lateinit var promptInfo: BiometricPrompt.PromptInfo

    var onFingerprintReady: ((String) -> Unit)? = null
    var onAuthError: ((String) -> Unit)? = null

    init {
        if (canAuthenticate()) {
            setupBiometrics()
        } else {
            onAuthError?.invoke("Biometric authentication is not available on this device")
        }
    }

    private fun canAuthenticate(): Boolean {
        val biometricManager = BiometricManager.from(activity)
        return when (biometricManager.canAuthenticate(
                        BiometricManager.Authenticators.BIOMETRIC_STRONG
                )
        ) {
            BiometricManager.BIOMETRIC_SUCCESS -> true
            BiometricManager.BIOMETRIC_ERROR_NO_HARDWARE -> {
                onAuthError?.invoke("No biometric hardware")
                false
            }
            BiometricManager.BIOMETRIC_ERROR_HW_UNAVAILABLE -> {
                onAuthError?.invoke("Biometric hardware unavailable")
                false
            }
            BiometricManager.BIOMETRIC_ERROR_NONE_ENROLLED -> {
                onAuthError?.invoke("No biometric credentials enrolled")
                false
            }
            else -> {
                onAuthError?.invoke("Biometric authentication unavailable")
                false
            }
        }
    }

    private fun setupBiometrics() {
        val executor = ContextCompat.getMainExecutor(activity)

        val callback =
                object : BiometricPrompt.AuthenticationCallback() {
                    override fun onAuthenticationSucceeded(
                            result: BiometricPrompt.AuthenticationResult
                    ) {
                        super.onAuthenticationSucceeded(result)
                        val visitorId = UUID.randomUUID().toString()
                        onFingerprintReady?.invoke(visitorId)
                    }

                    override fun onAuthenticationError(errorCode: Int, errString: CharSequence) {
                        super.onAuthenticationError(errorCode, errString)
                        onAuthError?.invoke(errString.toString())
                    }
                }

        biometricPrompt = BiometricPrompt(activity, executor, callback)

        promptInfo =
                BiometricPrompt.PromptInfo.Builder()
                        .setTitle("Biometric Authentication")
                        .setSubtitle("Verify your identity")
                        .setNegativeButtonText("Cancel")
                        .build()

        authenticate()
    }

    fun authenticate() {
        biometricPrompt.authenticate(promptInfo)
    }
}
