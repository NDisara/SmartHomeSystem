package com.example.smarthomesystem

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.smarthomesystem.ui.theme.SecondaryText

import java.util.Locale

@Composable
fun IronCard(device: Device, onToggle: () -> Unit) {
    val remainingSec = if (device.state == "ON" && device.turnedOnAt > 0) {
        val elapsed = (System.currentTimeMillis() - device.turnedOnAt) / 1000
        (device.maxDurationSec - elapsed).coerceAtLeast(0)
    } else null

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .padding(24.dp)
    ) {
        Text(
            text = device.name,
            style = MaterialTheme.typography.headlineSmall,
            fontWeight = FontWeight.Bold,
            color = Color.White
        )
        Text(
            text = "safety-critical slot",
            style = MaterialTheme.typography.bodySmall,
            color = SecondaryText
        )

        Spacer(modifier = Modifier.height(32.dp))

        val statusText = if (device.state == "ON") {
            val mins = (remainingSec ?: 0L) / 60
            val secs = (remainingSec ?: 0L) % 60
            String.format(Locale.getDefault(), "status: on — %02d:%02d remaining", mins, secs)
        } else {
            "status: off"
        }

        Text(text = statusText, color = Color.White)

        Spacer(modifier = Modifier.height(16.dp))

        Text(text = "max active duration", color = Color.White, style = MaterialTheme.typography.bodySmall)
        
        var sliderPosition by remember { mutableStateOf(device.maxDurationSec / 60f) }
        Slider(
            value = sliderPosition,
            onValueChange = { sliderPosition = it },
            valueRange = 0f..60f,
            colors = SliderDefaults.colors(
                thumbColor = Color.White,
                activeTrackColor = Color(0xFF2196F3)
            )
        )
        Text(text = "${sliderPosition.toInt()} minutes", color = Color.White)

        Spacer(modifier = Modifier.height(32.dp))

        Button(
            onClick = onToggle,
            modifier = Modifier.fillMaxWidth().height(56.dp),
            colors = ButtonDefaults.buttonColors(
                containerColor = if (device.state == "ON") Color(0xFFE0E0E0) else Color(0xFF2196F3)
            ),
            shape = RoundedCornerShape(8.dp)
        ) {
            Text(
                text = if (device.state == "ON") "turn off now" else "turn on",
                color = Color.Black,
                fontWeight = FontWeight.Bold
            )
        }

        Spacer(modifier = Modifier.height(24.dp))
        Text(
            text = "auto cutoff enforced by backend worker",
            style = MaterialTheme.typography.bodySmall,
            color = Color(0xFF555555)
        )
    }
}
