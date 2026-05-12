package com.example.grameenlight.ui.screens

import android.widget.Toast
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.tween
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.Logout
import androidx.compose.material.icons.rounded.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.grameenlight.data.firebase.FirebaseRepository
import com.example.grameenlight.data.firebase.PoleDto
import com.example.grameenlight.data.model.User
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore

// ─── GrameenLight Admin Theme ─────────────────────────────────────────────────
private val AdBgDeep      = Color(0xFF070D18)
private val AdBgCard      = Color(0xFF0D1421)
private val AdAmber       = Color(0xFFFFC400)   // admin accent
private val AdEmerald     = Color(0xFF00D68F)
private val AdSapphire    = Color(0xFF4D9FFF)
private val AdRed         = Color(0xFFF87171)
private val AdGreen       = Color(0xFF4ADE80)
private val AdTextPrimary = Color(0xFFEEF3FF)
private val AdTextSub     = Color(0xFF566880)
private val AdGlass       = Color(0x14FFFFFF)
private val AdGlassBorder = Color(0x20FFFFFF)

// ─────────────────────────────────────────────────────────────────────────────

@Composable
fun AdminScreen() {
    val context = LocalContext.current

    // ── Firebase state ────────────────────────────────────────────────────────
    val poles = remember { mutableStateListOf<PoleDto>() }
    var users by remember { mutableStateOf<List<User>>(emptyList()) }
    var adminUser by remember { mutableStateOf<User?>(null) }
    var selectedTab by remember { mutableStateOf(0) }  // 0 = Overview, 1 = Poles, 2 = Users

    val currentUid = FirebaseAuth.getInstance().currentUser?.uid ?: ""

    // Live poles listener
    DisposableEffect(Unit) {
        val poleListener = FirebaseRepository.listenPoles { poles.clear(); poles.addAll(it) }
        onDispose { poleListener.remove() }
    }

    // Live admin user
    DisposableEffect(currentUid) {
        if (currentUid.isEmpty()) return@DisposableEffect onDispose {}
        val userListener = FirebaseRepository.listenUser(currentUid) { adminUser = it }
        onDispose { userListener.remove() }
    }

    // All users (one-time snapshot, re-fetches when tab opens)
    LaunchedEffect(selectedTab) {
        if (selectedTab == 2) {
            FirebaseFirestore.getInstance()
                .collection("users")
                .get()
                .addOnSuccessListener { snap ->
                    users = snap.documents.mapNotNull { doc ->
                        doc.toObject(User::class.java)
                    }
                }
        }
    }

    // ── Computed stats ────────────────────────────────────────────────────────
    val totalPoles  = poles.size
    val working     = poles.count { it.status == "working" }
    val fused       = poles.count { it.status == "fused" }
    val burning     = poles.count { it.status == "burning" }
    val healthPct   = if (totalPoles > 0) (working * 100 / totalPoles) else 0

    // ── Root surface ──────────────────────────────────────────────────────────
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(AdBgDeep)
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .statusBarsPadding()
        ) {

            // ── Top bar ───────────────────────────────────────────────────────
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(
                        Brush.linearGradient(
                            listOf(AdAmber.copy(alpha = 0.10f), AdBgDeep)
                        )
                    )
                    .border(
                        width  = 0.dp,
                        color  = Color.Transparent,
                        shape  = RoundedCornerShape(0.dp)
                    )
                    .padding(horizontal = 20.dp, vertical = 14.dp)
            ) {
                Row(
                    modifier          = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // Admin avatar
                    Box(
                        modifier = Modifier
                            .size(44.dp)
                            .clip(CircleShape)
                            .background(AdAmber.copy(alpha = 0.15f))
                            .border(
                                width = 2.dp,
                                brush = Brush.linearGradient(listOf(AdAmber, AdAmber.copy(alpha = 0.3f))),
                                shape = CircleShape
                            ),
                        contentAlignment = Alignment.Center
                    ) {
                        Text("🛡️", fontSize = 20.sp)
                    }

                    Spacer(Modifier.width(14.dp))

                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            "Admin Panel",
                            fontSize   = 18.sp,
                            fontWeight = FontWeight.Bold,
                            color      = AdTextPrimary
                        )
                        Text(
                            adminUser?.name ?: FirebaseAuth.getInstance().currentUser?.email ?: "Administrator",
                            fontSize = 12.sp,
                            color    = AdTextSub
                        )
                    }

                    // Logout button
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(10.dp))
                            .background(AdRed.copy(alpha = 0.12f))
                            .border(0.5.dp, AdRed.copy(alpha = 0.35f), RoundedCornerShape(10.dp))
                            .clickable {
                                FirebaseAuth.getInstance().signOut()
                                Toast.makeText(context, "Signed out", Toast.LENGTH_SHORT).show()
                            }
                            .padding(horizontal = 12.dp, vertical = 8.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Row(
                            verticalAlignment     = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(5.dp)
                        ) {
                            Icon(
                                Icons.AutoMirrored.Rounded.Logout, null,
                                tint     = AdRed,
                                modifier = Modifier.size(16.dp)
                            )
                            Text("Logout", fontSize = 12.sp, fontWeight = FontWeight.SemiBold, color = AdRed)
                        }
                    }
                }
            }

            // ── Tab bar ───────────────────────────────────────────────────────
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp, vertical = 8.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                listOf(
                    Pair("Overview",  Icons.Rounded.Dashboard),
                    Pair("Poles",     Icons.Rounded.Bolt),
                    Pair("Users",     Icons.Rounded.Group)
                ).forEachIndexed { idx, (label, icon) ->
                    val selected = selectedTab == idx
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .clip(RoundedCornerShape(12.dp))
                            .background(
                                if (selected) AdAmber.copy(alpha = 0.18f) else AdGlass
                            )
                            .border(
                                1.dp,
                                if (selected) AdAmber.copy(alpha = 0.5f) else AdGlassBorder,
                                RoundedCornerShape(12.dp)
                            )
                            .clickable { selectedTab = idx }
                            .padding(vertical = 10.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Row(
                            verticalAlignment     = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(5.dp)
                        ) {
                            Icon(
                                icon, null,
                                tint     = if (selected) AdAmber else AdTextSub,
                                modifier = Modifier.size(14.dp)
                            )
                            Text(
                                label,
                                fontSize   = 12.sp,
                                fontWeight = if (selected) FontWeight.Bold else FontWeight.Normal,
                                color      = if (selected) AdAmber else AdTextSub
                            )
                        }
                    }
                }
            }

            // ── Content ───────────────────────────────────────────────────────
            LazyColumn(
                modifier            = Modifier
                    .fillMaxSize()
                    .padding(horizontal = 20.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp),
                contentPadding      = PaddingValues(bottom = 32.dp, top = 4.dp)
            ) {

                // ════════════════════════════════════════════════════════════
                // TAB 0 — OVERVIEW
                // ════════════════════════════════════════════════════════════
                if (selectedTab == 0) {

                    // Network health banner
                    item {
                        AnimatedVisibility(
                            visible = true,
                            enter   = fadeIn(tween(400)) + expandVertically(tween(400))
                        ) {
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clip(RoundedCornerShape(18.dp))
                                    .background(
                                        Brush.linearGradient(
                                            listOf(AdAmber.copy(alpha = 0.18f), AdBgCard.copy(alpha = 0.9f))
                                        )
                                    )
                                    .border(1.dp, AdAmber.copy(alpha = 0.30f), RoundedCornerShape(18.dp))
                                    .padding(20.dp)
                            ) {
                                Column {
                                    Row(
                                        verticalAlignment     = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        modifier              = Modifier.fillMaxWidth()
                                    ) {
                                        Column {
                                            Text(
                                                "NETWORK HEALTH",
                                                fontSize      = 10.sp,
                                                letterSpacing = 1.5.sp,
                                                color         = AdTextSub,
                                                fontWeight    = FontWeight.SemiBold
                                            )
                                            Text(
                                                "$healthPct%",
                                                fontSize   = 36.sp,
                                                fontWeight = FontWeight.Bold,
                                                color      = when {
                                                    healthPct >= 75 -> AdGreen
                                                    healthPct >= 40 -> AdAmber
                                                    else            -> AdRed
                                                }
                                            )
                                            Text(
                                                "$working of $totalPoles poles operational",
                                                fontSize = 12.sp,
                                                color    = AdTextSub
                                            )
                                        }
                                        Box(
                                            modifier = Modifier
                                                .size(64.dp)
                                                .clip(CircleShape)
                                                .background(AdAmber.copy(alpha = 0.12f))
                                                .border(2.dp, AdAmber.copy(alpha = 0.35f), CircleShape),
                                            contentAlignment = Alignment.Center
                                        ) {
                                            Icon(
                                                Icons.Rounded.ElectricBolt, null,
                                                tint     = AdAmber,
                                                modifier = Modifier.size(30.dp)
                                            )
                                        }
                                    }
                                    Spacer(Modifier.height(14.dp))
                                    // Health bar
                                    Box(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .height(6.dp)
                                            .clip(RoundedCornerShape(3.dp))
                                            .background(AdGlass)
                                    ) {
                                        Box(
                                            modifier = Modifier
                                                .fillMaxWidth(healthPct / 100f)
                                                .fillMaxHeight()
                                                .clip(RoundedCornerShape(3.dp))
                                                .background(
                                                    Brush.horizontalGradient(
                                                        listOf(
                                                            when {
                                                                healthPct >= 75 -> AdGreen
                                                                healthPct >= 40 -> AdAmber
                                                                else            -> AdRed
                                                            },
                                                            AdAmber.copy(alpha = 0.6f)
                                                        )
                                                    )
                                                )
                                        )
                                    }
                                }
                            }
                        }
                    }

                    // Stat cards row 1
                    item {
                        Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                            AdminStatCard(
                                label    = "Total Poles",
                                value    = "$totalPoles",
                                icon     = Icons.Rounded.Bolt,
                                accent   = AdAmber,
                                modifier = Modifier.weight(1f)
                            )
                            AdminStatCard(
                                label    = "Working",
                                value    = "$working",
                                icon     = Icons.Rounded.CheckCircle,
                                accent   = AdGreen,
                                modifier = Modifier.weight(1f)
                            )
                        }
                    }

                    // Stat cards row 2
                    item {
                        Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                            AdminStatCard(
                                label    = "Fused",
                                value    = "$fused",
                                icon     = Icons.Rounded.ErrorOutline,
                                accent   = AdSapphire,
                                modifier = Modifier.weight(1f)
                            )
                            AdminStatCard(
                                label    = "Burning",
                                value    = "$burning",
                                icon     = Icons.Rounded.LocalFireDepartment,
                                accent   = AdRed,
                                modifier = Modifier.weight(1f)
                            )
                        }
                    }

                    // Energy estimate card
                    item {
                        val energySaved    = working * 2
                        val energyWasted   = (fused + burning) * 3
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(16.dp))
                                .background(AdGlass)
                                .border(0.5.dp, AdGlassBorder, RoundedCornerShape(16.dp))
                                .padding(18.dp)
                        ) {
                            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                                Text(
                                    "ENERGY OVERVIEW",
                                    fontSize      = 10.sp,
                                    letterSpacing = 1.5.sp,
                                    color         = AdTextSub,
                                    fontWeight    = FontWeight.SemiBold
                                )
                                Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                                    AdminInfoRow(
                                        label  = "Est. Saved",
                                        value  = "$energySaved kWh",
                                        color  = AdGreen,
                                        modifier = Modifier.weight(1f)
                                    )
                                    AdminInfoRow(
                                        label  = "Est. Wasted",
                                        value  = "$energyWasted kWh",
                                        color  = AdRed,
                                        modifier = Modifier.weight(1f)
                                    )
                                }
                            }
                        }
                    }

                    // System info card
                    item {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(16.dp))
                                .background(AdGlass)
                                .border(0.5.dp, AdGlassBorder, RoundedCornerShape(16.dp))
                                .padding(18.dp)
                        ) {
                            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                                Text(
                                    "SYSTEM INFO",
                                    fontSize      = 10.sp,
                                    letterSpacing = 1.5.sp,
                                    color         = AdTextSub,
                                    fontWeight    = FontWeight.SemiBold
                                )
                                AdminDetailRow("App",      "GrameenLight v1.0",   Icons.Rounded.Info,        AdAmber)
                                AdminDetailRow("Database", "Firebase Firestore",   Icons.Rounded.Storage,     AdSapphire)
                                AdminDetailRow("Map",      "OpenStreetMap (OSM)",  Icons.Rounded.Map,         AdGreen)
                                AdminDetailRow("Auth",     "Firebase Auth",        Icons.Rounded.Lock,        AdAmber)
                                AdminDetailRow("UID",      currentUid.take(16) + "…", Icons.Rounded.Badge,   AdTextSub)
                            }
                        }
                    }
                }

                // ════════════════════════════════════════════════════════════
                // TAB 1 — POLES
                // ════════════════════════════════════════════════════════════
                if (selectedTab == 1) {

                    // Summary strip
                    item {
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            listOf(
                                Triple("All",     "$totalPoles", AdAmber),
                                Triple("Working", "$working",    AdGreen),
                                Triple("Fused",   "$fused",      AdSapphire),
                                Triple("Burning", "$burning",    AdRed)
                            ).forEach { (label, count, color) ->
                                Box(
                                    modifier = Modifier
                                        .weight(1f)
                                        .clip(RoundedCornerShape(12.dp))
                                        .background(color.copy(alpha = 0.12f))
                                        .border(0.5.dp, color.copy(alpha = 0.35f), RoundedCornerShape(12.dp))
                                        .padding(vertical = 10.dp),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                        Text(count, fontSize = 18.sp, fontWeight = FontWeight.Bold, color = color)
                                        Text(label,  fontSize = 10.sp, color = AdTextSub)
                                    }
                                }
                            }
                        }
                    }

                    // Pole list header
                    item {
                        Text(
                            "ALL POLES",
                            fontSize      = 10.sp,
                            letterSpacing = 1.5.sp,
                            color         = AdTextSub,
                            fontWeight    = FontWeight.SemiBold,
                            modifier      = Modifier.padding(top = 4.dp, start = 2.dp)
                        )
                    }

                    // Pole rows
                    items(poles) { pole ->
                        val statusColor = when (pole.status) {
                            "working" -> AdGreen
                            "burning" -> AdRed
                            "fused"   -> AdSapphire
                            else      -> AdTextSub
                        }
                        val statusIcon = when (pole.status) {
                            "working" -> Icons.Rounded.CheckCircle
                            "burning" -> Icons.Rounded.LocalFireDepartment
                            "fused"   -> Icons.Rounded.ErrorOutline
                            else      -> Icons.Rounded.HelpOutline
                        }
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(14.dp))
                                .background(AdGlass)
                                .border(0.5.dp, AdGlassBorder, RoundedCornerShape(14.dp))
                                .padding(horizontal = 16.dp, vertical = 12.dp)
                        ) {
                            Row(
                                modifier          = Modifier.fillMaxWidth(),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(38.dp)
                                        .clip(RoundedCornerShape(10.dp))
                                        .background(statusColor.copy(alpha = 0.14f))
                                        .border(0.5.dp, statusColor.copy(alpha = 0.35f), RoundedCornerShape(10.dp)),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(statusIcon, null, tint = statusColor, modifier = Modifier.size(18.dp))
                                }
                                Spacer(Modifier.width(12.dp))
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(pole.id, fontSize = 13.sp, fontWeight = FontWeight.SemiBold, color = AdTextPrimary)
                                    Text(
                                        "%.5f, %.5f".format(pole.lat, pole.lon),
                                        fontSize = 11.sp,
                                        color    = AdTextSub
                                    )
                                }
                                Box(
                                    modifier = Modifier
                                        .clip(RoundedCornerShape(20.dp))
                                        .background(statusColor.copy(alpha = 0.14f))
                                        .border(0.5.dp, statusColor.copy(alpha = 0.40f), RoundedCornerShape(20.dp))
                                        .padding(horizontal = 10.dp, vertical = 4.dp)
                                ) {
                                    Text(
                                        pole.status.replaceFirstChar { it.uppercase() },
                                        fontSize   = 11.sp,
                                        fontWeight = FontWeight.SemiBold,
                                        color      = statusColor
                                    )
                                }
                            }
                        }
                    }

                    if (poles.isEmpty()) {
                        item {
                            Box(
                                modifier         = Modifier.fillMaxWidth().height(120.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                CircularProgressIndicator(color = AdAmber, strokeWidth = 2.dp)
                            }
                        }
                    }
                }

                // ════════════════════════════════════════════════════════════
                // TAB 2 — USERS
                // ════════════════════════════════════════════════════════════
                if (selectedTab == 2) {

                    item {
                        Text(
                            "REGISTERED USERS",
                            fontSize      = 10.sp,
                            letterSpacing = 1.5.sp,
                            color         = AdTextSub,
                            fontWeight    = FontWeight.SemiBold,
                            modifier      = Modifier.padding(top = 4.dp, start = 2.dp)
                        )
                    }

                    if (users.isEmpty()) {
                        item {
                            Box(
                                modifier         = Modifier.fillMaxWidth().height(120.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                CircularProgressIndicator(color = AdAmber, strokeWidth = 2.dp)
                            }
                        }
                    }

                    items(users) { u ->
                        val roleAccent = when (u.role) {
                            "worker" -> AdSapphire
                            "admin"  -> AdAmber
                            else     -> AdEmerald
                        }
                        val roleEmoji = when (u.role) {
                            "worker" -> "🔧"
                            "admin"  -> "🛡️"
                            else     -> "👤"
                        }
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(14.dp))
                                .background(AdGlass)
                                .border(0.5.dp, AdGlassBorder, RoundedCornerShape(14.dp))
                                .padding(horizontal = 16.dp, vertical = 14.dp)
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                // Avatar
                                Box(
                                    modifier = Modifier
                                        .size(44.dp)
                                        .clip(CircleShape)
                                        .background(roleAccent.copy(alpha = 0.14f))
                                        .border(1.dp, roleAccent.copy(alpha = 0.40f), CircleShape),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Text(roleEmoji, fontSize = 20.sp)
                                }
                                Spacer(Modifier.width(14.dp))
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(u.name,  fontSize = 14.sp, fontWeight = FontWeight.SemiBold, color = AdTextPrimary)
                                    Text(u.email, fontSize = 11.sp, color = AdTextSub)
                                    Spacer(Modifier.height(4.dp))
                                    Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                        // Role pill
                                        Box(
                                            modifier = Modifier
                                                .clip(RoundedCornerShape(20.dp))
                                                .background(roleAccent.copy(alpha = 0.14f))
                                                .border(0.5.dp, roleAccent.copy(alpha = 0.40f), RoundedCornerShape(20.dp))
                                                .padding(horizontal = 8.dp, vertical = 2.dp)
                                        ) {
                                            Text(
                                                u.role.replaceFirstChar { it.uppercase() },
                                                fontSize   = 10.sp,
                                                fontWeight = FontWeight.SemiBold,
                                                color      = roleAccent
                                            )
                                        }
                                        // Badge pill
                                        if (u.badge.isNotEmpty()) {
                                            Box(
                                                modifier = Modifier
                                                    .clip(RoundedCornerShape(20.dp))
                                                    .background(AdAmber.copy(alpha = 0.10f))
                                                    .border(0.5.dp, AdAmber.copy(alpha = 0.30f), RoundedCornerShape(20.dp))
                                                    .padding(horizontal = 8.dp, vertical = 2.dp)
                                            ) {
                                                Text(
                                                    "⭐ ${u.badge}",
                                                    fontSize = 10.sp,
                                                    color    = AdAmber
                                                )
                                            }
                                        }
                                    }
                                }
                                Column(horizontalAlignment = Alignment.End) {
                                    Text("${u.reportCount}", fontSize = 18.sp, fontWeight = FontWeight.Bold, color = roleAccent)
                                    Text("reports", fontSize = 10.sp, color = AdTextSub)
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

// ─── Admin helper composables ──────────────────────────────────────────────────

@Composable
private fun AdminStatCard(
    label    : String,
    value    : String,
    icon     : ImageVector,
    accent   : Color,
    modifier : Modifier = Modifier
) {
    Box(
        modifier = modifier
            .clip(RoundedCornerShape(16.dp))
            .background(accent.copy(alpha = 0.10f))
            .border(0.5.dp, accent.copy(alpha = 0.30f), RoundedCornerShape(16.dp))
            .padding(16.dp)
    ) {
        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Box(
                modifier = Modifier
                    .size(36.dp)
                    .clip(RoundedCornerShape(10.dp))
                    .background(accent.copy(alpha = 0.15f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(icon, null, tint = accent, modifier = Modifier.size(18.dp))
            }
            Text(value, fontSize = 24.sp, fontWeight = FontWeight.Bold, color = accent)
            Text(label, fontSize = 11.sp, color = AdTextSub)
        }
    }
}

@Composable
private fun AdminDetailRow(
    label  : String,
    value  : String,
    icon   : ImageVector,
    accent : Color
) {
    Row(
        modifier          = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(10.dp))
            .background(AdGlass)
            .border(0.5.dp, AdGlassBorder, RoundedCornerShape(10.dp))
            .padding(horizontal = 12.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(icon, null, tint = accent, modifier = Modifier.size(16.dp))
        Spacer(Modifier.width(10.dp))
        Text(label, fontSize = 11.sp, color = AdTextSub, modifier = Modifier.width(70.dp))
        Text(value, fontSize = 12.sp, fontWeight = FontWeight.Medium, color = AdTextPrimary)
    }
}

@Composable
private fun AdminInfoRow(
    label    : String,
    value    : String,
    color    : Color,
    modifier : Modifier = Modifier
) {
    Box(
        modifier = modifier
            .clip(RoundedCornerShape(12.dp))
            .background(color.copy(alpha = 0.10f))
            .border(0.5.dp, color.copy(alpha = 0.30f), RoundedCornerShape(12.dp))
            .padding(12.dp)
    ) {
        Column {
            Text(label, fontSize = 10.sp, color = AdTextSub)
            Text(value, fontSize = 15.sp, fontWeight = FontWeight.Bold, color = color)
        }
    }
}
