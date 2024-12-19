package com.antoinetawil.polyhome.Activities

import android.content.Context
import android.os.Bundle
import android.view.View
import android.widget.AdapterView
import android.widget.ArrayAdapter
import android.widget.Spinner
import android.widget.TextView
import android.widget.Toast
import androidx.drawerlayout.widget.DrawerLayout
import com.antoinetawil.polyhome.Models.House
import com.antoinetawil.polyhome.R
import com.antoinetawil.polyhome.Utils.Api
import com.antoinetawil.polyhome.Utils.HeaderUtils
import com.antoinetawil.polyhome.Utils.StatisticsTranslator
import com.google.android.material.progressindicator.LinearProgressIndicator

class StatisticsActivity : BaseActivity() {

    private lateinit var drawerLayout: DrawerLayout
    private val api = Api()
    private var totalDevices = 0
    private var totalCO2 = 0.0
    private var avgEfficiency = 0
    private var activeLights = 0
    private var activeShutters = 0
    private var totalLights = 0
    private var totalShutters = 0
    private lateinit var houseSpinner: Spinner
    private var houses: List<House> = emptyList()
    private var selectedHouseId: Int? = null
    private lateinit var translator: StatisticsTranslator

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_statistics)

        translator = StatisticsTranslator(this)

        drawerLayout = findViewById(R.id.drawer_layout)
        HeaderUtils.setupHeaderWithDrawer(this, drawerLayout)

        houseSpinner = findViewById(R.id.houseSpinner)
        setupHouseSpinner()

        initializeViews()
        fetchHouses()
    }

    private fun initializeViews() {
        findViewById<TextView>(R.id.totalHousesValue).text = getString(R.string.loading)
        findViewById<TextView>(R.id.totalDevicesValue).text = getString(R.string.loading)
        findViewById<TextView>(R.id.dailyCO2Value).text = getString(R.string.loading)
        findViewById<TextView>(R.id.activeLightsValue).text = getString(R.string.loading)
        findViewById<TextView>(R.id.activeShuttersValue).text = getString(R.string.loading)
        findViewById<LinearProgressIndicator>(R.id.dailyCO2Progress).progress = 0
        findViewById<LinearProgressIndicator>(R.id.efficiencyProgress).progress = 0
    }

    private fun setupHouseSpinner() {
        houseSpinner.onItemSelectedListener =
                object : AdapterView.OnItemSelectedListener {
                    override fun onItemSelected(
                            parent: AdapterView<*>?,
                            view: View?,
                            position: Int,
                            id: Long
                    ) {
                        if (position == 0) {
                            selectedHouseId = null
                        } else {
                            selectedHouseId = houses[position - 1].houseId
                        }
                        fetchStatistics()
                    }

                    override fun onNothingSelected(parent: AdapterView<*>?) {
                        selectedHouseId = null
                    }
                }
    }

    private fun fetchHouses() {
        val sharedPreferences = getSharedPreferences("PolyHomePrefs", Context.MODE_PRIVATE)
        val token = sharedPreferences.getString("auth_token", null)

        if (token != null) {
            api.get<List<House>>(
                    path = "https://polyhome.lesmoulinsdudev.com/api/houses",
                    securityToken = token,
                    onSuccess = { responseCode, response ->
                        if (responseCode == 200 && response != null) {
                            houses = response
                            runOnUiThread { updateHouseSpinner() }
                        } else {
                            showError(getString(R.string.failed_to_fetch_houses))
                        }
                    }
            )
        } else {
            Toast.makeText(this, getString(R.string.auth_token_missing), Toast.LENGTH_SHORT).show()
        }
    }

    private fun updateHouseSpinner() {
        val houseNames = mutableListOf(getString(R.string.all_houses))
        houseNames.addAll(houses.map { translator.getHouseLabel(it.houseId) })

        val adapter =
                ArrayAdapter(this, android.R.layout.simple_spinner_item, houseNames).apply {
                    setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
                }

        houseSpinner.adapter = adapter
    }

    private fun fetchStatistics() {
        resetCounters()
        val sharedPreferences = getSharedPreferences("PolyHomePrefs", Context.MODE_PRIVATE)
        val token = sharedPreferences.getString("auth_token", null)

        if (token != null) {
            if (selectedHouseId != null) {
                // Fetch statistics for selected house
                fetchDevicesForHouse(token, selectedHouseId!!) { devices ->
                    processDevices(devices, 1)
                }
            } else {
                // Fetch statistics for all houses
                fetchHousesAndDevices(token)
            }
        } else {
            Toast.makeText(this, getString(R.string.auth_token_missing), Toast.LENGTH_SHORT).show()
        }
    }

    private fun resetCounters() {
        activeLights = 0
        activeShutters = 0
        totalLights = 0
        totalShutters = 0
    }

    private fun processDevices(devices: List<Device>?, houseCount: Int) {
        var deviceCount = 0
        var totalPower = 0.0
        var activeDevices = 0

        devices?.forEach { device ->
            deviceCount++

            when {
                device.id.startsWith("Light", ignoreCase = true) -> {
                    totalLights++
                    if (device.power == 1) {
                        activeLights++
                        activeDevices++
                        totalPower += 0.1
                    }
                }
                device.id.startsWith("Shutter", ignoreCase = true) -> {
                    totalShutters++
                    if (device.isShutterOpen) {
                        activeShutters++
                        activeDevices++
                        // Calculate power based on opening percentage
                        totalPower += 0.2 * (device.shutterOpeningPercentage / 100.0)
                    }
                }
            }
        }

        updateStatistics(
                houseCount,
                deviceCount,
                totalPower,
                calculateEfficiency(activeDevices, deviceCount)
        )
    }

    private fun fetchDevicesForHouse(
            token: String,
            houseId: Int,
            onComplete: (List<Device>?) -> Unit
    ) {
        api.get<Map<String, List<Device>>>(
                path = "https://polyhome.lesmoulinsdudev.com/api/houses/$houseId/devices",
                securityToken = token,
                onSuccess = { responseCode, response ->
                    if (responseCode == 200 && response != null) {
                        onComplete(response["devices"])
                    } else {
                        onComplete(null)
                    }
                }
        )
    }

    private fun calculateEfficiency(activeDevices: Int, totalDevices: Int): Int {
        if (totalDevices == 0) return 100
        // Higher efficiency means fewer devices are active
        return ((1 - (activeDevices.toDouble() / totalDevices)) * 100).toInt()
    }

    private fun updateStatistics(
            houseCount: Int,
            deviceCount: Int,
            dailyCO2: Double,
            efficiency: Int
    ) {
        runOnUiThread {
            findViewById<TextView>(R.id.totalHousesValue).apply {
                text = houseCount.toString()
                alpha = 0f
                animate().alpha(1f).setDuration(500).start()
            }

            findViewById<TextView>(R.id.totalDevicesValue).apply {
                text = deviceCount.toString()
                alpha = 0f
                animate().alpha(1f).setDuration(500).start()
            }

            findViewById<TextView>(R.id.activeLightsValue).apply {
                text = getString(R.string.shutter_status_format, activeLights, totalLights)
                alpha = 0f
                animate().alpha(1f).setDuration(500).start()
            }

            findViewById<TextView>(R.id.activeShuttersValue).apply {
                text = translator.formatShutterStatus(activeShutters, totalShutters)
                alpha = 0f
                animate().alpha(1f).setDuration(500).start()
            }

            findViewById<TextView>(R.id.dailyCO2Value).text = translator.formatDailyCO2(dailyCO2)

            findViewById<LinearProgressIndicator>(R.id.dailyCO2Progress).apply {
                setProgressCompat(((dailyCO2 / MAX_DAILY_CO2) * 100).toInt().coerceIn(0, 100), true)
            }

            findViewById<TextView>(R.id.efficiencyValue).text =
                    translator.formatEfficiency(efficiency)

            findViewById<LinearProgressIndicator>(R.id.efficiencyProgress).apply {
                setProgressCompat(efficiency, true)
            }
        }
    }

    private fun showError(message: String) {
        runOnUiThread {
            Toast.makeText(this, message, Toast.LENGTH_SHORT).show()
            findViewById<TextView>(R.id.totalHousesValue).text = "0"
            findViewById<TextView>(R.id.totalDevicesValue).text = "0"
            findViewById<TextView>(R.id.dailyCO2Value).text = translator.formatDailyCO2(0.0)
        }
    }

    private fun fetchHousesAndDevices(token: String) {
        api.get<List<House>>(
                path = "https://polyhome.lesmoulinsdudev.com/api/houses",
                securityToken = token,
                onSuccess = { responseCode, houses ->
                    if (responseCode == 200 && houses != null) {
                        houses.forEach { house ->
                            fetchDevicesForHouse(token, house.houseId) { devices ->
                                processDevices(devices, houses.size)
                            }
                        }
                    } else {
                        showError(getString(R.string.failed_to_fetch_houses))
                    }
                }
        )
    }

    companion object {
        private const val MAX_DAILY_CO2 = 50.0 // Maximum expected daily CO2 in kg
    }

    data class Device(
            val id: String,
            val power: Int = 0,
            val opening: Double = 0.0,
            val openingMode: Int = 0
    ) {
        val isShutterOpen: Boolean
            get() =
                    when (openingMode) {
                        0 -> opening == 1.0
                        1 -> opening != 0.0
                        2 -> opening > 0.0
                        else -> false
                    }

        val shutterOpeningPercentage: Int
            get() =
                    when (openingMode) {
                        0 -> if (opening == 1.0) 100 else 0
                        1 -> if (opening == 0.0) 0 else 100
                        2 -> (opening * 100).toInt()
                        else -> 0
                    }
    }
}
