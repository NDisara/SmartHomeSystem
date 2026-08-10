package com.example.smarthomesystem

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
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
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
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
    var showAddDeviceDialog by remember { mutableStateOf(false) }
    val scrollState = rememberScrollState()

    // Debugging check
    println("FloorDetailScreen: Recomposing for floor $floorId. Dialog visible: $showAddDeviceDialog")

    LaunchedEffect(floorId) {
        FirebaseRepository.listenToFloors { floors ->
            floorName = floors.find { it.id == floorId }?.name ?: "Unknown Floor"
        }
        FirebaseRepository.listenToDevices(floorId) { updatedDevices ->
            devices = updatedDevices
        }
    }

    // --- Safety Cutoff Timer Simulation ---
    var serverTimeOffset by remember { mutableLongStateOf(0L) }
    LaunchedEffect(Unit) {
        FirebaseRepository.listenToServerTimeOffset { offset ->
            serverTimeOffset = offset
        }
    }

    LaunchedEffect(devices) {
        while (true) {
            val adjustedNow = System.currentTimeMillis() + serverTimeOffset
            devices.forEach { device ->
                if (device.type.lowercase() == "iron" && device.state.uppercase() == "ON" && device.maxDurationSec > 0 && device.turnedOnAt > 0) {
                    val elapsed = (adjustedNow - device.turnedOnAt) / 1000
                    if (elapsed >= device.maxDurationSec) {
                        FirebaseRepository.updateDeviceState(floorId, device.id, "OFF")
                        FirebaseRepository.logUsage(device.id, device.name, "AUTO-OFF (Safety Cutoff)")
                    }
                }
            }
            delay(2000) // Check every 2 seconds for safety
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

    val activeRooms = devices.map { it.room }.filter { it.isNotBlank() }.distinct().sorted()

    if (showAddDeviceDialog) {
        AddDeviceDialog(
            onDismiss = { showAddDeviceDialog = false },
            onConfirm = { name, type, room ->
                FirebaseRepository.addDevice(floorId, name, type, room)
                showAddDeviceDialog = false
            }
        )
    }

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(Color(0xFF121212))
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(scrollState)
                .padding(horizontal = 24.dp)
        ) {
            Spacer(modifier = Modifier.height(24.dp))

            // Header Section
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = floorName,
                        style = MaterialTheme.typography.headlineMedium,
                        fontWeight = FontWeight.Bold,
                        color = Color.White
                    )
                    Text(
                        text = "${devices.size} devices · ${activeRooms.size} rooms",
                        color = Color(0xFF888888),
                        fontSize = 14.sp
                    )
                }
            }

            Spacer(modifier = Modifier.height(32.dp))

            if (devices.isEmpty()) {
                EmptyFloorState { showAddDeviceDialog = true }
            } else {
                // Show floor plan grid for Ground floor or First floor
                val isMainFloor = floorName.contains("Ground", ignoreCase = true) || 
                                 floorName.contains("First", ignoreCase = true) ||
                                 floorId == "ground"
                
                if (isMainFloor && activeRooms.isNotEmpty()) {
                    FloorPlanGrid(floorName, devices, onDeviceClick)
                    Spacer(modifier = Modifier.height(32.dp))
                }

                // Device List Sections (Grouped by Actual Rooms)
                activeRooms.forEach { roomName ->
                    val roomDevices = devices.filter { it.room == roomName }
                    RoomLabel(roomName)
                    roomDevices.forEach { device ->
                        DeviceItemRow(device, onClick = { onDeviceClick(device.id) })
                    }
                    Spacer(Modifier.height(24.dp))
                }
            }
            
            Spacer(modifier = Modifier.height(32.dp))
        }

        // --- NEW: Using a Floating Action Button for better reliability ---
        FloatingActionButton(
            onClick = { 
                println("FAB Clicked: Opening Add Device Dialog")
                showAddDeviceDialog = true 
            },
            containerColor = Color(0xFF2196F3),
            contentColor = Color.White,
            shape = CircleShape,
            modifier = Modifier
                .align(Alignment.BottomEnd)
                .padding(24.dp)
                .size(64.dp) // Large touch target
        ) {
            Icon(Icons.Default.Add, contentDescription = "Add Device", modifier = Modifier.size(32.dp))
        }
    }
}

@Composable
fun AddDeviceDialog(onDismiss: () -> Unit, onConfirm: (String, String, String) -> Unit) {
    var name by remember { mutableStateOf("") }
    var room by remember { mutableStateOf("") }
    var selectedType by remember { mutableStateOf("light") }
    val types = listOf("light", "outlet", "iron", "camera", "multiswitch")

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Add New Device", color = Color.White) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it },
                    label = { Text("Device Name") },
                    placeholder = { Text("e.g. Bedside Lamp") },
                    colors = OutlinedTextFieldDefaults.colors(focusedTextColor = Color.White, unfocusedTextColor = Color.White)
                )
                OutlinedTextField(
                    value = room,
                    onValueChange = { room = it },
                    label = { Text("Room Name") },
                    placeholder = { Text("e.g. Bedroom") },
                    colors = OutlinedTextFieldDefaults.colors(focusedTextColor = Color.White, unfocusedTextColor = Color.White)
                )
                Text("Device Type", color = Color.Gray, fontSize = 12.sp)
                Row(modifier = Modifier.horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    types.forEach { type ->
                        FilterChip(
                            selected = selectedType == type,
                            onClick = { selectedType = type },
                            label = { Text(type) },
                            colors = FilterChipDefaults.filterChipColors(labelColor = Color.Gray, selectedLabelColor = Color.White, selectedContainerColor = Color(0xFF2196F3))
                        )
                    }
                }
            }
        },
        confirmButton = {
            Button(onClick = { if (name.isNotBlank() && room.isNotBlank()) onConfirm(name, selectedType, room) }) {
                Text("Add Device")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Cancel", color = Color.Gray) }
        },
        containerColor = Color(0xFF1E1E1E)
    )
}

@Composable
fun EmptyFloorState(onAddClick: () -> Unit) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(300.dp)
            .background(Color(0xFF1A1A1A), RoundedCornerShape(16.dp))
            .border(1.dp, Color(0xFF222222), RoundedCornerShape(16.dp))
            .clickable { onAddClick() },
        contentAlignment = Alignment.Center
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.padding(32.dp)) {
            Icon(Icons.Outlined.Devices, contentDescription = null, tint = Color(0xFF333333), modifier = Modifier.size(64.dp))
            Spacer(Modifier.height(16.dp))
            Text("No devices here", color = Color.White, fontWeight = FontWeight.Bold)
            Text("Tap here or use the + button to add your first smart device.", color = Color(0xFF666666), fontSize = 12.sp, textAlign = TextAlign.Center)
        }
    }
}

@Composable
fun FloorPlanGrid(floorName: String, devices: List<Device>, onDeviceClick: (String) -> Unit) {
    val isFirstFloor = floorName.contains("First", ignoreCase = true)
    val isGroundFloor = floorName.contains("Ground", ignoreCase = true) || floorName.lowercase() == "ground"

    val gridHeight = if (isGroundFloor) 220.dp else 160.dp

    Box(modifier = Modifier.fillMaxWidth().height(gridHeight + 40.dp), contentAlignment = Alignment.Center) {
        Column(modifier = Modifier.width(280.dp).height(gridHeight).border(1.dp, Color(0xFF222222), RoundedCornerShape(12.dp))) {
            if (isGroundFloor) {
                // Row 1: Kitchen | Porch
                Row(modifier = Modifier.weight(1f)) {
                    Box(Modifier.weight(1f).border(0.5.dp, Color(0xFF222222)).padding(8.dp)) { Text("Kitchen", color = Color(0xFF444444), fontSize = 10.sp) }
                    Box(Modifier.weight(1f).border(0.5.dp, Color(0xFF222222)).padding(8.dp)) { Text("Porch", color = Color(0xFF444444), fontSize = 10.sp) }
                }
                // Row 2: Utility | Living
                Row(modifier = Modifier.weight(1f)) {
                    Box(Modifier.weight(1f).border(0.5.dp, Color(0xFF222222)).padding(8.dp)) { Text("Utility", color = Color(0xFF444444), fontSize = 10.sp) }
                    Box(Modifier.weight(1f).border(0.5.dp, Color(0xFF222222)).padding(8.dp)) { Text("Living room", color = Color(0xFF444444), fontSize = 10.sp) }
                }
                // Row 3: Garage
                Row(modifier = Modifier.weight(1f)) {
                    Box(Modifier.weight(1f).border(0.5.dp, Color(0xFF222222)).padding(8.dp)) { Text("Garage", color = Color(0xFF444444), fontSize = 10.sp) }
                    Box(Modifier.weight(1f).border(0.5.dp, Color(0xFF222222)))
                }
            } else {
                Row(modifier = Modifier.weight(1f)) {
                    Box(Modifier.weight(1f).border(0.5.dp, Color(0xFF222222)).padding(8.dp)) { Text("Master Bed", color = Color(0xFF444444), fontSize = 10.sp) }
                    Box(Modifier.weight(1f).border(0.5.dp, Color(0xFF222222)).padding(8.dp)) { Text("Bathroom", color = Color(0xFF444444), fontSize = 10.sp) }
                }
                Row(modifier = Modifier.weight(1f)) {
                    Box(Modifier.weight(1f).border(0.5.dp, Color(0xFF222222)).padding(8.dp)) { Text("Bedroom 2", color = Color(0xFF444444), fontSize = 10.sp) }
                    Box(Modifier.weight(1f).border(0.5.dp, Color(0xFF222222)).padding(8.dp)) { Text("Balcony", color = Color(0xFF444444), fontSize = 10.sp) }
                }
            }
        }
        // Icon mapping
        devices.forEach { device ->
            val room = device.room.lowercase()
            val pos = if (isGroundFloor) {
                when {
                    "kitchen" in room -> (-100).dp to (-70).dp
                    "porch" in room -> 100.dp to (-70).dp
                    "utility" in room -> (-100).dp to 0.dp
                    "living" in room -> 100.dp to 0.dp
                    "garage" in room -> (-100).dp to 70.dp
                    else -> null
                }
            } else {
                when {
                    "master bedroom" in room || "master bed" in room -> (-100).dp to (-40).dp
                    "bathroom" in room -> 100.dp to (-40).dp
                    "bedroom 2" in room -> (-100).dp to 40.dp
                    "balcony" in room -> 100.dp to 40.dp
                    else -> null
                }
            }
            pos?.let { (x, y) ->
                DeviceMapIcon(device, Modifier.offset(x, y).clickable { onDeviceClick(device.id) })
            }
        }
    }
}

@Composable
fun DeviceItemRow(
    device: Device,
    onClick: () -> Unit
) {
    val statusColor = getDeviceStatusColor(device)
    val deviceIcon = getDeviceIcon(device)

    val isIronOn = device.type.lowercase() == "iron" && device.state.uppercase() == "ON"
    var currentTime by remember { mutableLongStateOf(System.currentTimeMillis()) }
    var serverTimeOffset by remember { mutableLongStateOf(0L) }

    LaunchedEffect(isIronOn, device.turnedOnAt) {
        if (isIronOn && device.turnedOnAt > 0) {
            currentTime = System.currentTimeMillis() // Update immediately on restart
            while (true) {
                delay(1000)
                currentTime = System.currentTimeMillis()
            }
        }
    }

    LaunchedEffect(Unit) {
        FirebaseRepository.listenToServerTimeOffset { offset ->
            serverTimeOffset = offset
        }
    }

    val adjustedCurrentTime = currentTime + serverTimeOffset

    val statusText = when {
        isIronOn && device.maxDurationSec > 0 && device.turnedOnAt > 0 -> {
            val elapsedSec = (maxOf(adjustedCurrentTime, device.turnedOnAt) - device.turnedOnAt) / 1000
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

    Surface(
        onClick = onClick,
        color = if (isIronOn) Color(0xFF2D1B1B) else Color(0xFF1A1A1A),
        shape = RoundedCornerShape(12.dp),
        modifier = Modifier.padding(vertical = 4.dp).fillMaxWidth()
    ) {
        Row(modifier = Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
            Icon(deviceIcon, null, tint = statusColor, modifier = Modifier.size(20.dp))
            Spacer(Modifier.width(16.dp))
            Text(text = device.name, color = Color.White, modifier = Modifier.weight(1f), fontWeight = FontWeight.Medium)
            Text(text = statusText, color = statusColor, fontSize = 14.sp)
        }
    }
}

@Composable
fun DeviceMapIcon(device: Device, modifier: Modifier) {
    val color = getDeviceStatusColor(device)
    val icon = if (device.type.lowercase() == "outlet" || device.type.lowercase() == "multiswitch") {
        getRoomIcon(device.room)
    } else {
        getDeviceIcon(device)
    }
    
    Box(
        modifier = modifier.size(32.dp).background(Color(0xFF1A1A1A), CircleShape).border(1.dp, color.copy(0.4f), CircleShape),
        contentAlignment = Alignment.Center
    ) {
        Icon(icon, null, tint = color, modifier = Modifier.size(16.dp))
    }
}

@Composable
fun RoomLabel(name: String) {
    Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.padding(bottom = 8.dp)) {
        Icon(getRoomIcon(name), null, tint = Color(0xFF666666), modifier = Modifier.size(16.dp))
        Spacer(Modifier.width(8.dp))
        Text(text = name, color = Color(0xFF666666), fontSize = 14.sp, fontWeight = FontWeight.Bold)
    }
}

fun getRoomIcon(room: String): ImageVector {
    val r = room.lowercase()
    return when {
        "kitchen" in r -> Icons.Outlined.Restaurant
        "living" in r -> Icons.Outlined.Weekend
        "utility" in r -> Icons.Outlined.LocalLaundryService
        "porch" in r -> Icons.Outlined.DoorFront
        "master bedroom" in r || "master bed" in r -> Icons.Outlined.Bed
        "bed" in r -> Icons.Outlined.SingleBed
        "bath" in r -> Icons.Outlined.Bathtub
        "balcony" in r -> Icons.Outlined.Deck
        "garage" in r -> Icons.Outlined.DirectionsCar
        else -> Icons.Outlined.Home
    }
}

fun getDeviceIcon(device: Device): ImageVector {
    return when (device.type.lowercase()) {
        "light" -> Icons.Outlined.Lightbulb
        "outlet" -> Icons.Outlined.Power
        "iron" -> Icons.Outlined.Iron
        "camera" -> Icons.Outlined.PhotoCamera
        "multiswitch" -> Icons.AutoMirrored.Outlined.AltRoute
        else -> Icons.Outlined.Devices
    }
}

fun getDeviceStatusColor(device: Device): Color {
    return when {
        device.type == "camera" -> Color(0xFFFFA500)
        device.state == "ON" && device.type == "iron" -> Color.Red
        device.state == "ON" -> Color(0xFF4CAF50)
        else -> Color(0xFF666666)
    }
}
