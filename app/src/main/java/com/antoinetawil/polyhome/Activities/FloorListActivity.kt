package com.antoinetawil.polyhome.Activities

import android.content.Intent
import android.os.Bundle
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.antoinetawil.polyhome.Adapters.FloorListAdapter
import com.antoinetawil.polyhome.R
import com.antoinetawil.polyhome.Utils.HeaderUtils
import okhttp3.*
import org.json.JSONObject
import java.io.IOException

class FloorListActivity : AppCompatActivity() {

    private lateinit var titleTextView: TextView
    private lateinit var recyclerView: RecyclerView
    private val floors = listOf("All", "First Floor", "Second Floor")
    private var peripherals = mutableListOf<JSONObject>()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_floor_list)

        HeaderUtils.setupHeader(this)

        titleTextView = findViewById(R.id.titleTextView)
        recyclerView = findViewById(R.id.recyclerView)
        recyclerView.layoutManager = LinearLayoutManager(this)

        val houseId = intent.getIntExtra("houseId", -1)
        val peripheralType = intent.getStringExtra("type")

        if (houseId == -1 || peripheralType.isNullOrEmpty()) {
            Toast.makeText(this, "Invalid data", Toast.LENGTH_SHORT).show()
            finish()
            return
        }

        titleTextView.text = generateTitle(peripheralType)

        fetchPeripherals(houseId, peripheralType)
    }

    private fun generateTitle(type: String): String {
        return when (type.lowercase()) {
            "light" -> "Lights"
            "rolling shutter" -> "Rolling Shutters"
            else -> type.capitalize()
        }
    }

    private fun fetchPeripherals(houseId: Int, peripheralType: String) {
        val url = "https://polyhome.lesmoulinsdudev.com/api/houses/$houseId/devices"
        val client = OkHttpClient()

        val sharedPreferences = getSharedPreferences("PolyHomePrefs", MODE_PRIVATE)
        val token = sharedPreferences.getString("auth_token", null)

        if (token.isNullOrEmpty()) {
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
                    Toast.makeText(this@FloorListActivity, "Failed to fetch peripherals", Toast.LENGTH_SHORT).show()
                }
            }

            override fun onResponse(call: Call, response: Response) {
                if (response.isSuccessful) {
                    val responseBody = response.body?.string()
                    if (!responseBody.isNullOrEmpty()) {
                        peripherals = parsePeripherals(responseBody, peripheralType)

                        runOnUiThread {
                            if (peripheralType.equals("garage door", ignoreCase = true)) {
                                navigateDirectlyToPeripheralList(houseId, peripheralType, peripherals)
                            } else {
                                setupFloorAdapter(houseId, peripheralType)
                            }
                        }
                    }
                } else {
                    runOnUiThread {
                        Toast.makeText(this@FloorListActivity, "Failed to fetch peripherals", Toast.LENGTH_SHORT).show()
                    }
                }
            }
        })
    }

    private fun parsePeripherals(jsonResponse: String, type: String): MutableList<JSONObject> {
        val jsonArray = JSONObject(jsonResponse).getJSONArray("devices")
        val filteredPeripherals = mutableListOf<JSONObject>()

        for (i in 0 until jsonArray.length()) {
            val device = jsonArray.getJSONObject(i)
            if (device.getString("type").equals(type, ignoreCase = true)) {
                filteredPeripherals.add(device)
            }
        }

        return filteredPeripherals
    }

    private fun setupFloorAdapter(houseId: Int, peripheralType: String) {
        val adapter = FloorListAdapter(floors) { selectedFloor ->
            val filteredPeripherals = when (selectedFloor) {
                "First Floor" -> peripherals.filter { it.getString("id").contains("1.") }
                "Second Floor" -> peripherals.filter { it.getString("id").contains("2.") }
                else -> peripherals
            }
            navigateToPeripheralList(houseId, peripheralType, selectedFloor, filteredPeripherals)
        }
        recyclerView.adapter = adapter
    }

    private fun navigateDirectlyToPeripheralList(houseId: Int, type: String, peripherals: List<JSONObject>) {
        val intent = Intent(this, PeripheralListActivity::class.java)
        intent.putExtra("houseId", houseId)
        intent.putExtra("type", type)
        intent.putExtra("floor", "All")
        intent.putExtra("filteredPeripherals", ArrayList(peripherals.map { it.toString() }))
        startActivity(intent)
        finish()
    }

    private fun navigateToPeripheralList(houseId: Int, type: String, floor: String, filteredPeripherals: List<JSONObject>) {
        val intent = Intent(this, PeripheralListActivity::class.java)
        intent.putExtra("houseId", houseId)
        intent.putExtra("type", type)
        intent.putExtra("floor", floor)
        intent.putExtra("filteredPeripherals", ArrayList(filteredPeripherals.map { it.toString() }))
        startActivity(intent)
    }
}
