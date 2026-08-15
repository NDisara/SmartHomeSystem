package com.example.smarthomesystem

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.smarthomesystem.ui.theme.SecondaryText
import kotlinx.coroutines.delay

import java.util.Locale

@Composable
fun IronCard(device: Device, floorId: String, onToggle: () -> Unit) {
    var currentTime by remember { mutableLongStateOf(System.currentTimeMillis()) }
    var serverTimeOffset by remember { mutableLongStateOf(0L) }

    LaunchedEffect(Unit) {
        FirebaseRepository.listenToServerTimeOffset { offset ->
            serverTimeOffset = offset
        }
    }

    LaunchedEffect(device.state, device.turnedOnAt) {
        if (device.state == "ON" && device.turnedOnAt > 0) {
            currentTime = System.currentTimeMillis() // Update immediately on restart
            while (true) {
                delay(1000)
                currentTime = System.currentTimeMillis()
            }
        }
    }

    val adjustedCurrentTime = currentTime + serverTimeOffset

    val remainingSec = if (device.state == "ON" && device.turnedOnAt > 0) {
        // Use maxOf to ensure we don't get negative elapsed time if adjustedCurrentTime is slightly behind the new turnedOnAt
        val elapsed = (maxOf(adjustedCurrentTime, device.turnedOnAt) - device.turnedOnAt) / 1000
        (device.maxDurationSec - elapsed).coerceAtLeast(0)
    } else null

    Column(
        modifier = Modifier
            .fillMaxSize()
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
        
        var sliderPosition by remember(device.maxDurationSec) { mutableFloatStateOf(device.maxDurationSec / 60f) }
        Slider(
            value = sliderPosition,
            onValueChange = { sliderPosition = it },
            onValueChangeFinished = {
                val newDurationSec = (sliderPosition * 60).toInt()
                // Optimistically reset local timer if device is ON
                if (device.state == "ON") {
                    currentTime = System.currentTimeMillis()
                }
                FirebaseRepository.updateMaxDuration(floorId, device.id, newDurationSec, device.state == "ON")
            },
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
