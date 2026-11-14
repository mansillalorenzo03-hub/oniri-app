package com.kanso.focus.service

import android.service.notification.NotificationListenerService
import android.service.notification.StatusBarNotification
import com.kanso.focus.data.FocusSessionManager

class FocusNotificationListener : NotificationListenerService() {

    override fun onCreate() {
        super.onCreate()
        FocusSessionManager.init(applicationContext)
    }

    override fun onNotificationPosted(sbn: StatusBarNotification?) {
        super.onNotificationPosted(sbn)
        if (sbn == null) return
        FocusSessionManager.init(applicationContext)

        if (FocusSessionManager.isSessionActive.value &&
            FocusSessionManager.mutedPackages.value.contains(sbn.packageName)
        ) {
            cancelNotification(sbn.key)
        }
    }
}
