package com.nightowl.remote

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent

class DisconnectReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        val sessionId = intent.getStringExtra("sessionId") ?: return
        PairingManager(context).setSessionStatus(sessionId, "ended")
        context.stopService(Intent(context, RemoteControlService::class.java))
    }
}
