package com.example.smarthomesystem

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.smarthomesystem.ui.theme.SecondaryText
import com.example.smarthomesystem.ui.theme.StatusGreen

@Composable
fun MultiSwitchCard(device: Device, onSwitchToggle: (switchId: String) -> Unit) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .fillMaxHeight()
            .background(
                Brush.verticalGradient(
                    colors = listOf(Color(0xFF0F0F0F), Color(0xFF1A1A1A))
                )
            )
            .padding(24.dp)
    ) {
        Text(
            text = device.name,
            style = MaterialTheme.typography.headlineSmall,
            fontWeight = FontWeight.Bold,
            color = Color.White
        )
        Text(
            text = "${device.switches.size}-switch unit",
            style = MaterialTheme.typography.bodySmall,
            color = SecondaryText
        )

        Spacer(modifier = Modifier.height(24.dp))

        device.switches.toSortedMap().forEach { (switchId, switchState) ->
            val parts = switchId.split("_")
            val idNum = parts.firstOrNull()?.filter { it.isDigit() } ?: "1"
            val label = parts.lastOrNull() ?: "lamp"
            
            SwitchItem(
                label = "Switch $idNum · $label",
                isOn = switchState == "ON",
                onToggle = { onSwitchToggle(switchId) }
            )
        }

        Spacer(modifier = Modifier.height(24.dp))
        
        Text(
            text = "unit id: ${device.id} · last sync 2s ago",
            style = MaterialTheme.typography.bodySmall,
            color = Color(0xFF555555),
            modifier = Modifier.align(Alignment.Start)
        )
    }
}

@Composable
fun SwitchItem(label: String, isOn: Boolean, onToggle: () -> Unit) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 8.dp),
        colors = CardDefaults.cardColors(containerColor = Color(0xFF2A2A2A)),
        shape = RoundedCornerShape(12.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(text = label, color = Color.White)
            Switch(
                checked = isOn,
                onCheckedChange = { onToggle() },
                colors = SwitchDefaults.colors(
                    checkedThumbColor = Color.White,
                    checkedTrackColor = StatusGreen,
                    uncheckedThumbColor = Color(0xFF9E9E9E),
                    uncheckedTrackColor = Color(0xFF424242)
                )
            )
        }
    }
}
