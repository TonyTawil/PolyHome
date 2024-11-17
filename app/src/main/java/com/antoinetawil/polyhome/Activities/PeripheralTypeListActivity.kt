package com.antoinetawil.polyhome.Activities

import android.content.Intent
import android.os.Bundle
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.antoinetawil.polyhome.Adapters.PeripheralTypeAdapter
import com.antoinetawil.polyhome.R
import com.antoinetawil.polyhome.Utils.HeaderUtils
import okhttp3.*
import org.json.JSONObject
import java.io.IOException

class PeripheralTypeListActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_peripheral_type_list)

        HeaderUtils.setupHeader(this)

        val recyclerView: RecyclerView = findViewById(R.id.recyclerView)
        recyclerView.layoutManager = LinearLayoutManager(this)

        val houseId = intent.getIntExtra("houseId", -1)
        val availableTypes = intent.getStringArrayListExtra("availableTypes")

        if (houseId == -1 || availableTypes.isNullOrEmpty()) {
            Toast.makeText(this, "Invalid house data", Toast.LENGTH_SHORT).show()
            finish()
            return
        }

        val adapter = PeripheralTypeAdapter(availableTypes) { selectedType ->
            if (selectedType.lowercase() == "garage door") {
                fetchAndNavigateToPeripheralList(houseId, selectedType)
            } else {
                val intent = Intent(this, FloorListActivity::class.java)
                intent.putExtra("houseId", houseId)
                intent.putExtra("type", selectedType)
                startActivity(intent)
            }
        }

        recyclerView.adapter = adapter
    }

    private fun fetchAndNavigateToPeripheralList(houseId: Int, peripheralType: String) {
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
                    Toast.makeText(this@PeripheralTypeListActivity, "Failed to fetch peripherals", Toast.LENGTH_SHORT).show()
                }
            }

            override fun onResponse(call: Call, response: Response) {
                if (response.isSuccessful) {
                    val responseBody = response.body?.string()
                    if (!responseBody.isNullOrEmpty()) {
                        val peripherals = parsePeripherals(responseBody, peripheralType)
                        runOnUiThread {
                            navigateToPeripheralList(houseId, peripheralType, peripherals)
                        }
                    }
                } else {
                    runOnUiThread {
                        Toast.makeText(this@PeripheralTypeListActivity, "Failed to fetch peripherals", Toast.LENGTH_SHORT).show()
                    }
                }
            }
        })
    }

    private fun parsePeripherals(jsonResponse: String, type: String): List<JSONObject> {
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

    private fun navigateToPeripheralList(houseId: Int, type: String, peripherals: List<JSONObject>) {
        val intent = Intent(this, PeripheralListActivity::class.java)
        intent.putExtra("houseId", houseId)
        intent.putExtra("type", type)
        intent.putExtra("floor", "All") // Garage doors don’t have floors
        intent.putExtra("filteredPeripherals", ArrayList(peripherals.map { it.toString() }))
        startActivity(intent)
    }
}
