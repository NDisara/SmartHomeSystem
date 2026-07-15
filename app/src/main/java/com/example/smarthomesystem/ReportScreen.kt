package com.example.smarthomesystem

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@Composable
fun ReportScreen(modifier: Modifier = Modifier) {
    var logs by remember { mutableStateOf<List<UsageLog>>(emptyList()) }
    val dateFormat = remember { SimpleDateFormat("MMM dd, HH:mm:ss", Locale.getDefault()) }

    LaunchedEffect(Unit) {
        FirebaseRepository.listenToLogs { updatedLogs ->
            logs = updatedLogs
        }
    }

    Column(modifier = modifier.fillMaxSize().padding(16.dp)) {
        Text(
            text = "Usage Report",
            style = MaterialTheme.typography.headlineMedium,
            modifier = Modifier.padding(bottom = 16.dp)
        )

        if (logs.isEmpty()) {
            Text(text = "No activity yet. Toggle some devices!")
        } else {
            LazyColumn {
                items(logs.sortedByDescending { it.timestamp }) { log ->
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 4.dp)
                            .background(Color(0xFFF0F0F0), RoundedCornerShape(6.dp))
                            .padding(12.dp)
                    ) {
                        Text(text = "${log.deviceName} → ${log.action}", style = MaterialTheme.typography.bodyMedium)
                        Text(
                            text = dateFormat.format(Date(log.timestamp)),
                            style = MaterialTheme.typography.labelSmall
                        )
                    }
                }
            }
        }
    }
}