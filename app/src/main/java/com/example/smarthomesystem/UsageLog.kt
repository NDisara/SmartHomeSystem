package com.example.smarthomesystem

data class UsageLog(
    val deviceId: String = "",
    val deviceName: String = "",
    val action: String = "",   // "ON" or "OFF"
    val timestamp: Long = System.currentTimeMillis()
)