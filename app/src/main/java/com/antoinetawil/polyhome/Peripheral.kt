package com.antoinetawil.polyhome

data class Peripheral(
    val id: String,
    val type: String,
    val availableCommands: List<String>,
    val opening: Int? = null,
    val openingMode: Int? = null,
    val power: Int? = null
)
