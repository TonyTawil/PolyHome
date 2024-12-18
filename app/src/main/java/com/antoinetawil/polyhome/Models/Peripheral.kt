package com.antoinetawil.polyhome.Models

data class Peripheral(
        val id: String,
        val type: String,
        val availableCommands: List<String>,
        var power: Int? = null,
        var opening: Double? = null,
        val openingMode: Int? = null
) {
    val isShutterOpen: Boolean
        get() =
                when (openingMode) {
                    0 -> opening == 1.0
                    1 -> opening != 0.0
                    2 -> opening?.let { it > 0.0 } ?: false
                    else -> false
                }

    val shutterOpeningPercentage: Int
        get() =
                when (openingMode) {
                    0 -> if (opening == 1.0) 100 else 0
                    1 -> if (opening == 0.0) 0 else 100
                    2 -> opening?.let { (it * 100).toInt() } ?: 0
                    else -> 0
                }

    // Helper function to safely get power value
    fun getPowerSafely(): Int = power ?: 0

    // Helper function to safely get opening value as percentage
    fun getOpeningSafely(): Int = shutterOpeningPercentage
}
