package com.example.grameenlight.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import com.example.grameenlight.navigation.Routes

@Composable
fun RoleSelectionScreen(navController: NavController) {

    Column(
        modifier = Modifier
            .fillMaxSize()
            // Setting a solid distinct background color to ensure changes are visible
            .background(Color(0xFFE8F5E9)) 
            .padding(24.dp),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {

        // Title
        Text(
            text = "Grameen Light",
            style = MaterialTheme.typography.headlineLarge,
            color = Color(0xFF2E7D32)
        )

        Spacer(modifier = Modifier.height(8.dp))

        // Subtitle
        Text(
            text = "Smart Village Energy System",
            style = MaterialTheme.typography.bodyMedium,
            color = Color.Gray
        )

        Spacer(modifier = Modifier.height(50.dp))

        // 👤 Citizen Button
        Button(
            onClick = { navController.navigate(Routes.HOME) },
            colors = ButtonDefaults.buttonColors(
                containerColor = Color(0xFF2E7D32)
            ),
            shape = RoundedCornerShape(16.dp),
            modifier = Modifier
                .fillMaxWidth()
                .height(60.dp)
        ) {
            Text(
                text = "👤 Continue as Citizen",
                color = Color.White
            )
        }

        Spacer(modifier = Modifier.height(20.dp))

        // 🛠 Worker Button
        OutlinedButton(
            onClick = { navController.navigate(Routes.WORKER) },
            shape = RoundedCornerShape(16.dp),
            modifier = Modifier
                .fillMaxWidth()
                .height(60.dp)
        ) {
            Text(
                text = "🛠 Login as Worker",
                color = Color(0xFF2E7D32)
            )
        }
    }
}
