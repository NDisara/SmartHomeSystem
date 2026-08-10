package com.example.smarthomesystem

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.smarthomesystem.ui.theme.SecondaryText
import java.util.*
import android.app.TimePickerDialog
import kotlinx.coroutines.delay

@Composable
fun DeviceDetailScreen(floorId: String, deviceId: String) {
    var device by remember { mutableStateOf<Device?>(null) }

    LaunchedEffect(floorId, deviceId) {
        FirebaseRepository.listenToDevices(floorId) { devices ->
            device = devices.find { it.id == deviceId }
        }
    }

    // --- Auto-scheduling and Safety Cutoff logic ---
    var serverTimeOffset by remember { mutableLongStateOf(0L) }
    LaunchedEffect(Unit) {
        FirebaseRepository.listenToServerTimeOffset { offset ->
            serverTimeOffset = offset
        }
    }

    LaunchedEffect(device) {
        while (true) {
            val currentDevice = device ?: break
            val calendar = Calendar.getInstance()
            val currentTotalMinutes = calendar.get(Calendar.HOUR_OF_DAY) * 60 + calendar.get(Calendar.MINUTE)
            val adjustedNow = System.currentTimeMillis() + serverTimeOffset

            // 1. Safety Cutoff for Iron
            if (currentDevice.type.lowercase() == "iron" && currentDevice.state.uppercase() == "ON" && currentDevice.maxDurationSec > 0 && currentDevice.turnedOnAt > 0) {
                val elapsed = (adjustedNow - currentDevice.turnedOnAt) / 1000
                if (elapsed >= currentDevice.maxDurationSec) {
                    FirebaseRepository.updateDeviceState(floorId, currentDevice.id, "OFF")
                    FirebaseRepository.logUsage(currentDevice.id, currentDevice.name, "AUTO-OFF (Safety Cutoff)")
                }
            }

            // 2. Scheduling for Light/Outlet
            val startParts = currentDevice.scheduleOn?.split(":")
            val endParts = currentDevice.scheduleOff?.split(":")

            if (startParts?.size == 2 && endParts?.size == 2) {
                val startMinutes = startParts[0].toIntOrNull()?.let { it * 60 + (startParts[1].toIntOrNull() ?: 0) }
                val endMinutes = endParts[0].toIntOrNull()?.let { it * 60 + (endParts[1].toIntOrNull() ?: 0) }

                if (startMinutes != null && endMinutes != null) {
                    val shouldBeOn = if (startMinutes <= endMinutes) {
                        currentTotalMinutes in startMinutes until endMinutes
                    } else {
                        currentTotalMinutes >= startMinutes || currentTotalMinutes < endMinutes
                    }

                    val targetState = if (shouldBeOn) "ON" else "OFF"
                    if (currentDevice.state != targetState && (currentDevice.type == "light" || currentDevice.type == "outlet")) {
                        FirebaseRepository.updateDeviceState(floorId, currentDevice.id, targetState)
                        FirebaseRepository.logUsage(currentDevice.id, currentDevice.name, "AUTO-$targetState (Schedule)")
                    }
                }
            }
            delay(2000) // Check more frequently for safety (every 2s instead of 30s)
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
                    floorId = floorId,
                    onToggle = {
                        val newState = if (dev.state == "ON") "OFF" else "ON"
                        FirebaseRepository.updateDeviceState(floorId, dev.id, newState)
                        FirebaseRepository.logUsage(dev.id, dev.name, newState)
                    }
                )
                "camera" -> CameraCard(device = dev)
                "light" -> LightControlCard(
                    device = dev,
                    floorId = floorId,
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
fun LightControlCard(device: Device, floorId: String, onToggle: () -> Unit) {
    val context = LocalContext.current
    
    fun showTimePicker(type: String) {
        val calendar = Calendar.getInstance()
        val hour = calendar.get(Calendar.HOUR_OF_DAY)
        val minute = calendar.get(Calendar.MINUTE)
        
        TimePickerDialog(context, { _, h, m ->
            val time = String.format("%02d:%02d", h, m)
            FirebaseRepository.updateSchedule(floorId, device.id, type, time)
        }, hour, minute, true).show()
    }

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
                ScheduleRow(
                    label = "Turn ON at", 
                    time = device.scheduleOn ?: "Not set",
                    onClick = { showTimePicker("ON") }
                )
                HorizontalDivider(color = Color(0xFF333333), modifier = Modifier.padding(vertical = 12.dp))
                ScheduleRow(
                    label = "Turn OFF at", 
                    time = device.scheduleOff ?: "Not set",
                    onClick = { showTimePicker("OFF") }
                )
            }
        }
    }
}

@Composable
fun ScheduleRow(label: String, time: String, onClick: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() }, 
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(text = label, color = SecondaryText)
        Text(text = time, color = Color.White, fontWeight = FontWeight.Medium)
    }
}

@Composable
fun OutletControlCard(device: Device, floorId: String) {
    val context = LocalContext.current

    fun showTimePicker(type: String) {
        val calendar = Calendar.getInstance()
        val hour = calendar.get(Calendar.HOUR_OF_DAY)
        val minute = calendar.get(Calendar.MINUTE)

        TimePickerDialog(context, { _, h, m ->
            val time = String.format("%02d:%02d", h, m)
            FirebaseRepository.updateSchedule(floorId, device.id, type, time)
        }, hour, minute, true).show()
    }

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
                ScheduleRow(
                    label = "Turn ON at",
                    time = device.scheduleOn ?: "Not set",
                    onClick = { showTimePicker("ON") }
                )
                HorizontalDivider(color = Color(0xFF333333), modifier = Modifier.padding(vertical = 12.dp))
                ScheduleRow(
                    label = "Turn OFF at",
                    time = device.scheduleOff ?: "Not set",
                    onClick = { showTimePicker("OFF") }
                )
            }
        }
    }
}
