package com.nightowl.remote

import android.content.Intent
import android.graphics.BitmapFactory
import android.net.Uri
import android.os.Bundle
import android.util.Base64
import android.view.MotionEvent
import android.widget.Button
import android.widget.ImageView
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity

class ControllerActivity : AppCompatActivity() {

    private lateinit var pairing: PairingManager
    private lateinit var sessionId: String
    private lateinit var remoteScreenView: ImageView
    private lateinit var sessionStatus: TextView

    private val filePicker = registerForActivityResult(
        androidx.activity.result.contract.ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        uri?.let { FileTransferManager(this).uploadFile(sessionId, it) }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_controller)
        pairing = PairingManager(this)
        sessionId = intent.getStringExtra("sessionId") ?: run { finish(); return }

        remoteScreenView = findViewById(R.id.remoteScreenView)
        sessionStatus = findViewById(R.id.sessionStatus)

        pairing.listenFrame(sessionId) { base64 ->
            try {
                val bytes = Base64.decode(base64, Base64.NO_WRAP)
                val bitmap = BitmapFactory.decodeByteArray(bytes, 0, bytes.size)
                runOnUiThread { remoteScreenView.setImageBitmap(bitmap) }
            } catch (_: Exception) { }
        }

        pairing.listenSessionStatus(sessionId) { status ->
            runOnUiThread {
                sessionStatus.text = status.replaceFirstChar { it.uppercase() }
                if (status == "ended") {
                    Toast.makeText(this, "Session ended", Toast.LENGTH_SHORT).show()
                    finish()
                }
            }
        }

        remoteScreenView.setOnTouchListener { view, event ->
            if (event.action == MotionEvent.ACTION_DOWN) {
                val xPct = event.x / view.width
                val yPct = event.y / view.height
                pairing.pushInput(sessionId, xPct, yPct)
            }
            true
        }

        findViewById<Button>(R.id.disconnectButton).setOnClickListener {
            pairing.setSessionStatus(sessionId, "ended")
            finish()
        }

        findViewById<Button>(R.id.sendFileButton).setOnClickListener {
            filePicker.launch("*/*")
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        if (isFinishing) {
            pairing.setSessionStatus(sessionId, "ended")
        }
    }
}
