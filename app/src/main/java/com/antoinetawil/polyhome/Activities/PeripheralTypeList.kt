package com.antoinetawil.polyhome.Activities

import android.content.Intent
import android.os.Bundle
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.antoinetawil.polyhome.Adapters.PeripheralTypeAdapter
import com.antoinetawil.polyhome.R
import com.antoinetawil.polyhome.Utils.HeaderUtils

class PeripheralTypeListActivity : AppCompatActivity() {

    private lateinit var recyclerView: RecyclerView
    private val peripheralTypes = mutableListOf<String>()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_peripheral_type_list)

        HeaderUtils.setupHeader(this)

        recyclerView = findViewById(R.id.recyclerView)
        recyclerView.layoutManager = LinearLayoutManager(this)

        val houseId = intent.getIntExtra("houseId", -1)
        val availableTypes = intent.getStringArrayListExtra("availableTypes")

        if (houseId == -1 || availableTypes == null) {
            Toast.makeText(this, "Invalid house data", Toast.LENGTH_SHORT).show()
            finish()
            return
        }

        peripheralTypes.clear()
        peripheralTypes.addAll(availableTypes)

        val adapter = PeripheralTypeAdapter(peripheralTypes) { selectedType ->
            val intent = Intent(this, PeripheralListActivity::class.java)
            intent.putExtra("houseId", houseId)
            intent.putExtra("type", selectedType)
            startActivity(intent)
        }
        recyclerView.adapter = adapter
    }
}
