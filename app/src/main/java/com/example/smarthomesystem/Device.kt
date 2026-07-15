package com.example.smarthomesystem

data class Device(
    val id: String = "",
    val name: String = "",
    val type: String = "",       // "outlet", "multiswitch", "iron", "camera"
    val state: String = "OFF",   // used for outlet/iron/camera (single state)
    val switches: Map<String, String> = emptyMap(), // used for multiswitch: e.g. {"sw1": "ON", "sw2": "OFF"}
    val maxDurationSec: Int = 0, // used for iron (0 = no limit)
    val turnedOnAt: Long = 0L,   // used for iron (timestamp when turned ON)
    val streamUrl: String = ""   // used for camera
)