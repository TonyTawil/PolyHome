package com.antoinetawil.polyhome.Activities

import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.widget.EditText
import android.widget.LinearLayout
import android.widget.PopupWindow
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.drawerlayout.widget.DrawerLayout
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.antoinetawil.polyhome.Adapters.PeripheralListAdapter
import com.antoinetawil.polyhome.Models.Peripheral
import com.antoinetawil.polyhome.R
import com.antoinetawil.polyhome.Utils.HeaderUtils
import okhttp3.*
import org.json.JSONObject
import java.io.IOException

class PeripheralListActivity : AppCompatActivity() {

    private val TAG = "PeripheralListActivity"
    private lateinit var drawerLayout: DrawerLayout
    private lateinit var recyclerView: RecyclerView
    private lateinit var titleTextView: TextView
    private lateinit var searchEditText: EditText
    private lateinit var searchPopup: PopupWindow
    private lateinit var adapter: PeripheralListAdapter
    private val peripherals = mutableListOf<Peripheral>()
    private val filteredPeripherals = mutableListOf<Peripheral>()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_peripheral_list)

        drawerLayout = findViewById(R.id.drawer_layout)
        HeaderUtils.setupHeaderWithDrawer(this, drawerLayout)

        titleTextView = findViewById(R.id.titleTextView)
        recyclerView = findViewById(R.id.recyclerView)
        recyclerView.layoutManager = LinearLayoutManager(this)

        val houseId = intent.getIntExtra("houseId", -1)
        val peripheralType = intent.getStringExtra("type")
        val floor = intent.getStringExtra("floor") ?: "All"
        val filteredPeripheralsJson = intent.getStringArrayListExtra("filteredPeripherals")

        if (houseId == -1 || peripheralType.isNullOrEmpty() || filteredPeripheralsJson.isNullOrEmpty()) {
            Toast.makeText(this, "Invalid data", Toast.LENGTH_SHORT).show()
            finish()
            return
        }

        peripherals.clear()
        peripherals.addAll(filteredPeripheralsJson.map { parsePeripheral(JSONObject(it)) })
        filteredPeripherals.addAll(peripherals)

        titleTextView.text = generateTitle(peripheralType, floor)

        adapter = PeripheralListAdapter(filteredPeripherals, this)
        recyclerView.adapter = adapter

        setupSearchPopup()
        logPeripheralFetchDetails(houseId, peripheralType)
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
                filterPeripherals(s.toString())
            }
            override fun afterTextChanged(s: Editable?) {}
        })
    }

    private fun filterPeripherals(query: String) {
        filteredPeripherals.clear()
        if (query.isEmpty()) {
            filteredPeripherals.addAll(peripherals)
        } else {
            filteredPeripherals.addAll(peripherals.filter { it.id.contains(query, ignoreCase = true) })
        }
        adapter.notifyDataSetChanged()
    }

    private fun generateTitle(type: String, floor: String): String {
        val pluralType = when (type.lowercase()) {
            "light" -> "Lights"
            "rolling shutter" -> "Rolling Shutters"
            "garage door" -> "Garage Door"
            else -> type + "s"
        }
        return when (type.lowercase()) {
            "garage door" -> pluralType
            else -> when (floor) {
                "All" -> "All $pluralType"
                else -> "$floor $pluralType"
            }
        }
    }

    private fun parsePeripheral(json: JSONObject): Peripheral {
        val id = json.getString("id")
        val type = json.getString("type")
        val availableCommands = json.getJSONArray("availableCommands").let { array ->
            MutableList(array.length()) { array.getString(it) }
        }
        val opening = json.optInt("opening", -1).takeIf { it != -1 }
        val openingMode = json.optInt("openingMode", -1).takeIf { it != -1 }
        val power = json.optInt("power", -1).takeIf { it != -1 }

        return Peripheral(id, type, availableCommands, opening, openingMode, power)
    }

    private fun logPeripheralFetchDetails(houseId: Int, peripheralType: String) {
        val url = "https://polyhome.lesmoulinsdudev.com/api/houses/$houseId/devices"
        val sharedPreferences = getSharedPreferences("PolyHomePrefs", MODE_PRIVATE)
        val token = sharedPreferences.getString("auth_token", null)

        if (token == null) {
            Log.e(TAG, "Auth token missing")
            Toast.makeText(this, "Authentication token missing", Toast.LENGTH_SHORT).show()
            return
        }

        val request = Request.Builder()
            .url(url)
            .addHeader("Authorization", "Bearer $token")
            .build()

        Log.d(TAG, "Sending request to fetch peripherals")
        Log.d(TAG, "Request URL: $url")
        Log.d(TAG, "Request Headers: Authorization: Bearer $token")

        OkHttpClient().newCall(request).enqueue(object : Callback {
            override fun onFailure(call: Call, e: IOException) {
                Log.e(TAG, "Failed to fetch peripherals: ${e.message}", e)
            }

            override fun onResponse(call: Call, response: Response) {
                if (response.isSuccessful) {
                    val responseBody = response.body?.string()
                    Log.d(TAG, "Peripheral fetch successful")
                    Log.d(TAG, "Response Body: $responseBody")
                } else {
                    Log.e(TAG, "Failed to fetch peripherals, Status Code: ${response.code}")
                }
            }
        })
    }
}
