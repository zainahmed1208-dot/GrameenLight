package com.example.grameenlight.navigation

import androidx.compose.runtime.Composable
import androidx.navigation.compose.*
import com.example.grameenlight.ui.screens.*
import com.example.grameenlight.ui.screens.AdminScreen
import com.google.firebase.auth.FirebaseAuth

object Routes {
    const val LOGIN = "login"
    const val HOME = "home"
    const val WORKER = "worker"
    const val ADMIN = "admin"
}

@Composable
fun AppNavHost() {

    val navController = rememberNavController()

    // 🔥 Auto-login: if already signed in, go to HOME (default)
    val user = FirebaseAuth.getInstance().currentUser
    val startDestination = if (user != null) Routes.HOME else Routes.LOGIN

    NavHost(
        navController = navController,
        startDestination = startDestination
    ) {

        // 🔐 LOGIN SCREEN (3-button role selection)
        composable(Routes.LOGIN) {
            LoginScreen(
                onNavigate = { route ->
                    navController.navigate(route) {
                        popUpTo(Routes.LOGIN) { inclusive = true }
                    }
                }
            )
        }

        // 🗺 CITIZEN
        composable(Routes.HOME) {
            HomeScreen(navController)
        }
        composable(Routes.WORKER) {
            WorkerScreen(navController)
        }
        // 👑 ADMIN
        composable(Routes.ADMIN) {
            AdminScreen()
        }
    }
}