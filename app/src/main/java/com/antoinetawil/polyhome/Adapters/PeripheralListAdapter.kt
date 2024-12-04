package com.antoinetawil.polyhome.Adapters

import android.content.ContentValues.TAG
import android.content.Context
import android.util.Log
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

        holder.peripheralIdTextView.text = context.getString(R.string.peripheral_id, peripheral.id)
        holder.peripheralTypeTextView.text = getLocalizedType(peripheral.type)
        holder.commandsContainer.removeAllViews()

        when (peripheral.type.lowercase()) {
            "light" -> configureLightToggle(holder, peripheral)
            "rolling shutter", "garage door" -> configureShutterAndDoorButtons(holder, peripheral)
        }
    }

    override fun getItemCount(): Int = peripheralList.size

    private fun configureLightToggle(holder: PeripheralViewHolder, peripheral: Peripheral) {
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
                                context.getString(if (peripheral.power == 1) R.string.light_turned_on else R.string.light_turned_off),
                                Toast.LENGTH_SHORT
                            ).show()
                        } else {
                            Toast.makeText(
                                context,
                                context.getString(R.string.failed_toggle_light),
                                Toast.LENGTH_SHORT
                            ).show()
                        }
                    }
                }
            }
        }
        holder.commandsContainer.addView(lightToggleButton)
    }

    private fun configureShutterAndDoorButtons(holder: PeripheralViewHolder, peripheral: Peripheral) {
        val openButton = createTextButton(context.getString(R.string.open), "OPEN", peripheral)
        val stopButton = createTextButton(context.getString(R.string.stop), "STOP", peripheral)
        val closeButton = createTextButton(context.getString(R.string.close), "CLOSE", peripheral)

        holder.commandsContainer.apply {
            addView(openButton)
            addView(stopButton)
            addView(closeButton)
        }
    }

    private fun createTextButton(text: String, command: String, peripheral: Peripheral): Button {
        return Button(context).apply {
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.WRAP_CONTENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            ).apply {
                setMargins(8, 8, 8, 8)
            }
            this.text = text
            isEnabled = peripheral.availableCommands.contains(command)

            setOnClickListener {
                sendCommandToPeripheral(peripheral.id, command) { success ->
                    (context as PeripheralListActivity).runOnUiThread {
                        if (success) {
                            Toast.makeText(
                                context,
                                context.getString(R.string.command_sent_successfully, command, peripheral.id),
                                Toast.LENGTH_SHORT
                            ).show()
                        } else {
                            Toast.makeText(
                                context,
                                context.getString(R.string.failed_command, command, peripheral.id),
                                Toast.LENGTH_SHORT
                            ).show()
                        }
                    }
                }
            }
        }
    }

    private fun getLocalizedType(type: String): String {
        return when (type.lowercase()) {
            "light" -> context.getString(R.string.light)
            "rolling shutter" -> context.getString(R.string.rolling_shutter)
            "garage door" -> context.getString(R.string.garage_door)
            else -> type
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
                    Log.e(TAG, "Command $command failed for $deviceId")
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


