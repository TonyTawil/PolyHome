package com.antoinetawil.polyhome.Activities

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.widget.*
import androidx.appcompat.app.AppCompatActivity
import androidx.drawerlayout.widget.DrawerLayout
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.antoinetawil.polyhome.Adapters.HouseListAdapter
import com.antoinetawil.polyhome.Models.House
import com.antoinetawil.polyhome.R
import com.antoinetawil.polyhome.Utils.HeaderUtils
import okhttp3.*
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONArray
import org.json.JSONObject
import java.io.IOException

class HouseListActivity : AppCompatActivity() {

    companion object {
        private const val TAG = "HouseListActivity"
    }

    private lateinit var drawerLayout: DrawerLayout
    private lateinit var recyclerView: RecyclerView
    private lateinit var adapter: HouseListAdapter
    private lateinit var searchEditText: EditText
    private lateinit var searchPopup: PopupWindow
    private val houses = mutableListOf<House>()
    private val filteredHouses = mutableListOf<House>()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_house_list)

        drawerLayout = findViewById(R.id.drawer_layout)
        HeaderUtils.setupHeaderWithDrawer(this, drawerLayout)

        recyclerView = findViewById(R.id.recyclerView)
        adapter = HouseListAdapter(filteredHouses, this,
            onManagePermission = { houseId, view -> showPermissionPopup(houseId, view) },
            onHouseSelected = { houseId -> fetchPeripheralTypes(houseId) }
        )
        recyclerView.layoutManager = LinearLayoutManager(this)
        recyclerView.adapter = adapter

        val sharedPreferences = getSharedPreferences("PolyHomePrefs", Context.MODE_PRIVATE)
        val token = sharedPreferences.getString("auth_token", null)

        if (token != null) {
            fetchHouseList(token)
        } else {
            Toast.makeText(this, "Authentication token not found", Toast.LENGTH_SHORT).show()
            Log.e(TAG, "Authentication token missing")
        }

        setupSearchPopup()
    }

    private fun setupSearchPopup() {
        val popupView = LayoutInflater.from(this).inflate(R.layout.search_popup, null)
        searchEditText = popupView.findViewById(R.id.searchEditText)

        searchPopup = PopupWindow(
            popupView,
            LinearLayout.LayoutParams.MATCH_PARENT,
            LinearLayout.LayoutParams.WRAP_CONTENT,
            true
        )

        val searchButton: View? = findViewById(R.id.searchIcon)
        searchButton?.setOnClickListener {
            if (!searchPopup.isShowing) {
                searchPopup.showAsDropDown(it, 0, 0)
            }
        }

        searchEditText.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
                filterHouses(s.toString())
            }

            override fun afterTextChanged(s: Editable?) {}
        })
    }

    private fun filterHouses(query: String) {
        filteredHouses.clear()
        if (query.isEmpty()) {
            filteredHouses.addAll(houses)
        } else {
            filteredHouses.addAll(houses.filter { it.houseId.toString().contains(query, ignoreCase = true) })
        }
        adapter.notifyDataSetChanged()
    }

    private fun fetchHouseList(token: String) {
        val url = "https://polyhome.lesmoulinsdudev.com/api/houses"
        val client = OkHttpClient()

        val request = Request.Builder()
            .url(url)
            .addHeader("Authorization", "Bearer $token")
            .build()

        Log.d(TAG, "Fetching house list with token: $token")

        client.newCall(request).enqueue(object : Callback {
            override fun onFailure(call: Call, e: IOException) {
                Log.e(TAG, "Network request failed: ${e.message}", e)
                runOnUiThread {
                    Toast.makeText(this@HouseListActivity, "Failed to fetch houses: ${e.message}", Toast.LENGTH_LONG).show()
                }
            }

            override fun onResponse(call: Call, response: Response) {
                if (response.isSuccessful) {
                    val responseBody = response.body?.string()
                    Log.d(TAG, "House list response: $responseBody")

                    if (responseBody != null) {
                        parseAndDisplayHouses(responseBody)
                    }
                } else {
                    Log.e(TAG, "Failed to fetch houses: ${response.code}")
                    runOnUiThread {
                        Toast.makeText(this@HouseListActivity, "Failed to fetch houses", Toast.LENGTH_SHORT).show()
                    }
                }
            }
        })
    }

    private fun parseAndDisplayHouses(jsonResponse: String) {
        try {
            val jsonArray = JSONArray(jsonResponse)
            houses.clear()
            filteredHouses.clear()

            for (i in 0 until jsonArray.length()) {
                val houseObject = jsonArray.getJSONObject(i)
                val houseId = houseObject.getInt("houseId")
                val isOwner = houseObject.getBoolean("owner")
                houses.add(House(houseId, isOwner))
            }

            filteredHouses.addAll(houses)
            runOnUiThread {
                adapter.notifyDataSetChanged()
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error parsing house list: ${e.message}", e)
        }
    }

    private fun fetchPeripheralTypes(houseId: Int) {
        val url = "https://polyhome.lesmoulinsdudev.com/api/houses/$houseId/devices"
        val client = OkHttpClient()

        val sharedPreferences = getSharedPreferences("PolyHomePrefs", Context.MODE_PRIVATE)
        val token = sharedPreferences.getString("auth_token", null)

        if (token == null) {
            Toast.makeText(this, "Authentication token missing", Toast.LENGTH_SHORT).show()
            return
        }

        val request = Request.Builder()
            .url(url)
            .addHeader("Authorization", "Bearer $token")
            .build()

        client.newCall(request).enqueue(object : Callback {
            override fun onFailure(call: Call, e: IOException) {
                runOnUiThread {
                    Toast.makeText(this@HouseListActivity, "Failed to fetch peripheral types", Toast.LENGTH_SHORT).show()
                }
            }

            override fun onResponse(call: Call, response: Response) {
                if (response.isSuccessful) {
                    val responseBody = response.body?.string()
                    if (responseBody != null) {
                        val types = parsePeripheralTypes(responseBody)
                        runOnUiThread {
                            navigateToPeripheralTypeList(houseId, types)
                        }
                    }
                } else {
                    runOnUiThread {
                        Toast.makeText(this@HouseListActivity, "Failed to fetch peripheral types", Toast.LENGTH_SHORT).show()
                    }
                }
            }
        })
    }

    private fun parsePeripheralTypes(jsonResponse: String): List<String> {
        val jsonArray = JSONObject(jsonResponse).getJSONArray("devices")
        val types = mutableSetOf<String>()

        for (i in 0 until jsonArray.length()) {
            val device = jsonArray.getJSONObject(i)
            val type = device.getString("type")
            types.add(type)
        }
        return types.toList()
    }

    private fun navigateToPeripheralTypeList(houseId: Int, types: List<String>) {
        val intent = Intent(this, PeripheralTypeListActivity::class.java)
        intent.putExtra("houseId", houseId)
        intent.putStringArrayListExtra("availableTypes", ArrayList(types))
        startActivity(intent)
    }

    private fun showPermissionPopup(houseId: Int, anchorView: View) {
        val popupView = LayoutInflater.from(this).inflate(R.layout.permission_popup, null)

        val desiredWidth = resources.displayMetrics.widthPixels * 0.7
        val popupWindow = PopupWindow(popupView, desiredWidth.toInt(), LinearLayout.LayoutParams.WRAP_CONTENT, true)

        val emailEditText = popupView.findViewById<EditText>(R.id.emailEditText)
        val givePermissionButton = popupView.findViewById<Button>(R.id.givePermissionButton)
        val removePermissionButton = popupView.findViewById<Button>(R.id.removePermissionButton)

        givePermissionButton.setOnClickListener {
            val email = emailEditText.text.toString()
            if (email.isNotEmpty()) {
                managePermission(houseId, email, true)
                popupWindow.dismiss()
            } else {
                Toast.makeText(this, "Please enter an email address", Toast.LENGTH_SHORT).show()
            }
        }

        removePermissionButton.setOnClickListener {
            val email = emailEditText.text.toString()
            if (email.isNotEmpty()) {
                managePermission(houseId, email, false)
                popupWindow.dismiss()
            } else {
                Toast.makeText(this, "Please enter an email address", Toast.LENGTH_SHORT).show()
            }
        }

        popupWindow.elevation = 8f
        popupWindow.animationStyle = android.R.style.Animation_Dialog
        popupWindow.showAsDropDown(anchorView, 50, 80)
    }

    private fun managePermission(houseId: Int, email: String, isGrant: Boolean) {
        val url = "https://polyhome.lesmoulinsdudev.com/api/houses/$houseId/users"
        val method = if (isGrant) "POST" else "DELETE"
        val sharedPreferences = getSharedPreferences("PolyHomePrefs", MODE_PRIVATE)
        val token = sharedPreferences.getString("auth_token", null)

        if (token != null) {
            val client = OkHttpClient()
            val jsonObject = JSONObject().apply { put("userLogin", email) }
            val requestBody = jsonObject.toString().toRequestBody("application/json".toMediaTypeOrNull())

            val request = Request.Builder()
                .url(url)
                .method(method, requestBody)
                .addHeader("Authorization", "Bearer $token")
                .build()

            client.newCall(request).enqueue(object : Callback {
                override fun onFailure(call: Call, e: IOException) {
                    runOnUiThread {
                        Toast.makeText(this@HouseListActivity, "Failed to ${if (isGrant) "give" else "remove"} permission: ${e.message}", Toast.LENGTH_LONG).show()
                    }
                }

                override fun onResponse(call: Call, response: Response) {
                    if (response.isSuccessful) {
                        runOnUiThread {
                            Toast.makeText(this@HouseListActivity, "${if (isGrant) "Permission granted" else "Permission removed"} successfully!", Toast.LENGTH_SHORT).show()
                        }
                    } else {
                        runOnUiThread {
                            Toast.makeText(this@HouseListActivity, "Failed to ${if (isGrant) "give" else "remove"} permission: ${response.code}", Toast.LENGTH_LONG).show()
                        }
                    }
                }
            })
        } else {
            Toast.makeText(this, "Authentication token missing. Please log in again.", Toast.LENGTH_SHORT).show()
        }
    }
}
