package com.antoinetawil.polyhome

import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.widget.*
import androidx.appcompat.app.AppCompatActivity
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

        val emailEditText: EditText = findViewById(R.id.editTextEmailAddress)
        val passwordEditText: EditText = findViewById(R.id.editTextPassword)
        val togglePasswordButton: ImageButton = findViewById(R.id.togglePasswordVisibility)
        val signupButton: Button = findViewById(R.id.signupButton)
        val loginTextView: TextView = findViewById(R.id.linkToLoginTextView)

        // Navigate to LoginActivity
        loginTextView.setOnClickListener {
            Log.d(TAG, "Navigating to LoginActivity")
            val intent = Intent(this, LoginActivity::class.java)
            startActivity(intent)
            finish()
        }

        // Password visibility toggle
        togglePasswordButton.setOnClickListener {
            if (passwordEditText.inputType == android.text.InputType.TYPE_TEXT_VARIATION_VISIBLE_PASSWORD) {
                passwordEditText.inputType = android.text.InputType.TYPE_CLASS_TEXT or android.text.InputType.TYPE_TEXT_VARIATION_PASSWORD
                togglePasswordButton.setImageResource(R.drawable.ic_eye_closed)
            } else {
                passwordEditText.inputType = android.text.InputType.TYPE_TEXT_VARIATION_VISIBLE_PASSWORD
                togglePasswordButton.setImageResource(R.drawable.ic_eye_open)
            }
            passwordEditText.setSelection(passwordEditText.text.length) // Keep cursor at the end
        }

        // Handle Signup
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

        // Create JSON body
        val jsonObject = JSONObject()
        jsonObject.put("login", email)
        jsonObject.put("password", password)
        val jsonBody = jsonObject.toString()
        val mediaType = "application/json".toMediaType()
        val requestBody = jsonBody.toRequestBody(mediaType)

        Log.d(TAG, "Signup request body created: $jsonBody")

        // Build the request
        val request = Request.Builder()
            .url(url)
            .post(requestBody)
            .addHeader("Content-Type", "application/json")
            .build()

        Log.d(TAG, "Signup request built with URL: $url")

        // Execute the request
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
