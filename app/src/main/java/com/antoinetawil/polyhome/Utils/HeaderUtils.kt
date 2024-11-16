package com.antoinetawil.polyhome.Utils

import androidx.appcompat.app.AppCompatActivity
import android.view.View
import com.antoinetawil.polyhome.R

object HeaderUtils {
    fun setupHeader(activity: AppCompatActivity) {
        val backButton: View? = activity.findViewById(R.id.backButton)
        backButton?.setOnClickListener {
            activity.onBackPressedDispatcher.onBackPressed() // Ensure AppCompatActivity is used for back functionality
        }

        val searchButton: View? = activity.findViewById(R.id.searchIcon)
        searchButton?.setOnClickListener {
            // Add functionality for the search button if needed
        }
    }
}
