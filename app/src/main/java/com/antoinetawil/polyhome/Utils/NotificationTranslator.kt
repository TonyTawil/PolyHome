package com.antoinetawil.polyhome.Utils

import android.content.Context
import com.antoinetawil.polyhome.R

object NotificationTranslator {
    fun translateScheduleNotification(context: Context, success: Boolean): Pair<String, String> {
        val title =
                if (success) {
                    context.getString(R.string.schedule_executed_success)
                } else {
                    context.getString(R.string.schedule_executed_failure)
                }
        val content = context.getString(R.string.schedule_execution_complete)

        // Return both the translated and English versions
        return Pair(title, content)
    }

    // Convert English stored notification to localized version
    fun getLocalizedNotification(
            context: Context,
            englishTitle: String,
            englishContent: String
    ): Pair<String, String> {
        val localizedTitle =
                when (englishTitle) {
                    "Schedule Executed Successfully" ->
                            context.getString(R.string.schedule_executed_success)
                    "Schedule Execution Failed" ->
                            context.getString(R.string.schedule_executed_failure)
                    else -> englishTitle
                }

        val localizedContent =
                when (englishContent) {
                    "Schedule execution completed" ->
                            context.getString(R.string.schedule_execution_complete)
                    else -> englishContent
                }

        return Pair(localizedTitle, localizedContent)
    }

    // Convert localized notification to English for storage
    fun getEnglishNotification(
            context: Context,
            localizedTitle: String,
            localizedContent: String
    ): Pair<String, String> {
        val englishTitle =
                when (localizedTitle) {
                    context.getString(R.string.schedule_executed_success) ->
                            "Schedule Executed Successfully"
                    context.getString(R.string.schedule_executed_failure) ->
                            "Schedule Execution Failed"
                    else -> localizedTitle
                }

        val englishContent =
                when (localizedContent) {
                    context.getString(R.string.schedule_execution_complete) ->
                            "Schedule execution completed"
                    else -> localizedContent
                }

        return Pair(englishTitle, englishContent)
    }
}
