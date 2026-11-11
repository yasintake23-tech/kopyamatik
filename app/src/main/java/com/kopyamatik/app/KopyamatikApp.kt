package com.kopyamatik.app

import android.app.Application
import android.app.NotificationChannel
import android.app.NotificationManager
import android.os.Build

class KopyamatikApp : Application() {

    companion object {
        const val PLAYBACK_CHANNEL_ID = "kopyamatik_playback_channel"
    }

    override fun onCreate() {
        super.onCreate()
        createNotificationChannel()
    }

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                PLAYBACK_CHANNEL_ID,
                "Soru Çözüm Ses Oynatıcı",
                NotificationManager.IMPORTANCE_LOW
            ).apply {
                description = "Arka planda soru çözümlerini seslendirme bildirimleri"
                setShowBadge(false)
            }
            val notificationManager = getSystemService(NotificationManager::class.java)
            notificationManager?.createNotificationChannel(channel)
        }
    }
}
