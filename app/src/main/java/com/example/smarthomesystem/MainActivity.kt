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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.clickable
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.List
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.BarChart
import androidx.compose.material.icons.filled.GridView
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.example.smarthomesystem.ui.theme.SmartHomeSystemTheme
import com.example.smarthomesystem.ui.theme.SecondaryText

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
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
        containerColor = MaterialTheme.colorScheme.background,
        tonalElevation = 8.dp
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
                    selectedIconColor = Color.White,
                    unselectedIconColor = SecondaryText,
                    indicatorColor = Color(0xFF333333)
                )
            )
        }
    }
}

@Composable
fun AppNavigation(navController: NavHostController, modifier: Modifier = Modifier) {
    NavHost(navController = navController, startDestination = "home") {
        composable("home") {
            FloorListScreen(
                modifier = modifier,
                onFloorClick = { floorId -> navController.navigate("floorDetail/$floorId") }
            )
        }
        composable("alerts") {
            AlertsPlaceholderScreen(modifier)
        }
        composable("reports") {
            ReportScreen(modifier = modifier)
        }
        composable("settings") {
            SettingsPlaceholderScreen(modifier)
        }
        composable("floorDetail/{floorId}") { backStackEntry ->
            val floorId = backStackEntry.arguments?.getString("floorId") ?: ""
            FloorDetailScreen(
                floorId = floorId,
                modifier = modifier,
                onDeviceClick = { deviceId: String -> navController.navigate("deviceDetail/$floorId/$deviceId") }
            )
        }
        composable("deviceDetail/{floorId}/{deviceId}") { backStackEntry ->
            val floorId = backStackEntry.arguments?.getString("floorId") ?: ""
            val deviceId = backStackEntry.arguments?.getString("deviceId") ?: ""
            DeviceDetailScreen(floorId = floorId, deviceId = deviceId)
        }
    }
}

@Composable
fun AlertsPlaceholderScreen(modifier: Modifier) {
    Column(
        modifier = modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .padding(24.dp)
    ) {
        Text("Alerts", style = MaterialTheme.typography.headlineMedium, color = Color.White, fontWeight = FontWeight.Bold)
        Spacer(Modifier.height(24.dp))
        
        AlertItem("Critical: Iron left ON", "Safety cutoff triggered in Ground Floor", Color.Red)
        AlertItem("Warning: Camera disconnected", "Porch camera is offline", Color.Yellow)
        AlertItem("Info: Schedule run", "Hallway light turned OFF", Color.Cyan)
    }
}

@Composable
fun AlertItem(title: String, desc: String, color: Color) {
    Card(
        modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp),
        colors = CardDefaults.cardColors(containerColor = Color(0xFF1A1A1A)),
        border = BorderStroke(1.dp, color.copy(alpha = 0.5f))
    ) {
        Row(modifier = Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
            Box(Modifier.size(12.dp).background(color, RoundedCornerShape(2.dp)))
            Spacer(Modifier.width(16.dp))
            Column {
                Text(text = title, color = Color.White, fontWeight = FontWeight.Bold)
                Text(text = desc, color = SecondaryText, style = MaterialTheme.typography.bodySmall)
            }
        }
    }
}

@Composable
fun SettingsPlaceholderScreen(modifier: Modifier) {
    Column(modifier.fillMaxSize().background(MaterialTheme.colorScheme.background).padding(24.dp)) {
        Text("Settings", style = MaterialTheme.typography.headlineMedium, color = Color.White)
        Spacer(Modifier.height(16.dp))
        Text("Account: demo@example.com", color = SecondaryText)
        Text("Database: Connected", color = Color.Green)
    }
}

@Composable
fun FloorListScreen(
    modifier: Modifier = Modifier,
    onFloorClick: (String) -> Unit
) {
    var floors by remember { mutableStateOf<List<Floor>>(emptyList()) }

    LaunchedEffect(Unit) {
        FirebaseRepository.listenToFloors { updatedFloors ->
            floors = updatedFloors
        }
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .padding(24.dp)
    ) {
        Text(
            text = "My home",
            style = MaterialTheme.typography.headlineMedium,
            fontWeight = FontWeight.Bold,
            color = Color.White
        )
        Text(
            text = "${floors.size} floor plans",
            style = MaterialTheme.typography.bodyMedium,
            color = SecondaryText
        )

        Spacer(modifier = Modifier.height(24.dp))

        if (floors.isEmpty()) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text(text = "Loading floors...", color = Color.White)
                Button(
                    onClick = { FirebaseRepository.addSampleData() },
                    modifier = Modifier.padding(top = 16.dp)
                ) {
                    Text("Add Sample Data")
                }
            }
        } else {
            LazyColumn(modifier = Modifier.weight(1f)) {
                items(floors) { floor ->
                    FloorItem(floor = floor, onClick = { onFloorClick(floor.id) })
                }
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        Button(
            onClick = { /* Add floor plan logic */ },
            modifier = Modifier
                .fillMaxWidth()
                .height(56.dp)
                .border(1.dp, SecondaryText, RoundedCornerShape(12.dp)),
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
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 8.dp)
            .clickable { onClick() },
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        shape = RoundedCornerShape(16.dp),
        border = BorderStroke(1.dp, Color(0xFF333333))
    ) {
        Row(
            modifier = Modifier
                .padding(20.dp)
                .fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = floor.name,
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.SemiBold,
                    color = Color.White
                )
                Text(
                    text = "Devices loaded",
                    style = MaterialTheme.typography.bodySmall,
                    color = SecondaryText
                )
            }
            Icon(
                Icons.Default.ChevronRight,
                contentDescription = null,
                tint = SecondaryText
            )
        }
    }
}
