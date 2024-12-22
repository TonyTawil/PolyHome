package com.antoinetawil.polyhome.Activities

import android.content.Intent
import android.os.Bundle
import android.text.SpannableString
import android.text.Spanned
import android.text.style.ClickableSpan
import android.text.style.ForegroundColorSpan
import android.view.View
import android.widget.AdapterView
import android.widget.ArrayAdapter
import android.widget.Spinner
import android.widget.Toast
import androidx.core.content.ContextCompat
import com.antoinetawil.polyhome.R
import com.antoinetawil.polyhome.Utils.Api
import com.google.android.material.textfield.TextInputEditText

class LoginActivity : BaseActivity() {

    companion object {
        private const val TAG = "LoginActivity"
    }

    private val api = Api()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        if (isUserLoggedIn()) {
            navigateToHouseList()
            return
        }
        setContentView(R.layout.activity_login)

        val emailEditText: TextInputEditText = findViewById(R.id.editTextEmailAddress)
        val passwordEditText: TextInputEditText = findViewById(R.id.editTextPassword)
        val loginButton: androidx.appcompat.widget.AppCompatButton = findViewById(R.id.loginButton)
        val registerTextView: androidx.appcompat.widget.AppCompatTextView =
                findViewById(R.id.linkToRegisterTextView)

        setupClickableRegisterText(registerTextView)
        setupLanguageSpinner()

        loginButton.setOnClickListener {
            val email = emailEditText.text.toString()
            val password = passwordEditText.text.toString()

            if (email.isNotEmpty() && password.isNotEmpty()) {
                login(email, password)
            } else {
                Toast.makeText(this, getString(R.string.please_fill_fields), Toast.LENGTH_SHORT)
                        .show()
            }
        }
    }

    private fun setupClickableRegisterText(
            registerTextView: androidx.appcompat.widget.AppCompatTextView
    ) {
        val fullText = getString(R.string.no_account_register)
        val registerText = getString(R.string.register)
        val spannableString = SpannableString(fullText)

        val clickableSpan =
                object : ClickableSpan() {
                    override fun onClick(widget: View) {
                        val intent = Intent(this@LoginActivity, SignupActivity::class.java)
                        startActivity(intent)
                    }
                }

        val colorSpan = ForegroundColorSpan(ContextCompat.getColor(this, R.color.PolytechBlue))

        val startIndex = fullText.indexOf(registerText)
        if (startIndex >= 0) {
            val endIndex = startIndex + registerText.length

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

        registerTextView.text = spannableString
        registerTextView.movementMethod = android.text.method.LinkMovementMethod.getInstance()
    }

    private fun setupLanguageSpinner() {
        val spinner: Spinner = findViewById(R.id.languageSpinner)

        // Define language pairs (code to display name)
        val languagePairs =
                listOf(
                        "en" to getString(R.string.english),
                        "fr" to getString(R.string.french),
                        "es" to getString(R.string.spanish),
                        "ar" to getString(R.string.arabic),
                        "ko" to getString(R.string.korean)
                )

        // Create adapter with display names only
        val adapter =
                ArrayAdapter(
                        this,
                        android.R.layout.simple_spinner_item,
                        languagePairs.map { it.second }
                )
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        spinner.adapter = adapter

        // Set current selection based on language code
        val currentLanguage = getCurrentLanguage()
        val position =
                languagePairs.indexOfFirst { it.first == currentLanguage }.takeIf { it != -1 } ?: 0
        spinner.setSelection(position)

        spinner.onItemSelectedListener =
                object : AdapterView.OnItemSelectedListener {
                    override fun onItemSelected(
                            parent: AdapterView<*>?,
                            view: View?,
                            position: Int,
                            id: Long
                    ) {
                        val newLocale = languagePairs[position].first
                        if (newLocale != getCurrentLanguage()) {
                            setLocalePreference(newLocale)
                        }
                    }

                    override fun onNothingSelected(parent: AdapterView<*>?) {}
                }
    }

    private fun getCurrentLanguage(): String {
        return getLanguagePreference(this)
    }

    private fun setLocale(languageCode: String) {
        setLocalePreference(languageCode)
    }

    private fun login(email: String, password: String) {
        val url = "https://polyhome.lesmoulinsdudev.com/api/users/auth"
        val requestData = mapOf("login" to email, "password" to password)

        api.post<Map<String, String>, Map<String, String>>(
                path = url,
                data = requestData,
                onSuccess = { responseCode, response ->
                    runOnUiThread {
                        if (responseCode == 200 && response != null) {
                            val token = response["token"]
                            if (!token.isNullOrEmpty()) {
                                saveAuthToken(token)
                                navigateToHouseList()
                            } else {
                                Toast.makeText(
                                                this,
                                                getString(R.string.invalid_server_response),
                                                Toast.LENGTH_SHORT
                                        )
                                        .show()
                            }
                        } else {
                            Toast.makeText(
                                            this,
                                            getString(R.string.invalid_credentials),
                                            Toast.LENGTH_SHORT
                                    )
                                    .show()
                        }
                    }
                }
        )
    }

    private fun saveAuthToken(token: String) {
        val sharedPreferences = getSharedPreferences("PolyHomePrefs", MODE_PRIVATE)
        val editor = sharedPreferences.edit()
        editor.putString("auth_token", token)
        editor.apply()
    }

    private fun isUserLoggedIn(): Boolean {
        val sharedPreferences = getSharedPreferences("PolyHomePrefs", MODE_PRIVATE)
        return sharedPreferences.getString("auth_token", null) != null
    }

    private fun navigateToHouseList() {
        val intent = Intent(this, HouseListActivity::class.java)
        startActivity(intent)
        finish()
    }
}
