package com.antoinetawil.polyhome

import android.content.Intent
import android.os.Bundle
import android.text.InputType
import android.util.Log
import android.widget.*
import androidx.appcompat.app.AppCompatActivity
import okhttp3.*
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONObject
import java.io.IOException

class LoginActivity : AppCompatActivity() {

    companion object {
        private const val TAG = "LoginActivity"
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_login)

        val emailEditText: EditText = findViewById(R.id.editTextEmailAddress)
        val passwordEditText: EditText = findViewById(R.id.editTextPassword)
        val togglePasswordButton: ImageButton = findViewById(R.id.togglePasswordVisibility)
        val loginButton: Button = findViewById(R.id.loginButton)
        val registerTextView: TextView = findViewById(R.id.linkToRegisterTextView)

        loginButton.setOnClickListener {
            val email = emailEditText.text.toString()
            val password = passwordEditText.text.toString()

            Log.d(TAG, "Login button clicked with email: $email")

            if (email.isNotEmpty() && password.isNotEmpty()) {
                login(email, password)
            } else {
                Toast.makeText(this, "Please fill in all fields", Toast.LENGTH_SHORT).show()
                Log.w(TAG, "Empty fields: Email or password is missing.")
            }
        }

        // Password visibility toggle
        togglePasswordButton.setOnClickListener {
            if (passwordEditText.inputType == InputType.TYPE_TEXT_VARIATION_VISIBLE_PASSWORD) {
                passwordEditText.inputType = InputType.TYPE_CLASS_TEXT or InputType.TYPE_TEXT_VARIATION_PASSWORD
                togglePasswordButton.setImageResource(R.drawable.ic_eye_closed)
            } else {
                passwordEditText.inputType = InputType.TYPE_TEXT_VARIATION_VISIBLE_PASSWORD
                togglePasswordButton.setImageResource(R.drawable.ic_eye_open)
            }
            passwordEditText.setSelection(passwordEditText.text.length) // Keep cursor at the end
        }

        // Link to SignupActivity
        registerTextView.setOnClickListener {
            Log.d(TAG, "Navigating to SignupActivity")
            val intent = Intent(this, SignupActivity::class.java)
            startActivity(intent)
        }
    }

    private fun login(email: String, password: String) {
        val url = "https://polyhome.lesmoulinsdudev.com/api/users/auth"
        val client = OkHttpClient()

        // Create JSON body
        val jsonObject = JSONObject()
        jsonObject.put("login", email)
        jsonObject.put("password", password)
        val jsonBody = jsonObject.toString()
        val mediaType = "application/json".toMediaType()
        val requestBody = jsonBody.toRequestBody(mediaType)

        Log.d(TAG, "Login request body created: $jsonBody")

        // Build the request
        val request = Request.Builder()
            .url(url)
            .post(requestBody)
            .addHeader("Content-Type", "application/json")
            .build()

        Log.d(TAG, "Login request built with URL: $url")

        // Execute the request
        client.newCall(request).enqueue(object : Callback {
            override fun onFailure(call: Call, e: IOException) {
                Log.e(TAG, "Network request failed: ${e.message}", e)
                runOnUiThread {
                    Toast.makeText(this@LoginActivity, "Login failed: ${e.message}", Toast.LENGTH_LONG).show()
                }
            }

            override fun onResponse(call: Call, response: Response) {
                val responseBody = response.body
                Log.d(TAG, "Login response received: ${response.code}")

                if (response.isSuccessful && responseBody != null) {
                    val responseBodyString = responseBody.string()
                    Log.d(TAG, "Response body: $responseBodyString")

                    val token = JSONObject(responseBodyString).optString("token")
                    Log.d(TAG, "Authentication token received: $token")

                    runOnUiThread {
                        Toast.makeText(this@LoginActivity, "Login successful!", Toast.LENGTH_SHORT).show()
                        saveAuthToken(token)

                        // Navigate to HouseListActivity
                        val intent = Intent(this@LoginActivity, HouseListActivity::class.java)
                        startActivity(intent)
                        finish() // Finish LoginActivity to prevent returning to it
                    }
                } else {
                    val errorMessage = responseBody?.string() ?: "No response body"
                    Log.w(TAG, "Login failed with code: ${response.code}, message: $errorMessage")
                    runOnUiThread {
                        Toast.makeText(this@LoginActivity, "Invalid credentials", Toast.LENGTH_SHORT).show()
                    }
                }
            }

        })
    }

    private fun saveAuthToken(token: String) {
        Log.d(TAG, "Saving authentication token: $token")

        val sharedPreferences = getSharedPreferences("PolyHomePrefs", MODE_PRIVATE)
        val editor = sharedPreferences.edit()
        editor.putString("auth_token", token)
        editor.apply()

        Log.d(TAG, "Token saved successfully")
    }
}
