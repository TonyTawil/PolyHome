package com.antoinetawil.polyhome.Utils

import android.Manifest
import android.content.pm.PackageManager
import android.os.Build
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import com.antoinetawil.polyhome.R
import com.google.android.material.dialog.MaterialAlertDialogBuilder

class NotificationPermissionHelper(private val activity: AppCompatActivity) {

    private val requestPermissionLauncher =
            activity.registerForActivityResult(ActivityResultContracts.RequestPermission()) {
                    isGranted ->
            }

    fun checkNotificationPermission() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            when {
                ContextCompat.checkSelfPermission(
                        activity,
                        Manifest.permission.POST_NOTIFICATIONS
                ) == PackageManager.PERMISSION_GRANTED -> {
                }
                activity.shouldShowRequestPermissionRationale(
                        Manifest.permission.POST_NOTIFICATIONS
                ) -> {
                    showNotificationPermissionRationale()
                }
                else -> {
                    requestPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
                }
            }
        }
    }

    private fun showNotificationPermissionRationale() {
        MaterialAlertDialogBuilder(activity)
                .setTitle(R.string.notification_permission_title)
                .setMessage(R.string.notification_permission_message)
                .setPositiveButton(R.string.allow) { _, _ ->
                    requestPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
                }
                .setNegativeButton(R.string.deny) { dialog, _ -> dialog.dismiss() }
                .show()
    }
}
