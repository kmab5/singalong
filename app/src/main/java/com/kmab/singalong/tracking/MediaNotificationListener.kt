package com.kmab.singalong.tracking

import android.service.notification.NotificationListenerService
import com.kmab.singalong.app.SingalongApplication

class MediaNotificationListener : NotificationListenerService() {
    private val container get() = (application as SingalongApplication).container

    override fun onListenerConnected() {
        super.onListenerConnected()
        container.listenerConnected(true)
    }

    override fun onListenerDisconnected() {
        container.listenerConnected(false)
        super.onListenerDisconnected()
    }

    override fun onDestroy() {
        container.listenerConnected(false)
        super.onDestroy()
    }
}
