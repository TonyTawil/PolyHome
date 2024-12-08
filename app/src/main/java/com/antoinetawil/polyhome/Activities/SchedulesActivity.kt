package com.antoinetawil.polyhome.Activities

import android.app.DatePickerDialog
import android.app.TimePickerDialog
import android.os.Bundle
import android.util.Log
import android.widget.*
import androidx.drawerlayout.widget.DrawerLayout
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.antoinetawil.polyhome.Adapters.PeripheralListAdapter
import com.antoinetawil.polyhome.Models.House
import com.antoinetawil.polyhome.Models.Peripheral
import com.antoinetawil.polyhome.R
import com.antoinetawil.polyhome.Utils.Api
import com.antoinetawil.polyhome.Utils.BaseActivity
import com.antoinetawil.polyhome.Utils.HeaderUtils
import org.json.JSONObject
import java.text.SimpleDateFormat
import java.util.*

class SchedulesActivity : BaseActivity() {

    private val TAG = "SchedulesActivity"
    private lateinit var drawerLayout: DrawerLayout
    private lateinit var houseSpinner: Spinner
    private lateinit var dateButton: Button
    private lateinit var timeButton: Button
    private lateinit var peripheralTypeSpinner: Spinner
    private lateinit var floorsSpinner: Spinner
    private lateinit var searchButton: Button
    private lateinit var peripheralsRecyclerView: RecyclerView
    private lateinit var saveScheduleButton: Button

    private val houses = mutableListOf<House>()
    private lateinit var floors: List<String>
    private val peripherals = mutableListOf<Peripheral>()
    private val api = Api()
    private lateinit var adapter: PeripheralListAdapter

    private var selectedHouse: House? = null
    private var selectedPeripheralType: String? = null
    private var selectedFloor: String? = null
    private var selectedDate: String? = null
    private var selectedTime: String? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_schedules)

        floors = listOf(
            getString(R.string.all_floors),
            getString(R.string.first_floor),
            getString(R.string.second_floor)
        )

        drawerLayout = findViewById(R.id.drawer_layout)
        HeaderUtils.setupHeaderWithDrawer(this, drawerLayout)

        houseSpinner = findViewById(R.id.houseSpinner)
        dateButton = findViewById(R.id.dateButton)
        timeButton = findViewById(R.id.timeButton)
        peripheralTypeSpinner = findViewById(R.id.peripheralTypeSpinner)
        floorsSpinner = findViewById(R.id.floorsSpinner)
        searchButton = findViewById(R.id.searchButton)
        peripheralsRecyclerView = findViewById(R.id.peripheralsRecyclerView)
        saveScheduleButton = findViewById(R.id.saveScheduleButton)

        setupHouseSpinner()
        setupPeripheralTypeSpinner()
        setupFloorsSpinner()
        setupRecyclerView()

        dateButton.setOnClickListener { showDatePicker() }
        timeButton.setOnClickListener { showTimePicker() }
        searchButton.setOnClickListener { fetchPeripherals() }

        fetchHouses()
    }

    private fun setupHouseSpinner() {
        val adapter = ArrayAdapter(
            this,
            android.R.layout.simple_spinner_item,
            houses.map { getString(R.string.house_id_label, it.houseId) }
        )
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        houseSpinner.adapter = adapter

        houseSpinner.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
            override fun onItemSelected(parent: AdapterView<*>?, view: android.view.View?, position: Int, id: Long) {
                selectedHouse = houses[position]
            }

            override fun onNothingSelected(parent: AdapterView<*>?) {}
        }
    }

    private fun setupPeripheralTypeSpinner() {
        val peripheralTypes = listOf(getString(R.string.light), getString(R.string.shutter), getString(R.string.garage_door))
        val adapter = ArrayAdapter(this, android.R.layout.simple_spinner_item, peripheralTypes)
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        peripheralTypeSpinner.adapter = adapter

        peripheralTypeSpinner.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
            override fun onItemSelected(parent: AdapterView<*>?, view: android.view.View?, position: Int, id: Long) {
                selectedPeripheralType = peripheralTypes[position]
            }

            override fun onNothingSelected(parent: AdapterView<*>?) {}
        }
    }

    private fun setupFloorsSpinner() {
        val adapter = ArrayAdapter(this, android.R.layout.simple_spinner_item, floors)
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        floorsSpinner.adapter = adapter

        floorsSpinner.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
            override fun onItemSelected(parent: AdapterView<*>?, view: android.view.View?, position: Int, id: Long) {
                selectedFloor = floors[position]
            }

            override fun onNothingSelected(parent: AdapterView<*>?) {}
        }
    }

    private fun setupRecyclerView() {
        adapter = PeripheralListAdapter(peripherals, this)
        peripheralsRecyclerView.layoutManager = LinearLayoutManager(this)
        peripheralsRecyclerView.adapter = adapter
    }

    private fun showDatePicker() {
        val calendar = Calendar.getInstance()
        val datePicker = DatePickerDialog(
            this,
            { _, year, month, dayOfMonth ->
                val selectedDate = Calendar.getInstance()
                selectedDate.set(year, month, dayOfMonth)
                this.selectedDate = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(selectedDate.time)
                dateButton.text = this.selectedDate
            },
            calendar.get(Calendar.YEAR),
            calendar.get(Calendar.MONTH),
            calendar.get(Calendar.DAY_OF_MONTH)
        )
        datePicker.show()
    }

    private fun showTimePicker() {
        val calendar = Calendar.getInstance()
        val timePicker = TimePickerDialog(
            this,
            { _, hourOfDay, minute ->
                selectedTime = String.format("%02d:%02d", hourOfDay, minute)
                timeButton.text = selectedTime
            },
            calendar.get(Calendar.HOUR_OF_DAY),
            calendar.get(Calendar.MINUTE),
            true
        )
        timePicker.show()
    }

    private fun fetchHouses() {
        val token = getSharedPreferences("PolyHomePrefs", MODE_PRIVATE).getString("auth_token", null)

        if (token != null) {
            api.get<List<House>>(
                path = "https://polyhome.lesmoulinsdudev.com/api/houses",
                securityToken = token,
                onSuccess = { responseCode, response ->
                    runOnUiThread {
                        if (responseCode == 200 && response != null) {
                            houses.clear()
                            houses.addAll(response)
                            setupHouseSpinner()
                        } else {
                            Toast.makeText(this, getString(R.string.failed_to_fetch_houses), Toast.LENGTH_SHORT).show()
                        }
                    }
                }
            )
        }
    }

    private fun fetchPeripherals() {
        val houseId = selectedHouse?.houseId ?: return
        val type = selectedPeripheralType ?: return
        val floor = selectedFloor ?: getString(R.string.all_floors)

        val url = "https://polyhome.lesmoulinsdudev.com/api/houses/$houseId/devices"

        api.get<Map<String, List<Map<String, Any>>>>(
            path = url,
            securityToken = getSharedPreferences("PolyHomePrefs", MODE_PRIVATE).getString("auth_token", "")!!,
            onSuccess = { responseCode, response ->
                runOnUiThread {
                    if (responseCode == 200 && response != null) {
                        val devices = response["devices"] ?: emptyList()
                        peripherals.clear()
                        peripherals.addAll(
                            devices.map { JSONObject(it).toPeripheral() }.filter { peripheral ->
                                peripheral.type.equals(type, ignoreCase = true) &&
                                        (floor == getString(R.string.all_floors) || peripheral.id.contains("$floor.", ignoreCase = true))
                            }
                        )

                        if (peripherals.isEmpty()) {
                            Toast.makeText(this, "No matching peripherals", Toast.LENGTH_SHORT).show()
                        } else {
                            Toast.makeText(this, getString(R.string.peripherals_fetched), Toast.LENGTH_SHORT).show()
                        }
                        adapter.notifyDataSetChanged()
                    }
                }
            }
        )
    }

    private fun JSONObject.toPeripheral(): Peripheral {
        val id = this.getString("id")
        val type = this.optString("type", "Unknown")
        val availableCommands = this.optJSONArray("availableCommands")?.let { array ->
            MutableList(array.length()) { array.getString(it) }
        } ?: emptyList()
        val opening = this.optInt("opening", -1).takeIf { it != -1 }
        val openingMode = this.optInt("openingMode", -1).takeIf { it != -1 }
        val power = this.optInt("power", -1).takeIf { it != -1 }

        return Peripheral(id, type, availableCommands, opening, openingMode, power)
    }
}
