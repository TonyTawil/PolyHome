package com.antoinetawil.polyhome.Utils

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import com.antoinetawil.polyhome.Activities.SchedulesListActivity
import com.antoinetawil.polyhome.Database.DatabaseHelper
import com.antoinetawil.polyhome.Models.Notification
import com.antoinetawil.polyhome.R
import java.util.*

class NotificationHelper(private val context: Context) {

    companion object {
        const val CHANNEL_ID = "schedule_notifications"
        private var notificationId = 0
    }

    init {
        createNotificationChannel()
    }

    private fun getCurrentLanguage(): String {
        val sharedPreferences = context.getSharedPreferences("PolyHomePrefs", Context.MODE_PRIVATE)
        return sharedPreferences.getString("LANGUAGE", "en") ?: "en"
    }

    private fun updateContextWithStoredLanguage(): Context {
        val currentLanguage = getCurrentLanguage()
        val locale =
                when (currentLanguage) {
                    "fr" -> Locale("fr")
                    "es" -> Locale("es")
                    "ar" -> Locale("ar")
                    else -> Locale("en")
                }

        val configuration = context.resources.configuration
        configuration.setLocale(locale)
        return context.createConfigurationContext(configuration)
    }

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            // Use localized context for channel creation
            val localizedContext = updateContextWithStoredLanguage()
            val importance = NotificationManager.IMPORTANCE_DEFAULT
            val channel =
                    NotificationChannel(
                                    CHANNEL_ID,
                                    localizedContext.getString(R.string.notification_channel_name),
                                    importance
                            )
                            .apply {
                                description =
                                        localizedContext.getString(
                                                R.string.notification_channel_description
                                        )
                            }
            val notificationManager =
                    context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            notificationManager.createNotificationChannel(channel)
        }
    }

    fun showScheduleExecutionNotification(title: String, content: String, success: Boolean = true) {
        // Use localized context for notification content
        val localizedContext = updateContextWithStoredLanguage()

        // Get localized strings directly from resources using localized context
        val localizedTitle =
                if (success) {
                    localizedContext.getString(R.string.schedule_executed_success)
                } else {
                    localizedContext.getString(R.string.schedule_executed_failure)
                }
        val localizedContent = localizedContext.getString(R.string.schedule_execution_complete)

        val intent =
                Intent(context, SchedulesListActivity::class.java).apply {
                    flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
                }

        val pendingIntent =
                PendingIntent.getActivity(
                        context,
                        0,
                        intent,
                        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
                        } else {
                            PendingIntent.FLAG_UPDATE_CURRENT
                        }
                )

        val builder =
                NotificationCompat.Builder(context, CHANNEL_ID)
                        .setSmallIcon(if (success) R.drawable.ic_success else R.drawable.ic_error)
                        .setContentTitle(localizedTitle)
                        .setContentText(localizedContent)
                        .setPriority(NotificationCompat.PRIORITY_DEFAULT)
                        .setAutoCancel(true)
                        .setContentIntent(pendingIntent)

        with(NotificationManagerCompat.from(context)) { notify(notificationId++, builder.build()) }

        // Save notification to database in English
        val dbHelper = DatabaseHelper(context)
        val notification =
                Notification(
                        title =
                                if (success) "Schedule Executed Successfully"
                                else "Schedule Execution Failed",
                        content = "Schedule execution completed",
                        timestamp = System.currentTimeMillis(),
                        success = success
                )
        dbHelper.insertNotification(notification)
    }

    fun showScheduleUpdateNotification(houseId: Int, commandCount: Int) {
        // Use localized context for notification content
        val localizedContext = updateContextWithStoredLanguage()

        val title = localizedContext.getString(R.string.schedule_updated_success)
        val content = localizedContext.getString(
            R.string.schedule_update_details,
            houseId,
            commandCount,
            if (commandCount > 1) "s" else ""
        )

        val intent = Intent(context, SchedulesListActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
        }

        val pendingIntent = PendingIntent.getActivity(
            context,
            0,
            intent,
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
            } else {
                PendingIntent.FLAG_UPDATE_CURRENT
            }
        )

        val builder = NotificationCompat.Builder(context, CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_success)
            .setContentTitle(title)
            .setContentText(content)
            .setPriority(NotificationCompat.PRIORITY_DEFAULT)
            .setAutoCancel(true)
            .setContentIntent(pendingIntent)

        with(NotificationManagerCompat.from(context)) {
            notify(notificationId++, builder.build())
        }

        // Save notification to database in English
        val dbHelper = DatabaseHelper(context)
        val notification = Notification(
            title = "Schedule Updated Successfully",
            content = "Schedule for House $houseId updated with $commandCount command${if (commandCount > 1) "s" else ""}",
            timestamp = System.currentTimeMillis(),
            success = true
        )
        dbHelper.insertNotification(notification)
    }
}
