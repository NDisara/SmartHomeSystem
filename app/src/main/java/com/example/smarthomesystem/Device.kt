package com.example.smarthomesystem

data class Device(
    val id: String = "",
    val name: String = "",
    val type: String = "",       // "outlet", "multiswitch", "iron", "camera", "light"
    val state: String = "OFF",   // "ON", "OFF", "ERROR", "DISCONNECTED", "STREAMING"
    val room: String = "",       // "Kitchen", "Living room", "Utility", "Porch"
    val switches: Map<String, String> = emptyMap(), // For multiswitch: {"sw1": "ON", "sw2": "OFF"}
    val maxDurationSec: Int = 0, // For iron: maximum allowed time ON
    val turnedOnAt: Long = 0L,   // Timestamp when turned ON
    val streamUrl: String = "",  // For camera
    val scheduleOn: String? = null,  // Format "HH:mm"
    val scheduleOff: String? = null, // Format "HH:mm"
    val floorId: String = "",
    val scheduleEnabled: Boolean = false,
    val scheduleStartHour: Int = 0,
    val scheduleEndHour: Int = 0
)
