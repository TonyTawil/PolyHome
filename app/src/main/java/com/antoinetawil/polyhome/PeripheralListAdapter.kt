package com.antoinetawil.polyhome

import android.content.Context
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast
import androidx.recyclerview.widget.RecyclerView
import okhttp3.*
import org.json.JSONObject
import java.io.IOException
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.RequestBody.Companion.toRequestBody

class PeripheralListAdapter(
    private val peripheralList: List<Peripheral>,
    private val context: Context
) : RecyclerView.Adapter<PeripheralListAdapter.PeripheralViewHolder>() {

    class PeripheralViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val peripheralIdTextView: TextView = itemView.findViewById(R.id.peripheralIdTextView)
        val peripheralTypeTextView: TextView = itemView.findViewById(R.id.peripheralTypeTextView)
        val commandsContainer: LinearLayout = itemView.findViewById(R.id.commandsContainer)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): PeripheralViewHolder {
        val view = LayoutInflater.from(parent.context).inflate(R.layout.peripheral_list_item, parent, false)
        return PeripheralViewHolder(view)
    }

    override fun onBindViewHolder(holder: PeripheralViewHolder, position: Int) {
        val peripheral = peripheralList[position]

        holder.peripheralIdTextView.text = "ID: ${peripheral.id}"
        holder.peripheralTypeTextView.text = "Type: ${peripheral.type}"

        // Clear any existing views in the container
        holder.commandsContainer.removeAllViews()

        // Dynamically add buttons for available commands
        for (command in peripheral.availableCommands) {
            val button = Button(context).apply {
                text = command
                setOnClickListener {
                    sendCommandToPeripheral(peripheral.id, command)
                }
            }
            holder.commandsContainer.addView(button)
        }
    }

    override fun getItemCount(): Int = peripheralList.size

    private fun sendCommandToPeripheral(deviceId: String, command: String) {
        val sharedPreferences = context.getSharedPreferences("PolyHomePrefs", Context.MODE_PRIVATE)
        val token = sharedPreferences.getString("auth_token", null)
        val houseId = (context as PeripheralListActivity).intent.getIntExtra("houseId", -1)

        if (token != null && houseId != -1) {
            val url = "https://polyhome.lesmoulinsdudev.com/api/houses/$houseId/devices/$deviceId/command"
            val client = OkHttpClient()

            val jsonObject = JSONObject()
            jsonObject.put("command", command)

            val requestBody = jsonObject.toString().toRequestBody("application/json".toMediaTypeOrNull())

            val request = Request.Builder()
                .url(url)
                .post(requestBody)
                .addHeader("Authorization", "Bearer $token")
                .build()

            client.newCall(request).enqueue(object : Callback {
                override fun onFailure(call: Call, e: IOException) {
                    (context as PeripheralListActivity).runOnUiThread {
                        Toast.makeText(context, "Failed to send command: ${e.message}", Toast.LENGTH_LONG).show()
                    }
                }

                override fun onResponse(call: Call, response: Response) {
                    if (response.isSuccessful) {
                        (context as PeripheralListActivity).runOnUiThread {
                            Toast.makeText(context, "Command $command sent successfully!", Toast.LENGTH_SHORT).show()
                        }
                    } else {
                        (context as PeripheralListActivity).runOnUiThread {
                            Toast.makeText(context, "Failed to send command: ${response.code}", Toast.LENGTH_LONG).show()
                        }
                    }
                }
            })
        } else {
            Toast.makeText(context, "Authentication error. Please log in again.", Toast.LENGTH_SHORT).show()
        }
    }
}
