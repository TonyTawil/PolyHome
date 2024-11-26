package com.antoinetawil.polyhome.Activities

import android.content.Intent
import android.os.Bundle
import android.text.SpannableString
import android.text.Spanned
import android.text.style.ClickableSpan
import android.text.style.ForegroundColorSpan
import android.util.Log
import android.view.View
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import com.antoinetawil.polyhome.R
import com.antoinetawil.polyhome.Utils.Api
import com.google.android.material.textfield.TextInputEditText

class SignupActivity : AppCompatActivity() {

    companion object {
        private const val TAG = "SignupActivity"
    }

    private val api = Api()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_signup)

        val emailEditText: TextInputEditText = findViewById(R.id.editTextEmailAddress)
        val passwordEditText: TextInputEditText = findViewById(R.id.editTextPassword)
        val signupButton: androidx.appcompat.widget.AppCompatButton = findViewById(R.id.signupButton)
        val loginTextView: androidx.appcompat.widget.AppCompatTextView = findViewById(R.id.linkToLoginTextView)

        val fullText = "Already have an account? Log in"
        val spannableString = SpannableString(fullText)

        val clickableSpan = object : ClickableSpan() {
            override fun onClick(widget: View) {
                Log.d(TAG, "Navigating to LoginActivity")
                val intent = Intent(this@SignupActivity, LoginActivity::class.java)
                startActivity(intent)
                finish()
            }
        }

        val colorSpan = ForegroundColorSpan(ContextCompat.getColor(this, R.color.PolytechBlue))
        val startIndex = fullText.indexOf("Log in")
        val endIndex = startIndex + "Log in".length

        spannableString.setSpan(clickableSpan, startIndex, endIndex, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE)
        spannableString.setSpan(colorSpan, startIndex, endIndex, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE)

        loginTextView.text = spannableString
        loginTextView.movementMethod = android.text.method.LinkMovementMethod.getInstance()

        signupButton.setOnClickListener {
            val email = emailEditText.text.toString()
            val password = passwordEditText.text.toString()

            Log.d(TAG, "Signup button clicked with email: $email")

            if (email.isNotEmpty() && password.isNotEmpty()) {
                signup(email, password)
            } else {
                Toast.makeText(this, "Please fill in all fields", Toast.LENGTH_SHORT).show()
                Log.w(TAG, "Empty fields: Email or password is missing.")
            }
        }
    }

    private fun signup(email: String, password: String) {
        val url = "https://polyhome.lesmoulinsdudev.com/api/users/register"
        val requestData = mapOf(
            "login" to email,
            "password" to password
        )

        Log.d(TAG, "Signup request initiated with data: $requestData")

        api.post<Map<String, String>, Map<String, String>>(
            path = url,
            data = requestData,
            onSuccess = { responseCode, response ->
                runOnUiThread {
                    if (responseCode == 200) {
                        if (response != null) {
                            Log.d(TAG, "Signup successful: $response")
                            Toast.makeText(this, "Signup successful! Redirecting to login.", Toast.LENGTH_SHORT).show()
                            navigateToLogin()
                        } else {
                            Log.w(TAG, "Signup successful but no response body.")
                            Toast.makeText(this, "Signup successful! Please log in.", Toast.LENGTH_SHORT).show()
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
