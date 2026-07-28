package com.nightowl.remote

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.Service
import android.content.Intent
import android.os.Build
import android.os.IBinder
import androidx.core.app.NotificationCompat

class PairingListenerService : Service() {

    private val channelId = "night_owl_listen_channel"

    override fun onCreate() {
        super.onCreate()
        startForeground(2, buildNotification())

        val pairing = PairingManager(this)
        pairing.getOrCreateMyCode { code ->
            pairing.listenForIncomingRequests(code) { requestId, fromLabel, sessionId ->
                val intent = Intent(this, ConsentActivity::class.java).apply {
                    addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                    putExtra("myCode", code)
                    putExtra("requestId", requestId)
                    putExtra("fromLabel", fromLabel)
                    putExtra("sessionId", sessionId)
                }
                startActivity(intent)
            }
        }
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int = START_STICKY

    override fun onBind(intent: Intent?): IBinder? = null

    private fun buildNotification(): Notification {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                channelId, "Night Owl Remote Listening", NotificationManager.IMPORTANCE_LOW
            )
            getSystemService(NotificationManager::class.java).createNotificationChannel(channel)
        }
        return NotificationCompat.Builder(this, channelId)
            .setContentTitle("Night Owl Remote")
            .setContentText("Waiting for connection requests")
            .setSmallIcon(android.R.drawable.ic_menu_view)
            .setOngoing(true)
            .build()
    }
}
