package com.example.smarthomesystem

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.smarthomesystem.ui.theme.SecondaryText

@Composable
fun DeviceDetailScreen(floorId: String, deviceId: String) {
    var device by remember { mutableStateOf<Device?>(null) }

    LaunchedEffect(floorId, deviceId) {
        FirebaseRepository.listenToDevices(floorId) { devices ->
            device = devices.find { it.id == deviceId }
        }
    }

    Box(modifier = Modifier.fillMaxSize().background(MaterialTheme.colorScheme.background)) {
        device?.let { dev ->
            when (dev.type) {
                "multiswitch" -> MultiSwitchCard(
                    device = dev,
                    onSwitchToggle = { swId ->
                        val current = dev.switches[swId] ?: "OFF"
                        val newState = if (current == "ON") "OFF" else "ON"
                        FirebaseRepository.updateSwitchState(floorId, dev.id, swId, newState)
                        FirebaseRepository.logUsage(dev.id, "${dev.name} ($swId)", newState)
                    }
                )
                "iron" -> IronCard(
                    device = dev,
                    onToggle = {
                        val newState = if (dev.state == "ON") "OFF" else "ON"
                        val timestamp = if (newState == "ON") System.currentTimeMillis() else 0L
                        FirebaseRepository.updateDeviceState(floorId, dev.id, newState, timestamp)
                        FirebaseRepository.logUsage(dev.id, dev.name, newState)
                    }
                )
                "camera" -> CameraCard(device = dev)
                "light" -> LightControlCard(
                    device = dev,
                    onToggle = {
                        val newState = if (dev.state == "ON") "OFF" else "ON"
                        FirebaseRepository.updateDeviceState(floorId, dev.id, newState)
                        FirebaseRepository.logUsage(dev.id, dev.name, newState)
                    }
                )
                else -> OutletControlCard(dev, floorId)
            }
        } ?: Text("Loading device details...", modifier = Modifier.align(Alignment.Center), color = Color.White)
    }
}

@Composable
fun LightControlCard(device: Device, onToggle: () -> Unit) {
    Column(modifier = Modifier.fillMaxSize().padding(24.dp)) {
        Text(text = device.name, style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold, color = Color.White)
        Text(text = "smart light bulb", style = MaterialTheme.typography.bodySmall, color = SecondaryText)

        Spacer(modifier = Modifier.height(32.dp))

        SwitchItem(label = "Power", isOn = device.state == "ON", onToggle = onToggle)

        Spacer(modifier = Modifier.height(32.dp))
        
        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(containerColor = Color(0xFF1A1A1A)),
            shape = RoundedCornerShape(16.dp)
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.Schedule, contentDescription = null, tint = Color.White)
                    Spacer(Modifier.width(8.dp))
                    Text(text = "Scheduling", color = Color.White, fontWeight = FontWeight.Bold)
                }
                Spacer(Modifier.height(16.dp))
                ScheduleRow(label = "Turn ON at", time = device.scheduleOn ?: "Not set")
                HorizontalDivider(color = Color(0xFF333333), modifier = Modifier.padding(vertical = 12.dp))
                ScheduleRow(label = "Turn OFF at", time = device.scheduleOff ?: "Not set")
            }
        }
    }
}

@Composable
fun ScheduleRow(label: String, time: String) {
    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
        Text(text = label, color = SecondaryText)
        Text(text = time, color = Color.White, fontWeight = FontWeight.Medium)
    }
}

@Composable
fun OutletControlCard(device: Device, floorId: String) {
    Column(modifier = Modifier.fillMaxSize().padding(24.dp)) {
        Text(text = device.name, style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold, color = Color.White)
        Text(text = "power outlet", style = MaterialTheme.typography.bodySmall, color = SecondaryText)
        Spacer(modifier = Modifier.height(32.dp))
        SwitchItem(
            label = "State",
            isOn = device.state == "ON",
            onToggle = {
                val newState = if (device.state == "ON") "OFF" else "ON"
                FirebaseRepository.updateDeviceState(floorId, device.id, newState)
                FirebaseRepository.logUsage(device.id, device.name, newState)
            }
        )
    }
}
