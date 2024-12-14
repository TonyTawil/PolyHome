package com.antoinetawil.polyhome.Activities

import android.app.AlarmManager
import android.app.DatePickerDialog
import android.app.PendingIntent
import android.app.TimePickerDialog
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.provider.Settings
import android.util.Log
import android.widget.*
import androidx.annotation.RequiresApi
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
import com.antoinetawil.polyhome.Utils.ScheduleReceiver
import java.text.SimpleDateFormat
import java.util.*
import org.json.JSONObject

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

    companion object {
        private const val ALARM_PERMISSION_REQUEST_CODE = 123
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_schedules)

        floors =
                listOf(
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
        saveScheduleButton.setOnClickListener { saveSchedule() }

        fetchHouses()

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            requestAlarmPermission()
        }
    }

    @RequiresApi(Build.VERSION_CODES.S)
    private fun requestAlarmPermission() {
        if (!hasAlarmPermission()) {
            val intent = Intent().apply { action = Settings.ACTION_REQUEST_SCHEDULE_EXACT_ALARM }
            startActivityForResult(intent, ALARM_PERMISSION_REQUEST_CODE)
        }
    }

    private fun hasAlarmPermission(): Boolean {
        val alarmManager = getSystemService(Context.ALARM_SERVICE) as AlarmManager
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            alarmManager.canScheduleExactAlarms()
        } else {
            true
        }
    }

    private fun setupHouseSpinner() {
        val adapter =
                ArrayAdapter(
                        this,
                        android.R.layout.simple_spinner_item,
                        houses.map { getString(R.string.house_id_label, it.houseId) }
                )
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        houseSpinner.adapter = adapter

        houseSpinner.onItemSelectedListener =
                object : AdapterView.OnItemSelectedListener {
                    override fun onItemSelected(
                            parent: AdapterView<*>?,
                            view: android.view.View?,
                            position: Int,
                            id: Long
                    ) {
                        selectedHouse = houses[position]
                    }

                    override fun onNothingSelected(parent: AdapterView<*>?) {}
                }
    }

    private fun setupPeripheralTypeSpinner() {
        val peripheralTypes =
                listOf(
                        getString(R.string.light),
                        getString(R.string.shutter),
                        getString(R.string.garage_door)
                )
        val adapter = ArrayAdapter(this, android.R.layout.simple_spinner_item, peripheralTypes)
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        peripheralTypeSpinner.adapter = adapter

        peripheralTypeSpinner.onItemSelectedListener =
                object : AdapterView.OnItemSelectedListener {
                    override fun onItemSelected(
                            parent: AdapterView<*>?,
                            view: android.view.View?,
                            position: Int,
                            id: Long
                    ) {
                        selectedPeripheralType = peripheralTypes[position]
                    }

                    override fun onNothingSelected(parent: AdapterView<*>?) {}
                }
    }

    private fun setupFloorsSpinner() {
        val adapter = ArrayAdapter(this, android.R.layout.simple_spinner_item, floors)
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        floorsSpinner.adapter = adapter

        floorsSpinner.onItemSelectedListener =
                object : AdapterView.OnItemSelectedListener {
                    override fun onItemSelected(
                            parent: AdapterView<*>?,
                            view: android.view.View?,
                            position: Int,
                            id: Long
                    ) {
                        selectedFloor = floors[position]
                    }

                    override fun onNothingSelected(parent: AdapterView<*>?) {}
                }
    }

    private fun setupRecyclerView() {
        adapter = PeripheralListAdapter(peripherals, this, isScheduleMode = true)
        peripheralsRecyclerView.layoutManager = LinearLayoutManager(this)
        peripheralsRecyclerView.adapter = adapter
    }

    private fun showDatePicker() {
        val calendar = Calendar.getInstance()
        val datePicker =
                DatePickerDialog(
                        this,
                        { _, year, month, dayOfMonth ->
                            val selectedDate = Calendar.getInstance()
                            selectedDate.set(year, month, dayOfMonth)
                            this.selectedDate =
                                    SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
                                            .format(selectedDate.time)
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
        val timePicker =
                TimePickerDialog(
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
        val token =
                getSharedPreferences("PolyHomePrefs", MODE_PRIVATE).getString("auth_token", null)

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
                                Toast.makeText(
                                                this,
                                                getString(R.string.failed_to_fetch_houses),
                                                Toast.LENGTH_SHORT
                                        )
                                        .show()
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
                securityToken =
                        getSharedPreferences("PolyHomePrefs", MODE_PRIVATE)
                                .getString("auth_token", "")!!,
                onSuccess = { responseCode, response ->
                    runOnUiThread {
                        if (responseCode == 200 && response != null) {
                            val devices = response["devices"] ?: emptyList()
                            peripherals.clear()

                            peripherals.addAll(
                                    devices.mapNotNull { device ->
                                        val json = JSONObject(device)
                                        val id = json.optString("id")
                                        val inferredType =
                                                when {
                                                    id.startsWith("Light", ignoreCase = true) ->
                                                            getString(R.string.light)
                                                    id.startsWith("Shutter", ignoreCase = true) ->
                                                            getString(R.string.shutter)
                                                    id.startsWith(
                                                            "GarageDoor",
                                                            ignoreCase = true
                                                    ) -> getString(R.string.garage_door)
                                                    else -> null
                                                }

                                        if (inferredType != null &&
                                                        inferredType.equals(
                                                                type,
                                                                ignoreCase = true
                                                        ) &&
                                                        (floor == getString(R.string.all_floors) ||
                                                                matchesFloor(id, floor))
                                        ) {
                                            json.toPeripheral()
                                        } else {
                                            null
                                        }
                                    }
                            )

                            if (peripherals.isEmpty()) {
                                Toast.makeText(
                                                this,
                                                getString(R.string.no_matching_peripherals),
                                                Toast.LENGTH_SHORT
                                        )
                                        .show()
                            } else {
                                Toast.makeText(
                                                this,
                                                getString(R.string.peripherals_fetched),
                                                Toast.LENGTH_SHORT
                                        )
                                        .show()
                            }
                            adapter.notifyDataSetChanged()
                        } else {
                            Toast.makeText(
                                            this,
                                            getString(R.string.failed_to_fetch_peripherals),
                                            Toast.LENGTH_SHORT
                                    )
                                    .show()
                        }
                    }
                }
        )
    }

    private fun matchesFloor(id: String, floor: String): Boolean {
        return when (floor) {
            getString(R.string.first_floor) -> id.contains("1.", ignoreCase = true)
            getString(R.string.second_floor) -> id.contains("2.", ignoreCase = true)
            else -> false
        }
    }

    private fun JSONObject.toPeripheral(): Peripheral {
        val id = this.getString("id")
        val type = this.optString("type", "Unknown")
        val availableCommands =
                this.optJSONArray("availableCommands")?.let { array ->
                    MutableList(array.length()) { array.getString(it) }
                }
                        ?: emptyList()
        val opening = this.optInt("opening", -1).takeIf { it != -1 }
        val openingMode = this.optInt("openingMode", -1).takeIf { it != -1 }
        val power = this.optInt("power", -1).takeIf { it != -1 }

        return Peripheral(id, type, availableCommands, opening, openingMode, power)
    }

    private fun saveSchedule() {
        if (selectedDate == null || selectedTime == null) {
            Toast.makeText(this, getString(R.string.select_date_time), Toast.LENGTH_SHORT).show()
            return
        }

        val selectedPeripherals =
                peripherals.filter { peripheral ->
                    when (peripheral.type.lowercase()) {
                        "light" -> peripheral.power == 1
                        "rolling shutter", "garage door" -> peripheral.opening != null
                        else -> false
                    }
                }

        if (selectedPeripherals.isEmpty()) {
            Toast.makeText(this, getString(R.string.no_peripherals_selected), Toast.LENGTH_SHORT)
                    .show()
            return
        }

        val scheduleDateTime = "$selectedDate $selectedTime"
        val scheduleTimeInMillis = getScheduleTimeInMillis(scheduleDateTime)

        if (scheduleTimeInMillis == null || scheduleTimeInMillis <= System.currentTimeMillis()) {
            Toast.makeText(this, getString(R.string.invalid_schedule_time), Toast.LENGTH_SHORT)
                    .show()
            return
        }

        getSharedPreferences("PolyHomePrefs", MODE_PRIVATE).edit().apply {
            putInt("selected_house_id", selectedHouse?.houseId ?: -1)
            apply()
        }

        selectedPeripherals.forEach { peripheral ->
            scheduleCommand(peripheral, scheduleTimeInMillis)
        }

        resetFields()
        Toast.makeText(this, getString(R.string.schedule_saved), Toast.LENGTH_SHORT).show()
    }

    private fun resetFields() {
        selectedDate = null
        selectedTime = null
        dateButton.text = getString(R.string.select_date)
        timeButton.text = getString(R.string.select_time)
        peripherals.forEach {
            it.power = 0
            it.opening = null
        }
        adapter.notifyDataSetChanged()
    }

    private fun getScheduleTimeInMillis(dateTime: String): Long? {
        return try {
            val format = SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.getDefault())
            val date = format.parse(dateTime)
            date?.time
        } catch (e: Exception) {
            Log.e(TAG, "Error parsing date time: $e")
            null
        }
    }

    private fun scheduleCommand(peripheral: Peripheral, timeInMillis: Long) {
        val command = determineCommand(peripheral)
        Log.d(
                TAG,
                "Scheduling command: $command for peripheral: ${peripheral.id} at time: $timeInMillis"
        )

        val intent =
                Intent(this, ScheduleReceiver::class.java).apply {
                    putExtra("peripheralId", peripheral.id)
                    putExtra("command", command)
                    data = Uri.parse("custom://${System.currentTimeMillis()}")
                }

        val flags =
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                    PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
                } else {
                    PendingIntent.FLAG_UPDATE_CURRENT
                }

        val pendingIntent =
                PendingIntent.getBroadcast(
                        this,
                        peripheral.id.hashCode() + System.currentTimeMillis().toInt(),
                        intent,
                        flags
                )

        val alarmManager = getSystemService(Context.ALARM_SERVICE) as AlarmManager
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            alarmManager.setExactAndAllowWhileIdle(
                    AlarmManager.RTC_WAKEUP,
                    timeInMillis,
                    pendingIntent
            )
        } else {
            alarmManager.setExact(AlarmManager.RTC_WAKEUP, timeInMillis, pendingIntent)
        }

        Log.d(TAG, "Scheduled command for ${peripheral.id} at $timeInMillis")
    }

    private fun determineCommand(peripheral: Peripheral): String {
        return when (peripheral.type.lowercase()) {
            "light" -> if (peripheral.power == 1) "TURN ON" else "TURN OFF"
            "rolling shutter", "garage door" ->
                    when (peripheral.opening) {
                        100 -> "OPEN"
                        0 -> "CLOSE"
                        50 -> "STOP"
                        else -> "STOP"
                    }
            else -> ""
        }
    }
}
