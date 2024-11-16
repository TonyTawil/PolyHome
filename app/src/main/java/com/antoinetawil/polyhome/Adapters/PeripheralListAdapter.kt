package com.antoinetawil.polyhome.Adapters

import android.content.Context
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageButton
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast
import androidx.recyclerview.widget.RecyclerView
import com.antoinetawil.polyhome.Activities.PeripheralListActivity
import com.antoinetawil.polyhome.Models.Peripheral
import com.antoinetawil.polyhome.R
import okhttp3.*
import org.json.JSONObject
import java.io.IOException
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.RequestBody.Companion.toRequestBody
import java.net.URLEncoder

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

        if (peripheral.type == "light") {
            // Add a toggle button for light peripherals
            val lightToggleButton = ImageButton(context).apply {
                layoutParams = LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.WRAP_CONTENT,
                    LinearLayout.LayoutParams.WRAP_CONTENT
                ).apply {
                    setMargins(8, 8, 8, 8)
                }
                setPadding(16, 16, 16, 16)
                setBackgroundResource(android.R.color.transparent)

                // Set the initial icon based on the power state
                updateLightIcon(peripheral.power == 1)

                setOnClickListener {
                    val isCurrentlyOn = peripheral.power == 1
                    val command = if (isCurrentlyOn) "TURN OFF" else "TURN ON"

                    sendCommandToPeripheral(peripheral.id, command) { success ->
                        (context as PeripheralListActivity).runOnUiThread {
                            if (success) {
                                // Toggle power state and update the icon
                                peripheral.power = if (isCurrentlyOn) 0 else 1
                                updateLightIcon(peripheral.power == 1)
                                Toast.makeText(
                                    context,
                                    "Light turned ${if (peripheral.power == 1) "on" else "off"}",
                                    Toast.LENGTH_SHORT
                                ).show()
                            } else {
                                Toast.makeText(
                                    context,
                                    "Failed to toggle light state",
                                    Toast.LENGTH_SHORT
                                ).show()
                            }
                        }
                    }
                }
            }
            holder.commandsContainer.addView(lightToggleButton)
        } else {
            // Add buttons for other commands
            for (command in peripheral.availableCommands) {
                val button = TextView(context).apply {
                    text = command.capitalize()
                    setPadding(12, 8, 12, 8)
                    setBackgroundResource(R.drawable.button_background)
                    setOnClickListener {
                        sendCommandToPeripheral(peripheral.id, command) { success ->
                            (context as PeripheralListActivity).runOnUiThread {
                                if (success) {
                                    Toast.makeText(
                                        context,
                                        "Command $command sent successfully!",
                                        Toast.LENGTH_SHORT
                                    ).show()
                                } else {
                                    Toast.makeText(
                                        context,
                                        "Failed to send $command",
                                        Toast.LENGTH_SHORT
                                    ).show()
                                }
                            }
                        }
                    }
                }
                holder.commandsContainer.addView(button)
            }
        }
    }

    override fun getItemCount(): Int = peripheralList.size

    private fun ImageButton.updateLightIcon(isOn: Boolean) {
        setImageResource(if (isOn) R.drawable.ic_light_on else R.drawable.ic_light_off)
    }

    private fun sendCommandToPeripheral(deviceId: String, command: String, callback: (Boolean) -> Unit) {
        val sharedPreferences = context.getSharedPreferences("PolyHomePrefs", Context.MODE_PRIVATE)
        val token = sharedPreferences.getString("auth_token", null)
        val houseId = (context as PeripheralListActivity).intent.getIntExtra("houseId", -1)

        if (token != null && houseId != -1) {
            // Properly encode the deviceId to handle spaces or special characters
            val encodedDeviceId = URLEncoder.encode("light 2.5", "UTF-8")

            val url = "https://polyhome.lesmoulinsdudev.com/api/houses/$houseId/devices/$encodedDeviceId/command"
            val client = OkHttpClient()

            val jsonObject = JSONObject().apply { put("command", command) }
            val requestBody = jsonObject.toString().toRequestBody("application/json".toMediaTypeOrNull())

            val request = Request.Builder()
                .url(url)
                .post(requestBody)
                .addHeader("Authorization", "Bearer $token")
                .build()

            // Log the request details
            Log.d("PeripheralAdapter", "Sending command: $command to $url with body: $jsonObject")

            client.newCall(request).enqueue(object : Callback {
                override fun onFailure(call: Call, e: IOException) {
                    Log.e("PeripheralAdapter", "Failed to send command: ${e.message}", e)
                    (context as PeripheralListActivity).runOnUiThread {
                        callback(false)
                    }
                }

                override fun onResponse(call: Call, response: Response) {
                    val responseBody = response.body?.string()
                    Log.d("PeripheralAdapter", "Response: $responseBody")
                    if (response.isSuccessful) {
                        callback(true)
                    } else {
                        Log.e("PeripheralAdapter", "Failed response code: ${response.code}, body: $responseBody")
                        callback(false)
                    }
                }
            })
        } else {
            Log.e("PeripheralAdapter", "Authentication token or houseId missing")
            (context as PeripheralListActivity).runOnUiThread {
                callback(false)
            }
        }
    }


}
