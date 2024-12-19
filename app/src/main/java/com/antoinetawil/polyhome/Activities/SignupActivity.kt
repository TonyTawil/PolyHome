package com.antoinetawil.polyhome.Activities

import android.content.Intent
import android.os.Bundle
import android.text.SpannableString
import android.text.Spanned
import android.text.style.ClickableSpan
import android.text.style.ForegroundColorSpan
import android.util.Log
import android.view.View
import android.widget.ArrayAdapter
import android.widget.Spinner
import android.widget.Toast
import androidx.core.content.ContextCompat
import com.antoinetawil.polyhome.R
import com.antoinetawil.polyhome.Utils.Api
import com.google.android.material.textfield.TextInputEditText
import java.util.*

class SignupActivity : BaseActivity() {

    companion object {
        private const val TAG = "SignupActivity"
    }

    private val api = Api()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_signup)

        val emailEditText: TextInputEditText = findViewById(R.id.editTextEmailAddress)
        val passwordEditText: TextInputEditText = findViewById(R.id.editTextPassword)
        val signupButton: androidx.appcompat.widget.AppCompatButton =
                findViewById(R.id.signupButton)
        val loginTextView: androidx.appcompat.widget.AppCompatTextView =
                findViewById(R.id.linkToLoginTextView)

        setupClickableLoginText(loginTextView)
        setupLanguageSpinner()

        signupButton.setOnClickListener {
            val email = emailEditText.text.toString()
            val password = passwordEditText.text.toString()

            Log.d(TAG, "Signup button clicked with email: $email")

            if (email.isNotEmpty() && password.isNotEmpty()) {
                signup(email, password)
            } else {
                Toast.makeText(this, getString(R.string.please_fill_fields), Toast.LENGTH_SHORT)
                        .show()
                Log.w(TAG, "Empty fields: Email or password is missing.")
            }
        }
    }

    private fun setupClickableLoginText(
            loginTextView: androidx.appcompat.widget.AppCompatTextView
    ) {
        val fullText = getString(R.string.have_account_login)
        val loginText = getString(R.string.login)
        val spannableString = SpannableString(fullText)

        val clickableSpan =
                object : ClickableSpan() {
                    override fun onClick(widget: View) {
                        Log.d(TAG, "Navigating to LoginActivity")
                        val intent = Intent(this@SignupActivity, LoginActivity::class.java)
                        startActivity(intent)
                        finish()
                    }

                    override fun updateDrawState(ds: android.text.TextPaint) {
                        super.updateDrawState(ds)
                        ds.isUnderlineText = true // Keep underline
                    }
                }

        val colorSpan = ForegroundColorSpan(ContextCompat.getColor(this, R.color.PolytechBlue))

        val startIndex = fullText.lastIndexOf(loginText)
        if (startIndex >= 0) {
            val endIndex = startIndex + loginText.length

            spannableString.setSpan(
                    clickableSpan,
                    startIndex,
                    endIndex,
                    Spanned.SPAN_EXCLUSIVE_EXCLUSIVE
            )
            spannableString.setSpan(
                    colorSpan,
                    startIndex,
                    endIndex,
                    Spanned.SPAN_EXCLUSIVE_EXCLUSIVE
            )
        }

        loginTextView.text = spannableString
        loginTextView.movementMethod = android.text.method.LinkMovementMethod.getInstance()
    }

    private fun setupLanguageSpinner() {
        val spinner: Spinner = findViewById(R.id.languageSpinner)

        val languages =
                listOf(
                        getString(R.string.english),
                        getString(R.string.french),
                        getString(R.string.spanish),
                        getString(R.string.arabic),
                        getString(R.string.korean)
                )

        val adapter = ArrayAdapter(this, android.R.layout.simple_spinner_item, languages)
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        spinner.adapter = adapter

        // Set current language as selected
        val currentLanguage = getCurrentLanguage()
        val position =
                when (currentLanguage) {
                    "fr" -> 1
                    "es" -> 2
                    "ar" -> 3
                    "ko" -> 4
                    else -> 0
                }
        spinner.setSelection(position)

        spinner.onItemSelectedListener =
                object : android.widget.AdapterView.OnItemSelectedListener {
                    override fun onItemSelected(
                            parent: android.widget.AdapterView<*>?,
                            view: View?,
                            position: Int,
                            id: Long
                    ) {
                        val locale =
                                when (position) {
                                    1 -> "fr"
                                    2 -> "es"
                                    3 -> "ar"
                                    4 -> "ko"
                                    else -> "en"
                                }
                        if (locale != getCurrentLanguage()) {
                            setLocale(locale)
                            recreate()
                        }
                    }

                    override fun onNothingSelected(parent: android.widget.AdapterView<*>?) {}
                }
    }

    private fun getCurrentLanguage(): String {
        return getLanguagePreference(this)
    }

    private fun setLocale(languageCode: String) {
        setLocalePreference(languageCode)
    }

    private fun signup(email: String, password: String) {
        val url = "https://polyhome.lesmoulinsdudev.com/api/users/register"
        val requestData = mapOf("login" to email, "password" to password)

        Log.d(TAG, "Signup request initiated with data: $requestData")

        api.post<Map<String, String>, Map<String, String>>(
                path = url,
                data = requestData,
                onSuccess = { responseCode, response ->
                    runOnUiThread {
                        if (responseCode == 200) {
                            if (response != null) {
                                Log.d(TAG, "Signup successful: $response")
                                Toast.makeText(
                                                this,
                                                getString(R.string.signup_successful),
                                                Toast.LENGTH_SHORT
                                        )
                                        .show()
                                navigateToLogin()
                            } else {
                                Log.w(TAG, "Signup successful but no response body.")
                                Toast.makeText(
                                                this,
                                                getString(R.string.signup_successful_login),
                                                Toast.LENGTH_SHORT
                                        )
                                        .show()
                                navigateToLogin()
                            }
                        }
                    }
                }
        )
    }

    private fun navigateToLogin() {
        val intent = Intent(this, LoginActivity::class.java)
        startActivity(intent)
        finish()
    }
}
