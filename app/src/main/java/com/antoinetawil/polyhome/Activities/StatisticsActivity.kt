package com.antoinetawil.polyhome.Activities

import android.content.Context
import android.os.Bundle
import android.widget.ProgressBar
import android.widget.TextView
import android.widget.Toast
import androidx.drawerlayout.widget.DrawerLayout
import com.antoinetawil.polyhome.Models.House
import com.antoinetawil.polyhome.R
import com.antoinetawil.polyhome.Utils.Api
import com.antoinetawil.polyhome.Utils.BaseActivity
import com.antoinetawil.polyhome.Utils.HeaderUtils

class StatisticsActivity : BaseActivity() {

    private lateinit var drawerLayout: DrawerLayout
    private val api = Api()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_statistics)

        // Set up the drawer and header
        drawerLayout = findViewById(R.id.drawer_layout)
        HeaderUtils.setupHeaderWithDrawer(this, drawerLayout)

        // Populate mock data
        setupMockStatistics()

        // Fetch and display real house count
        fetchAndDisplayHouseCount()
    }

    private fun setupMockStatistics() {
        // Setting up statistics with mock values
        findViewById<TextView>(R.id.totalHousesValue).text = "--" // Placeholder for house count
        findViewById<TextView>(R.id.totalDevicesValue).text = "25" // Example mock data
        findViewById<TextView>(R.id.dailyCO2Value).text = "15.7 kg" // Mock CO2 data
        findViewById<ProgressBar>(R.id.dailyCO2Progress).progress = 40 // Mock progress for CO2 usage
        findViewById<ProgressBar>(R.id.efficiencyProgress).progress = 92 // Mock efficiency percentage
    }

    private fun fetchAndDisplayHouseCount() {
        val sharedPreferences = getSharedPreferences("PolyHomePrefs", Context.MODE_PRIVATE)
        val token = sharedPreferences.getString("auth_token", null)

        if (token != null) {
            val url = "https://polyhome.lesmoulinsdudev.com/api/houses"

            api.get<List<House>>(
                path = url,
                securityToken = token,
                onSuccess = { responseCode, response ->
                    runOnUiThread {
                        if (responseCode == 200 && response != null) {
                            // Update the total houses value
                            val houseCount = response.size
                            findViewById<TextView>(R.id.totalHousesValue).text = houseCount.toString()
                        } else {
                            // Show an error message
                            findViewById<TextView>(R.id.totalHousesValue).text = "0"
                            Toast.makeText(
                                this,
                                getString(R.string.failed_to_fetch_houses),
                                Toast.LENGTH_SHORT
                            ).show()
                        }
                    }
                }
            )
        } else {
            // Show an error message if token is missing
            Toast.makeText(this, getString(R.string.auth_token_missing), Toast.LENGTH_SHORT).show()
        }
    }
}
