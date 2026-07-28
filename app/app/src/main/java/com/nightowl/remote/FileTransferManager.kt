package com.nightowl.remote

import android.content.Context
import android.net.Uri
import android.widget.Toast
import com.google.firebase.storage.FirebaseStorage

class FileTransferManager(private val context: Context) {

    private val storage = FirebaseStorage.getInstance()

    fun uploadFile(sessionId: String, fileUri: Uri) {
        val fileName = "file-${System.currentTimeMillis()}"
        val ref = storage.reference.child("sessions/$sessionId/$fileName")
        ref.putFile(fileUri)
            .addOnSuccessListener {
                Toast.makeText(context, "File sent", Toast.LENGTH_SHORT).show()
            }
            .addOnFailureListener {
                Toast.makeText(context, "File send failed: ${it.message}", Toast.LENGTH_SHORT).show()
            }
    }
}
