package com.example.grameenlight.data.model

data class Report(
    val reportId: String = "",
    val poleId: String = "",
    val userId: String = "",
    val issueType: String = "",
    val status: String = "open",
    val assignedWorker: String? = null,
    val timestamp: Long = System.currentTimeMillis()
)