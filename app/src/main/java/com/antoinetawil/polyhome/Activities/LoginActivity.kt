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

class LoginActivity : AppCompatActivity() {

    companion object {
        private const val TAG = "LoginActivity"
    }

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
        val registerTextView: androidx.appcompat.widget.AppCompatTextView = findViewById(R.id.linkToRegisterTextView)

        val fullText = "No account yet? Register"
        val spannableString = SpannableString(fullText)

        val clickableSpan = object : ClickableSpan() {
            override fun onClick(widget: View) {
                val intent = Intent(this@LoginActivity, SignupActivity::class.java)
                startActivity(intent)
            }
        }

        val colorSpan = ForegroundColorSpan(ContextCompat.getColor(this, R.color.PolytechBlue))
        val startIndex = fullText.indexOf("Register")
        val endIndex = startIndex + "Register".length

        spannableString.setSpan(clickableSpan, startIndex, endIndex, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE)
        spannableString.setSpan(colorSpan, startIndex, endIndex, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE)

        registerTextView.text = spannableString
        registerTextView.movementMethod = android.text.method.LinkMovementMethod.getInstance()

        loginButton.setOnClickListener {
            val email = emailEditText.text.toString()
            val password = passwordEditText.text.toString()

            if (email.isNotEmpty() && password.isNotEmpty()) {
                login(email, password)
            } else {
                Toast.makeText(this, "Please fill in all fields", Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun login(email: String, password: String) {
        val url = "https://polyhome.lesmoulinsdudev.com/api/users/auth"
        val client = OkHttpClient()

        val jsonObject = JSONObject()
        jsonObject.put("login", email)
        jsonObject.put("password", password)
        val requestBody = jsonObject.toString().toRequestBody("application/json".toMediaType())

        val request = Request.Builder()
            .url(url)
            .post(requestBody)
            .addHeader("Content-Type", "application/json")
            .build()

        client.newCall(request).enqueue(object : Callback {
            override fun onFailure(call: Call, e: IOException) {
                runOnUiThread {
                    Toast.makeText(this@LoginActivity, "Login failed: ${e.message}", Toast.LENGTH_LONG).show()
                }
            }

            override fun onResponse(call: Call, response: Response) {
                if (response.isSuccessful) {
                    val responseBody = response.body?.string()
                    val token = JSONObject(responseBody ?: "").optString("token")

                    runOnUiThread {
                        saveAuthToken(token)
                        navigateToHouseList()
                    }
                } else {
                    runOnUiThread {
                        Toast.makeText(this@LoginActivity, "Invalid credentials", Toast.LENGTH_SHORT).show()
                    }
                }
            }
        })
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
