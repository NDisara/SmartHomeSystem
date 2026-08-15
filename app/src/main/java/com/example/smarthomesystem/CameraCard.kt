package com.example.smarthomesystem

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.example.smarthomesystem.ui.theme.SecondaryText

@Composable
fun CameraCard(device: Device) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(24.dp)
    ) {
        Text(
            text = device.name, 
            style = MaterialTheme.typography.headlineSmall,
            fontWeight = FontWeight.Bold,
            color = Color.White
        )
        Text(text = "Security Camera Feed", style = MaterialTheme.typography.bodySmall, color = SecondaryText)

        Spacer(modifier = Modifier.padding(12.dp))

        Box(
            modifier = Modifier
                .fillMaxWidth()
                .aspectRatio(16f / 9f)
                .background(Color(0xFF1A1A1A), RoundedCornerShape(16.dp)),
            contentAlignment = Alignment.Center
        ) {
            AsyncImage(
                model = "https://images.unsplash.com/photo-1557597774-9d273605dfa9?auto=format&fit=crop&q=80&w=600",
                contentDescription = "Camera Feed",
                modifier = Modifier.fillMaxWidth().aspectRatio(16f / 9f),
                contentScale = ContentScale.Crop
            )
            
            // Rec Dot and Text
            Row(
                modifier = Modifier
                    .align(Alignment.TopStart)
                    .padding(16.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(
                    modifier = Modifier
                        .size(8.dp)
                        .background(Color.Red, RoundedCornerShape(4.dp))
                )
                Spacer(Modifier.padding(4.dp))
                Text(
                    text = "LIVE",
                    color = Color.White,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold
                )
            }
        }
        
        Spacer(modifier = Modifier.padding(16.dp))
        
        Text(
            text = "Status: Streaming stable",
            color = Color(0xFF4CAF50),
            fontSize = 14.sp,
            fontWeight = FontWeight.Medium
        )
    }
}