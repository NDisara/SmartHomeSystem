package com.example.smarthomesystem

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp

@Composable
fun IronCard(device: Device, onToggle: () -> Unit) {
    val bgColor = if (device.state == "ON") Color(0xFFFFCC80) else Color(0xFFE0E0E0)
    val remainingSec = if (device.state == "ON" && device.maxDurationSec > 0) {
        val elapsed = (System.currentTimeMillis() - device.turnedOnAt) / 1000
        (device.maxDurationSec - elapsed).coerceAtLeast(0)
    } else null

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 8.dp)
            .background(bgColor, RoundedCornerShape(8.dp))
            .padding(12.dp)
            .clickable { onToggle() }
    ) {
        Text(text = device.name, style = MaterialTheme.typography.titleMedium)
        Text(text = "State: ${device.state}", style = MaterialTheme.typography.bodySmall)
        Text(
            text = "Max duration: ${device.maxDurationSec / 60} min",
            style = MaterialTheme.typography.labelSmall
        )
        if (remainingSec != null) {
            Text(
                text = "Auto-off in: ${remainingSec / 60}m ${remainingSec % 60}s",
                style = MaterialTheme.typography.labelSmall,
                color = Color.Red
            )
        }
    }
}