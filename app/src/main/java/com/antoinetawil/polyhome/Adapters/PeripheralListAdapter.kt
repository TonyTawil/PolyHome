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
import androidx.core.content.ContextCompat
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
        private val isScheduleMode: Boolean = false // Special behavior for SchedulesActivity
) : RecyclerView.Adapter<PeripheralListAdapter.PeripheralViewHolder>() {

    class PeripheralViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val peripheralIdText: TextView = itemView.findViewById(R.id.peripheralIdTextView)
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
        holder.peripheralIdText.text = peripheral.id

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
                                    1 -> -1 // ON -> OFF
                                    -1 -> 0 // OFF -> unselected
                                    else -> 1 // unselected -> ON
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
                                    .apply { setMargins(8, 8, 8, 8) }
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

        // Set initial state based on current opening state
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
                    LinearLayout.LayoutParams(
                                    0, // Width will be 0 with weight for equal sizing
                                    LinearLayout.LayoutParams.WRAP_CONTENT
                            )
                            .apply {
                                weight = 1f // Equal width for both buttons
                                marginStart = 8.dpToPx()
                                marginEnd = 8.dpToPx()
                            }
            this.text = text
            setBackgroundResource(R.drawable.button_background_secondary)
            elevation = 0f // Remove button shadow
            textSize = 14f
            minimumHeight = 48.dpToPx() // Standard button height
        }
    }

    // Add extension function for dp conversion
    private fun Int.dpToPx(): Int {
        return (this * context.resources.displayMetrics.density).toInt()
    }

    private fun configureShutterAndDoorButtons(container: LinearLayout, peripheral: Peripheral) {
        // Add status text to show opening percentage
        val statusText =
                TextView(context).apply {
                    layoutParams =
                            LinearLayout.LayoutParams(
                                            LinearLayout.LayoutParams.WRAP_CONTENT,
                                            LinearLayout.LayoutParams.WRAP_CONTENT
                                    )
                                    .apply { setMargins(8, 8, 8, 8) }

                    text =
                            when {
                                peripheral.type.equals("garage door", ignoreCase = true) -> {
                                    if (peripheral.isShutterOpen)
                                            context.getString(R.string.status_open)
                                    else context.getString(R.string.status_closed)
                                }
                                peripheral.openingMode == 2 -> {
                                    context.getString(
                                            R.string.status_percentage,
                                            peripheral.shutterOpeningPercentage
                                    )
                                }
                                else -> {
                                    if (peripheral.isShutterOpen)
                                            context.getString(R.string.status_open)
                                    else context.getString(R.string.status_closed)
                                }
                            }

                    setTextColor(ContextCompat.getColor(context, R.color.secondary_text))
                }
        container.addView(statusText)

        // Add the control buttons
        val openButton = createTextButton(context.getString(R.string.open), "OPEN", peripheral)
        val stopButton = createTextButton(context.getString(R.string.stop), "STOP", peripheral)
        val closeButton = createTextButton(context.getString(R.string.close), "CLOSE", peripheral)

        container.apply {
            addView(openButton)
            addView(stopButton)
            addView(closeButton)
        }
    }

    private fun createTextButton(text: String, command: String, peripheral: Peripheral): Button {
        return Button(context).apply {
            layoutParams =
                    LinearLayout.LayoutParams(
                                    LinearLayout.LayoutParams.WRAP_CONTENT,
                                    LinearLayout.LayoutParams.WRAP_CONTENT
                            )
                            .apply { setMargins(8, 8, 8, 8) }
            this.text = text
            isEnabled = peripheral.availableCommands.contains(command)

            setOnClickListener {
                sendCommandToPeripheral(peripheral.id, command) { success ->
                    (context as PeripheralListActivity).runOnUiThread {
                        if (success) {
                            Toast.makeText(
                                            context,
                                            context.getString(
                                                    R.string.command_sent_successfully,
                                                    command,
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
                                                    command,
                                                    peripheral.id
                                            ),
                                            Toast.LENGTH_SHORT
                                    )
                                    .show()
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
