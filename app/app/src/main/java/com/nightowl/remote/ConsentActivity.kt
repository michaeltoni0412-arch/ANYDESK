package com.nightowl.remote

import android.content.Intent
import android.media.projection.MediaProjectionManager
import android.os.Bundle
import android.widget.Button
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity

class ConsentActivity : AppCompatActivity() {

    private lateinit var pairing: PairingManager
    private lateinit var myCode: String
    private lateinit var requestId: String
    private lateinit var sessionId: String

    private val projectionRequestCode = 301

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_consent)
        pairing = PairingManager(this)

        myCode = intent.getStringExtra("myCode") ?: ""
        requestId = intent.getStringExtra("requestId") ?: ""
        sessionId = intent.getStringExtra("sessionId") ?: ""
        val fromLabel = intent.getStringExtra("fromLabel") ?: "A device"

        findViewById<TextView>(R.id.requesterText).text = "$fromLabel wants to control this device"

        findViewById<Button>(R.id.allowButton).setOnClickListener {
            pairing.respondToRequest(myCode, requestId, accept = true)
            pairing.setSessionStatus(sessionId, "active")
            val mpm = getSystemService(MEDIA_PROJECTION_SERVICE) as MediaProjectionManager
            startActivityForResult(mpm.createScreenCaptureIntent(), projectionRequestCode)
        }

        findViewById<Button>(R.id.denyButton).setOnClickListener {
            pairing.respondToRequest(myCode, requestId, accept = false)
            finish()
        }
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (requestCode == projectionRequestCode) {
            if (resultCode == RESULT_OK && data != null) {
                val serviceIntent = Intent(this, RemoteControlService::class.java).apply {
                    putExtra("sessionId", sessionId)
                    putExtra("resultCode", resultCode)
                    putExtra("resultData", data)
                }
                if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.O) {
                    startForegroundService(serviceIntent)
                } else {
                    startService(serviceIntent)
                }
            } else {
                pairing.setSessionStatus(sessionId, "ended")
            }
            finish()
        }
    }
}
