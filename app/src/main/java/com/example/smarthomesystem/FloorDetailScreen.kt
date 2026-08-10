package com.example.smarthomesystem

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.AltRoute
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.smarthomesystem.ui.theme.*
import java.util.Calendar
import kotlinx.coroutines.delay

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
            floorName = floors.find { it.id == floorId }?.name ?: "Ground floor"
        }
        FirebaseRepository.listenToDevices(floorId) { updatedDevices ->
            devices = updatedDevices
        }
    }

    val roomGroupedDevices = remember(devices) {
        devices.groupBy { getRoomName(it) }
    }
    val uniqueRooms = remember(roomGroupedDevices) {
        roomGroupedDevices.keys.toList()
    }

    LazyColumn(
        modifier = modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background),
        contentPadding = PaddingValues(start = 20.dp, end = 20.dp, top = 24.dp, bottom = 140.dp)
    ) {
        // --- Top Header ---
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(
                        text = floorName,
                        style = MaterialTheme.typography.headlineMedium,
                        fontWeight = FontWeight.Bold,
                        color = Color.White
                    )
                    Text(
                        text = "${devices.size} devices · ${if (uniqueRooms.isEmpty()) 4 else uniqueRooms.size} rooms",
                        style = MaterialTheme.typography.bodyMedium,
                        color = SecondaryText
                    )
                }

                Box(
                    modifier = Modifier
                        .size(40.dp)
                        .background(Color(0xFF282828), CircleShape)
                        .clickable { /* Action to add device */ },
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.Add,
                        contentDescription = "Add",
                        tint = Color.White,
                        modifier = Modifier.size(22.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(20.dp))
        }

        // --- Floor Structure Layout Card (2x2 Grid with device icons inside rooms) ---
        item {
            FloorStructurePlanCard(
                devices = devices,
                uniqueRooms = uniqueRooms
            )

            Spacer(modifier = Modifier.height(24.dp))
        }

        // --- Grouped Devices by Room List ---
        if (devices.isEmpty()) {
            item {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 40.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(text = "No devices on this floor.", color = SecondaryText)
                }
            }
        } else {
            items(uniqueRooms) { roomName ->
                val roomDevices = roomGroupedDevices[roomName] ?: emptyList()
                RoomGroupSection(
                    roomName = roomName,
                    devices = roomDevices,
                    onDeviceClick = onDeviceClick
                )
            }
        }
    }

    // --- Safety Cutoff Timer Simulation ---
    LaunchedEffect(devices) {
        devices.forEach { device ->
            if (device.type.lowercase() == "iron" && device.state.uppercase() == "ON" && device.maxDurationSec > 0 && device.turnedOnAt > 0) {
                val elapsed = (System.currentTimeMillis() - device.turnedOnAt) / 1000
                if (elapsed >= device.maxDurationSec) {
                    FirebaseRepository.updateDeviceState(floorId, device.id, "OFF")
                    FirebaseRepository.logUsage(device.id, device.name, "AUTO-OFF (Safety Cutoff)")
                }
            }
        }
    }

    // --- Auto-scheduled devices check (Improved for precise time) ---
    LaunchedEffect(devices) {
        while (true) {
            val calendar = Calendar.getInstance()
            val currentTotalMinutes = calendar.get(Calendar.HOUR_OF_DAY) * 60 + calendar.get(Calendar.MINUTE)

            devices.forEach { device ->
                val startParts = device.scheduleOn?.split(":")
                val endParts = device.scheduleOff?.split(":")

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
                        if (device.state != targetState && (device.type == "light" || device.type == "outlet")) {
                            FirebaseRepository.updateDeviceState(floorId, device.id, targetState)
                            FirebaseRepository.logUsage(device.id, device.name, "AUTO-$targetState (Schedule)")
                        }
                    }
                }
            }
            delay(30000) // Check every 30 seconds
        }
    }
}

@Composable
fun FloorStructurePlanCard(
    devices: List<Device>,
    uniqueRooms: List<String>,
    modifier: Modifier = Modifier
) {
    val defaultRooms = listOf("Kitchen", "Porch", "Utility", "Living room")
    val displayRooms = (uniqueRooms + defaultRooms).distinct().take(4)

    Card(
        modifier = modifier
            .fillMaxWidth()
            .height(230.dp),
        colors = CardDefaults.cardColors(containerColor = Color(0xFF1E1E1E)),
        shape = RoundedCornerShape(20.dp),
        border = BorderStroke(1.dp, Color(0xFF333333))
    ) {
        Column(modifier = Modifier.fillMaxSize()) {
            Row(modifier = Modifier.weight(1f).fillMaxWidth()) {
                // Top-Left Quadrant
                RoomQuadrantCell(
                    roomName = displayRooms.getOrNull(0) ?: "Kitchen",
                    devices = devices.filter { getRoomName(it).equals(displayRooms.getOrNull(0) ?: "Kitchen", ignoreCase = true) },
                    modifier = Modifier.weight(1f).fillMaxHeight()
                )
                HorizontalDivider(
                    modifier = Modifier
                        .fillMaxHeight()
                        .width(1.dp),
                    color = Color(0xFF333333)
                )
                // Top-Right Quadrant
                RoomQuadrantCell(
                    roomName = displayRooms.getOrNull(1) ?: "Porch",
                    devices = devices.filter { getRoomName(it).equals(displayRooms.getOrNull(1) ?: "Porch", ignoreCase = true) },
                    modifier = Modifier.weight(1f).fillMaxHeight()
                )
            }
            HorizontalDivider(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(1.dp),
                color = Color(0xFF333333)
            )
            Row(modifier = Modifier.weight(1f).fillMaxWidth()) {
                // Bottom-Left Quadrant
                RoomQuadrantCell(
                    roomName = displayRooms.getOrNull(2) ?: "Utility",
                    devices = devices.filter { getRoomName(it).equals(displayRooms.getOrNull(2) ?: "Utility", ignoreCase = true) },
                    modifier = Modifier.weight(1f).fillMaxHeight()
                )
                HorizontalDivider(
                    modifier = Modifier
                        .fillMaxHeight()
                        .width(1.dp),
                    color = Color(0xFF333333)
                )
                // Bottom-Right Quadrant
                RoomQuadrantCell(
                    roomName = displayRooms.getOrNull(3) ?: "Living room",
                    devices = devices.filter { getRoomName(it).equals(displayRooms.getOrNull(3) ?: "Living room", ignoreCase = true) },
                    modifier = Modifier.weight(1f).fillMaxHeight()
                )
            }
        }
    }
}

@Composable
fun RoomQuadrantCell(
    roomName: String,
    devices: List<Device>,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier.padding(14.dp),
        verticalArrangement = Arrangement.SpaceBetween
    ) {
        Text(
            text = roomName,
            fontSize = 13.sp,
            color = SecondaryText,
            fontWeight = FontWeight.Medium
        )

        Row(
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            devices.forEach { device ->
                val icon = getDeviceIcon(device)
                val statusColor = getDeviceStatusColor(device)

                Box(
                    modifier = Modifier
                        .size(32.dp)
                        .background(Color(0xFF282828), CircleShape)
                        .border(1.dp, statusColor.copy(alpha = 0.4f), CircleShape),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = icon,
                        contentDescription = device.name,
                        tint = statusColor,
                        modifier = Modifier.size(18.dp)
                    )
                }
            }
        }
    }
}

@Composable
fun RoomGroupSection(
    roomName: String,
    devices: List<Device>,
    onDeviceClick: (String) -> Unit
) {
    Column(modifier = Modifier.padding(bottom = 20.dp)) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.padding(bottom = 10.dp)
        ) {
            Icon(
                imageVector = getRoomIcon(roomName),
                contentDescription = null,
                tint = SecondaryText,
                modifier = Modifier.size(20.dp)
            )
            Spacer(modifier = Modifier.width(8.dp))
            Text(
                text = roomName,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = Color.White
            )
        }

        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(containerColor = Color(0xFF1E1E1E)),
            shape = RoundedCornerShape(16.dp),
            border = BorderStroke(1.dp, Color(0xFF2A2A2A))
        ) {
            Column {
                devices.forEachIndexed { index, device ->
                    DeviceRowItem(
                        device = device,
                        onClick = { onDeviceClick(device.id) }
                    )
                    if (index < devices.size - 1) {
                        HorizontalDivider(
                            color = Color(0xFF2A2A2A),
                            thickness = 1.dp,
                            modifier = Modifier.padding(horizontal = 16.dp)
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun DeviceRowItem(
    device: Device,
    onClick: () -> Unit
) {
    val statusColor = getDeviceStatusColor(device)
    val deviceIcon = getDeviceIcon(device)

    val isIronOn = device.type.lowercase() == "iron" && device.state.uppercase() == "ON"
    var currentTime by remember { mutableLongStateOf(System.currentTimeMillis()) }

    LaunchedEffect(isIronOn, device.turnedOnAt) {
        if (isIronOn && device.turnedOnAt > 0) {
            while (true) {
                currentTime = System.currentTimeMillis()
                delay(1000)
            }
        }
    }

    val statusText = when {
        isIronOn && device.maxDurationSec > 0 && device.turnedOnAt > 0 -> {
            val elapsedSec = (currentTime - device.turnedOnAt) / 1000
            val remainingSec = (device.maxDurationSec - elapsedSec).coerceAtLeast(0)
            val mins = remainingSec / 60
            val secs = remainingSec % 60
            String.format("On · %02d:%02d left", mins, secs)
        }
        device.type.lowercase() == "camera" || device.state.uppercase() == "STREAMING" -> "Streaming"
        else -> {
            val baseStatus = when (device.state.uppercase()) {
                "ON" -> "On"
                "OFF" -> "Off"
                else -> device.state.lowercase().replaceFirstChar { it.uppercase() }
            }
            if (device.scheduleOn != null && device.scheduleOff != null) {
                "$baseStatus · ${device.scheduleOn} - ${device.scheduleOff}"
            } else {
                baseStatus
            }
        }
    }

    val rowModifier = if (isIronOn) {
        Modifier
            .fillMaxWidth()
            .background(Color(0xFF2D1B1B), RoundedCornerShape(12.dp))
            .border(1.dp, StatusRed.copy(alpha = 0.5f), RoundedCornerShape(12.dp))
            .clickable { onClick() }
            .padding(horizontal = 16.dp, vertical = 14.dp)
    } else {
        Modifier
            .fillMaxWidth()
            .clickable { onClick() }
            .padding(horizontal = 16.dp, vertical = 14.dp)
    }

    Row(
        modifier = rowModifier,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .size(36.dp)
                .background(statusColor.copy(alpha = 0.15f), CircleShape),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = deviceIcon,
                contentDescription = null,
                tint = statusColor,
                modifier = Modifier.size(20.dp)
            )
        }

        Spacer(modifier = Modifier.width(14.dp))

        Text(
            text = device.name,
            style = MaterialTheme.typography.bodyLarge,
            fontWeight = FontWeight.SemiBold,
            color = Color.White,
            modifier = Modifier.weight(1f)
        )

        Text(
            text = statusText,
            style = MaterialTheme.typography.bodyMedium,
            fontWeight = FontWeight.Medium,
            color = statusColor
        )
    }
}

fun getRoomName(device: Device): String {
    if (device.room.isNotBlank()) return device.room
    val nameLower = device.name.lowercase()
    return when {
        "kitchen" in nameLower || "toaster" in nameLower || "outlet" in nameLower || "light 1" in nameLower -> "Kitchen"
        "living" in nameLower || "gang" in nameLower || "lamp" in nameLower -> "Living room"
        "utility" in nameLower || "iron" in nameLower -> "Utility"
        "porch" in nameLower || "camera" in nameLower -> "Porch"
        "garage" in nameLower || "door" in nameLower -> "Garage"
        else -> "Kitchen"
    }
}

fun getRoomIcon(roomName: String): ImageVector {
    val nameLower = roomName.lowercase()
    return when {
        "kitchen" in nameLower -> Icons.Outlined.Restaurant
        "living" in nameLower -> Icons.Outlined.Weekend
        "utility" in nameLower -> Icons.Outlined.LocalLaundryService
        "porch" in nameLower -> Icons.Outlined.DoorFront
        "garage" in nameLower -> Icons.Outlined.DirectionsCar
        "bed" in nameLower -> Icons.Outlined.SingleBed
        "bath" in nameLower -> Icons.Outlined.Bathtub
        "balcony" in nameLower -> Icons.Outlined.Balcony
        else -> Icons.Outlined.HomeWork
    }
}

fun getDeviceIcon(device: Device): ImageVector {
    val typeLower = device.type.lowercase()
    val nameLower = device.name.lowercase()
    return when {
        typeLower == "camera" || "camera" in nameLower -> Icons.Outlined.PhotoCamera
        typeLower == "iron" || "iron" in nameLower -> Icons.Outlined.Iron
        typeLower == "multiswitch" || "gang" in nameLower || "switch" in nameLower -> Icons.AutoMirrored.Outlined.AltRoute
        typeLower == "light" || "lamp" in nameLower || "light" in nameLower -> Icons.Outlined.Lightbulb
        typeLower == "outlet" || "plug" in nameLower || "outlet" in nameLower -> Icons.Outlined.Power
        else -> Icons.Outlined.Power
    }
}

fun getDeviceStatusColor(device: Device): Color {
    val stateUpper = device.state.uppercase()
    return when {
        stateUpper == "STREAMING" || device.type.lowercase() == "camera" -> Color(0xFFF57C00)
        stateUpper == "ON" && device.type.lowercase() == "iron" -> StatusRed
        stateUpper == "ON" -> StatusGreen
        stateUpper == "ERROR" -> StatusRed
        else -> SecondaryText
    }
}
