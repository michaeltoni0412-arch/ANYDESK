package com.nightowl.remote

import android.content.Context
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.ValueEventListener
import kotlin.random.Random

class PairingManager(context: Context) {

    private val db = FirebaseDatabase.getInstance()
    private val prefs = context.getSharedPreferences("night_owl_remote", Context.MODE_PRIVATE)

    val deviceId: String
        get() {
            var id = prefs.getString("device_id", null)
            if (id == null) {
                id = "dev-" + Random.nextInt(100000, 999999)
                prefs.edit().putString("device_id", id).apply()
            }
            return id
        }

    fun getOrCreateMyCode(onReady: (String) -> Unit) {
        val existing = prefs.getString("my_code", null)
        if (existing != null) {
            db.getReference("codes").child(existing).setValue(deviceId)
            onReady(existing)
            return
        }
        val code = (100000..999999).random().toString()
        prefs.edit().putString("my_code", code).apply()
        db.getReference("codes").child(code).setValue(deviceId)
        onReady(code)
    }

    fun requestConnect(
        targetCode: String,
        myLabel: String,
        onStatusChange: (status: String, sessionId: String?) -> Unit
    ) {
        val requestId = "req-" + Random.nextInt(100000, 999999)
        val sessionId = "sess-" + Random.nextInt(1000000, 9999999)
        val requestRef = db.getReference("requests").child(targetCode).child(requestId)

        val data = mapOf(
            "fromDeviceId" to deviceId,
            "fromLabel" to myLabel,
            "status" to "pending",
            "sessionId" to sessionId
        )
        requestRef.setValue(data)

        requestRef.addValueEventListener(object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                val status = snapshot.child("status").getValue(String::class.java) ?: return
                onStatusChange(status, sessionId)
            }
            override fun onCancelled(error: DatabaseError) {}
        })
    }

    fun listenForIncomingRequests(
        myCode: String,
        onRequest: (requestId: String, fromLabel: String, sessionId: String) -> Unit
    ) {
        db.getReference("requests").child(myCode)
            .addChildEventListener(object : com.google.firebase.database.ChildEventListener {
                override fun onChildAdded(snapshot: DataSnapshot, previousChildName: String?) {
                    val status = snapshot.child("status").getValue(String::class.java)
                    if (status == "pending") {
                        val fromLabel = snapshot.child("fromLabel").getValue(String::class.java) ?: "Unknown device"
                        val sessionId = snapshot.child("sessionId").getValue(String::class.java) ?: return
                        onRequest(snapshot.key ?: return, fromLabel, sessionId)
                    }
                }
                override fun onChildChanged(snapshot: DataSnapshot, previousChildName: String?) {}
                override fun onChildRemoved(snapshot: DataSnapshot) {}
                override fun onChildMoved(snapshot: DataSnapshot, previousChildName: String?) {}
                override fun onCancelled(error: DatabaseError) {}
            })
    }

    fun respondToRequest(myCode: String, requestId: String, accept: Boolean) {
        db.getReference("requests").child(myCode).child(requestId).child("status")
            .setValue(if (accept) "accepted" else "denied")
    }

    fun setSessionStatus(sessionId: String, status: String) {
        db.getReference("sessions").child(sessionId).child("status").setValue(status)
    }

    fun listenSessionStatus(sessionId: String, onChange: (String) -> Unit) {
        db.getReference("sessions").child(sessionId).child("status")
            .addValueEventListener(object : ValueEventListener {
                override fun onDataChange(snapshot: DataSnapshot) {
                    snapshot.getValue(String::class.java)?.let { onChange(it) }
                }
                override fun onCancelled(error: DatabaseError) {}
            })
    }

    fun pushFrame(sessionId: String, base64Jpeg: String) {
        db.getReference("sessions").child(sessionId).child("frame").setValue(base64Jpeg)
    }

    fun listenFrame(sessionId: String, onFrame: (String) -> Unit) {
        db.getReference("sessions").child(sessionId).child("frame")
            .addValueEventListener(object : ValueEventListener {
                override fun onDataChange(snapshot: DataSnapshot) {
                    snapshot.getValue(String::class.java)?.let { onFrame(it) }
                }
                override fun onCancelled(error: DatabaseError) {}
            })
    }

    fun pushInput(sessionId: String, xPct: Float, yPct: Float) {
        val data = mapOf("x" to xPct, "y" to yPct, "ts" to System.currentTimeMillis())
        db.getReference("sessions").child(sessionId).child("input").setValue(data)
    }

    fun listenInput(sessionId: String, onTap: (xPct: Float, yPct: Float) -> Unit) {
        db.getReference("sessions").child(sessionId).child("input")
            .addValueEventListener(object : ValueEventListener {
                override fun onDataChange(snapshot: DataSnapshot) {
                    val x = snapshot.child("x").getValue(Float::class.java) ?: return
                    val y = snapshot.child("y").getValue(Float::class.java) ?: return
                    onTap(x, y)
                }
                override fun onCancelled(error: DatabaseError) {}
            })
    }
}
