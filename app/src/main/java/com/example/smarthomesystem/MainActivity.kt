package com.example.smarthomesystem

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.clickable
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.example.smarthomesystem.ui.theme.SmartHomeSystemTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            SmartHomeSystemTheme {
                Scaffold(modifier = Modifier.fillMaxSize()) { innerPadding ->
                    AppNavigation(modifier = Modifier.padding(innerPadding))
                }
            }
        }
    }
}

@Composable
fun AppNavigation(modifier: Modifier = Modifier) {
    val navController: NavHostController = rememberNavController()

    NavHost(navController = navController, startDestination = "floorList") {
        composable("floorList") {
            FloorListScreen(
                modifier = modifier,
                onFloorClick = { floorId -> navController.navigate("floorDetail/$floorId") },
                onReportClick = { navController.navigate("report") }
            )
        }
        composable("floorDetail/{floorId}") { backStackEntry ->
            val floorId = backStackEntry.arguments?.getString("floorId") ?: ""
            FloorDetailScreen(floorId = floorId, modifier = modifier)
        }
        composable("report") {
            ReportScreen(modifier = modifier)
        }
    }
}

@Composable
fun FloorListScreen(
    modifier: Modifier = Modifier,
    onFloorClick: (String) -> Unit,
    onReportClick: () -> Unit
) {
    var floors by remember { mutableStateOf<List<Floor>>(emptyList()) }

    LaunchedEffect(Unit) {
        FirebaseRepository.listenToFloors { updatedFloors ->
            floors = updatedFloors
        }
    }

    Column(modifier = modifier.fillMaxSize().padding(16.dp)) {
        Row(modifier = Modifier.fillMaxWidth().padding(bottom = 16.dp)) {
            Text(
                text = "My Floors",
                style = MaterialTheme.typography.headlineMedium,
                modifier = Modifier.weight(1f)
            )
            Button(onClick = onReportClick) {
                Text("View Reports")
            }
        }

        if (floors.isEmpty()) {
            Text(text = "Loading floors...")
        } else {
            LazyColumn {
                items(floors) { floor ->
                    FloorCard(floor = floor, onClick = { onFloorClick(floor.id) })
                }
            }
        }
    }
}

@Composable
fun FloorCard(floor: Floor, onClick: () -> Unit) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 8.dp)
            .clickable { onClick() }
    ) {
        Row(modifier = Modifier.padding(16.dp)) {
            Text(text = floor.name, style = MaterialTheme.typography.titleMedium)
        }
    }
}