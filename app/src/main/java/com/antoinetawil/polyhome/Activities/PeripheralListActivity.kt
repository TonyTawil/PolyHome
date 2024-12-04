package com.antoinetawil.polyhome.Activities

import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.widget.*
import androidx.drawerlayout.widget.DrawerLayout
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.antoinetawil.polyhome.Adapters.PeripheralListAdapter
import com.antoinetawil.polyhome.Models.Peripheral
import com.antoinetawil.polyhome.R
import com.antoinetawil.polyhome.Utils.Api
import com.antoinetawil.polyhome.Utils.BaseActivity
import com.antoinetawil.polyhome.Utils.HeaderUtils
import org.json.JSONObject

class PeripheralListActivity : BaseActivity() {

    private val TAG = "PeripheralListActivity"
    private lateinit var drawerLayout: DrawerLayout
    private lateinit var recyclerView: RecyclerView
    private lateinit var titleTextView: TextView
    private lateinit var searchEditText: EditText
    private lateinit var searchPopup: PopupWindow
    private lateinit var adapter: PeripheralListAdapter
    private val peripherals = mutableListOf<Peripheral>()
    private val filteredPeripherals = mutableListOf<Peripheral>()
    private lateinit var actionButtonsContainer: LinearLayout
    private var isOperationInProgress = false
    private val api = Api()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_peripheral_list)

        drawerLayout = findViewById(R.id.drawer_layout)
        HeaderUtils.setupHeaderWithDrawer(this, drawerLayout)

        titleTextView = findViewById(R.id.titleTextView)
        recyclerView = findViewById(R.id.recyclerView)
        recyclerView.layoutManager = LinearLayoutManager(this)
        actionButtonsContainer = findViewById(R.id.actionButtonsContainer)

        val houseId = intent.getIntExtra("houseId", -1)
        val peripheralType = intent.getStringExtra("type") ?: ""
        Log.d(TAG, "Peripheral Type: $peripheralType")

        if (peripheralType.isEmpty()) {
            Toast.makeText(this, getString(R.string.invalid_data), Toast.LENGTH_SHORT).show()
            finish()
            return
        }

        setupActionButtons(peripheralType)
        val floor = intent.getStringExtra("floor") ?: getString(R.string.all_floors)
        val filteredPeripheralsJson = intent.getStringArrayListExtra("filteredPeripherals")

        if (houseId == -1 || filteredPeripheralsJson.isNullOrEmpty()) {
            Toast.makeText(this, getString(R.string.invalid_data), Toast.LENGTH_SHORT).show()
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
    }

    private fun setupActionButtons(peripheralType: String) {
        actionButtonsContainer.removeAllViews()

        when (peripheralType.lowercase()) {
            "light" -> {
                addActionButton(getString(R.string.turn_on_all)) { performBulkOperation("TURN ON", "light") }
                addActionButton(getString(R.string.turn_off_all)) { performBulkOperation("TURN OFF", "light") }
            }
            "shutter", "garage door" -> {
                addActionButton(getString(R.string.open_all)) { performBulkOperation("OPEN", peripheralType.lowercase()) }
                addActionButton(getString(R.string.close_all)) { performBulkOperation("CLOSE", peripheralType.lowercase()) }
                addActionButton(getString(R.string.stop_all)) { performBulkOperation("STOP", peripheralType.lowercase()) }
            }
        }
    }

    private fun addActionButton(text: String, action: () -> Unit) {
        Log.d(TAG, "Adding action button: $text")
        val button = Button(this).apply {
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.WRAP_CONTENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            ).apply {
                setMargins(8, 8, 8, 8)
            }
            this.text = text
            setOnClickListener { action() }
        }
        actionButtonsContainer.addView(button)
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

        searchEditText.hint = getString(R.string.search_by_id)
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
        val isFrench = resources.configuration.locales[0].language == "fr" // Detect current locale

        if (type.lowercase() == "garagedoor") {
            return if (isFrench) getString(R.string.garage_door_fr) else getString(R.string.garage_door)
        }

        val pluralType = when (type.lowercase()) {
            "light" -> if (isFrench) getString(R.string.lights_fr) else getString(R.string.lights)
            "shutter" -> if (isFrench) getString(R.string.rolling_shutters_fr) else getString(R.string.rolling_shutters)
            else -> type + "s"
        }

        return when (floor) {
            getString(R.string.all_floors) -> if (isFrench) "${pluralType} ${getString(R.string.all_floors_fr_suffix)}" else "${getString(R.string.all)} $pluralType"
            getString(R.string.first_floor) -> if (isFrench) "${pluralType} ${getString(R.string.first_floor_fr_suffix)}" else "$floor $pluralType"
            getString(R.string.second_floor) -> if (isFrench) "${pluralType} ${getString(R.string.second_floor_fr_suffix)}" else "$floor $pluralType"
            else -> "$floor $pluralType"
        }
    }



    private fun parsePeripheral(json: JSONObject): Peripheral {
        val id = json.getString("id")
        val type = json.optString("type", "Unknown")
        val availableCommands = json.getJSONArray("availableCommands").let { array ->
            MutableList(array.length()) { array.getString(it) }
        }
        val opening = json.optInt("opening", -1).takeIf { it != -1 }
        val openingMode = json.optInt("openingMode", -1).takeIf { it != -1 }
        val power = json.optInt("power", -1).takeIf { it != -1 }

        return Peripheral(id, type, availableCommands, opening, openingMode, power)
    }

    private fun performBulkOperation(command: String, type: String) {
        if (isOperationInProgress) return

        isOperationInProgress = true
        toggleButtons(false)

        Log.d(TAG, "Performing Bulk Operation: $command for type: $type")

        val normalizedType = when (type.lowercase()) {
            "shutter" -> "rolling shutter"
            "garage door" -> "garage door"
            "light" -> "light"
            else -> type.lowercase()
        }

        val affectedPeripherals = peripherals.filter {
            it.type.equals(normalizedType, ignoreCase = true) &&
                    it.availableCommands.any { availableCommand ->
                        availableCommand.equals(command, ignoreCase = true)
                    }
        }

        if (affectedPeripherals.isEmpty()) {
            runOnUiThread {
                Toast.makeText(this, getString(R.string.no_devices_to_operate), Toast.LENGTH_SHORT).show()
                isOperationInProgress = false
                toggleButtons(true)
            }
            return
        }

        affectedPeripherals.forEach { peripheral ->
            val houseId = intent.getIntExtra("houseId", -1)
            val sharedPreferences = getSharedPreferences("PolyHomePrefs", MODE_PRIVATE)
            val token = sharedPreferences.getString("auth_token", null)

            if (token == null || houseId == -1) {
                Log.e(TAG, "Invalid token or houseId")
                return@forEach
            }

            val url = "https://polyhome.lesmoulinsdudev.com/api/houses/$houseId/devices/${peripheral.id}/command"
            Log.d(TAG, "Sending command $command to URL: $url")

            api.post<Map<String, String>, Unit>(
                path = url,
                data = mapOf("command" to command),
                securityToken = token,
                onSuccess = { responseCode, _ ->
                    if (responseCode == 200) {
                        runOnUiThread {
                            if (type.equals("light", ignoreCase = true)) {
                                peripheral.power = if (command == "TURN ON") 1 else 0
                            } else if (type.equals("shutter", ignoreCase = true) || type.equals("garage door", ignoreCase = true)) {
                                peripheral.opening = when (command.uppercase()) {
                                    "OPEN" -> 100
                                    "CLOSE" -> 0
                                    "STOP" -> peripheral.opening
                                    else -> peripheral.opening
                                }
                            }
                            adapter.notifyDataSetChanged()
                        }
                    } else {
                        Log.e(TAG, "Failed to execute $command for ${peripheral.id}")
                    }
                }
            )
        }

        runOnUiThread {
            isOperationInProgress = false
            toggleButtons(true)
            Toast.makeText(this, getString(R.string.operation_completed, type), Toast.LENGTH_SHORT).show()
        }
    }

    private fun toggleButtons(enable: Boolean) {
        for (i in 0 until actionButtonsContainer.childCount) {
            actionButtonsContainer.getChildAt(i).isEnabled = enable
        }
    }
}
