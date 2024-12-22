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
import java.io.IOException
import okhttp3.*
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONObject

class PeripheralListAdapter(
        private val peripheralList: List<Peripheral>,
        private val context: Context,
        private val isScheduleMode: Boolean = false
) : RecyclerView.Adapter<PeripheralListAdapter.PeripheralViewHolder>() {

    class PeripheralViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val peripheralIdText: TextView = itemView.findViewById(R.id.peripheralIdTextView)
        val statusText: TextView = itemView.findViewById(R.id.statusTextView)
        val controlsContainer: LinearLayout = itemView.findViewById(R.id.commandsContainer)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): PeripheralViewHolder {
        val layoutId =
                if (isScheduleMode) {
                    R.layout.schedule_peripheral_list_item
                } else {
                    R.layout.peripheral_list_item
                }
        val view = LayoutInflater.from(parent.context).inflate(layoutId, parent, false)
        return PeripheralViewHolder(view)
    }

    override fun onBindViewHolder(holder: PeripheralViewHolder, position: Int) {
        val peripheral = peripheralList[position]
        holder.peripheralIdText.text =
                when (peripheral.type.lowercase()) {
                    "garage door" -> "Door"
                    else -> peripheral.id
                }

        when (peripheral.type.lowercase()) {
            "light" -> {
                holder.statusText.text =
                        if (peripheral.power == 1) context.getString(R.string.status_on)
                        else context.getString(R.string.status_off)
            }
            "rolling shutter", "garage door" -> {
                holder.statusText.text =
                        when {
                            peripheral.isStopped -> context.getString(R.string.status_stopped)
                            peripheral.opening == 1.0 -> context.getString(R.string.status_open)
                            peripheral.opening == 0.0 -> context.getString(R.string.status_closed)
                            else ->
                                    context.getString(
                                            R.string.status_percentage,
                                            ((peripheral.opening ?: 0.0) * 100).toInt()
                                    )
                        }
            }
        }

        holder.controlsContainer.removeAllViews()

        when (peripheral.type.lowercase()) {
            "light" -> {
                if (isScheduleMode) {
                    configureLightToggleForSchedule(holder.controlsContainer, peripheral)
                } else {
                    configureLightToggle(holder.controlsContainer, peripheral)
                }
            }
            "rolling shutter", "garage door" -> {
                if (isScheduleMode) {
                    configureShutterAndDoorButtonsForSchedule(holder.controlsContainer, peripheral)
                } else {
                    configureShutterAndDoorButtons(holder.controlsContainer, peripheral)
                }
            }
        }
    }

    override fun getItemCount(): Int = peripheralList.size

    private fun configureLightToggleForSchedule(container: LinearLayout, peripheral: Peripheral) {
        val lightToggleButton =
                ImageButton(context).apply {
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
                                    1 -> -1
                                    -1 -> 0
                                    else -> 1
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

    private fun configureLightToggle(container: LinearLayout, peripheral: Peripheral) {
        val lightToggleButton =
                ImageButton(context).apply {
                    layoutParams =
                            LinearLayout.LayoutParams(
                                            LinearLayout.LayoutParams.WRAP_CONTENT,
                                            LinearLayout.LayoutParams.WRAP_CONTENT
                                    )
                                    .apply {
                                        setMargins(8, 8, 8, 8)
                                        gravity = android.view.Gravity.END
                                    }
                    setPadding(16, 16, 16, 16)
                    setBackgroundResource(android.R.color.transparent)
                    setImageResource(
                            if (peripheral.power == 1) R.drawable.ic_light_on
                            else R.drawable.ic_light_off
                    )

                    setOnClickListener {
                        val isCurrentlyOn = peripheral.power == 1
                        val command = if (isCurrentlyOn) "TURN OFF" else "TURN ON"
                        sendCommandToPeripheral(peripheral.id, command) { success ->
                            (context as PeripheralListActivity).runOnUiThread {
                                if (success) {
                                    peripheral.power = if (isCurrentlyOn) 0 else 1
                                    setImageResource(
                                            if (peripheral.power == 1) R.drawable.ic_light_on
                                            else R.drawable.ic_light_off
                                    )

                                    val parentView = parent.parent as? View
                                    parentView?.findViewById<TextView>(R.id.statusTextView)?.text =
                                            if (peripheral.power == 1)
                                                    context.getString(R.string.status_on)
                                            else context.getString(R.string.status_off)

                                    Toast.makeText(
                                                    context,
                                                    context.getString(
                                                            if (peripheral.power == 1)
                                                                    R.string.light_turned_on
                                                            else R.string.light_turned_off
                                                    ),
                                                    Toast.LENGTH_SHORT
                                            )
                                            .show()
                                } else {
                                    Toast.makeText(
                                                    context,
                                                    context.getString(R.string.failed_toggle_light),
                                                    Toast.LENGTH_SHORT
                                            )
                                            .show()
                                }
                            }
                        }
                    }
                }
        container.addView(lightToggleButton)
    }

    private fun configureShutterAndDoorButtonsForSchedule(
            container: LinearLayout,
            peripheral: Peripheral
    ) {
        val openButton =
                createStyledButton(context.getString(R.string.open)).apply {
                    setOnClickListener {
                        peripheral.opening = 1.0
                        updateButtonStates(container, this)
                    }
                }

        val closeButton =
                createStyledButton(context.getString(R.string.close)).apply {
                    setOnClickListener {
                        peripheral.opening = 0.0
                        updateButtonStates(container, this)
                    }
                }

        if (peripheral.isShutterOpen) {
            updateButtonStates(container, openButton)
        } else {
            updateButtonStates(container, closeButton)
        }

        container.apply {
            addView(openButton)
            addView(closeButton)
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

    private fun createStyledButton(text: String): Button {
        return Button(context).apply {
            layoutParams =
                    LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT).apply {
                        weight = 1f
                        marginStart = 8.dpToPx()
                        marginEnd = 8.dpToPx()
                    }
            this.text = text
            setBackgroundResource(R.drawable.button_background_secondary)
            elevation = 0f
            textSize = 14f
            minimumHeight = 48.dpToPx()
        }
    }

    private fun Int.dpToPx(): Int {
        return (this * context.resources.displayMetrics.density).toInt()
    }

    private fun configureShutterAndDoorButtons(container: LinearLayout, peripheral: Peripheral) {
        val buttonsView =
                LayoutInflater.from(context)
                        .inflate(R.layout.shutter_buttons_layout, container, false)

        val openButton = buttonsView.findViewById<Button>(R.id.openButton)
        val stopButton = buttonsView.findViewById<Button>(R.id.stopButton)
        val closeButton = buttonsView.findViewById<Button>(R.id.closeButton)

        openButton.isEnabled = peripheral.availableCommands.contains("OPEN")
        stopButton.isEnabled = peripheral.availableCommands.contains("STOP")
        closeButton.isEnabled = peripheral.availableCommands.contains("CLOSE")

        openButton.setOnClickListener {
            sendCommandToPeripheral(peripheral.id, "OPEN") { success ->
                (context as PeripheralListActivity).runOnUiThread {
                    if (success) {
                        // Initial state update
                        peripheral.opening = 1.0
                        updateShutterStatus(peripheral, container.parent as View, "OPEN")
                        Toast.makeText(
                                        context,
                                        context.getString(
                                                R.string.command_sent_successfully,
                                                "OPEN",
                                                peripheral.id
                                        ),
                                        Toast.LENGTH_SHORT
                                )
                                .show()

                        // Start periodic refresh
                        var refreshCount = 0
                        val handler = android.os.Handler(android.os.Looper.getMainLooper())
                        val refreshRunnable =
                                object : Runnable {
                                    override fun run() {
                                        refreshCount++
                                        if (refreshCount < 5) { // 5 refreshes total (2s * 5 = 10s)
                                            (context as PeripheralListActivity).refreshDevicesState(
                                                    ""
                                            )
                                            handler.postDelayed(
                                                    this,
                                                    2000
                                            ) // Run again in 2 seconds
                                        } else {
                                            // Final refresh
                                            (context as PeripheralListActivity).refreshDevicesState(
                                                    ""
                                            )
                                        }
                                    }
                                }
                        handler.postDelayed(refreshRunnable, 2000) // Start first delayed refresh
                    } else {
                        Toast.makeText(
                                        context,
                                        context.getString(
                                                R.string.failed_command,
                                                "OPEN",
                                                peripheral.id
                                        ),
                                        Toast.LENGTH_SHORT
                                )
                                .show()
                    }
                }
            }
        }

        stopButton.setOnClickListener {
            sendCommandToPeripheral(peripheral.id, "STOP") { success ->
                (context as PeripheralListActivity).runOnUiThread {
                    if (success) {
                        // Single refresh for STOP command
                        (context as PeripheralListActivity).refreshDevicesState("")
                        Toast.makeText(
                                        context,
                                        context.getString(
                                                R.string.command_sent_successfully,
                                                "STOP",
                                                peripheral.id
                                        ),
                                        Toast.LENGTH_SHORT
                                )
                                .show()
                    } else {
                        Toast.makeText(
                                        context,
                                        context.getString(
                                                R.string.failed_command,
                                                "STOP",
                                                peripheral.id
                                        ),
                                        Toast.LENGTH_SHORT
                                )
                                .show()
                    }
                }
            }
        }

        closeButton.setOnClickListener {
            sendCommandToPeripheral(peripheral.id, "CLOSE") { success ->
                (context as PeripheralListActivity).runOnUiThread {
                    if (success) {
                        // Initial state update
                        peripheral.opening = 0.0
                        updateShutterStatus(peripheral, container.parent as View, "CLOSE")
                        Toast.makeText(
                                        context,
                                        context.getString(
                                                R.string.command_sent_successfully,
                                                "CLOSE",
                                                peripheral.id
                                        ),
                                        Toast.LENGTH_SHORT
                                )
                                .show()

                        // Start periodic refresh
                        var refreshCount = 0
                        val handler = android.os.Handler(android.os.Looper.getMainLooper())
                        val refreshRunnable =
                                object : Runnable {
                                    override fun run() {
                                        refreshCount++
                                        if (refreshCount < 5) { // 5 refreshes total (2s * 5 = 10s)
                                            (context as PeripheralListActivity).refreshDevicesState(
                                                    ""
                                            )
                                            handler.postDelayed(
                                                    this,
                                                    2000
                                            ) // Run again in 2 seconds
                                        } else {
                                            // Final refresh
                                            (context as PeripheralListActivity).refreshDevicesState(
                                                    ""
                                            )
                                        }
                                    }
                                }
                        handler.postDelayed(refreshRunnable, 2000) // Start first delayed refresh
                    } else {
                        Toast.makeText(
                                        context,
                                        context.getString(
                                                R.string.failed_command,
                                                "CLOSE",
                                                peripheral.id
                                        ),
                                        Toast.LENGTH_SHORT
                                )
                                .show()
                    }
                }
            }
        }

        container.addView(buttonsView)
    }

    private fun updateShutterStatus(
            peripheral: Peripheral,
            parentView: View,
            command: String? = null
    ) {
        val statusText = parentView.findViewById<TextView>(R.id.statusTextView)
        statusText?.text =
                when {
                    peripheral.opening == 1.0 -> context.getString(R.string.status_open)
                    peripheral.opening == 0.0 -> context.getString(R.string.status_closed)
                    command == "STOP" -> context.getString(R.string.status_stopped)
                    else ->
                            context.getString(
                                    R.string.status_percentage,
                                    ((peripheral.opening ?: 0.0) * 100).toInt()
                            )
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

    private fun sendCommandToPeripheral(
            deviceId: String,
            command: String,
            callback: (Boolean) -> Unit
    ) {
        val sharedPreferences = context.getSharedPreferences("PolyHomePrefs", Context.MODE_PRIVATE)
        val token = sharedPreferences.getString("auth_token", null)
        val houseId = (context as PeripheralListActivity).intent.getIntExtra("houseId", -1)

        if (token != null && houseId != -1) {
            val url =
                    "https://polyhome.lesmoulinsdudev.com/api/houses/$houseId/devices/$deviceId/command"
            val client = OkHttpClient()
            val jsonObject = JSONObject().apply { put("command", command) }
            val requestBody =
                    jsonObject.toString().toRequestBody("application/json".toMediaTypeOrNull())

            val request =
                    Request.Builder()
                            .url(url)
                            .post(requestBody)
                            .addHeader("Authorization", "Bearer $token")
                            .build()

            client.newCall(request)
                    .enqueue(
                            object : Callback {
                                override fun onFailure(call: Call, e: IOException) {
                                    Log.e(TAG, "Command $command failed for $deviceId")
                                    callback(false)
                                }

                                override fun onResponse(call: Call, response: Response) {
                                    callback(response.isSuccessful)
                                }
                            }
                    )
        } else {
            callback(false)
        }
    }
}
