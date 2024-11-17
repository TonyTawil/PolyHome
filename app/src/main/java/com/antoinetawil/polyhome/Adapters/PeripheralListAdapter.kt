package com.antoinetawil.polyhome.Adapters

import android.content.Context
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
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

        holder.commandsContainer.removeAllViews()

        if (peripheral.type == "light") {
            val lightToggleButton = ImageButton(context).apply {
                layoutParams = LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.WRAP_CONTENT,
                    LinearLayout.LayoutParams.WRAP_CONTENT
                ).apply {
                    setMargins(8, 8, 8, 8)
                }
                setPadding(16, 16, 16, 16)
                setBackgroundResource(android.R.color.transparent)
                setImageResource(if (peripheral.power == 1) R.drawable.ic_light_on else R.drawable.ic_light_off)

                setOnClickListener {
                    val isCurrentlyOn = peripheral.power == 1
                    val command = if (isCurrentlyOn) "TURN OFF" else "TURN ON"
                    sendCommandToPeripheral(peripheral.id, command) { success ->
                        (context as PeripheralListActivity).runOnUiThread {
                            if (success) {
                                peripheral.power = if (isCurrentlyOn) 0 else 1
                                setImageResource(if (peripheral.power == 1) R.drawable.ic_light_on else R.drawable.ic_light_off)
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
        } else if (peripheral.type == "rolling shutter" || peripheral.type == "garage door") {
            val openButton = createTextButton("Open", "OPEN", peripheral)
            val stopButton = createTextButton("Stop", "STOP", peripheral)
            val closeButton = createTextButton("Close", "CLOSE", peripheral)

            holder.commandsContainer.apply {
                addView(openButton)
                addView(stopButton)
                addView(closeButton)
            }
        }
    }

    override fun getItemCount(): Int = peripheralList.size

    private fun createTextButton(text: String, command: String, peripheral: Peripheral): Button {
        return Button(context).apply {
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.WRAP_CONTENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            ).apply {
                setMargins(8, 8, 8, 8)
            }
            setText(text)
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
    }

    private fun sendCommandToPeripheral(deviceId: String, command: String, callback: (Boolean) -> Unit) {
        val sharedPreferences = context.getSharedPreferences("PolyHomePrefs", Context.MODE_PRIVATE)
        val token = sharedPreferences.getString("auth_token", null)
        val houseId = (context as PeripheralListActivity).intent.getIntExtra("houseId", -1)

        if (token != null && houseId != -1) {
            val url = "https://polyhome.lesmoulinsdudev.com/api/houses/$houseId/devices/$deviceId/command"
            val client = OkHttpClient()
            val jsonObject = JSONObject().apply { put("command", command) }
            val requestBody = jsonObject.toString().toRequestBody("application/json".toMediaTypeOrNull())

            val request = Request.Builder()
                .url(url)
                .post(requestBody)
                .addHeader("Authorization", "Bearer $token")
                .build()

            client.newCall(request).enqueue(object : Callback {
                override fun onFailure(call: Call, e: IOException) {
                    callback(false)
                }

                override fun onResponse(call: Call, response: Response) {
                    callback(response.isSuccessful)
                }
            })
        } else {
            callback(false)
        }
    }
}
