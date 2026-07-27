package com.example.smarthomesystem

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.Divider
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.smarthomesystem.ui.theme.SecondaryText
import java.text.SimpleDateFormat
import java.util.*

@Composable
fun ReportScreen(modifier: Modifier = Modifier) {
    var logs by remember { mutableStateOf<List<UsageLog>>(emptyList()) }
    var activeDevicesCount by remember { mutableStateOf(0) }
    val dateFormat = remember { SimpleDateFormat("MMM dd, HH:mm:ss", Locale.getDefault()) }

    LaunchedEffect(Unit) {
        FirebaseRepository.listenToLogs { updatedLogs ->
            logs = updatedLogs
        }
        // Count active devices across all floors (simplified count)
        FirebaseRepository.listenToFloors { floors ->
            var count = 0
            floors.forEach { floor ->
                FirebaseRepository.listenToDevices(floor.id) { devices ->
                    count += devices.count { it.state == "ON" }
                    activeDevicesCount = count
                }
            }
        }
    }

    Column(modifier = modifier.fillMaxSize().background(MaterialTheme.colorScheme.background).padding(24.dp)) {
        Text(
            text = "Usage report",
            style = MaterialTheme.typography.headlineMedium,
            fontWeight = FontWeight.Bold,
            color = Color.White
        )

        Spacer(modifier = Modifier.height(24.dp))

        // Stats Cards
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(16.dp)) {
            StatCard(label = "active devices", value = activeDevicesCount.toString(), modifier = Modifier.weight(1f))
            StatCard(label = "alerts today", value = logs.count { it.action.contains("AUTO") }.toString(), modifier = Modifier.weight(1f))
        }

        Spacer(modifier = Modifier.height(32.dp))

        Text(text = "Recent Activity", color = SecondaryText, fontSize = 14.sp, fontWeight = FontWeight.Bold)
        Spacer(modifier = Modifier.height(16.dp))

        if (logs.isEmpty()) {
            Text(text = "No activity yet.", color = SecondaryText)
        } else {
            LazyColumn {
                items(logs.sortedByDescending { it.timestamp }) { log ->
                    ActivityRow(log, dateFormat)
                }
            }
        }
    }
}

@Composable
fun StatCard(label: String, value: String, modifier: Modifier) {
    Column(
        modifier = modifier
            .background(Color(0xFF1A1A1A), RoundedCornerShape(16.dp))
            .padding(16.dp)
    ) {
        Text(text = label, color = SecondaryText, fontSize = 12.sp)
        Text(text = value, color = Color.White, fontSize = 24.sp, fontWeight = FontWeight.Bold)
    }
}

@Composable
fun ActivityRow(log: UsageLog, dateFormat: SimpleDateFormat) {
    Column(modifier = Modifier.fillMaxWidth().padding(vertical = 12.dp)) {
        Text(
            text = "${log.deviceName} → ${log.action}",
            style = MaterialTheme.typography.bodyLarge,
            color = Color.White
        )
        Text(
            text = dateFormat.format(Date(log.timestamp)),
            style = MaterialTheme.typography.bodySmall,
            color = SecondaryText
        )
        Divider(color = Color(0xFF333333), modifier = Modifier.padding(top = 12.dp))
    }
}
