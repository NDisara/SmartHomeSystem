package com.example.smarthomesystem

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp

@Composable
fun FloorDetailScreen(floorId: String, modifier: Modifier = Modifier) {
    var devices by remember { mutableStateOf<List<Device>>(emptyList()) }

    LaunchedEffect(floorId) {
        FirebaseRepository.listenToDevices(floorId) { updatedDevices ->
            devices = updatedDevices
        }
    }

    Box(modifier = modifier.fillMaxSize()) {

        // ---- Background Floor Plan Image ----
        Image(
            painter = painterResource(id = R.drawable.floor1),
            contentDescription = "Floor plan background",
            modifier = Modifier.fillMaxSize(),
            contentScale = ContentScale.Crop
        )

        // ---- Semi-transparent overlay for readability ----
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Color.Black.copy(alpha = 0.45f))
        )

        // ---- Foreground content (devices) ----
        Column(modifier = Modifier.fillMaxSize().padding(16.dp)) {
            Text(
                text = "Floor: $floorId",
                style = MaterialTheme.typography.headlineSmall,
                color = Color.White,
                modifier = Modifier.padding(bottom = 16.dp)
            )

            if (devices.isEmpty()) {
                Text(text = "Loading devices...", color = Color.White)
            } else {
                LazyColumn {
                    items(devices) { device ->
                        when (device.type) {
                            "multiswitch" -> MultiSwitchCard(
                                device = device,
                                onSwitchToggle = { switchId ->
                                    val current = device.switches[switchId] ?: "OFF"
                                    val newState = if (current == "ON") "OFF" else "ON"
                                    FirebaseRepository.updateSwitchState(floorId, device.id, switchId, newState)
                                    FirebaseRepository.logUsage(device.id, "${device.name} ($switchId)", newState)
                                }
                            )
                            "iron" -> IronCard(
                                device = device,
                                onToggle = {
                                    if (device.state == "ON") {
                                        FirebaseRepository.updateDeviceState(floorId, device.id, "OFF")
                                        FirebaseRepository.logUsage(device.id, device.name, "OFF")
                                    } else {
                                        FirebaseRepository.updateDeviceState(
                                            floorId, device.id, "ON", System.currentTimeMillis()
                                        )
                                        FirebaseRepository.logUsage(device.id, device.name, "ON")
                                    }
                                }
                            )
                            "camera" -> CameraCard(device = device)
                            else -> DeviceSlot(
                                device = device,
                                onClick = {
                                    val newState = if (device.state == "ON") "OFF" else "ON"
                                    FirebaseRepository.updateDeviceState(floorId, device.id, newState)
                                    FirebaseRepository.logUsage(device.id, device.name, newState)
                                }
                            )
                        }
                    }
                }
            }
        }
    }

    // ---- Safety cutoff check ----
    LaunchedEffect(devices) {
        devices.forEach { device ->
            if (device.type == "iron" && device.state == "ON" && device.maxDurationSec > 0) {
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
fun DeviceSlot(device: Device, onClick: () -> Unit) {
    val bgColor = if (device.state == "ON") Color(0xFFA5D6A7) else Color(0xFFE0E0E0)

    Box(
        modifier = Modifier
            .fillMaxSize()
            .padding(vertical = 8.dp)
            .background(bgColor, RoundedCornerShape(8.dp))
            .clickable { onClick() }
    ) {
        Column(modifier = Modifier.padding(12.dp)) {
            Text(text = device.name, style = MaterialTheme.typography.bodyMedium)
            Text(text = device.state, style = MaterialTheme.typography.labelSmall)
        }
    }
}