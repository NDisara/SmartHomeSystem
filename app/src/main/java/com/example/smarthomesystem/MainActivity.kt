package com.example.smarthomesystem

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.clickable
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.BarChart
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.graphics.Brush
import java.util.Calendar
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import android.content.Intent
import android.os.Build
import android.Manifest
import androidx.activity.result.contract.ActivityResultContracts
import com.example.smarthomesystem.ui.theme.SmartHomeSystemTheme
import com.example.smarthomesystem.ui.theme.SecondaryText

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        
        // Start Safety Service
        val serviceIntent = Intent(this, SafetyService::class.java)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            startForegroundService(serviceIntent)
        } else {
            startService(serviceIntent)
        }

        // Request notification permission for Android 13+
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            val permissionLauncher = registerForActivityResult(ActivityResultContracts.RequestPermission()) { isGranted ->
                // Handle permission result if needed
            }
            permissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
        }

        FirebaseRepository.addSampleData()
        enableEdgeToEdge()
        setContent {
            SmartHomeSystemTheme {
                val navController = rememberNavController()
                val navBackStackEntry by navController.currentBackStackEntryAsState()
                val currentRoute = navBackStackEntry?.destination?.route
                
                val mainRoutes = listOf("home", "alerts", "reports", "settings")
                val showBottomBar = currentRoute in mainRoutes

                Scaffold(
                    modifier = Modifier.fillMaxSize(),
                    bottomBar = { 
                        if (showBottomBar) {
                            BottomNavigationBar(navController, currentRoute) 
                        }
                    }
                ) { innerPadding ->
                    AppNavigation(navController, modifier = Modifier.padding(innerPadding))
                }
            }
        }
    }
}

@Composable
fun BottomNavigationBar(navController: NavHostController, currentRoute: String?) {
    NavigationBar(
        containerColor = Color(0xFF121212),
        tonalElevation = 0.dp
    ) {
        val items = listOf(
            Triple("home", Icons.Default.Home, "Home"),
            Triple("alerts", Icons.Filled.Notifications, "Alerts"),
            Triple("reports", Icons.Default.BarChart, "Reports"),
            Triple("settings", Icons.Default.Settings, "Settings")
        )

        items.forEach { (route, icon, label) ->
            NavigationBarItem(
                icon = { Icon(icon, contentDescription = label) },
                selected = currentRoute == route,
                onClick = {
                    navController.navigate(route) {
                        popUpTo(navController.graph.findStartDestination().id) {
                            saveState = true
                        }
                        launchSingleTop = true
                        restoreState = true
                    }
                },
                colors = NavigationBarItemDefaults.colors(
                    selectedIconColor = Color(0xFF2196F3),
                    unselectedIconColor = Color(0xFF666666),
                    indicatorColor = Color.Transparent
                )
            )
        }
    }
}

@Composable
fun AppNavigation(navController: NavHostController, modifier: Modifier = Modifier) {
    NavHost(navController = navController, startDestination = "login") {
        composable("login") {
            LoginScreen(
                onLoginSuccess = {
                    navController.navigate("home") {
                        popUpTo("login") { inclusive = true }
                    }
                }
            )
        }
        composable("home") {
            FloorListScreen(
                modifier = modifier,
                onFloorClick = { floorId -> navController.navigate("floorDetail/$floorId") }
            )
        }
        composable("alerts") {
            AlertsScreen(modifier)
        }
        composable("reports") {
            ReportScreen(modifier = modifier)
        }
        composable("settings") {
            SettingsPlaceholderScreen(navController, modifier)
        }
        composable("floorDetail/{floorId}") { backStackEntry ->
            val floorId = backStackEntry.arguments?.getString("floorId") ?: ""
            FloorDetailScreen(
                floorId = floorId,
                modifier = Modifier.fillMaxSize(),
                onDeviceClick = { deviceId: String -> 
                    if (deviceId == "gang1") {
                        navController.navigate("multiSwitchDetail/$floorId/$deviceId")
                    } else {
                        navController.navigate("deviceDetail/$floorId/$deviceId")
                    }
                }
            )
        }
        composable("deviceDetail/{floorId}/{deviceId}") { backStackEntry ->
            val floorId = backStackEntry.arguments?.getString("floorId") ?: ""
            val deviceId = backStackEntry.arguments?.getString("deviceId") ?: ""
            DeviceDetailScreen(floorId = floorId, deviceId = deviceId)
        }
        composable("multiSwitchDetail/{floorId}/{deviceId}") { backStackEntry ->
            val floorId = backStackEntry.arguments?.getString("floorId") ?: ""
            val deviceId = backStackEntry.arguments?.getString("deviceId") ?: ""
            MultiSwitchDetailScreen(floorId = floorId, deviceId = deviceId)
        }
    }
}

@Composable
fun FloorListScreen(
    modifier: Modifier = Modifier,
    onFloorClick: (String) -> Unit
) {
    var floors by remember { mutableStateOf<List<Floor>>(emptyList()) }
    var showAddFloorDialog by remember { mutableStateOf(false) }
    var newFloorName by remember { mutableStateOf("") }

    LaunchedEffect(Unit) {
        FirebaseRepository.listenToFloors { updatedFloors ->
            floors = updatedFloors
        }
    }

    if (showAddFloorDialog) {
        AlertDialog(
            onDismissRequest = { showAddFloorDialog = false },
            title = { Text("Add Floor Plan", color = Color.White) },
            text = {
                OutlinedTextField(
                    value = newFloorName,
                    onValueChange = { newFloorName = it },
                    label = { Text("Floor Name", color = Color.Gray) },
                    placeholder = { Text("e.g. Garage", color = Color.DarkGray) },
                    modifier = Modifier.fillMaxWidth(),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedTextColor = Color.White,
                        unfocusedTextColor = Color.White,
                        focusedBorderColor = Color(0xFF2196F3),
                        unfocusedBorderColor = Color.Gray
                    )
                )
            },
            confirmButton = {
                Button(
                    onClick = {
                        if (newFloorName.isNotBlank()) {
                            FirebaseRepository.addFloor(newFloorName)
                            newFloorName = ""
                            showAddFloorDialog = false
                        }
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF2196F3))
                ) {
                    Text("Add", color = Color.White)
                }
            },
            dismissButton = {
                TextButton(onClick = { showAddFloorDialog = false }) {
                    Text("Cancel", color = Color.Gray)
                }
            },
            containerColor = Color(0xFF1E1E1E)
        )
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(Color(0xFF0F0F0F))
            .padding(24.dp)
    ) {
        // Header
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column {
                val calendar = Calendar.getInstance()
                val greeting = when (calendar.get(Calendar.HOUR_OF_DAY)) {
                    in 0..11 -> "Good Morning"
                    in 12..16 -> "Good Afternoon"
                    else -> "Good Night"
                }
                Text(text = greeting, color = Color(0xFF888888), fontSize = 14.sp)
                Text(
                    text = "Smart Home",
                    style = MaterialTheme.typography.headlineMedium,
                    fontWeight = FontWeight.Bold,
                    color = Color.White
                )
            }
        }

        Spacer(modifier = Modifier.height(24.dp))

        // Quick Summary Row
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            SummaryCard(
                label = "Temperature",
                value = "24°C",
                icon = Icons.Default.Home, // Placeholder for temp icon
                modifier = Modifier.weight(1f)
            )
            SummaryCard(
                label = "Humidity",
                value = "45%",
                icon = Icons.Default.Notifications, // Placeholder for humidity icon
                modifier = Modifier.weight(1f)
            )
        }

        Spacer(modifier = Modifier.height(32.dp))
        
        Text(
            text = "Floors",
            color = Color.White,
            fontSize = 18.sp,
            fontWeight = FontWeight.Bold
        )
        
        Spacer(modifier = Modifier.height(16.dp))

        if (floors.isEmpty()) {
            Box(modifier = Modifier.weight(1f).fillMaxWidth(), contentAlignment = Alignment.Center) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    CircularProgressIndicator(color = Color(0xFF2196F3))
                    Spacer(Modifier.height(16.dp))
                    Text(text = "Scanning your home...", color = Color(0xFF666666))
                }
            }
        } else {
            LazyColumn(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(8.dp),
                contentPadding = PaddingValues(bottom = 16.dp)
            ) {
                items(floors) { floor ->
                    FloorItem(floor = floor, onClick = { onFloorClick(floor.id) })
                }
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        Button(
            onClick = { showAddFloorDialog = true },
            modifier = Modifier
                .fillMaxWidth()
                .height(56.dp)
                .border(1.dp, Color(0xFF333333), RoundedCornerShape(12.dp)),
            colors = ButtonDefaults.buttonColors(containerColor = Color.Transparent),
            shape = RoundedCornerShape(12.dp)
        ) {
            Icon(Icons.Default.Add, contentDescription = null, tint = Color.White)
            Spacer(modifier = Modifier.size(8.dp))
            Text("Add floor plan", color = Color.White)
        }
    }
}

@Composable
fun FloorItem(floor: Floor, onClick: () -> Unit) {
    var devices by remember(floor.id) { mutableStateOf<List<Device>>(emptyList()) }

    LaunchedEffect(floor.id) {
        FirebaseRepository.listenToDevices(floor.id) { updatedDevices ->
            devices = updatedDevices
        }
    }

    val roomCount = remember(devices) {
        devices.map { it.room }.filter { it.isNotBlank() }.distinct().size
    }
    
    val deviceCount = remember(devices) { devices.size }
    val activeCount = remember(devices) { devices.count { it.state == "ON" } }

    Surface(
        onClick = onClick,
        color = Color(0xFF1A1A1A),
        shape = RoundedCornerShape(20.dp),
        border = BorderStroke(1.dp, Color(0xFF222222)),
        modifier = Modifier.padding(vertical = 4.dp).fillMaxWidth()
    ) {
        Column {
            if (floor.imageUrl.isNotBlank()) {
                Box(modifier = Modifier.fillMaxWidth().height(160.dp)) {
                    AsyncImage(
                        model = floor.imageUrl,
                        contentDescription = null,
                        modifier = Modifier.fillMaxSize(),
                        contentScale = ContentScale.Crop
                    )
                    
                    // Chip for active devices
                    if (activeCount > 0) {
                        Surface(
                            color = Color(0xFF4CAF50),
                            shape = RoundedCornerShape(8.dp),
                            modifier = Modifier.padding(12.dp).align(Alignment.TopEnd)
                        ) {
                            Text(
                                text = "$activeCount ACTIVE",
                                color = Color.White,
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Bold,
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                            )
                        }
                    }

                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .background(
                                Brush.verticalGradient(
                                    colors = listOf(Color.Transparent, Color.Black.copy(alpha = 0.5f))
                                )
                            )
                    )
                }
            }
            
            Row(
                modifier = Modifier.padding(16.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(
                    modifier = Modifier
                        .size(40.dp)
                        .background(Color(0xFF222222), CircleShape)
                        .border(1.dp, Color(0xFF333333), CircleShape),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        Icons.Default.Home, 
                        contentDescription = null, 
                        tint = Color(0xFF2196F3), 
                        modifier = Modifier.size(20.dp)
                    )
                }
                
                Spacer(modifier = Modifier.width(16.dp))
                
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = floor.name,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = Color.White
                    )
                    Text(
                        text = "$deviceCount devices · $roomCount rooms",
                        style = MaterialTheme.typography.bodySmall,
                        color = Color(0xFF888888)
                    )
                }
                Icon(
                    Icons.Default.ChevronRight,
                    contentDescription = null,
                    tint = Color(0xFF666666)
                )
            }
        }
    }
}

@Composable
fun SummaryCard(label: String, value: String, icon: ImageVector, modifier: Modifier) {
    Row(
        modifier = modifier
            .background(Color(0xFF1A1A1A), RoundedCornerShape(16.dp))
            .border(1.dp, Color(0xFF222222), RoundedCornerShape(16.dp))
            .padding(16.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(icon, contentDescription = null, tint = Color(0xFF2196F3), modifier = Modifier.size(20.dp))
        Spacer(modifier = Modifier.width(12.dp))
        Column {
            Text(text = label, color = Color(0xFF888888), fontSize = 11.sp)
            Text(text = value, color = Color.White, fontSize = 16.sp, fontWeight = FontWeight.Bold)
        }
    }
}


@Composable
fun AlertsScreen(modifier: Modifier = Modifier) {
    var alerts by remember { mutableStateOf<List<Alert>>(emptyList()) }

    LaunchedEffect(Unit) {
        FirebaseRepository.listenToAlerts { updatedAlerts ->
            alerts = updatedAlerts
        }
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(Color(0xFF0F0F0F))
            .padding(24.dp)
    ) {
        Text(
            text = "Alerts",
            style = MaterialTheme.typography.headlineMedium,
            color = Color.White,
            fontWeight = FontWeight.Bold
        )

        Spacer(modifier = Modifier.height(24.dp))

        if (alerts.isEmpty()) {
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Text("No alerts at the moment", color = Color.Gray)
            }
        } else {
            LazyColumn(
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                items(alerts) { alert ->
                    AlertItem(alert)
                }
            }
        }
    }
}

@Composable
fun AlertItem(alert: Alert) {
    val backgroundColor = when (alert.type) {
        "critical" -> Color(0xFF311111)
        "warning" -> Color(0xFF312811)
        else -> Color(0xFF1A1A1A)
    }
    
    val icon = when (alert.type) {
        "critical" -> Icons.Default.Notifications
        "warning" -> Icons.Default.Settings
        else -> Icons.Default.Notifications
    }
    
    val iconColor = when (alert.type) {
        "critical" -> Color.Red
        "warning" -> Color(0xFFFFA500)
        else -> Color(0xFF2196F3)
    }

    Surface(
        color = backgroundColor,
        shape = RoundedCornerShape(16.dp),
        border = BorderStroke(1.dp, iconColor.copy(alpha = 0.2f)),
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier.padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(40.dp)
                    .background(iconColor.copy(alpha = 0.1f), CircleShape),
                contentAlignment = Alignment.Center
            ) {
                Icon(icon, contentDescription = null, tint = iconColor, modifier = Modifier.size(20.dp))
            }
            
            Spacer(modifier = Modifier.width(16.dp))
            
            Column {
                Text(
                    text = alert.title,
                    color = Color.White,
                    fontWeight = FontWeight.Bold,
                    fontSize = 16.sp
                )
                Text(
                    text = alert.message,
                    color = Color.White.copy(alpha = 0.7f),
                    fontSize = 14.sp
                )
                Text(
                    text = java.text.DateFormat.getDateTimeInstance().format(java.util.Date(alert.timestamp)),
                    color = Color.Gray,
                    fontSize = 12.sp
                )
            }
        }
    }
}


@Composable
fun SettingsPlaceholderScreen(navController: NavHostController, modifier: Modifier) {
    Column(
        modifier
            .fillMaxSize()
            .background(Color(0xFF0F0F0F))
            .padding(24.dp)
            .verticalScroll(rememberScrollState())
    ) {
        Text("Settings", style = MaterialTheme.typography.headlineMedium, color = Color.White, fontWeight = FontWeight.Bold)
        
        Spacer(Modifier.height(32.dp))
        
        SettingItem("Security", Icons.Default.Settings)
        SettingItem("Language", Icons.Default.Home)
        SettingItem("Help & Support", Icons.Default.ChevronRight)
        
        Spacer(Modifier.height(32.dp))
        
        Text("Developer Tools", color = Color(0xFF4CAF50), fontSize = 12.sp, fontWeight = FontWeight.Bold)
        Spacer(Modifier.height(16.dp))
        
        Button(
            onClick = { FirebaseRepository.addSampleData() },
            modifier = Modifier.fillMaxWidth().height(56.dp),
            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF1A1A1A)),
            shape = RoundedCornerShape(12.dp),
            border = BorderStroke(1.dp, Color(0xFF333333))
        ) {
            Text("Reset / Add Sample Data", color = Color.White)
        }
        
        Spacer(Modifier.height(48.dp))
        
        Button(
            onClick = {
                navController.navigate("login") {
                    popUpTo(0)
                }
            },
            modifier = Modifier.fillMaxWidth().height(56.dp),
            colors = ButtonDefaults.buttonColors(containerColor = Color.Red.copy(alpha = 0.1f), contentColor = Color.Red),
            shape = RoundedCornerShape(12.dp),
            border = BorderStroke(1.dp, Color.Red.copy(alpha = 0.5f))
        ) {
            Text("Logout", fontWeight = FontWeight.Bold)
        }
        
        Spacer(Modifier.height(24.dp))
        Text("App Version 1.0.24", color = Color(0xFF333333), modifier = Modifier.align(Alignment.CenterHorizontally), fontSize = 12.sp)
    }
}

@Composable
fun SettingItem(label: String, icon: ImageVector) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 12.dp)
            .clickable { },
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(modifier = Modifier.size(40.dp).background(Color(0xFF1A1A1A), RoundedCornerShape(8.dp)), contentAlignment = Alignment.Center) {
            Icon(icon, contentDescription = null, tint = Color(0xFF888888), modifier = Modifier.size(20.dp))
        }
        Spacer(Modifier.width(16.dp))
        Text(text = label, color = Color.White, modifier = Modifier.weight(1f))
        Icon(Icons.Default.ChevronRight, contentDescription = null, tint = Color(0xFF333333))
    }
}
