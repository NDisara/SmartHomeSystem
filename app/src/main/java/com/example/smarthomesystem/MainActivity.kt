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
                
                val mainRoutes = listOf("home", "alerts", "reports", "settings", "floorDetail/{floorId}")
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
            AlertsPlaceholderScreen(modifier)
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
                modifier = modifier,
                onDeviceClick = { deviceId: String -> 
                    // We check if it's the gang switch unit to navigate to the specialized detail screen
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

    LaunchedEffect(Unit) {
        FirebaseRepository.listenToFloors { updatedFloors ->
            floors = updatedFloors
        }
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(Color(0xFF121212))
            .padding(24.dp)
    ) {
        Text("Home", color = Color(0xFF888888), fontSize = 14.sp)
        Text(
            text = "My home",
            style = MaterialTheme.typography.headlineMedium,
            fontWeight = FontWeight.Bold,
            color = Color.White
        )
        Text(
            text = "${floors.size} floor plans",
            style = MaterialTheme.typography.bodyMedium,
            color = Color(0xFF888888)
        )

        Spacer(modifier = Modifier.height(32.dp))

        if (floors.isEmpty()) {
            Box(modifier = Modifier.weight(1f).fillMaxWidth(), contentAlignment = Alignment.Center) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    CircularProgressIndicator(color = Color.White)
                    Spacer(Modifier.height(16.dp))
                    Text(text = "Looking for floors...", color = Color(0xFF666666))
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
    val (deviceCount, roomCount) = when(floor.id) {
        "ground" -> 7 to 4
        "first" -> 5 to 3
        "garage" -> 2 to 1
        else -> 0 to 0
    }

    Surface(
        onClick = onClick,
        color = Color(0xFF1A1A1A),
        shape = RoundedCornerShape(16.dp),
        border = BorderStroke(1.dp, Color(0xFF222222)),
        modifier = Modifier.padding(vertical = 8.dp).fillMaxWidth()
    ) {
        Row(
            modifier = Modifier.padding(20.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(44.dp)
                    .background(Color(0xFF222222), CircleShape)
                    .border(1.dp, Color(0xFF333333), CircleShape),
                contentAlignment = Alignment.Center
            ) {
                Icon(Icons.Default.Home, contentDescription = null, tint = Color(0xFF2196F3), modifier = Modifier.size(20.dp))
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
                    color = Color(0xFF666666)
                )
            }
            Icon(
                Icons.Default.ChevronRight,
                contentDescription = null,
                tint = Color(0xFF444444)
            )
        }
    }
}

@Composable
fun AlertsPlaceholderScreen(modifier: Modifier) {
    Column(modifier.fillMaxSize().background(Color(0xFF121212)).padding(24.dp)) {
        Text("Alerts", style = MaterialTheme.typography.headlineMedium, color = Color.White, fontWeight = FontWeight.Bold)
    }
}

@Composable
fun SettingsPlaceholderScreen(navController: NavHostController, modifier: Modifier) {
    Column(modifier.fillMaxSize().background(Color(0xFF121212)).padding(24.dp)) {
        Text("Settings", style = MaterialTheme.typography.headlineMedium, color = Color.White, fontWeight = FontWeight.Bold)
        Spacer(Modifier.height(32.dp))
        Button(
            onClick = { FirebaseRepository.addSampleData() },
            modifier = Modifier.fillMaxWidth().height(56.dp),
            shape = RoundedCornerShape(12.dp)
        ) {
            Text("Reset / Add Sample Data")
        }
        Spacer(Modifier.weight(1f))
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
    }
}
