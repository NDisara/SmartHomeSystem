package com.example.smarthomesystem

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

@Composable
fun MultiSwitchDetailScreen(floorId: String, deviceId: String) {
    var device by remember { mutableStateOf<Device?>(null) }

    LaunchedEffect(floorId, deviceId) {
        FirebaseRepository.listenToDevices(floorId) { devices ->
            device = devices.find { it.id == deviceId }
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFF121212))
            .padding(24.dp)
    ) {
        device?.let { dev ->
            Text(text = dev.name, style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.Bold, color = Color.White)
            Text(text = "3-switch unit", color = Color(0xFF888888), fontSize = 14.sp)

            Spacer(modifier = Modifier.height(32.dp))

            val switches = listOf(
                "sw1_lamp" to "Switch 1 · lamp",
                "sw2_fan" to "Switch 2 · fan",
                "sw3_TV plug" to "Switch 3 · TV plug"
            )

            switches.forEach { (id, label) ->
                val state = dev.switches[id] ?: "OFF"
                val isOn = state == "ON"

                Surface(
                    color = Color(0xFF1A1A1A),
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier.padding(vertical = 8.dp).fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(text = label, color = Color.White, fontSize = 16.sp, fontWeight = FontWeight.Medium)
                        Switch(
                            checked = isOn,
                            onCheckedChange = { checked ->
                                val newState = if (checked) "ON" else "OFF"
                                FirebaseRepository.updateSwitchState(floorId, dev.id, id, newState)
                                FirebaseRepository.logUsage(dev.id, "${dev.name} ($id)", newState)
                            },
                            colors = SwitchDefaults.colors(
                                checkedThumbColor = Color.White,
                                checkedTrackColor = Color(0xFF4CAF50),
                                uncheckedThumbColor = Color(0xFF888888),
                                uncheckedTrackColor = Color(0xFF333333),
                                uncheckedBorderColor = Color.Transparent
                            )
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.weight(1f))
            Text(
                text = "unit id: gang-box-04 · synced 2s ago",
                color = Color(0xFF444444),
                fontSize = 12.sp,
                modifier = Modifier.align(Alignment.CenterHorizontally)
            )
        }
    }
}
