package com.example.smarthomesystem

data class Alert(
    val id: String = "",
    val title: String = "",
    val message: String = "",
    val timestamp: Long = 0L,
    val type: String = "info", // "info", "warning", "critical"
    val isRead: Boolean = false
)
