package com.nightowl.remote

import android.Manifest
import android.accessibilityservice.AccessibilityServiceInfo
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.provider.Settings
import android.view.accessibility.AccessibilityManager
import android.widget.Button
import android.widget.EditText
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat

class MainActivity : AppCompatActivity() {

    private lateinit var pairing: PairingManager
    private lateinit var notifButton: Button
    private lateinit var accessibilityButton: Button
    private lateinit var myCodeText: TextView
    private lateinit var listenStatus: TextView
    private lateinit var codeInput: EditText
    private lateinit var connectButton: Button
    private lateinit var connectStatus: TextView

    private val notifRequestCode = 201

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        pairing = PairingManager(this)

        notifButton = findViewById(R.id.notifButton)
        accessibilityButton = findViewById(R.id.accessibilityButton)
        myCodeText = findViewById(R.id.myCodeText)
        listenStatus = findViewById(R.id.listenStatus)
        codeInput = findViewById(R.id.codeInput)
        connectButton = findViewById(R.id.connectButton)
        connectStatus = findViewById(R.id.connectStatus)

        notifButton.setOnClickListener { requestNotifPermission() }
        accessibilityButton.setOnClickListener { openAccessibilitySettings() }

        pairing.getOrCreateMyCode { code ->
            runOnUiThread { myCodeText.text = code }
        }

        connectButton.setOnClickListener {
            val target = codeInput.text.toString().trim()
            if (target.length < 4) {
                connectStatus.text = "Enter a valid code."
                return@setOnClickListener
            }
            connectStatus.text = "Waiting for the other person to respond..."
            pairing.requestConnect(target, myLabel = Build.MODEL ?: "A device") { status, sessionId ->
                runOnUiThread {
                    when (status) {
                        "accepted" -> {
                            connectStatus.text = "Accepted! Opening session..."
                            val intent = Intent(this, ControllerActivity::class.java)
                            intent.putExtra("sessionId", sessionId)
                            startActivity(intent)
                        }
                        "denied" -> connectStatus.text = "The other person denied the request."
                        "pending" -> connectStatus.text = "Waiting for the other person to respond..."
                    }
                }
            }
        }

        startService(Intent(this, PairingListenerService::class.java))
    }

    override fun onResume() {
        super.onResume()
        refreshState()
    }

    private fun refreshState() {
        val notifGranted = if (Build.VERSION.SDK_INT >= 33) {
            ContextCompat.checkSelfPermission(this, Manifest.permission.POST_NOTIFICATIONS) ==
                PackageManager.PERMISSION_GRANTED
        } else true
        notifButton.text = if (notifGranted) "✓ Notifications allowed" else "1. Allow Notifications"
        notifButton.isEnabled = !notifGranted

        val accessibilityOn = isAccessibilityServiceEnabled()
        accessibilityButton.text = if (accessibilityOn) "✓ Control Access enabled" else "2. Enable Control Access"
        accessibilityButton.isEnabled = !accessibilityOn

        listenStatus.text = "Share this code with someone you trust. Listening for connection requests: ON"
    }

    p
