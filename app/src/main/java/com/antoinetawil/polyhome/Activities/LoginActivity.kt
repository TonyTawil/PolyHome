package com.antoinetawil.polyhome.Activities

import android.content.Intent
import android.content.res.Configuration
import android.os.Bundle
import android.text.SpannableString
import android.text.Spanned
import android.text.style.ClickableSpan
import android.text.style.ForegroundColorSpan
import android.view.View
import android.widget.ArrayAdapter
import android.widget.Spinner
import android.widget.Toast
import androidx.core.content.ContextCompat
import com.antoinetawil.polyhome.R
import com.antoinetawil.polyhome.Utils.Api
import com.google.android.material.textfield.TextInputEditText
import java.util.Locale

class LoginActivity : BaseActivity() {

    companion object {
        private const val TAG = "LoginActivity"
    }

    private val api = Api()

    override fun onCreate(savedInstanceState: Bundle?) {
        setLocale(getCurrentLanguage())
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

        // Find the position of the register text in the full string
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

        val languages =
                listOf(
                        getString(R.string.english),
                        getString(R.string.french),
                        getString(R.string.spanish),
                        getString(R.string.arabic)
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
        return getSharedPreferences("Settings", MODE_PRIVATE).getString("language", "en") ?: "en"
    }

    private fun setLocale(languageCode: String) {
        val locale = Locale(languageCode)
        Locale.setDefault(locale)

        val config = Configuration()
        config.setLocale(locale)

        resources.updateConfiguration(config, resources.displayMetrics)

        // Save selected language
        getSharedPreferences("Settings", MODE_PRIVATE)
                .edit()
                .putString("language", languageCode)
                .apply()
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
