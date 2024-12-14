package com.antoinetawil.polyhome.Utils

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.util.Log
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

        val peripheralId = intent.getStringExtra("peripheralId")
        val command = intent.getStringExtra("command")

        if (peripheralId == null || command == null) {
            Log.e(TAG, "Invalid data received - peripheralId: $peripheralId, command: $command")
            return
        }

        Log.d(TAG, "Processing command: $command for peripheral: $peripheralId")
        sendCommandToPeripheral(context, peripheralId, command)
    }

    private fun sendCommandToPeripheral(context: Context, peripheralId: String, command: String) {
        val sharedPreferences = context.getSharedPreferences("PolyHomePrefs", Context.MODE_PRIVATE)
        val token = sharedPreferences.getString("auth_token", null)
        val houseId = sharedPreferences.getInt("selected_house_id", -1)

        if (token == null || houseId == -1) {
            Log.e(TAG, "Missing token or house ID - token: $token, houseId: $houseId")
            return
        }

        Log.d(
                TAG,
                "Sending command to API - houseId: $houseId, peripheralId: $peripheralId, command: $command"
        )

        val url =
                "https://polyhome.lesmoulinsdudev.com/api/houses/$houseId/devices/$peripheralId/command"
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
                                Log.e(TAG, "Command $command failed for $peripheralId: $e")
                            }

                            override fun onResponse(call: Call, response: Response) {
                                if (response.isSuccessful) {
                                    Log.d(
                                            TAG,
                                            "Command $command successfully sent to $peripheralId"
                                    )
                                } else {
                                    Log.e(TAG, "Failed to send command $command for $peripheralId")
                                }
                            }
                        }
                )
    }
}
