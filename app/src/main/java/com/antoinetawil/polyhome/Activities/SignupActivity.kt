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
import com.google.android.material.textfield.TextInputEditText
import okhttp3.*
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONObject
import java.io.IOException

class SignupActivity : AppCompatActivity() {

    companion object {
        private const val TAG = "SignupActivity"
    }

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
        val client = OkHttpClient()

        val jsonObject = JSONObject()
        jsonObject.put("login", email)
        jsonObject.put("password", password)
        val jsonBody = jsonObject.toString()
        val mediaType = "application/json".toMediaType()
        val requestBody = jsonBody.toRequestBody(mediaType)

        Log.d(TAG, "Signup request body created: $jsonBody")

        val request = Request.Builder()
            .url(url)
            .post(requestBody)
            .addHeader("Content-Type", "application/json")
            .build()

        Log.d(TAG, "Signup request built with URL: $url")

        client.newCall(request).enqueue(object : Callback {
            override fun onFailure(call: Call, e: IOException) {
                Log.e(TAG, "Network request failed: ${e.message}", e)
                runOnUiThread {
                    Toast.makeText(this@SignupActivity, "Signup failed: ${e.message}", Toast.LENGTH_LONG).show()
                }
            }

            override fun onResponse(call: Call, response: Response) {
                val responseBody = response.body
                Log.d(TAG, "Signup response received: ${response.code}")

                if (response.isSuccessful) {
                    Log.d(TAG, "Signup successful!")
                    runOnUiThread {
                        Toast.makeText(this@SignupActivity, "Signup successful! Redirecting to login.", Toast.LENGTH_SHORT).show()
                        navigateToLogin()
                    }
                } else {
                    val errorMessage = responseBody?.string() ?: "No response body"
                    Log.w(TAG, "Signup failed with code: ${response.code}, message: $errorMessage")
                    runOnUiThread {
                        Toast.makeText(this@SignupActivity, "Signup failed: $errorMessage", Toast.LENGTH_SHORT).show()
                    }
                }
            }
        })
    }

    private fun navigateToLogin() {
        val intent = Intent(this, LoginActivity::class.java)
        startActivity(intent)
        finish()
    }
}
