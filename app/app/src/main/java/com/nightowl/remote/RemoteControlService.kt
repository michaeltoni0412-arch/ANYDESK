package com.nightowl.remote

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.Service
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.PixelFormat
import android.hardware.display.DisplayManager
import android.hardware.display.VirtualDisplay
import android.media.ImageReader
import android.media.projection.MediaProjection
import android.media.projection.MediaProjectionManager
import android.os.Build
import android.os.IBinder
import android.util.DisplayMetrics
import androidx.core.app.NotificationCompat
import java.io.ByteArrayOutputStream
import android.util.Base64

class RemoteControlService : Service() {

    private lateinit var pairing: PairingManager
    private lateinit var sessionId: String
    private var mediaProjection: MediaProjection? = null
    private var virtualDisplay: VirtualDisplay? = null
    private var imageReader: ImageReader? = null

    private val channelId = "night_owl_active_channel"
    private var lastFrameTime = 0L
    private val frameIntervalMs = 400

    override fun onCreate() {
        super.onCreate()
        pairing = PairingManager(this)
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        if (intent == null) return START_NOT_STICKY
        sessionId = intent.getStringExtra("sessionId") ?: return START_NOT_STICKY
        val resultCode = intent.getIntExtra("resultCode", 0)
        val resultData = intent.getParcelableExtra<Intent>("resultData") ?: return START_NOT_STICKY

        startForeground(3, buildActiveNotification())

        val mpm = getSystemService(MEDIA_PROJECTION_SERVICE) as MediaProjectionManager
        mediaProjection = mpm.getMediaProjection(resultCode, resultData)

        startCapture()
        listenForRemoteInput()
        listenForSessionEnd()

        return START_STICKY
    }

    private fun startCapture() {
        val metrics = DisplayMetrics()
        val wm = getSystemService(WINDOW_SERVICE) as android.view.WindowManager
        @Suppress("DEPRECATION")
        wm.defaultDisplay.getMetrics(metrics)

        val width = 480
        val height = (480f * metrics.heightPixels / metrics.widthPixels).toInt()

        imageReader = ImageReader.newInstance(width, height, PixelFormat.RGBA_8888, 2)
        virtualDisplay = mediaProjection?.createVirtualDisplay(
            "NightOwlRemoteCapture", width, height, metrics.densityDpi,
            DisplayManager.VIRTUAL_DISPLAY_FLAG_AUTO_MIRROR,
            imageReader?.surface, null, null
        )

        imageReader?.setOnImageAvailableListener({ reader ->
            val now = System.currentTimeMillis()
            if (now - lastFrameTime < frameIntervalMs) {
                reader.acquireLatestImage()?.close()
                return@setOnImageAvailableListener
            }
            lastFrameTime = now

            val image = reader.acquireLatestImage() ?: return@setOnImageAvailableListener
            try {
                val plane = image.planes[0]
                val buffer = plane.buffer
                val pixelStride = plane.pixelStride
                val rowStride = plane.rowStride
                val rowPadding = rowStride - pixelStride * width

                val bitmap = Bitmap.createBitmap(
                    width + rowPadding / pixelStride, height, Bitmap.Config.ARGB_8888
                )
                bitmap.copyPixelsFromBuffer(buffer)
                val cropped = Bitmap.createBitmap(bitmap, 0, 0, width, height)

                val out = ByteArrayOutputStream()
                cropped.compress(Bitmap.CompressFormat.JPEG, 40, out)
                val base64 = Base64.encodeToString(out.toByteArray(), Base64.NO_WRAP)
                pairing.pushFrame(sessionId, base64)

                bitmap.recycle()
                cropped.recycle()
            } catch (_: Exception) {
            } finally {
                image.close()
            }
        }, null)
    }

    private fun listenForRemoteInput() {
        pairing.listenInput(sessionId) { xPct, yPct ->
            NightOwlAccessibilityService.instance?.performTapAtPercent(xPct, yPct)
        }
    }

    private fun listenForSessionEnd() {
        pairing.listenSessionStatus(sessionId) { status ->
            if (status == "ended") {
                stopSelf()
            }
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        pairing.setSessionStatus(sessionId, "ended")
        virtualDisplay?.release()
        imageReader?.close()
        mediaProjection?.stop()
    }

    override fun onBind(intent: Intent?): IBinder? = null

    private fun buildActiveNotification(): Notification {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                channelId, "Night Owl Remote Active", NotificationManager.IMPORTANCE_HIGH
            )
            getSystemService(NotificationManager::class.java).createNotificationChannel(channel)
        }

        val disconnectIntent = Intent(this, DisconnectReceiver::class.java).apply {
            putExtra("sessionId", sessionId)
        }
        val disconnectPending = android.app.PendingIntent.getBroadcast(
            this, 0, disconnectIntent,
            android.app.PendingIntent.FLAG_UPDATE_CURRENT or android.app.PendingIntent.FLAG_IMMUTABLE
        )

        return NotificationCompat.Builder(this, channelId)
            .setContentTitle("Night Owl Remote is ACTIVE")
            .setContentText("Someone can see and control this device right now")
            .setSmallIcon(android.R.drawable.ic_menu_view)
            .setOngoing(true)
            .addAction(android.R.drawable.ic_menu_close_clear_cancel, "Disconnect", disconnectPending)
            .build()
    }
}
