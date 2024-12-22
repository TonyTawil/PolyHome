package com.antoinetawil.polyhome.Models

data class ScheduleCommand(
    val peripheralId: String,
    val peripheralType: String,
    val command: String
)
