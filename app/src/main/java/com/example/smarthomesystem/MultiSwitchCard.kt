package com.example.smarthomesystem

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp

@Composable
fun MultiSwitchCard(device: Device, onSwitchToggle: (switchId: String) -> Unit) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 8.dp)
            .background(Color(0xFFF0F0F0), RoundedCornerShape(8.dp))
            .padding(12.dp)
    ) {
        Text(text = device.name, style = MaterialTheme.typography.titleMedium)
        Text(text = "${device.switches.size}-switch unit", style = MaterialTheme.typography.labelSmall)

        device.switches.forEach { (switchId, switchState) ->
            val bgColor = if (switchState == "ON") Color(0xFFA5D6A7) else Color(0xFFE0E0E0)

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 6.dp)
                    .background(bgColor, RoundedCornerShape(6.dp))
                    .clickable { onSwitchToggle(switchId) }
                    .padding(10.dp)
            ) {
                Text(text = "Switch: $switchId", modifier = Modifier.padding(end = 8.dp))
                Text(text = switchState, style = MaterialTheme.typography.labelMedium)
            }
        }
    }
}