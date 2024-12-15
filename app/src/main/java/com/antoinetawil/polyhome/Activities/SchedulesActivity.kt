package com.antoinetawil.polyhome.Activities

import android.app.AlarmManager
import android.app.Dialog
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.provider.Settings
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.widget.*
import androidx.annotation.RequiresApi
import androidx.drawerlayout.widget.DrawerLayout
import androidx.lifecycle.lifecycleScope
import com.antoinetawil.polyhome.Database.DatabaseHelper
import com.antoinetawil.polyhome.Models.House
import com.antoinetawil.polyhome.Models.Peripheral
import com.antoinetawil.polyhome.Models.Schedule
import com.antoinetawil.polyhome.Models.ScheduleCommand
import com.antoinetawil.polyhome.R
import com.antoinetawil.polyhome.Utils.Api
import com.antoinetawil.polyhome.Utils.BaseActivity
import com.antoinetawil.polyhome.Utils.HeaderUtils
import com.antoinetawil.polyhome.Utils.ScheduleReceiver
import java.text.SimpleDateFormat
import java.util.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.json.JSONObject

class SchedulesActivity : BaseActivity() {

    private val TAG = "SchedulesActivity"
    private lateinit var drawerLayout: DrawerLayout
    private lateinit var houseSpinner: Spinner
    private lateinit var peripheralTypeSpinner: Spinner
    private lateinit var floorsSpinner: Spinner
    private lateinit var searchButton: Button
    private lateinit var peripheralsContainer: LinearLayout
    private lateinit var saveScheduleButton: Button

    private val houses = mutableListOf<House>()
    private lateinit var floors: List<String>
    private val peripherals = mutableListOf<Peripheral>()
    private val api = Api()

    private var selectedHouse: House? = null
    private var selectedPeripheralType: String? = null
    private var selectedFloor: String? = null
    private var selectedDate: String? = null
    private var selectedTime: String? = null

    private lateinit var dbHelper: DatabaseHelper

    private lateinit var dayToggles: List<ToggleButton>

    private lateinit var hourPicker: NumberPicker
    private lateinit var minutePicker: NumberPicker
    private lateinit var calendarButton: Button

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
        peripheralTypeSpinner = findViewById(R.id.peripheralTypeSpinner)
        floorsSpinner = findViewById(R.id.floorsSpinner)
        searchButton = findViewById(R.id.searchButton)
        peripheralsContainer = findViewById(R.id.peripheralsContainer)
        saveScheduleButton = findViewById(R.id.saveScheduleButton)

        hourPicker = findViewById(R.id.hourPicker)
        minutePicker = findViewById(R.id.minutePicker)
        calendarButton = findViewById(R.id.calendarButton)
        calendarButton.setOnClickListener { showCalendarDialog() }

        setupHouseSpinner()
        setupPeripheralTypeSpinner()
        setupFloorsSpinner()
        setupPeripheralsList()

        searchButton.setOnClickListener { fetchPeripherals() }
        saveScheduleButton.setOnClickListener { saveSchedule() }

        fetchHouses()

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            requestAlarmPermission()
        }

        dbHelper = DatabaseHelper(this)

        dayToggles =
                listOf(
                        findViewById(R.id.mondayToggle),
                        findViewById(R.id.tuesdayToggle),
                        findViewById(R.id.wednesdayToggle),
                        findViewById(R.id.thursdayToggle),
                        findViewById(R.id.fridayToggle),
                        findViewById(R.id.saturdayToggle),
                        findViewById(R.id.sundayToggle)
                )

        hourPicker.apply {
            minValue = 0
            maxValue = 23
            value = Calendar.getInstance().get(Calendar.HOUR_OF_DAY)
        }

        minutePicker.apply {
            minValue = 0
            maxValue = 59
            value = Calendar.getInstance().get(Calendar.MINUTE)
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

    private fun setupPeripheralsList() {
        peripheralsContainer.removeAllViews()
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
                            updatePeripheralsList()
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
        val scheduleDateTime = getSelectedDateTime()
        if (scheduleDateTime == null) {
            Toast.makeText(this, getString(R.string.select_date_time), Toast.LENGTH_SHORT).show()
            return
        }

        val selectedPeripherals =
                peripherals.filter { peripheral ->
                    when (peripheral.type.lowercase()) {
                        "light" -> peripheral.power != 0
                        "rolling shutter", "garage door" -> peripheral.opening != null
                        else -> false
                    }
                }

        if (selectedPeripherals.isEmpty()) {
            Toast.makeText(this, getString(R.string.no_peripherals_selected), Toast.LENGTH_SHORT)
                    .show()
            return
        }

        val scheduleTimeInMillis = getScheduleTimeInMillis(scheduleDateTime)
        if (scheduleTimeInMillis == null) {
            Toast.makeText(this, getString(R.string.invalid_schedule_time), Toast.LENGTH_SHORT)
                    .show()
            return
        }

        getSharedPreferences("PolyHomePrefs", MODE_PRIVATE).edit().apply {
            putInt("selected_house_id", selectedHouse?.houseId ?: -1)
            apply()
        }

        lifecycleScope.launch(Dispatchers.IO) {
            val commands =
                    selectedPeripherals.map { peripheral ->
                        ScheduleCommand(
                                peripheralId = peripheral.id,
                                peripheralType = peripheral.type,
                                command = determineCommand(peripheral)
                        )
                    }

            val schedule =
                    Schedule(
                            dateTime = scheduleDateTime,
                            houseId = selectedHouse?.houseId ?: -1,
                            commands = commands,
                            recurringDays = getSelectedDays()
                    )

            dbHelper.insertSchedule(schedule)

            val selectedDays = getSelectedDays()
            if (selectedDays.isEmpty()) {
                selectedPeripherals.forEach { peripheral ->
                    scheduleCommand(peripheral, scheduleTimeInMillis, true)
                }
            } else {
                selectedPeripherals.forEach { peripheral ->
                    scheduleRecurringCommand(peripheral, scheduleTimeInMillis, selectedDays)
                }
            }

            withContext(Dispatchers.Main) {
                val intent = Intent(this@SchedulesActivity, SchedulesListActivity::class.java)
                intent.flags = Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_NEW_TASK
                startActivity(intent)
                finish()
            }
        }
    }

    private fun resetFields() {
        selectedDate = null
        selectedTime = null
        peripherals.forEach {
            it.power = 0
            it.opening = null
        }
        updatePeripheralsList()
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

    private fun scheduleCommand(
            peripheral: Peripheral,
            timeInMillis: Long,
            isLastCommand: Boolean
    ) {
        val command = determineCommand(peripheral)
        Log.d(
                TAG,
                "Scheduling command: $command for peripheral: ${peripheral.id} at time: $timeInMillis"
        )

        val intent =
                Intent(this, ScheduleReceiver::class.java).apply {
                    putExtra("peripheralId", peripheral.id)
                    putExtra("command", command)
                    putExtra("isLastCommand", isLastCommand)
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

    private fun getSelectedDays(): List<Int> {
        return dayToggles.mapIndexedNotNull { index, toggle ->
            if (toggle.isChecked) index + 1 else null
        }
    }

    private fun getSelectedDateTime(): String? {
        val calendar = Calendar.getInstance()
        val currentHour = calendar.get(Calendar.HOUR_OF_DAY)
        val currentMinute = calendar.get(Calendar.MINUTE)

        if (selectedDate == null) {
            val selectedHour = hourPicker.value
            val selectedMinute = minutePicker.value

            if (selectedHour < currentHour ||
                            (selectedHour == currentHour && selectedMinute <= currentMinute)
            ) {
                calendar.add(Calendar.DAY_OF_MONTH, 1)
            }

            selectedDate = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(calendar.time)
        }

        val hour = String.format("%02d", hourPicker.value)
        val minute = String.format("%02d", minutePicker.value)
        return "$selectedDate $hour:$minute"
    }

    private fun showCalendarDialog() {
        val dialog = Dialog(this)
        dialog.setContentView(R.layout.dialog_calendar)

        val calendarView = dialog.findViewById<CalendarView>(R.id.calendarView)
        calendarView.apply {
            minDate = System.currentTimeMillis()
            setOnDateChangeListener { _, year, month, dayOfMonth ->
                val calendar = Calendar.getInstance()
                calendar.set(year, month, dayOfMonth)
                selectedDate =
                        SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(calendar.time)
                calendarButton.text = selectedDate
                dialog.dismiss()
            }
        }

        dialog.show()
    }

    private fun scheduleRecurringCommand(
            peripheral: Peripheral,
            baseTimeInMillis: Long,
            days: List<Int>
    ) {
        val command = determineCommand(peripheral)
        val calendar = Calendar.getInstance().apply { timeInMillis = baseTimeInMillis }

        days.forEach { dayOfWeek ->
            val currentDay = calendar.get(Calendar.DAY_OF_WEEK)
            var daysToAdd = dayOfWeek - currentDay
            if (daysToAdd <= 0) daysToAdd += 7

            val adjustedCalendar =
                    Calendar.getInstance().apply {
                        timeInMillis = baseTimeInMillis
                        add(Calendar.DAY_OF_MONTH, daysToAdd)
                    }

            val intent =
                    Intent(this, ScheduleReceiver::class.java).apply {
                        putExtra("peripheralId", peripheral.id)
                        putExtra("command", command)
                        putExtra("recurring", true)
                        putExtra("dayOfWeek", dayOfWeek)
                        data = Uri.parse("custom://${peripheral.id}/$dayOfWeek")
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
                            "${peripheral.id}$dayOfWeek".hashCode(),
                            intent,
                            flags
                    )

            val alarmManager = getSystemService(Context.ALARM_SERVICE) as AlarmManager
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                alarmManager.setRepeating(
                        AlarmManager.RTC_WAKEUP,
                        adjustedCalendar.timeInMillis,
                        AlarmManager.INTERVAL_DAY * 7,
                        pendingIntent
                )
            } else {
                alarmManager.setRepeating(
                        AlarmManager.RTC_WAKEUP,
                        adjustedCalendar.timeInMillis,
                        AlarmManager.INTERVAL_DAY * 7,
                        pendingIntent
                )
            }
        }
    }

    private fun updatePeripheralsList() {
        peripheralsContainer.removeAllViews()
        peripherals.forEach { peripheral ->
            val view =
                    LayoutInflater.from(this)
                            .inflate(
                                    R.layout.schedule_peripheral_list_item,
                                    peripheralsContainer,
                                    false
                            )

            val idText = view.findViewById<TextView>(R.id.peripheralIdTextView)
            val controlsContainer = view.findViewById<LinearLayout>(R.id.commandsContainer)

            idText.text = peripheral.id

            when (peripheral.type.lowercase()) {
                "light" -> configureLightToggleForSchedule(controlsContainer, peripheral)
                "rolling shutter", "garage door" ->
                        configureShutterAndDoorButtonsForSchedule(controlsContainer, peripheral)
            }

            peripheralsContainer.addView(view)
        }
    }

    private fun configureLightToggleForSchedule(container: LinearLayout, peripheral: Peripheral) {
        val lightToggleButton =
                ImageButton(this).apply {
                    layoutParams =
                            LinearLayout.LayoutParams(
                                            LinearLayout.LayoutParams.WRAP_CONTENT,
                                            LinearLayout.LayoutParams.WRAP_CONTENT
                                    )
                                    .apply { setMargins(8, 8, 8, 8) }
                    setPadding(16, 16, 16, 16)
                    setBackgroundResource(android.R.color.transparent)
                    setImageResource(
                            if (peripheral.power == 1) R.drawable.ic_light_on
                            else R.drawable.ic_light_off
                    )

                    setOnClickListener {
                        peripheral.power =
                                when (peripheral.power) {
                                    1 -> -1 // ON -> OFF
                                    -1 -> 0 // OFF -> unselected
                                    else -> 1 // unselected -> ON
                                }
                        setImageResource(
                                when (peripheral.power) {
                                    1 -> R.drawable.ic_light_on
                                    -1 -> R.drawable.ic_light_off
                                    else -> R.drawable.ic_light_off
                                }
                        )
                    }
                }
        container.addView(lightToggleButton)
    }

    private fun configureShutterAndDoorButtonsForSchedule(
            container: LinearLayout,
            peripheral: Peripheral
    ) {
        val openButton =
                createStyledButton(getString(R.string.open)).apply {
                    setOnClickListener {
                        peripheral.opening = 100
                        updateButtonStates(container, this)
                    }
                }

        val closeButton =
                createStyledButton(getString(R.string.close)).apply {
                    setOnClickListener {
                        peripheral.opening = 0
                        updateButtonStates(container, this)
                    }
                }

        // Set initial state if exists
        when (peripheral.opening) {
            100 -> updateButtonStates(container, openButton)
            0 -> updateButtonStates(container, closeButton)
        }

        container.apply {
            addView(openButton)
            addView(closeButton)
        }
    }

    private fun createStyledButton(text: String): Button {
        return Button(this).apply {
            layoutParams =
                    LinearLayout.LayoutParams(
                                    0, // Width will be 0 with weight for equal sizing
                                    LinearLayout.LayoutParams.WRAP_CONTENT
                            )
                            .apply {
                                weight = 1f // Equal width for both buttons
                                marginStart = 8.dpToPx()
                                marginEnd = 8.dpToPx()
                            }
            this.text = text
            setBackgroundResource(R.drawable.button_background_secondary)
            elevation = 0f // Remove button shadow
            textSize = 14f
            minimumHeight = 48.dpToPx()
        }
    }

    private fun updateButtonStates(container: LinearLayout, selectedButton: Button) {
        selectedButton.setBackgroundResource(R.drawable.button_background)
        for (i in 0 until container.childCount) {
            val button = container.getChildAt(i) as? Button
            if (button != selectedButton) {
                button?.setBackgroundResource(R.drawable.button_background_secondary)
            }
        }
    }

    private fun Int.dpToPx(): Int {
        return (this * resources.displayMetrics.density).toInt()
    }
}
