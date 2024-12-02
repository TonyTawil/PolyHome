package com.antoinetawil.polyhome.Activities

import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.widget.Toast
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.antoinetawil.polyhome.Adapters.FloorListAdapter
import com.antoinetawil.polyhome.R
import com.antoinetawil.polyhome.Utils.Api
import com.antoinetawil.polyhome.Utils.BaseActivity
import com.antoinetawil.polyhome.Utils.HeaderUtils
import org.json.JSONObject

class FloorListActivity : BaseActivity() {

    private val TAG = "FloorListActivity"
    private lateinit var recyclerView: RecyclerView
    private val api = Api()
    private lateinit var floors: List<String>
    private var peripherals = mutableListOf<JSONObject>()

    override fun onCreate(savedInstanceState: Bundle?) {
        Log.d(TAG, "onCreate: Initializing FloorListActivity")
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_floor_list)

        val drawerLayout = findViewById<androidx.drawerlayout.widget.DrawerLayout>(R.id.drawer_layout)
        HeaderUtils.setupHeaderWithDrawer(this, drawerLayout)

        recyclerView = findViewById(R.id.recyclerView)
        recyclerView.layoutManager = LinearLayoutManager(this)

        floors = listOf(
            getString(R.string.all_floors),
            getString(R.string.first_floor),
            getString(R.string.second_floor)
        )

        val houseId = intent.getIntExtra("houseId", -1)
        val peripheralType = intent.getStringExtra("type")

        Log.d(TAG, "onCreate: houseId=$houseId, peripheralType=$peripheralType")

        if (houseId == -1 || peripheralType.isNullOrEmpty()) {
            Log.e(TAG, "Invalid data: houseId=$houseId, peripheralType=$peripheralType")
            Toast.makeText(this, getString(R.string.invalid_data), Toast.LENGTH_SHORT).show()
            finish()
            return
        }

        // Skip floor selection for GarageDoor
        if (peripheralType.equals("GarageDoor", ignoreCase = true)) {
            Log.d(TAG, "GarageDoor detected. Skipping floor selection.")
            fetchPeripherals(houseId, peripheralType, skipFloorSelection = true)
            return
        }

        fetchPeripherals(houseId, peripheralType)
    }


    private fun fetchPeripherals(houseId: Int, peripheralType: String, skipFloorSelection: Boolean = false) {
        val url = "https://polyhome.lesmoulinsdudev.com/api/houses/$houseId/devices"

        val sharedPreferences = getSharedPreferences("PolyHomePrefs", MODE_PRIVATE)
        val token = sharedPreferences.getString("auth_token", null)

        if (token.isNullOrEmpty()) {
            Log.e(TAG, "Authentication token is missing")
            Toast.makeText(this, getString(R.string.auth_token_missing), Toast.LENGTH_SHORT).show()
            return
        }

        Log.d(TAG, "Fetching peripherals from URL: $url with token: $token")

        api.get<Map<String, List<Map<String, Any>>>>(
            path = url,
            securityToken = token,
            onSuccess = { responseCode, response ->
                runOnUiThread {
                    Log.d(TAG, "API response code: $responseCode")
                    if (responseCode == 200 && response != null) {
                        val devices = response["devices"] ?: emptyList()
                        Log.d(TAG, "Fetched devices: $devices")

                        peripherals = parsePeripherals(devices, peripheralType)

                        if (skipFloorSelection) {
                            Log.d(TAG, "Navigating directly to PeripheralListActivity for GarageDoor")
                            navigateToPeripheralList(houseId, peripheralType, getString(R.string.all_floors), peripherals)
                            return@runOnUiThread
                        }

                        setupFloorAdapter(houseId, peripheralType)
                    } else {
                        Log.e(TAG, "Failed to fetch peripherals. Response: $response")
                        Toast.makeText(this, getString(R.string.fetch_failed), Toast.LENGTH_SHORT).show()
                    }
                }
            }
        )
    }


    private fun parsePeripherals(devices: List<Map<String, Any>>, expectedType: String): MutableList<JSONObject> {
        val filteredPeripherals = mutableListOf<JSONObject>()

        for (device in devices) {
            try {
                val jsonObject = JSONObject(device)
                val id = jsonObject.getString("id")

                // Infer type from the prefix of `id` (e.g., "Light", "Shutter", "GarageDoor")
                val inferredType = when {
                    id.startsWith("Light", ignoreCase = true) -> "Light"
                    id.startsWith("Shutter", ignoreCase = true) -> "Shutter"
                    id.startsWith("GarageDoor", ignoreCase = true) -> "GarageDoor"
                    else -> null
                }

                if (inferredType != null && inferredType.equals(expectedType, ignoreCase = true)) {
                    filteredPeripherals.add(jsonObject)
                }
            } catch (e: Exception) {
                Log.e(TAG, "Failed to parse device: $device", e)
            }
        }

        return filteredPeripherals
    }


    private fun setupFloorAdapter(houseId: Int, peripheralType: String) {
        Log.d(TAG, "Setting up floor adapter for houseId=$houseId, peripheralType=$peripheralType")

        val adapter = FloorListAdapter(floors, peripheralType) { selectedFloor ->
            Log.d(TAG, "Floor selected: $selectedFloor")
            val filteredPeripherals = when (selectedFloor) {
                getString(R.string.first_floor) -> peripherals.filter { it.getString("id").contains("1.") }
                getString(R.string.second_floor) -> peripherals.filter { it.getString("id").contains("2.") }
                else -> peripherals
            }

            Log.d(TAG, "Filtered peripherals for floor '$selectedFloor': $filteredPeripherals")

            if (filteredPeripherals.isEmpty()) {
                Log.w(TAG, "No peripherals found for floor '$selectedFloor'")
            }

            navigateToPeripheralList(houseId, peripheralType, selectedFloor, filteredPeripherals)
        }
        recyclerView.adapter = adapter
    }


    private fun navigateToPeripheralList(houseId: Int, type: String, floor: String, filteredPeripherals: List<JSONObject>) {
        Log.d(TAG, "Navigating to PeripheralListActivity with houseId=$houseId, type=$type, floor=$floor, peripherals=$filteredPeripherals")
        val intent = Intent(this, PeripheralListActivity::class.java)
        intent.putExtra("houseId", houseId)
        intent.putExtra("type", type)
        intent.putExtra("floor", floor)
        intent.putExtra("filteredPeripherals", ArrayList(filteredPeripherals.map { it.toString() }))
        startActivity(intent)
    }
}
