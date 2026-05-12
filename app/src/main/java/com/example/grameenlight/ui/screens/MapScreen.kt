package com.example.grameenlight.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.*
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.RectangleShape
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.grameenlight.data.model.GridItem

@Composable
fun MapScreen() {
    var selectedPole by remember { mutableStateOf<GridItem.Pole?>(null) }
    var isNight by remember { mutableStateOf(false) }
    var scale by remember { mutableFloatStateOf(1f) }

    // Define a 15-column grid for more detail
    val columns = 15
    val gridItems = remember {
        val list = mutableStateListOf<GridItem>()
        // Initialize with Grass
        repeat(columns * 18) { list.add(GridItem.Grass) }

        // Helper to set item at (row, col)
        fun set(r: Int, c: Int, item: GridItem) {
            if (r in 0 until 18 && c in 0 until columns) {
                list[r * columns + c] = item
            }
        }

        // --- Roads ---
        // Main Road (Horizontal)
        for (c in 0 until columns) set(8, c, GridItem.Road(if (c == 7) "Main Street" else null, isMain = true))
        
        // Side Street 1 (Vertical)
        for (r in 0 until 18) set(r, 3, GridItem.Road(if (r == 4) "Temple St" else null))
        
        // Side Street 2 (Vertical)
        for (r in 0 until 18) set(r, 11, GridItem.Road(if (r == 14) "School Ln" else null))

        // --- Buildings ---
        // Panchayath House (Larger)
        set(7, 7, GridItem.PanchayathHouse)
        
        // Temple
        set(2, 2, GridItem.Landmark("Temple", "🛕"))
        
        // School
        set(14, 12, GridItem.Landmark("School", "🏫"))
        
        // Houses
        val housePos = listOf(
            Pair(1, 1), Pair(1, 4), Pair(3, 1), Pair(3, 4),
            Pair(6, 2), Pair(6, 4), Pair(10, 2), Pair(10, 4),
            Pair(1, 9), Pair(1, 12), Pair(3, 9), Pair(3, 12),
            Pair(6, 10), Pair(6, 12), Pair(10, 10), Pair(10, 12),
            Pair(13, 2), Pair(13, 4), Pair(15, 2), Pair(15, 4)
        )
        housePos.forEach { set(it.first, it.second, GridItem.House()) }

        // --- Poles ---
        // Corner Poles
        set(8, 3, GridItem.Pole("P-M1", mutableStateOf("working")))
        set(8, 11, GridItem.Pole("P-M2", mutableStateOf("working")))
        
        // Along Side Street 1
        set(0, 3, GridItem.Pole("P-S1-1", mutableStateOf("working")))
        set(4, 3, GridItem.Pole("P-S1-2", mutableStateOf("fused")))
        set(12, 3, GridItem.Pole("P-S1-3", mutableStateOf("working")))
        set(17, 3, GridItem.Pole("P-S1-4", mutableStateOf("working")))

        // Along Side Street 2
        set(0, 11, GridItem.Pole("P-S2-1", mutableStateOf("working")))
        set(4, 11, GridItem.Pole("P-S2-2", mutableStateOf("working")))
        set(12, 11, GridItem.Pole("P-S2-3", mutableStateOf("burning")))
        set(17, 11, GridItem.Pole("P-S2-4", mutableStateOf("working")))

        list
    }

    val mapBg = if (isNight) Color(0xFF1B1B1B) else Color(0xFFC8E6C9) // Grassier Green
    val buildingColor = if (isNight) Color(0xFF424242) else Color(0xFFFFF9C4) // Soft yellow/tan
    val roadColor = if (isNight) Color(0xFF37474F) else Color(0xFFB0BEC5)
    val mainRoadColor = if (isNight) Color(0xFF455A64) else Color(0xFF90A4AE)

    Box(modifier = Modifier.fillMaxSize().background(mapBg)) {
        Column {
            // Header
            Row(
                modifier = Modifier.fillMaxWidth().padding(16.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text("Village View", style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.ExtraBold)
                
                Row(verticalAlignment = Alignment.CenterVertically) {
                    IconButton(onClick = { scale = (scale + 0.1f).coerceAtMost(2.5f) }) {
                        Icon(Icons.Default.Add, contentDescription = "Zoom In")
                    }
                    IconButton(onClick = { scale = (scale - 0.1f).coerceAtLeast(0.5f) }) {
                        // Using Text "-" since material-icons-extended is not available
                        Text("-", fontSize = 24.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurface)
                    }
                    Spacer(modifier = Modifier.width(8.dp))
                    IconButton(onClick = { isNight = !isNight }) {
                        Text(if (isNight) "🌙" else "☀️", fontSize = 20.sp)
                    }
                }
            }

            // Zoomable Map Area
            Box(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth()
                    .clip(RectangleShape)
            ) {
                LazyVerticalGrid(
                    columns = GridCells.Fixed(columns),
                    modifier = Modifier
                        .fillMaxSize()
                        .graphicsLayer(
                            scaleX = scale,
                            scaleY = scale
                        ),
                    contentPadding = PaddingValues(4.dp)
                ) {
                    items(gridItems) { item ->
                        Box(
                            modifier = Modifier.aspectRatio(1f),
                            contentAlignment = Alignment.Center
                        ) {
                            when (item) {
                                is GridItem.Road -> {
                                    Box(
                                        modifier = Modifier
                                            .fillMaxSize()
                                            .background(if (item.isMain) mainRoadColor else roadColor)
                                    ) {
                                        item.name?.let { name ->
                                            Text(
                                                text = name,
                                                fontSize = (6 / scale).sp,
                                                color = Color.White.copy(alpha = 0.7f),
                                                modifier = Modifier.align(Alignment.Center),
                                                textAlign = TextAlign.Center
                                            )
                                        }
                                    }
                                }
                                is GridItem.House -> {
                                    Box(
                                        modifier = Modifier
                                            .fillMaxSize(0.8f)
                                            .clip(RoundedCornerShape(2.dp))
                                            .background(buildingColor)
                                            .border(0.5.dp, Color.Gray.copy(alpha = 0.3f), RoundedCornerShape(2.dp))
                                    )
                                }
                                is GridItem.Landmark -> {
                                    Box(
                                        modifier = Modifier
                                            .fillMaxSize(0.9f)
                                            .clip(RoundedCornerShape(2.dp))
                                            .background(buildingColor)
                                            .border(1.dp, Color.Gray.copy(alpha = 0.5f), RoundedCornerShape(2.dp)),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                            Text(item.icon, fontSize = (12 / scale).sp)
                                            Text(item.name, fontSize = (5 / scale).sp, fontWeight = FontWeight.Bold)
                                        }
                                    }
                                }
                                is GridItem.PanchayathHouse -> {
                                    Box(
                                        modifier = Modifier
                                            .fillMaxSize(1.0f) 
                                            .clip(RoundedCornerShape(4.dp))
                                            .background(if (isNight) Color(0xFF5D4037) else Color(0xFFFFCCBC))
                                            .border(1.dp, Color.Black.copy(alpha = 0.2f), RoundedCornerShape(4.dp)),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Text("🏛️", fontSize = (14 / scale).sp)
                                    }
                                }
                                is GridItem.Pole -> {
                                    val statusColor = when (item.status.value) {
                                        "working" -> Color(0xFF4CAF50)
                                        "fused" -> Color(0xFFF44336)
                                        "burning" -> Color(0xFFFF9800)
                                        else -> Color.Gray
                                    }
                                    Box(
                                        modifier = Modifier
                                            .size((24 / scale).dp)
                                            .clip(CircleShape)
                                            .background(statusColor.copy(alpha = 0.15f))
                                            .clickable { selectedPole = item },
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Text("💡", fontSize = (14 / scale).sp, color = statusColor)
                                    }
                                }
                                else -> {}
                            }
                        }
                    }
                }
            }

            // Bottom Dashboard
            Card(
                modifier = Modifier.fillMaxWidth().padding(16.dp),
                shape = RoundedCornerShape(24.dp),
                colors = CardDefaults.cardColors(containerColor = if (isNight) Color(0xFF212121) else Color.White),
                elevation = CardDefaults.cardElevation(8.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth().padding(16.dp),
                    horizontalArrangement = Arrangement.SpaceEvenly
                ) {
                    val issues = gridItems.count { it is GridItem.Pole && it.status.value != "working" }
                    val working = gridItems.count { it is GridItem.Pole && it.status.value == "working" }
                    
                    DashboardItem("🔧", "$issues Issues")
                    DashboardItem("⚡", "18.2 kWh")
                    DashboardItem("✅", "$working Active")
                }
            }
        }

        // Legend
        Card(
            modifier = Modifier.align(Alignment.BottomEnd).padding(bottom = 110.dp, end = 16.dp),
            shape = RoundedCornerShape(12.dp),
            colors = CardDefaults.cardColors(containerColor = if (isNight) Color.Black.copy(0.7f) else Color.White.copy(0.9f))
        ) {
            Column(modifier = Modifier.padding(8.dp)) {
                LegendItem(Color(0xFF4CAF50), "Working")
                LegendItem(Color(0xFFF44336), "Fused")
                LegendItem(Color(0xFFFF9800), "Day Burn")
            }
        }

        // Status Update Popup
        selectedPole?.let { pole ->
            AlertDialog(
                onDismissRequest = { selectedPole = null },
                title = { Text("Update Pole ${pole.id}") },
                text = {
                    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                        Button(onClick = { pole.status.value = "working"; selectedPole = null }, modifier = Modifier.fillMaxWidth(), colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF4CAF50))) { Text("Set Working") }
                        Button(onClick = { pole.status.value = "fused"; selectedPole = null }, modifier = Modifier.fillMaxWidth(), colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFF44336))) { Text("Set Fused") }
                        Button(onClick = { pole.status.value = "burning"; selectedPole = null }, modifier = Modifier.fillMaxWidth(), colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFFF9800))) { Text("Set Day Burn") }
                    }
                },
                confirmButton = {}
            )
        }
    }
}

@Composable
fun DashboardItem(icon: String, label: String) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(icon, fontSize = 22.sp)
        Text(label, fontSize = 12.sp, fontWeight = FontWeight.Bold)
    }
}

@Composable
fun LegendItem(color: Color, text: String) {
    Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.padding(2.dp)) {
        Box(modifier = Modifier.size(8.dp).clip(CircleShape).background(color))
        Spacer(modifier = Modifier.width(6.dp))
        Text(text, fontSize = 10.sp)
    }
}
