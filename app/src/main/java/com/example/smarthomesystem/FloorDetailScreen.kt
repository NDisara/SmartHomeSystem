package com.example.smarthomesystem

import androidx.compose.foundation.BorderStroke
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
import coil.compose.AsyncImage
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.graphics.Brush
import java.util.Calendar
import kotlinx.coroutines.delay

@Composable
fun FloorDetailScreen(
    floorId: String,
    modifier: Modifier = Modifier,
    onDeviceClick: (String) -> Unit
) {
    var devices by remember { mutableStateOf<List<Device>>(emptyList()) }
    var currentFloor by remember { mutableStateOf<Floor?>(null) }
    var showAddDeviceDialog by remember { mutableStateOf(false) }
    val scrollState = rememberScrollState()

    // Debugging check
    println("FloorDetailScreen: Recomposing for floor $floorId. Dialog visible: $showAddDeviceDialog")

    LaunchedEffect(floorId) {
        FirebaseRepository.listenToFloors { floors ->
            currentFloor = floors.find { it.id == floorId }
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

    // Logic moved to SafetyService for background persistence

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
        ) {
            // Header Image Section
            Box(modifier = Modifier.fillMaxWidth().height(240.dp)) {
                if (currentFloor?.imageUrl?.isNotBlank() == true) {
                    AsyncImage(
                        model = currentFloor?.imageUrl,
                        contentDescription = null,
                        modifier = Modifier.fillMaxSize(),
                        contentScale = ContentScale.Crop
                    )
                } else {
                    Box(modifier = Modifier.fillMaxSize().background(Color(0xFF1A1A1A)))
                }
                
                // Gradient overlay
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(
                            Brush.verticalGradient(
                                colors = listOf(Color.Transparent, Color(0xFF121212))
                            )
                        )
                )
                
                // Overlay Header
                Row(
                    modifier = Modifier
                        .align(Alignment.BottomStart)
                        .fillMaxWidth()
                        .padding(24.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.Bottom
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = currentFloor?.name ?: "Loading...",
                            style = MaterialTheme.typography.headlineMedium,
                            fontWeight = FontWeight.Bold,
                            color = Color.White
                        )
                        Text(
                            text = "${devices.size} devices · ${activeRooms.size} rooms",
                            color = Color.White.copy(alpha = 0.7f),
                            fontSize = 14.sp
                        )
                    }
                    
                    if (devices.any { it.state == "ON" }) {
                        Button(
                            onClick = {
                                devices.forEach { device ->
                                    if (device.state == "ON") {
                                        FirebaseRepository.updateDeviceState(floorId, device.id, "OFF")
                                    }
                                }
                            },
                            colors = ButtonDefaults.buttonColors(
                                containerColor = Color.White.copy(alpha = 0.2f),
                                contentColor = Color.White
                            ),
                            contentPadding = PaddingValues(horizontal = 12.dp, vertical = 8.dp),
                            shape = RoundedCornerShape(8.dp),
                            modifier = Modifier.height(36.dp)
                        ) {
                            Text("All Off", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }

            Column(modifier = Modifier.padding(horizontal = 24.dp)) {
                if (devices.isEmpty()) {
                    EmptyFloorState { showAddDeviceDialog = true }
                } else {
                    // Show floor plan grid for Ground floor or First floor
                    val floorName = currentFloor?.name ?: ""
                    val isMainFloor = floorName.contains("Ground", ignoreCase = true) || 
                                     floorName.contains("First", ignoreCase = true) ||
                                     floorId == "ground"
                    
                    if (isMainFloor && activeRooms.isNotEmpty()) {
                        Spacer(modifier = Modifier.height(24.dp))
                        FloorPlanGrid(floorName, devices, onDeviceClick)
                        Spacer(modifier = Modifier.height(32.dp))
                    } else {
                        Spacer(modifier = Modifier.height(24.dp))
                    }

                    // Device List Sections (Grouped by Actual Rooms)
                    activeRooms.forEach { roomName ->
                        val roomDevices = devices.filter { it.room == roomName }
                        RoomLabel(roomName)
                        roomDevices.forEach { device ->
                            DeviceItemRow(device, floorId = floorId, onClick = { onDeviceClick(device.id) })
                        }
                        Spacer(Modifier.height(24.dp))
                    }
                }
                
                Spacer(modifier = Modifier.height(32.dp))
            }
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
    floorId: String,
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
            String.format("%02d:%02d left", mins, secs)
        }
        device.type.lowercase() == "camera" -> "Live"
        else -> ""
    }

    Surface(
        onClick = onClick,
        color = if (isIronOn) Color(0xFF2D1B1B) else Color(0xFF1A1A1A),
        shape = RoundedCornerShape(16.dp),
        border = if (device.state == "ON" || device.state == "STREAMING") 
            BorderStroke(1.dp, statusColor.copy(alpha = 0.5f)) 
            else BorderStroke(1.dp, Color(0xFF222222)),
        modifier = Modifier.padding(vertical = 6.dp).fillMaxWidth()
    ) {
        Row(
            modifier = Modifier.padding(16.dp), 
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(40.dp)
                    .background(statusColor.copy(alpha = 0.1f), CircleShape),
                contentAlignment = Alignment.Center
            ) {
                Icon(deviceIcon, null, tint = statusColor, modifier = Modifier.size(20.dp))
            }
            
            Spacer(Modifier.width(16.dp))
            
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = device.name, 
                    color = Color.White, 
                    fontWeight = FontWeight.Bold,
                    fontSize = 16.sp
                )
                if (statusText.isNotBlank()) {
                    Text(text = statusText, color = statusColor, fontSize = 12.sp)
                } else if (device.type != "multiswitch" && device.type != "camera") {
                    Text(
                        text = if (device.state == "ON") "Running" else "Off", 
                        color = if (device.state == "ON") statusColor else Color(0xFF666666),
                        fontSize = 12.sp
                    )
                }
            }

            if (device.type == "light" || device.type == "outlet" || device.type == "iron") {
                Switch(
                    checked = device.state == "ON",
                    onCheckedChange = { checked ->
                        val newState = if (checked) "ON" else "OFF"
                        // println("Switch clicked for ${device.id} in floor $floorId. New state: $newState")
                        FirebaseRepository.updateDeviceState(floorId, device.id, newState)
                        FirebaseRepository.logUsage(device.id, device.name, newState)
                    },
                    colors = SwitchDefaults.colors(
                        checkedThumbColor = Color.White,
                        checkedTrackColor = Color(0xFF4CAF50),
                        uncheckedThumbColor = Color(0xFF888888),
                        uncheckedTrackColor = Color(0xFF333333),
                        uncheckedBorderColor = Color.Transparent
                    )
                )
            } else {
                Icon(
                    Icons.Outlined.ChevronRight,
                    contentDescription = null,
                    tint = Color(0xFF444444)
                )
            }
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
