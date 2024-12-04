package com.antoinetawil.polyhome.Activities

import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.widget.TextView
import android.widget.Toast
import androidx.drawerlayout.widget.DrawerLayout
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.antoinetawil.polyhome.Adapters.PeripheralTypeAdapter
import com.antoinetawil.polyhome.R
import com.antoinetawil.polyhome.Utils.Api
import com.antoinetawil.polyhome.Utils.BaseActivity
import com.antoinetawil.polyhome.Utils.HeaderUtils
import org.json.JSONObject

class PeripheralTypeListActivity : BaseActivity() {

    companion object {
        private const val TAG = "PeripheralTypeList"
    }

    private lateinit var drawerLayout: DrawerLayout
    private val api = Api()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_peripheral_type_list)

        drawerLayout = findViewById(R.id.drawer_layout)
        HeaderUtils.setupHeaderWithDrawer(this, drawerLayout)

        val recyclerView: RecyclerView = findViewById(R.id.recyclerView)
        recyclerView.layoutManager = LinearLayoutManager(this)

        val houseId = intent.getIntExtra("houseId", -1)
        val availableTypes = intent.getStringArrayListExtra("availableTypes")

        Log.d(TAG, "Received houseId=$houseId, availableTypes=$availableTypes")

        if (houseId == -1 || availableTypes.isNullOrEmpty()) {
            Log.e(TAG, "Invalid house data: houseId=$houseId, availableTypes=$availableTypes")
            Toast.makeText(this, getString(R.string.invalid_house_data), Toast.LENGTH_SHORT).show()
            finish()
            return
        }

        findViewById<TextView>(R.id.titleTextView).text = getString(R.string.peripheral_types)

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

        val sharedPreferences = getSharedPreferences("PolyHomePrefs", MODE_PRIVATE)
        val token = sharedPreferences.getString("auth_token", null)

        if (token.isNullOrEmpty()) {
            Log.e(TAG, "Authentication token missing")
            Toast.makeText(this, getString(R.string.auth_token_missing), Toast.LENGTH_SHORT).show()
            return
        }

        Log.d(TAG, "Fetching peripherals: URL=$url, Token=$token")

        api.get<Map<String, List<Map<String, Any>>>>(
            path = url,
            securityToken = token,
            onSuccess = { responseCode, response ->
                runOnUiThread {
                    Log.d(TAG, "API Response Code: $responseCode")
                    if (responseCode == 200 && response != null) {
                        val devices = response["devices"] ?: emptyList()
                        Log.d(TAG, "Fetched devices: $devices")
                        val filteredPeripherals = devices.filter {
                            it["type"].toString().equals(peripheralType, ignoreCase = true)
                        }
                        Log.d(TAG, "Filtered peripherals for type '$peripheralType': $filteredPeripherals")
                        navigateToPeripheralList(houseId, peripheralType, filteredPeripherals)
                    } else {
                        Log.e(TAG, getString(R.string.failed_fetch_peripherals_log, responseCode))
                        Toast.makeText(this, getString(R.string.failed_fetch_peripherals), Toast.LENGTH_SHORT).show()
                    }
                }
            }
        )
    }

    private fun navigateToPeripheralList(houseId: Int, type: String, peripherals: List<Map<String, Any>>) {
        Log.d(TAG, "Navigating to PeripheralListActivity with houseId=$houseId, type=$type, peripherals=$peripherals")
        val intent = Intent(this, PeripheralListActivity::class.java)
        intent.putExtra("houseId", houseId)
        intent.putExtra("type", type)
        intent.putExtra("floor", getString(R.string.all_floors))
        intent.putExtra("filteredPeripherals", ArrayList(peripherals.map { JSONObject(it).toString() }))
        startActivity(intent)
    }
}
