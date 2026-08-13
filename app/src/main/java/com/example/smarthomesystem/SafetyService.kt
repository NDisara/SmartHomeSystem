package com.example.smarthomesystem

import android.app.*
import android.content.Context
import android.content.Intent
import android.os.Build
import android.os.IBinder
import androidx.core.app.NotificationCompat
import kotlinx.coroutines.*
import java.util.Calendar

class SafetyService : Service() {
    private val serviceScope = CoroutineScope(Dispatchers.IO + Job())
    private val CHANNEL_ID = "SafetyServiceChannel"

    override fun onCreate() {
        super.onCreate()
        createNotificationChannel()
        val notification = createNotification("SmartHome System is monitoring your home safety.")
        startForeground(1, notification)
        startMonitoring()
    }

    private fun startMonitoring() {
        serviceScope.launch {
            var serverTimeOffset = 0L
            
            // Initial offset fetch
            FirebaseRepository.listenToServerTimeOffset { offset ->
                serverTimeOffset = offset
            }

            while (isActive) {
                // Fetch all floors to check all devices across the house
                FirebaseRepository.listenToFloors { floors ->
                    floors.forEach { floor ->
                        FirebaseRepository.getDevicesOnce(floor.id) { devices ->
                            val adjustedNow = System.currentTimeMillis() + serverTimeOffset
                            val calendar = Calendar.getInstance()
                            val currentTotalMinutes = calendar.get(Calendar.HOUR_OF_DAY) * 60 + calendar.get(Calendar.MINUTE)

                            devices.forEach { device ->
                                // 1. Safety Cutoff for Iron
                                if (device.type.lowercase() == "iron" && device.state.uppercase() == "ON" && device.maxDurationSec > 0 && device.turnedOnAt > 0) {
                                    val elapsed = (adjustedNow - device.turnedOnAt) / 1000
                                    if (elapsed >= device.maxDurationSec) {
                                        FirebaseRepository.updateDeviceState(floor.id, device.id, "OFF")
                                        FirebaseRepository.logUsage(device.id, device.name, "AUTO-OFF (Safety Cutoff - BG)")
                                        FirebaseRepository.addAlert(
                                            title = "Safety Cutoff",
                                            message = "${device.name} was turned off automatically while you were away.",
                                            type = "warning"
                                        )
                                        updateNotification("${device.name} auto-off triggered!")
                                    }
                                }

                                // 2. Schedule Checks
                                val startParts = device.scheduleOn?.split(":")
                                val endParts = device.scheduleOff?.split(":")
                                if (startParts?.size == 2 && endParts?.size == 2) {
                                    val startMinutes = startParts[0].toIntOrNull()?.let { it * 60 + (startParts[1].toIntOrNull() ?: 0) }
                                    val endMinutes = endParts[0].toIntOrNull()?.let { it * 60 + (endParts[1].toIntOrNull() ?: 0) }

                                    if (startMinutes != null && endMinutes != null) {
                                        val shouldBeOn = if (startMinutes <= endMinutes) {
                                            currentTotalMinutes in startMinutes until endMinutes
                                        } else {
                                            currentTotalMinutes >= startMinutes || currentTotalMinutes < endMinutes
                                        }

                                        val targetState = if (shouldBeOn) "ON" else "OFF"
                                        if (device.state != targetState && (device.type == "light" || device.type == "outlet")) {
                                            FirebaseRepository.updateDeviceState(floor.id, device.id, targetState)
                                            FirebaseRepository.logUsage(device.id, device.name, "AUTO-$targetState (Schedule - BG)")
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
                delay(5000) // Check every 5 seconds in background
            }
        }
    }

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val serviceChannel = NotificationChannel(
                CHANNEL_ID,
                "Safety Monitor Service",
                NotificationManager.IMPORTANCE_LOW
            )
            val manager = getSystemService(NotificationManager::class.java)
            manager.createNotificationChannel(serviceChannel)
        }
    }

    private fun createNotification(content: String): Notification {
        return NotificationCompat.Builder(this, CHANNEL_ID)
            .setContentTitle("Safety Monitor Active")
            .setContentText(content)
            .setSmallIcon(android.R.drawable.ic_lock_idle_alarm)
            .build()
    }

    private fun updateNotification(content: String) {
        val notification = createNotification(content)
        val manager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        manager.notify(1, notification)
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        return START_STICKY
    }

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onDestroy() {
        super.onDestroy()
        serviceScope.cancel()
    }
}
