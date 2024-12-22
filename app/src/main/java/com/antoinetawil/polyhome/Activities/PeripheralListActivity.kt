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
        val floor = intent.getStringExtra("floor") ?: getString(R.string.all_floors)

        if (peripheralType.isEmpty() || houseId == -1) {
            Toast.makeText(this, getString(R.string.invalid_data), Toast.LENGTH_SHORT).show()
            finish()
            return
        }

        val token =
                getSharedPreferences("PolyHomePrefs", MODE_PRIVATE).getString("auth_token", null)
        if (token != null) {
            api.get<Map<String, List<Map<String, Any>>>>(
                    path = "https://polyhome.lesmoulinsdudev.com/api/houses/$houseId/devices",
                    securityToken = token,
                    onSuccess = { responseCode, response ->
                        if (responseCode == 200 && response != null) {
                            val devices = response["devices"] ?: emptyList()
                            val filteredDevices =
                                    devices.filter { device ->
                                        val id = device["id"] as? String ?: ""
                                        val inferredType =
                                                when {
                                                    id.startsWith("Light", ignoreCase = true) ->
                                                            "Light"
                                                    id.startsWith("Shutter", ignoreCase = true) ->
                                                            "Shutter"
                                                    id.startsWith(
                                                            "GarageDoor",
                                                            ignoreCase = true
                                                    ) -> "GarageDoor"
                                                    else -> null
                                                }
                                        inferredType?.equals(peripheralType, ignoreCase = true) ==
                                                true &&
                                                when (floor) {
                                                    getString(R.string.first_floor) ->
                                                            id.contains("1.")
                                                    getString(R.string.second_floor) ->
                                                            id.contains("2.")
                                                    else -> true
                                                }
                                    }

                            val newPeripherals =
                                    filteredDevices.map { device ->
                                        parsePeripheral(JSONObject(device))
                                    }

                            runOnUiThread {
                                peripherals.clear()
                                peripherals.addAll(newPeripherals)
                                filteredPeripherals.clear()
                                filteredPeripherals.addAll(newPeripherals)

                                setupActionButtons(peripheralType)
                                titleTextView.text = generateTitle(peripheralType, floor)
                                adapter = PeripheralListAdapter(filteredPeripherals, this)
                                recyclerView.adapter = adapter
                                setupSearchPopup()
                            }
                        } else {
                            runOnUiThread {
                                Toast.makeText(
                                                this,
                                                getString(R.string.fetch_failed),
                                                Toast.LENGTH_SHORT
                                        )
                                        .show()
                                finish()
                            }
                        }
                    }
            )
        } else {
            Toast.makeText(this, getString(R.string.auth_token_missing), Toast.LENGTH_SHORT).show()
            finish()
        }
    }

    private fun setupActionButtons(peripheralType: String) {
        actionButtonsContainer.removeAllViews()

        when (peripheralType.lowercase()) {
            "light" -> {
                val buttonsView =
                        LayoutInflater.from(this)
                                .inflate(
                                        R.layout.light_bulk_action_buttons_layout,
                                        actionButtonsContainer,
                                        false
                                )

                buttonsView.findViewById<Button>(R.id.turnOnAllButton).setOnClickListener {
                    performBulkOperation("TURN ON", "light")
                }

                buttonsView.findViewById<Button>(R.id.turnOffAllButton).setOnClickListener {
                    performBulkOperation("TURN OFF", "light")
                }

                actionButtonsContainer.addView(buttonsView)
            }
            "shutter", "garage door" -> {
                val buttonsView =
                        LayoutInflater.from(this)
                                .inflate(
                                        R.layout.bulk_action_buttons_layout,
                                        actionButtonsContainer,
                                        false
                                )

                buttonsView.findViewById<Button>(R.id.openAllButton).setOnClickListener {
                    performBulkOperation("OPEN", peripheralType.lowercase())
                }

                buttonsView.findViewById<Button>(R.id.stopAllButton).setOnClickListener {
                    performBulkOperation("STOP", peripheralType.lowercase())
                }

                buttonsView.findViewById<Button>(R.id.closeAllButton).setOnClickListener {
                    performBulkOperation("CLOSE", peripheralType.lowercase())
                }

                actionButtonsContainer.addView(buttonsView)
            }
        }
    }

    private fun Button.styledAsOutlinedButton() {
        setBackgroundResource(R.drawable.outlined_button_background)
        setTextColor(context.getColor(R.color.primary_text))
        textSize = 12f
        minimumHeight = 32.dpToPx()
        setPadding(8.dpToPx(), 0, 8.dpToPx(), 0)
    }

    private fun Int.dpToPx(): Int {
        return (this * resources.displayMetrics.density).toInt()
    }

    private fun setupSearchPopup() {
        val popupView = LayoutInflater.from(this).inflate(R.layout.search_popup, null)
        searchEditText = popupView.findViewById(R.id.searchEditText)

        searchPopup =
                PopupWindow(
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
        searchEditText.addTextChangedListener(
                object : TextWatcher {
                    override fun beforeTextChanged(
                            s: CharSequence?,
                            start: Int,
                            count: Int,
                            after: Int
                    ) {}
                    override fun onTextChanged(
                            s: CharSequence?,
                            start: Int,
                            before: Int,
                            count: Int
                    ) {
                        filterPeripherals(s.toString())
                    }
                    override fun afterTextChanged(s: Editable?) {}
                }
        )
    }

    private fun filterPeripherals(query: String) {
        filteredPeripherals.clear()
        if (query.isEmpty()) {
            filteredPeripherals.addAll(peripherals)
        } else {
            filteredPeripherals.addAll(
                    peripherals.filter { it.id.contains(query, ignoreCase = true) }
            )
        }
        adapter.notifyDataSetChanged()
    }

    private fun generateTitle(type: String, floor: String): String {
        val isFrench = resources.configuration.locales[0].language == "fr"

        if (type.lowercase() == "garagedoor") {
            return if (isFrench) getString(R.string.garage_door_fr)
            else getString(R.string.garage_door)
        }

        val pluralType =
                when (type.lowercase()) {
                    "light" ->
                            if (isFrench) getString(R.string.lights_fr)
                            else getString(R.string.lights)
                    "shutter" ->
                            if (isFrench) getString(R.string.rolling_shutters_fr)
                            else getString(R.string.rolling_shutters)
                    else -> type + "s"
                }

        return when (floor) {
            getString(R.string.all_floors) ->
                    if (isFrench) "${pluralType} ${getString(R.string.all_floors_fr_suffix)}"
                    else "${getString(R.string.all)} $pluralType"
            getString(R.string.first_floor) ->
                    if (isFrench) "${pluralType} ${getString(R.string.first_floor_fr_suffix)}"
                    else "$floor $pluralType"
            getString(R.string.second_floor) ->
                    if (isFrench) "${pluralType} ${getString(R.string.second_floor_fr_suffix)}"
                    else "$floor $pluralType"
            else -> "$floor $pluralType"
        }
    }

    private fun parsePeripheral(json: JSONObject): Peripheral {
        val id = json.getString("id")
        val type = json.optString("type", "Unknown")
        val availableCommands =
                json.getJSONArray("availableCommands").let { array ->
                    MutableList(array.length()) { array.getString(it) }
                }
        val opening = json.optDouble("opening")
        val openingMode = json.optInt("openingMode", -1).takeIf { it != -1 }
        val power = json.optInt("power", -1).takeIf { it != -1 }

        return Peripheral(id, type, availableCommands, power, opening, openingMode)
    }

    private fun performBulkOperation(command: String, type: String) {
        if (isOperationInProgress) return

        isOperationInProgress = true
        toggleButtons(false)

        Log.d(TAG, "Performing Bulk Operation: $command for type: $type")

        val normalizedType =
                when (type.lowercase()) {
                    "shutter" -> "rolling shutter"
                    "garage door" -> "garage door"
                    "light" -> "light"
                    else -> type.lowercase()
                }

        val affectedPeripherals =
                peripherals.filter {
                    it.type.equals(normalizedType, ignoreCase = true) &&
                            it.availableCommands.any { availableCommand ->
                                availableCommand.equals(command, ignoreCase = true)
                            }
                }

        if (affectedPeripherals.isEmpty()) {
            runOnUiThread {
                Toast.makeText(this, getString(R.string.no_devices_to_operate), Toast.LENGTH_SHORT)
                        .show()
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

            val url =
                    "https://polyhome.lesmoulinsdudev.com/api/houses/$houseId/devices/${peripheral.id}/command"
            Log.d(TAG, "Sending command $command to URL: $url")

            api.post<Map<String, String>, Unit>(
                    path = url,
                    data = mapOf("command" to command),
                    securityToken = token,
                    onSuccess = { responseCode, _ ->
                        if (responseCode == 200) {
                            // Instead of manually updating state, we'll refetch later
                        } else {
                            Log.e(TAG, "Failed to execute $command for ${peripheral.id}")
                        }
                    }
            )
        }

        // After all commands are sent, refetch the current state
        val houseId = intent.getIntExtra("houseId", -1)
        val token =
                getSharedPreferences("PolyHomePrefs", MODE_PRIVATE).getString("auth_token", null)

        if (token != null && houseId != -1) {
            api.get<Map<String, List<Map<String, Any>>>>(
                    path = "https://polyhome.lesmoulinsdudev.com/api/houses/$houseId/devices",
                    securityToken = token,
                    onSuccess = { responseCode, response ->
                        if (responseCode == 200 && response != null) {
                            val devices = response["devices"] ?: emptyList()
                            val updatedPeripherals =
                                    devices.map { device -> parsePeripheral(JSONObject(device)) }

                            runOnUiThread {
                                peripherals.clear()
                                peripherals.addAll(updatedPeripherals)
                                filteredPeripherals.clear()
                                filteredPeripherals.addAll(updatedPeripherals)
                                adapter.notifyDataSetChanged()
                                isOperationInProgress = false
                                toggleButtons(true)
                                Toast.makeText(
                                                this,
                                                getString(R.string.operation_completed, type),
                                                Toast.LENGTH_SHORT
                                        )
                                        .show()
                            }
                        } else {
                            runOnUiThread {
                                isOperationInProgress = false
                                toggleButtons(true)
                                Toast.makeText(
                                                this,
                                                getString(R.string.operation_completed, type),
                                                Toast.LENGTH_SHORT
                                        )
                                        .show()
                            }
                        }
                    }
            )
        } else {
            runOnUiThread {
                isOperationInProgress = false
                toggleButtons(true)
                Toast.makeText(
                                this,
                                getString(R.string.operation_completed, type),
                                Toast.LENGTH_SHORT
                        )
                        .show()
            }
        }
    }

    private fun toggleButtons(enable: Boolean) {
        for (i in 0 until actionButtonsContainer.childCount) {
            actionButtonsContainer.getChildAt(i).isEnabled = enable
        }
    }
}
