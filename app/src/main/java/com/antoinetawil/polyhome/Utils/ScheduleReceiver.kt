package com.antoinetawil.polyhome.Utils

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.util.Log
import com.antoinetawil.polyhome.R
import java.io.IOException
import okhttp3.*
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONObject

class ScheduleReceiver : BroadcastReceiver() {

    companion object {
        private const val TAG = "ScheduleReceiver"
    }

    override fun onReceive(context: Context, intent: Intent) {
        Log.d(TAG, "Received broadcast intent")

        val scheduleId = intent.getLongExtra("scheduleId", -1)
        val peripheralId = intent.getStringExtra("peripheralId") ?: return
        val command = intent.getStringExtra("command") ?: return
        val isLastCommand = intent.getBooleanExtra("isLastCommand", true)
        val notificationHelper = NotificationHelper(context)

        // Execute the command
        executeCommand(context, peripheralId, command) { success ->
            if (isLastCommand) { // Only show notification for the last command
                val title =
                        if (success) {
                            context.getString(R.string.schedule_executed_success)
                        } else {
                            context.getString(R.string.schedule_executed_failure)
                        }

                val content = context.getString(R.string.schedule_execution_complete)

                notificationHelper.showScheduleExecutionNotification(
                        title = title,
                        content = content,
                        success = success
                )
            }
        }
    }

    private fun executeCommand(
            context: Context,
            deviceId: String,
            command: String,
            callback: (Boolean) -> Unit
    ) {
        val sharedPreferences = context.getSharedPreferences("PolyHomePrefs", Context.MODE_PRIVATE)
        val token = sharedPreferences.getString("auth_token", null)
        val houseId = sharedPreferences.getInt("selected_house_id", -1)

        if (token == null || houseId == -1) {
            Log.e(TAG, "Missing token or house ID - token: $token, houseId: $houseId")
            callback(false)
            return
        }

        Log.d(
                TAG,
                "Sending command to API - houseId: $houseId, peripheralId: $deviceId, command: $command"
        )

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
                                Log.e(TAG, "Command $command failed for $deviceId: $e")
                                callback(false)
                            }

                            override fun onResponse(call: Call, response: Response) {
                                if (response.isSuccessful) {
                                    Log.d(TAG, "Command $command successfully sent to $deviceId")
                                    callback(true)
                                } else {
                                    Log.e(TAG, "Failed to send command $command for $deviceId")
                                    callback(false)
                                }
                            }
                        }
                )
    }
}
