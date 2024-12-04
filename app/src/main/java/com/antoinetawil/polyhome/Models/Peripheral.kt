package com.antoinetawil.polyhome.Models

data class Peripheral(
    val id: String,
    val type: String,
    val availableCommands: List<String>,
    var opening: Int? = null,
    val openingMode: Int? = null,
    var power: Int? = null
)
