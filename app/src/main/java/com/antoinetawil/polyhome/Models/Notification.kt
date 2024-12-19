package com.antoinetawil.polyhome.Models

data class Notification(
        val id: Long = 0,
        val title: String,
        val content: String,
        val timestamp: Long,
        val success: Boolean
)
