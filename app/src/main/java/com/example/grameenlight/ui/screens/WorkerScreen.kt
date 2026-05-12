package com.example.grameenlight.ui.screens

import androidx.compose.runtime.Composable
import androidx.navigation.NavController

@Composable
fun WorkerScreen(navController: NavController) {
    // Show the map screen in Technician/Worker Mode
    OSMMapScreen(navController, isWorker = true)
}
