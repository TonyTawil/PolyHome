package com.antoinetawil.polyhome.Models

data class Schedule(
        val id: Long = 0,
        val dateTime: String,
        val peripheralId: String,
        val peripheralType: String,
        val command: String,
        val houseId: Int
)
