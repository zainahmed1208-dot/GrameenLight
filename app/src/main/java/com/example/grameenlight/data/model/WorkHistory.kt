package com.example.grameenlight.data.model

data class WorkHistory(
    val historyId: String = "",
    val workerId: String = "",
    val poleId: String = "",
    val action: String = "",
    val previousStatus: String = "",
    val newStatus: String = "",
    val timestamp: Long = System.currentTimeMillis()
)