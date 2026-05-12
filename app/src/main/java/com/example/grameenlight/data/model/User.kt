package com.example.grameenlight.data.model

data class User(
    val userId: String = "",
    val name: String = "",
    val email: String = "",   // ✅ ADD THIS
    val phone: String = "",
    val address: String = "",
    val role: String = "",
    val reportCount: Long = 0,
    val badge: String = ""
)