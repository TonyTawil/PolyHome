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
import com.antoinetawil.polyhome.Adapters.HouseUsersAdapter
import com.antoinetawil.polyhome.Models.House
import com.antoinetawil.polyhome.R
import com.antoinetawil.polyhome.Utils.Api
import com.antoinetawil.polyhome.Utils.HeaderUtils
import com.google.android.material.floatingactionbutton.FloatingActionButton

data class User(val login: String)

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
        adapter =
                HouseListAdapter(
                        filteredHouses,
                        this,
                        onManagePermission = { house, view -> showUsersPopup(house, view) },
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
                        filterHouses(s.toString())
                    }

                    override fun afterTextChanged(s: Editable?) {}
                }
        )
    }

    private fun filterHouses(query: String) {
        filteredHouses.clear()
        if (query.isEmpty()) {
            filteredHouses.addAll(houses)
        } else {
            filteredHouses.addAll(
                    houses.filter { it.houseId.toString().contains(query, ignoreCase = true) }
            )
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
                                val types =
                                        devices
                                                .mapNotNull { device ->
                                                    val id = device["id"] as? String
                                                    id?.split(" ")?.firstOrNull()
                                                }
                                                .distinct()
                                navigateToPeripheralTypeList(houseId, types)
                            } else {
                                Log.e(TAG, getString(R.string.invalid_response))
                                Toast.makeText(
                                                this,
                                                getString(R.string.invalid_response),
                                                Toast.LENGTH_SHORT
                                        )
                                        .show()
                            }
                        } else {
                            Log.e(TAG, getString(R.string.failed_to_fetch_peripherals))
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

    private fun navigateToPeripheralTypeList(houseId: Int, types: List<String>) {
        val intent = Intent(this, PeripheralTypeListActivity::class.java)
        intent.putExtra("houseId", houseId)
        intent.putStringArrayListExtra("availableTypes", ArrayList(types))
        startActivity(intent)
    }

    private fun showUsersPopup(house: House, anchorView: View) {
        val popupView = LayoutInflater.from(this).inflate(R.layout.users_list_popup, null)
        val popupWindow =
                PopupWindow(
                        popupView,
                        (resources.displayMetrics.widthPixels * 0.8).toInt(),
                        LinearLayout.LayoutParams.WRAP_CONTENT,
                        true
                )

        val usersRecyclerView = popupView.findViewById<RecyclerView>(R.id.usersRecyclerView)
        val emailEditText = popupView.findViewById<AutoCompleteTextView>(R.id.emailEditText)
        val addUserButton = popupView.findViewById<FloatingActionButton>(R.id.addUserButton)
        val addUserSection = popupView.findViewById<View>(R.id.addUserSection)

        // Only show add user section if current user is the owner
        addUserSection.visibility = if (house.owner) View.VISIBLE else View.GONE

        usersRecyclerView.layoutManager = LinearLayoutManager(this)

        // Create a mutable property to hold the adapter reference
        lateinit var usersAdapter: HouseUsersAdapter

        // Initialize the adapter with a reference to itself
        usersAdapter =
                HouseUsersAdapter(
                        users = mutableListOf(),
                        isOwner = house.owner,
                        onRemoveUser = { userLogin ->
                            removeUserAccess(house.houseId, userLogin, usersAdapter, emailEditText)
                        }
                )

        // Set the adapter
        usersRecyclerView.adapter = usersAdapter

        // Fetch and display users
        fetchHouseUsers(house.houseId) { currentUsers -> 
            usersAdapter.updateUsers(currentUsers)
            
            // Fetch all users for autocomplete, excluding current users
            fetchAllUsers(currentUsers.map { it.userLogin }) { availableUsers ->
                val adapter = ArrayAdapter(
                    this,
                    R.layout.user_dropdown_item,
                    availableUsers.map { it.login }
                )
                emailEditText.setAdapter(adapter)
            }
        }

        addUserButton.setOnClickListener {
            val email = emailEditText.text.toString()
            if (email.isNotEmpty()) {
                giveUserAccess(house.houseId, email, usersAdapter) { success ->
                    if (success) {
                        emailEditText.text.clear()
                        // Refresh the autocomplete list after successful addition
                        fetchHouseUsers(house.houseId) { currentUsers ->
                            fetchAllUsers(currentUsers.map { it.userLogin }) { availableUsers ->
                                val adapter = ArrayAdapter(
                                    this,
                                    R.layout.user_dropdown_item,
                                    availableUsers.map { it.login }
                                )
                                emailEditText.setAdapter(adapter)
                            }
                        }
                    }
                }
            } else {
                Toast.makeText(this, getString(R.string.enter_email), Toast.LENGTH_SHORT).show()
            }
        }

        popupWindow.elevation = 8f
        popupWindow.animationStyle = android.R.style.Animation_Dialog
        popupWindow.showAsDropDown(anchorView)
    }

    private fun fetchHouseUsers(
            houseId: Int,
            onSuccess: (List<HouseUsersAdapter.HouseUser>) -> Unit
    ) {
        val url = "https://polyhome.lesmoulinsdudev.com/api/houses/$houseId/users"
        val token =
                getSharedPreferences("PolyHomePrefs", MODE_PRIVATE).getString("auth_token", null)

        if (token != null) {
            api.get<List<HouseUsersAdapter.HouseUser>>(
                    path = url,
                    securityToken = token,
                    onSuccess = { responseCode, response ->
                        if (responseCode == 200 && response != null) {
                            runOnUiThread { onSuccess(response) }
                        } else {
                            runOnUiThread {
                                Toast.makeText(
                                                this,
                                                getString(R.string.failed_to_fetch_users),
                                                Toast.LENGTH_SHORT
                                        )
                                        .show()
                            }
                        }
                    }
            )
        }
    }

    private fun giveUserAccess(
            houseId: Int, 
            email: String, 
            adapter: HouseUsersAdapter,
            onComplete: (Boolean) -> Unit
    ) {
        val url = "https://polyhome.lesmoulinsdudev.com/api/houses/$houseId/users"
        val token = getSharedPreferences("PolyHomePrefs", MODE_PRIVATE).getString("auth_token", null)

        if (token != null) {
            api.post<Map<String, String>, Unit>(
                    path = url,
                    data = mapOf("userLogin" to email),
                    securityToken = token,
                    onSuccess = { responseCode, _ ->
                        runOnUiThread {
                            if (responseCode == 200) {
                                Toast.makeText(
                                                this,
                                                getString(R.string.access_granted),
                                                Toast.LENGTH_SHORT
                                        )
                                        .show()

                                // Add the new user to the list immediately
                                adapter.addUser(HouseUsersAdapter.HouseUser(email, 0))

                                // Also refresh the full list in case of any changes
                                fetchHouseUsers(houseId) { users -> adapter.updateUsers(users) }
                                
                                onComplete(true)
                            } else {
                                Toast.makeText(
                                                this,
                                                getString(R.string.failed_to_grant_access),
                                                Toast.LENGTH_SHORT
                                        )
                                        .show()
                                onComplete(false)
                            }
                        }
                    }
            )
        }
    }

    private fun removeUserAccess(
            houseId: Int, 
            userLogin: String, 
            adapter: HouseUsersAdapter,
            emailEditText: AutoCompleteTextView
    ) {
        val url = "https://polyhome.lesmoulinsdudev.com/api/houses/$houseId/users"
        val token =
                getSharedPreferences("PolyHomePrefs", MODE_PRIVATE).getString("auth_token", null)

        if (token != null) {
            // Remove the user immediately from the list before making the API call
            adapter.removeUser(userLogin)

            api.delete(
                    path = url,
                    data = mapOf("userLogin" to userLogin),
                    securityToken = token,
                    onSuccess = { responseCode ->
                        runOnUiThread {
                            if (responseCode == 200) {
                                Toast.makeText(
                                                this,
                                                getString(R.string.access_removed),
                                                Toast.LENGTH_SHORT
                                        )
                                        .show()

                                // Refresh the autocomplete list after successful removal
                                fetchHouseUsers(houseId) { currentUsers ->
                                    fetchAllUsers(currentUsers.map { it.userLogin }) { availableUsers ->
                                        val adapter = ArrayAdapter(
                                            this,
                                            R.layout.user_dropdown_item,
                                            availableUsers.map { it.login }
                                        )
                                        emailEditText.setAdapter(adapter)
                                    }
                                }
                            } else {
                                Toast.makeText(
                                                this,
                                                getString(R.string.failed_to_remove_access),
                                                Toast.LENGTH_SHORT
                                        )
                                        .show()
                                // If the API call failed, refresh the list to restore the original
                                // state
                                fetchHouseUsers(houseId) { users -> adapter.updateUsers(users) }
                            }
                        }
                    }
            )
        }
    }

    private fun fetchAllUsers(excludeUsers: List<String>, onSuccess: (List<User>) -> Unit) {
        val url = "https://polyhome.lesmoulinsdudev.com/api/users"
        val token = getSharedPreferences("PolyHomePrefs", MODE_PRIVATE).getString("auth_token", null)
        val currentUserLogin = getSharedPreferences("PolyHomePrefs", MODE_PRIVATE).getString("user_login", null)

        if (token != null) {
            api.get<List<User>>(
                path = url,
                securityToken = token,
                onSuccess = { responseCode, response ->
                    if (responseCode == 200 && response != null) {
                        // Filter out current user and users who already have access
                        val filteredUsers = response.filter { user ->
                            user.login != currentUserLogin && !excludeUsers.contains(user.login)
                        }
                        runOnUiThread { onSuccess(filteredUsers) }
                    } else {
                        runOnUiThread {
                            Toast.makeText(
                                this,
                                getString(R.string.failed_to_fetch_users),
                                Toast.LENGTH_SHORT
                            ).show()
                        }
                    }
                }
            )
        }
    }
}
