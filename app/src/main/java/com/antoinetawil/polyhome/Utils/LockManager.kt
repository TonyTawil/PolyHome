package com.antoinetawil.polyhome.Utils

import android.content.Context
import android.content.SharedPreferences

object LockManager {

    private const val LOCKED_STATE_KEY = "is_locked"

    private fun getPreferences(context: Context): SharedPreferences {
        return context.getSharedPreferences("LockManagerPrefs", Context.MODE_PRIVATE)
    }

    fun isLocked(context: Context): Boolean {
        val prefs = getPreferences(context)
        return prefs.getBoolean(LOCKED_STATE_KEY, true) // Default is locked
    }

    fun setLocked(context: Context, isLocked: Boolean) {
        val prefs = getPreferences(context)
        prefs.edit().putBoolean(LOCKED_STATE_KEY, isLocked).apply()
    }
}
