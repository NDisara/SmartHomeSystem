package com.example.smarthomesystem

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp

@Composable
fun CameraCard(device: Device) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 8.dp)
            .background(Color(0xFFF0F0F0), RoundedCornerShape(8.dp))
            .padding(12.dp)
    ) {
        Text(text = device.name, style = MaterialTheme.typography.titleMedium)
        Text(text = "Camera Feed", style = MaterialTheme.typography.labelSmall)

        // Mock camera preview box (real image/stream පස්සේ Coil වගේ library එකකින් add කරන්න පුළුවන්)
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .aspectRatio(16f / 9f)
                .padding(top = 8.dp)
                .background(Color.Black, RoundedCornerShape(6.dp)),
            contentAlignment = Alignment.Center
        ) {
            Text(text = "📷 Mock Camera Feed", color = Color.White)
        }
    }
}