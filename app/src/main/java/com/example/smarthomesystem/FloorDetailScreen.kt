package com.example.smarthomesystem

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.smarthomesystem.ui.theme.*

@Composable
fun FloorDetailScreen(
    floorId: String,
    modifier: Modifier = Modifier,
    onDeviceClick: (String) -> Unit
) {
    var devices by remember { mutableStateOf<List<Device>>(emptyList()) }
    var floorName by remember { mutableStateOf("Loading...") }

    LaunchedEffect(floorId) {
        FirebaseRepository.listenToFloors { floors ->
            floorName = floors.find { it.id == floorId }?.name ?: "Unknown Floor"
        }
        FirebaseRepository.listenToDevices(floorId) { updatedDevices ->
            devices = updatedDevices
        }
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .padding(24.dp)
    ) {
        Text(
            text = floorName,
            style = MaterialTheme.typography.headlineMedium,
            fontWeight = FontWeight.Bold,
            color = Color.White
        )
        Text(text = "grid overlay", color = SecondaryText, fontSize = 12.sp)

        Spacer(modifier = Modifier.height(24.dp))

        // Abstract Grid Mapping Overlaid onto Floor Layout
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(250.dp)
                .background(CardBackground, RoundedCornerShape(24.dp))
                .padding(16.dp)
        ) {
            // Stylized abstract "floor plan" elements
            Box(Modifier.fillMaxSize().background(Color.White.copy(alpha = 0.05f), RoundedCornerShape(12.dp)))
            
            // Map devices to grid points based on their index for demo
            devices.forEachIndexed { index, device ->
                val alignment = when (index % 4) {
                    0 -> Alignment.TopStart
                    1 -> Alignment.TopEnd
                    2 -> Alignment.BottomStart
                    else -> Alignment.BottomEnd
                }
                
                DeviceGridNode(
                    device = device,
                    modifier = Modifier.align(alignment).padding(16.dp),
                    onClick = { onDeviceClick(device.id) }
                )
            }
        }

        Spacer(modifier = Modifier.height(32.dp))

        if (devices.isEmpty()) {
            Text(text = "No devices in this floor.", color = SecondaryText)
        } else {
            LazyColumn {
                items(devices) { device ->
                    DeviceRow(device = device, onClick = { onDeviceClick(device.id) })
                }
            }
        }
    }
    
    // Client-side Safety Cutoff Simulation
    LaunchedEffect(devices) {
        devices.forEach { device ->
            if (device.type == "iron" && device.state == "ON" && device.maxDurationSec > 0 && device.turnedOnAt > 0) {
                val elapsed = (System.currentTimeMillis() - device.turnedOnAt) / 1000
                if (elapsed >= device.maxDurationSec) {
                    FirebaseRepository.updateDeviceState(floorId, device.id, "OFF")
                    FirebaseRepository.logUsage(device.id, device.name, "AUTO-OFF (Safety Cutoff)")
                }
            }
        }
    }
}

@Composable
fun DeviceGridNode(device: Device, modifier: Modifier, onClick: () -> Unit) {
    val color = when (device.state) {
        "ON" -> StatusGreen
        "ERROR" -> StatusRed
        "DISCONNECTED" -> Color.Gray
        else -> SecondaryText
    }
    
    Box(
        modifier = modifier
            .size(48.dp)
            .background(color.copy(alpha = 0.2f), RoundedCornerShape(12.dp))
            .clickable { onClick() },
        contentAlignment = Alignment.Center
    ) {
        Box(
            modifier = Modifier
                .size(12.dp)
                .background(color, RoundedCornerShape(2.dp))
        )
    }
}

@Composable
fun DeviceRow(device: Device, onClick: () -> Unit) {
    val statusText = when {
        device.type == "camera" -> "streaming"
        device.state == "ON" -> "on"
        device.state == "OFF" -> "off"
        else -> device.state.lowercase()
    }

    val statusColor = when (statusText) {
        "on" -> StatusGreen
        "error" -> StatusRed
        "streaming" -> StatusBlue
        "off" -> SecondaryText
        else -> SecondaryText
    }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() }
            .padding(vertical = 12.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = device.name,
                style = MaterialTheme.typography.bodyLarge,
                color = Color.White
            )
            Text(
                text = statusText,
                style = MaterialTheme.typography.bodyMedium,
                color = statusColor
            )
        }
        Divider(color = Color(0xFF333333), modifier = Modifier.padding(top = 12.dp))
    }
}
