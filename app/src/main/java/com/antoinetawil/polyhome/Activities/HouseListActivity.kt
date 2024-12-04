package com.antoinetawil.polyhome.Activities

import android.content.Context
import android.content.Intent
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
import com.antoinetawil.polyhome.Adapters.HouseListAdapter
import com.antoinetawil.polyhome.Models.House
import com.antoinetawil.polyhome.R
import com.antoinetawil.polyhome.Utils.Api
import com.antoinetawil.polyhome.Utils.BaseActivity
import com.antoinetawil.polyhome.Utils.HeaderUtils

class HouseListActivity : BaseActivity() {

    companion object {
        private const val TAG = "HouseListActivity"
    }

    private val api = Api()
    private lateinit var drawerLayout: DrawerLayout
    private lateinit var recyclerView: RecyclerView
    private lateinit var adapter: HouseListAdapter
    private lateinit var searchEditText: EditText
    private lateinit var searchPopup: PopupWindow
    private val houses = mutableListOf<House>()
    private val filteredHouses = mutableListOf<House>()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_house_list)

        initializeUI()
    }

    private fun initializeUI() {
        drawerLayout = findViewById(R.id.drawer_layout)
        HeaderUtils.setupHeaderWithDrawer(this, drawerLayout)

        findViewById<TextView>(R.id.titleTextView).text = getString(R.string.all_houses)

        recyclerView = findViewById(R.id.recyclerView)
        adapter = HouseListAdapter(filteredHouses, this,
            onManagePermission = { houseId, view -> showPermissionPopup(houseId, view) },
            onHouseSelected = { houseId -> fetchPeripheralTypes(houseId) }
        )
        recyclerView.layoutManager = LinearLayoutManager(this)
        recyclerView.adapter = adapter

        val sharedPreferences = getSharedPreferences("PolyHomePrefs", Context.MODE_PRIVATE)
        val token = sharedPreferences.getString("auth_token", null)

        if (token != null) {
            fetchHouseList(token)
        } else {
            Toast.makeText(this, getString(R.string.auth_token_missing), Toast.LENGTH_SHORT).show()
            Log.e(TAG, getString(R.string.auth_token_missing))
        }

        setupSearchPopup()
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

        searchEditText.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
                filterHouses(s.toString())
            }

            override fun afterTextChanged(s: Editable?) {}
        })
    }

    private fun filterHouses(query: String) {
        filteredHouses.clear()
        if (query.isEmpty()) {
            filteredHouses.addAll(houses)
        } else {
            filteredHouses.addAll(houses.filter { it.houseId.toString().contains(query, ignoreCase = true) })
        }
        adapter.notifyDataSetChanged()
    }

    private fun fetchHouseList(token: String) {
        val url = "https://polyhome.lesmoulinsdudev.com/api/houses"

        api.get<List<House>>(
            path = url,
            securityToken = token,
            onSuccess = { responseCode, response ->
                runOnUiThread {
                    if (responseCode == 200 && response != null) {
                        Log.d(TAG, "Fetched house list successfully.")
                        houses.clear()
                        houses.addAll(response)
                        filteredHouses.clear()
                        filteredHouses.addAll(houses)
                        adapter.notifyDataSetChanged()
                    } else {
                        Log.e(TAG, getString(R.string.failed_to_fetch_houses))
                        Toast.makeText(this, getString(R.string.failed_to_fetch_houses), Toast.LENGTH_SHORT).show()
                    }
                }
            }
        )
    }

    private fun fetchPeripheralTypes(houseId: Int) {
        val url = "https://polyhome.lesmoulinsdudev.com/api/houses/$houseId/devices"
        Log.d(TAG, "Fetching peripheral types for houseId=$houseId with URL=$url")

        val sharedPreferences = getSharedPreferences("PolyHomePrefs", Context.MODE_PRIVATE)
        val token = sharedPreferences.getString("auth_token", null)

        if (token == null) {
            Log.e(TAG, getString(R.string.auth_token_missing))
            Toast.makeText(this, getString(R.string.auth_token_missing), Toast.LENGTH_SHORT).show()
            return
        }

        api.get<Map<String, Any>>(
            path = url,
            securityToken = token,
            onSuccess = { responseCode, response ->
                runOnUiThread {
                    if (responseCode == 200 && response != null) {
                        val devices = response["devices"] as? List<Map<String, Any>>
                        if (devices != null) {
                            val types = devices.mapNotNull { device ->
                                val id = device["id"] as? String
                                id?.split(" ")?.firstOrNull()
                            }.distinct()
                            navigateToPeripheralTypeList(houseId, types)
                        } else {
                            Log.e(TAG, getString(R.string.invalid_response))
                            Toast.makeText(this, getString(R.string.invalid_response), Toast.LENGTH_SHORT).show()
                        }
                    } else {
                        Log.e(TAG, getString(R.string.failed_to_fetch_peripherals))
                        Toast.makeText(this, getString(R.string.failed_to_fetch_peripherals), Toast.LENGTH_SHORT).show()
                    }
                }
            }
        )
    }

    private fun navigateToPeripheralTypeList(houseId: Int, types: List<String>) {
        val intent = Intent(this, PeripheralTypeListActivity::class.java)
        intent.putExtra("houseId", houseId)
        intent.putStringArrayListExtra("availableTypes", ArrayList(types))
        startActivity(intent)
    }

    private fun showPermissionPopup(houseId: Int, anchorView: View) {
        Log.d(TAG, "Displaying permission popup for houseId=$houseId") // Add log for debugging

        val popupView = LayoutInflater.from(this).inflate(R.layout.permission_popup, null)

        val popupWindow = PopupWindow(
            popupView,
            (resources.displayMetrics.widthPixels * 0.7).toInt(), // Ensure proper width
            LinearLayout.LayoutParams.WRAP_CONTENT,
            true
        )

        val emailEditText = popupView.findViewById<EditText>(R.id.emailEditText)
        val givePermissionButton = popupView.findViewById<Button>(R.id.givePermissionButton)
        val removePermissionButton = popupView.findViewById<Button>(R.id.removePermissionButton)

        givePermissionButton.setOnClickListener {
            val email = emailEditText.text.toString()
            if (email.isNotEmpty()) {
                managePermission(houseId, email, true)
                popupWindow.dismiss()
            } else {
                Toast.makeText(this, getString(R.string.enter_email), Toast.LENGTH_SHORT).show()
            }
        }

        removePermissionButton.setOnClickListener {
            val email = emailEditText.text.toString()
            if (email.isNotEmpty()) {
                managePermission(houseId, email, false)
                popupWindow.dismiss()
            } else {
                Toast.makeText(this, getString(R.string.enter_email), Toast.LENGTH_SHORT).show()
            }
        }

        // Set popup elevation and animation
        popupWindow.elevation = 8f
        popupWindow.animationStyle = android.R.style.Animation_Dialog

        // Show the popup below the anchor view
        popupWindow.showAsDropDown(anchorView, 50, 80)

        Log.d(TAG, "Permission popup displayed") // Add log for debugging
    }


    private fun managePermission(houseId: Int, email: String, isGrant: Boolean) {
        val url = "https://polyhome.lesmoulinsdudev.com/api/houses/$houseId/users"
        val sharedPreferences = getSharedPreferences("PolyHomePrefs", MODE_PRIVATE)
        val token = sharedPreferences.getString("auth_token", null)

        if (token != null) {
            val requestData = mapOf("userLogin" to email)

            if (isGrant) {
                api.post<Map<String, String>>(
                    path = url,
                    data = requestData,
                    securityToken = token,
                    onSuccess = { responseCode ->
                        handlePermissionResponse(responseCode, isGrant)
                    }
                )
            } else {
                api.delete<Map<String, String>>(
                    path = url,
                    data = requestData,
                    securityToken = token,
                    onSuccess = { responseCode ->
                        handlePermissionResponse(responseCode, isGrant)
                    }
                )
            }
        } else {
            Toast.makeText(this, getString(R.string.auth_token_missing), Toast.LENGTH_SHORT).show()
        }
    }

    private fun handlePermissionResponse(responseCode: Int, isGrant: Boolean) {
        runOnUiThread {
            if (responseCode == 200) {
                Toast.makeText(
                    this,
                    getString(if (isGrant) R.string.permission_granted else R.string.permission_removed),
                    Toast.LENGTH_SHORT
                ).show()
            } else {
                Toast.makeText(
                    this,
                    getString(R.string.failed_permission),
                    Toast.LENGTH_LONG
                ).show()
            }
        }
    }
}
