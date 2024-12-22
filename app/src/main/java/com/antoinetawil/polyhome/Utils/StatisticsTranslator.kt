package com.antoinetawil.polyhome.Utils

import android.content.Context
import com.antoinetawil.polyhome.R

class StatisticsTranslator(private val context: Context) {
    fun translateDeviceType(deviceId: String): String {
        return when {
            deviceId.startsWith("Light", ignoreCase = true) ->
                    context.getString(R.string.peripheral_light, deviceId.substringAfter("Light"))
            deviceId.startsWith("Shutter", ignoreCase = true) ->
                    context.getString(
                            R.string.peripheral_shutter,
                            deviceId.substringAfter("Shutter")
                    )
            deviceId.startsWith("Garage", ignoreCase = true) ->
                    context.getString(R.string.peripheral_garage, deviceId.substringAfter("Garage"))
            else -> deviceId
        }
    }

    fun getHouseLabel(houseId: Int): String {
        return context.getString(R.string.house_tag, houseId)
    }

    fun formatShutterStatus(active: Int, total: Int): String {
        return context.getString(R.string.shutter_status_format, active, total)
    }

    fun formatLightStatus(active: Int, total: Int): String {
        return context.getString(R.string.light_status_format, active, total)
    }

    fun formatEfficiency(efficiency: Int): String {
        return context.getString(R.string.efficiency_format, efficiency)
    }

    fun formatDailyCO2(co2: Double): String {
        return context.getString(R.string.daily_co2_format, co2)
    }
}
