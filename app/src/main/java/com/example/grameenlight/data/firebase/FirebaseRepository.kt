package com.example.grameenlight.data.firebase

import com.example.grameenlight.data.model.User
import com.example.grameenlight.data.model.Report
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.ListenerRegistration
import java.util.*

data class PoleDto(
    val id          : String = "",
    val lat         : Double = 0.0,
    val lon         : Double = 0.0,
    val status      : String = "working",
    val lastUpdated : Date   = Date()
)

object FirebaseRepository {

    private val db = FirebaseFirestore.getInstance()

    // ── LISTEN POLES ──────────────────────────────────────────────────────────
    fun listenPoles(onChange: (List<PoleDto>) -> Unit): ListenerRegistration {
        return db.collection("poles")
            .addSnapshotListener { snapshot, _ ->
                val list = snapshot?.documents?.mapNotNull { doc ->
                    val lat = doc.getDouble("lat") ?: return@mapNotNull null
                    val lon = doc.getDouble("lon") ?: return@mapNotNull null
                    PoleDto(
                        id          = doc.id,
                        lat         = lat,
                        lon         = lon,
                        status      = doc.getString("status") ?: "working",
                        lastUpdated = doc.getDate("lastUpdated") ?: Date()
                    )
                } ?: emptyList()
                onChange(list)
            }
    }

    // ── ADD POLE (lat/lon only — legacy) ──────────────────────────────────────
    fun addPole(lat: Double, lon: Double) {
        val poleId = "P${System.currentTimeMillis()}"
        db.collection("poles")
            .document(poleId)
            .set(
                mapOf(
                    "lat"         to lat,
                    "lon"         to lon,
                    "status"      to "working",
                    "lastUpdated" to Date()
                )
            )
    }

    // ── ADD POLE (PoleDto overload — used by worker long-press) ──────────────
    fun addPole(pole: PoleDto) {
        db.collection("poles")
            .document(pole.id)
            .set(
                mapOf(
                    "lat"         to pole.lat,
                    "lon"         to pole.lon,
                    "status"      to pole.status,
                    "lastUpdated" to pole.lastUpdated
                )
            )
    }

    // ── DELETE POLE ───────────────────────────────────────────────────────────
    fun deletePole(poleId: String) {
        db.collection("poles")
            .document(poleId)
            .delete()
    }

    // ── UPDATE POLE STATUS ────────────────────────────────────────────────────
    fun updatePoleStatus(
        poleId         : String,
        newStatus      : String,
        workerId       : String? = null,
        previousStatus : String  = "unknown"
    ) {
        val ref = db.collection("poles").document(poleId)

        ref.get().addOnSuccessListener { snapshot ->
            val prev = snapshot.getString("status") ?: previousStatus

            ref.update(
                mapOf(
                    "status"      to newStatus,
                    "lastUpdated" to Date()
                )
            )

            if (workerId != null) {
                addWorkHistory(
                    workerId       = workerId,
                    poleId         = poleId,
                    previousStatus = prev,
                    newStatus      = newStatus
                )
            }
        }
    }

    // ── ADD WORK HISTORY ──────────────────────────────────────────────────────
    fun addWorkHistory(
        workerId       : String,
        poleId         : String,
        previousStatus : String,
        newStatus      : String
    ) {
        val historyId = UUID.randomUUID().toString()

        db.collection("work_history")
            .document(historyId)
            .set(
                mapOf(
                    "historyId"      to historyId,
                    "workerId"       to workerId,
                    "poleId"         to poleId,
                    "action"         to "status_update",
                    "previousStatus" to previousStatus,
                    "newStatus"      to newStatus,
                    "timestamp"      to System.currentTimeMillis()
                )
            )
    }

    // ── CREATE REPORT ─────────────────────────────────────────────────────────
    fun createReport(poleId: String, userId: String, issue: String) {
        val reportId = UUID.randomUUID().toString()

        val report = Report(
            reportId  = reportId,
            poleId    = poleId,
            userId    = userId,
            issueType = issue,
            status    = "open",
            timestamp = System.currentTimeMillis()
        )

        db.collection("reports")
            .document(reportId)
            .set(report)

        updatePoleStatus(poleId, issue)

        val userRef = db.collection("users").document(userId)

        userRef.get().addOnSuccessListener { snapshot ->
            if (!snapshot.exists()) {
                userRef.set(
                    User(
                        userId      = userId,
                        name        = "Default User",
                        phone       = "0000000000",
                        address     = "Unknown",
                        role        = "citizen",
                        reportCount = 1,
                        badge       = "Beginner"
                    )
                )
            } else {
                val count    = (snapshot.get("reportCount") as? Number)?.toLong() ?: 0
                val newCount = count + 1
                userRef.update(
                    mapOf(
                        "reportCount" to newCount,
                        "badge"       to calculateBadge(newCount)
                    )
                )
            }
        }
    }

    // ── LISTEN USER ───────────────────────────────────────────────────────────
    fun listenUser(userId: String, onChange: (User?) -> Unit): ListenerRegistration {
        return db.collection("users")
            .document(userId)
            .addSnapshotListener { snapshot, _ ->
                if (snapshot != null && snapshot.exists()) {
                    onChange(snapshot.toObject(User::class.java))
                } else {
                    onChange(null)
                }
            }
    }

    // ── LISTEN WORK HISTORY ───────────────────────────────────────────────────
    fun listenWorkHistory(
        workerId : String,
        onChange : (List<Map<String, Any>>) -> Unit
    ): ListenerRegistration {
        return db.collection("work_history")
            .whereEqualTo("workerId", workerId)
            .addSnapshotListener { snapshot, _ ->
                val list = snapshot?.documents?.mapNotNull { it.data } ?: emptyList()
                onChange(list)
            }
    }

    // ── BADGE SYSTEM ──────────────────────────────────────────────────────────
    private fun calculateBadge(count: Long): String {
        return when {
            count >= 50 -> "Champion"
            count >= 20 -> "Active Citizen"
            count >= 10 -> "Contributor"
            else        -> "Beginner"
        }
    }
}