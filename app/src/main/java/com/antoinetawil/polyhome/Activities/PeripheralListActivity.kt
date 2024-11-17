package com.antoinetawil.polyhome.Activities

import android.os.Bundle
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.antoinetawil.polyhome.Adapters.PeripheralListAdapter
import com.antoinetawil.polyhome.Models.Peripheral
import com.antoinetawil.polyhome.R
import com.antoinetawil.polyhome.Utils.HeaderUtils
import org.json.JSONObject

class PeripheralListActivity : AppCompatActivity() {

    private lateinit var recyclerView: RecyclerView
    private lateinit var titleTextView: TextView
    private lateinit var adapter: PeripheralListAdapter
    private val peripherals = mutableListOf<Peripheral>()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_peripheral_list)

        HeaderUtils.setupHeader(this)

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

        titleTextView.text = generateTitle(peripheralType, floor)

        adapter = PeripheralListAdapter(peripherals, this)
        recyclerView.adapter = adapter
    }

    private fun generateTitle(type: String, floor: String): String {
        val pluralType = when (type.lowercase()) {
            "light" -> "Lights"
            "rolling shutter" -> "Rolling Shutters"
            "garage door" -> "Garage Door" // Singular for Garage Door
            else -> type + "s"
        }

        return when (type.lowercase()) {
            "garage door" -> pluralType // Skip "All" or floor reference for Garage Door
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
}
