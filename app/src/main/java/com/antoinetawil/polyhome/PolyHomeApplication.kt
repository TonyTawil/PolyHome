package com.antoinetawil.polyhome

import android.app.Application
import com.antoinetawil.polyhome.Utils.NotificationHelper

class PolyHomeApplication : Application() {
    lateinit var notificationHelper: NotificationHelper

    override fun onCreate() {
        super.onCreate()
        notificationHelper = NotificationHelper(this)
    }
}
