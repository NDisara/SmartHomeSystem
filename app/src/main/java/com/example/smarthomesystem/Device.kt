package com.example.smarthomesystem

data class Device(
    val id: String = "",
    val name: String = "",
    val type: String = "",
    val state: String = "OFF",
    val switches: Map<String, String> = emptyMap(),
    val maxDurationSec: Int = 0,
    val turnedOnAt: Long = 0L,
    val streamUrl: String = "",
    val scheduleEnabled: Boolean = false,   // light auto-schedule on/off
    val scheduleStartHour: Int = 0,         // 0-23 (24hr format)
    val scheduleEndHour: Int = 0            // 0-23
)