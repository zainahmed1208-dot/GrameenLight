package com.example.grameenlight.ui.screens

import androidx.compose.runtime.*
import androidx.compose.ui.platform.LocalContext
import androidx.navigation.NavController
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore

@Composable
fun RoleCheckScreen(navController: NavController) {

    val uid = FirebaseAuth.getInstance().currentUser?.uid

    LaunchedEffect(uid) {

        if (uid == null) return@LaunchedEffect

        val db = FirebaseFirestore.getInstance()

        db.collection("users").document(uid).get()
            .addOnSuccessListener { doc ->

                val role = doc.getString("role") ?: "citizen"

                if (role == "worker") {
                    navController.navigate("worker") {
                        popUpTo("roleCheck") { inclusive = true }
                    }
                } else {
                    navController.navigate("home") {
                        popUpTo("roleCheck") { inclusive = true }
                    }
                }
            }
    }
}