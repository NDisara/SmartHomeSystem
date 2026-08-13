package com.example.smarthomesystem

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.Surface
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Bolt
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.DeviceHub
import androidx.compose.material.icons.filled.NotificationsActive
import androidx.compose.material3.Icon
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.vector.ImageVector
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
    var activeDevicesCount by remember { mutableIntStateOf(0) }
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

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(
                Brush.verticalGradient(
                    colors = listOf(Color(0xFF0F0F0F), Color(0xFF1A1A1A))
                )
            )
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(24.dp)
        ) {
            Text(
                text = "Usage report",
                style = MaterialTheme.typography.headlineMedium,
                fontWeight = FontWeight.Bold,
                color = Color.White
            )

            Spacer(modifier = Modifier.height(24.dp))

            // Stats Cards
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                StatCard(
                    label = "ACTIVE DEVICES",
                    value = activeDevicesCount.toString(),
                    icon = Icons.Default.DeviceHub,
                    iconColor = Color(0xFF4CAF50),
                    modifier = Modifier.weight(1f)
                )
                StatCard(
                    label = "ALERTS TODAY",
                    value = logs.count { it.action.contains("AUTO") }.toString(),
                    icon = Icons.Default.NotificationsActive,
                    iconColor = Color(0xFFFF5252),
                    modifier = Modifier.weight(1f)
                )
            }

            Spacer(modifier = Modifier.height(24.dp))
            
            // Energy Usage Mock
            Surface(
                color = Color(0xFF1A1A1A),
                shape = RoundedCornerShape(20.dp),
                border = BorderStroke(1.dp, Color(0xFF222222)),
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier.padding(20.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text("ESTIMATED USAGE", color = SecondaryText, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                        Text("12.4 kWh", color = Color.White, fontSize = 24.sp, fontWeight = FontWeight.Bold)
                        Text("Low consumption today", color = Color(0xFF4CAF50), fontSize = 12.sp)
                    }
                    Icon(Icons.Default.Bolt, contentDescription = null, tint = Color(0xFFFFD600), modifier = Modifier.size(32.dp))
                }
            }

            Spacer(modifier = Modifier.height(32.dp))

            Text(
                text = "Recent Activity",
                color = SecondaryText,
                fontSize = 14.sp,
                fontWeight = FontWeight.Bold,
                letterSpacing = 1.sp
            )
            Spacer(modifier = Modifier.height(16.dp))

            if (logs.isEmpty()) {
                Box(modifier = Modifier.weight(1f).fillMaxWidth(), contentAlignment = Alignment.Center) {
                    Text(text = "No activity yet.", color = SecondaryText)
                }
            } else {
                LazyColumn(
                    modifier = Modifier.weight(1f),
                    contentPadding = PaddingValues(bottom = 16.dp)
                ) {
                    items(logs.sortedByDescending { it.timestamp }) { log ->
                        ActivityRow(log, dateFormat)
                    }
                }
            }
        }
    }
}

@Composable
fun StatCard(label: String, value: String, icon: ImageVector, iconColor: Color, modifier: Modifier) {
    Column(
        modifier = modifier
            .background(Color(0xFF222222), RoundedCornerShape(20.dp))
            .padding(20.dp)
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = iconColor,
            modifier = Modifier.size(24.dp)
        )
        Spacer(modifier = Modifier.height(12.dp))
        Text(text = label, color = SecondaryText, fontSize = 11.sp, fontWeight = FontWeight.Bold)
        Text(text = value, color = Color.White, fontSize = 28.sp, fontWeight = FontWeight.Bold)
    }
}

@Composable
fun ActivityRow(log: UsageLog, dateFormat: SimpleDateFormat) {
    val icon = when {
        log.action.contains("ON") -> Icons.Default.Bolt
        log.action.contains("AUTO") -> Icons.Default.NotificationsActive
        else -> Icons.Default.Info
    }
    
    val iconTint = when {
        log.action.contains("ON") -> Color(0xFFFFD600)
        log.action.contains("AUTO") -> Color(0xFFFF5252)
        else -> Color(0xFF2196F3)
    }

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 8.dp)
            .background(Color(0xFF1E1E1E), RoundedCornerShape(12.dp))
            .padding(16.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .size(40.dp)
                .background(iconTint.copy(alpha = 0.1f), CircleShape),
            contentAlignment = Alignment.Center
        ) {
            Icon(imageVector = icon, contentDescription = null, tint = iconTint, modifier = Modifier.size(20.dp))
        }
        
        Spacer(modifier = Modifier.width(16.dp))
        
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = log.deviceName,
                style = MaterialTheme.typography.bodyLarge,
                fontWeight = FontWeight.SemiBold,
                color = Color.White
            )
            Text(
                text = log.action,
                style = MaterialTheme.typography.bodySmall,
                color = SecondaryText
            )
        }
        
        Text(
            text = dateFormat.format(Date(log.timestamp)),
            style = MaterialTheme.typography.labelSmall,
            color = Color(0xFF555555)
        )
    }
}
