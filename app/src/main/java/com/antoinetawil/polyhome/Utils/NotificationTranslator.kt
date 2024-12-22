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

        return Pair(title, content)
    }

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
                    "Schedule Updated Successfully" ->
                            context.getString(R.string.schedule_updated_success)
                    else -> englishTitle
                }

        val localizedContent =
                when {
                    englishContent.startsWith("Schedule execution completed") ->
                            context.getString(R.string.schedule_execution_complete)
                    englishContent.startsWith("Schedule for House") ->
                            context.getString(
                                R.string.schedule_update_details,
                                extractHouseId(englishContent),
                                extractCommandCount(englishContent),
                                if (extractCommandCount(englishContent) > 1) "s" else ""
                            )
                    else -> englishContent
                }

        return Pair(localizedTitle, localizedContent)
    }

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
                    context.getString(R.string.schedule_updated_success) ->
                            "Schedule Updated Successfully"
                    else -> localizedTitle
                }

        val englishContent =
                when {
                    localizedContent == context.getString(R.string.schedule_execution_complete) ->
                            "Schedule execution completed"
                    localizedContent.startsWith(context.getString(R.string.schedule_update_details, 0, 0, "")) -> {
                        val houseId = extractHouseId(localizedContent)
                        val commandCount = extractCommandCount(localizedContent)
                        "Schedule for House $houseId updated with $commandCount command${if (commandCount > 1) "s" else ""}"
                    }
                    else -> localizedContent
                }

        return Pair(englishTitle, englishContent)
    }

    private fun extractHouseId(content: String): Int {
        return "House (\\d+)".toRegex().find(content)?.groupValues?.get(1)?.toIntOrNull() ?: 0
    }

    private fun extractCommandCount(content: String): Int {
        return "(\\d+) command".toRegex().find(content)?.groupValues?.get(1)?.toIntOrNull() ?: 0
    }
}
