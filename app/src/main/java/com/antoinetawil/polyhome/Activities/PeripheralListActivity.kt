package com.antoinetawil.polyhome.Activities

import android.content.Context
import android.os.Bundle
import android.util.Log
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
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

    companion object {
        private const val TAG = "PeripheralListActivity"
    }

    private lateinit var recyclerView: RecyclerView
    private lateinit var adapter: PeripheralListAdapter
    private val peripherals = mutableListOf<Peripheral>()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_peripheral_list)

        HeaderUtils.setupHeader(this)

        recyclerView = findViewById(R.id.recyclerView)
        adapter = PeripheralListAdapter(peripherals, this)
        recyclerView.layoutManager = LinearLayoutManager(this)
        recyclerView.adapter = adapter

        val sharedPreferences = getSharedPreferences("PolyHomePrefs", Context.MODE_PRIVATE)
        val token = sharedPreferences.getString("auth_token", null)
        val houseId = intent.getIntExtra("houseId", -1)
        val type = intent.getStringExtra("type")

        if (token != null && houseId != -1) {
            fetchPeripheralList(token, houseId, type)
        } else {
            Toast.makeText(this, "Error loading peripherals", Toast.LENGTH_SHORT).show()
            Log.e(TAG, "Token or houseId is invalid")
        }
    }

    private fun fetchPeripheralList(token: String, houseId: Int, type: String?) {
        val url = "https://polyhome.lesmoulinsdudev.com/api/houses/$houseId/devices"
        val client = OkHttpClient()

        val request = Request.Builder()
            .url(url)
            .addHeader("Authorization", "Bearer $token")
            .build()

        Log.d(TAG, "Fetching peripherals for house ID: $houseId, Type: $type")

        client.newCall(request).enqueue(object : Callback {
            override fun onFailure(call: Call, e: IOException) {
                Log.e(TAG, "Network request failed: ${e.message}", e)
                runOnUiThread {
                    Toast.makeText(this@PeripheralListActivity, "Failed to fetch peripherals", Toast.LENGTH_LONG).show()
                }
            }

            override fun onResponse(call: Call, response: Response) {
                if (response.isSuccessful) {
                    val responseBody = response.body?.string()
                    Log.d(TAG, "Peripheral list response: $responseBody")

                    if (responseBody != null) {
                        parseAndDisplayPeripherals(responseBody, type)
                    }
                } else {
                    Log.e(TAG, "Failed to fetch peripherals: ${response.code}")
                    runOnUiThread {
                        Toast.makeText(this@PeripheralListActivity, "Failed to fetch peripherals", Toast.LENGTH_SHORT).show()
                    }
                }
            }
        })
    }

    private fun parseAndDisplayPeripherals(jsonResponse: String, type: String?) {
        try {
            val jsonArray = JSONObject(jsonResponse).getJSONArray("devices")
            peripherals.clear()

            for (i in 0 until jsonArray.length()) {
                val deviceObject = jsonArray.getJSONObject(i)
                val id = deviceObject.getString("id")
                val deviceType = deviceObject.getString("type")
                val availableCommands = deviceObject.getJSONArray("availableCommands")

                if (type == null || deviceType == type) {
                    val commands = mutableListOf<String>()
                    for (j in 0 until availableCommands.length()) {
                        commands.add(availableCommands.getString(j))
                    }

                    val opening = deviceObject.optInt("opening", -1).takeIf { it != -1 }
                    val openingMode = deviceObject.optInt("openingMode", -1).takeIf { it != -1 }
                    val power = deviceObject.optInt("power", -1).takeIf { it != -1 }

                    peripherals.add(Peripheral(id, deviceType, commands, opening, openingMode, power))
                }
            }

            runOnUiThread {
                adapter.notifyDataSetChanged()
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error parsing peripherals: ${e.message}", e)
        }
    }
}
