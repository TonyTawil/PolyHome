package com.antoinetawil.polyhome.Models

data class Schedule(
        val id: Long = 0,
        val dateTime: String,
        val houseId: Int,
        val commands: List<ScheduleCommand>,
        val recurringDays: List<Int> = emptyList(),
        val isEnabled: Boolean = true,
        val isSpecificDate: Boolean = false
)

data class ScheduleCommand(
        val peripheralId: String,
        val peripheralType: String,
        val command: String
)
