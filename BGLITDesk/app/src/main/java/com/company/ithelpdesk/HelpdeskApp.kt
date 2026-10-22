package com.company.ithelpdesk

import android.app.Application
import com.company.ithelpdesk.data.notify.NotificationWatcher
import com.company.ithelpdesk.data.notify.SystemNotifier
import dagger.hilt.android.HiltAndroidApp
import javax.inject.Inject

@HiltAndroidApp
class HelpdeskApp : Application() {

    @Inject lateinit var notificationWatcher: NotificationWatcher

    override fun onCreate() {
        super.onCreate()
        SystemNotifier.createChannel(this)
        notificationWatcher.start()
    }
}
