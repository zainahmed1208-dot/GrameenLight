package com.example.grameenlight.ui.screens

import android.app.Activity
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccountCircle
import androidx.compose.material.icons.filled.Build
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Security
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.blur
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.draw.scale
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.*
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.google.android.gms.auth.api.signin.*
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.GoogleAuthProvider
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.SetOptions
import kotlin.math.sin
import kotlin.math.cos

// ─── Palette ───────────────────────────────────────────────────────────────
private val BgDeep    = Color(0xFF080E1A)
private val BgMid     = Color(0xFF0D1B2A)
private val Emerald   = Color(0xFF00D68F)
private val EmeraldDim= Color(0xFF00A86B)
private val Sapphire  = Color(0xFF0D6EFD)
private val Amber     = Color(0xFFFFC400)
private val GlassWhite= Color(0x18FFFFFF)
private val GlassBorder=Color(0x30FFFFFF)
private val TextPrimary = Color(0xFFF0F4FF)
private val TextSub   = Color(0xFF7A8BA6)

// ─── Role data ─────────────────────────────────────────────────────────────
private data class RoleConfig(
    val id: String,
    val label: String,
    val subtitle: String,
    val icon: androidx.compose.ui.graphics.vector.ImageVector,
    val accent: Color,
    val gradientStart: Color,
    val gradientEnd: Color
)

private val roles = listOf(
    RoleConfig(
        id = "citizen",
        label = "Citizen",
        subtitle = "Report & track issues",
        icon = Icons.Filled.Person,
        accent = Emerald,
        gradientStart = Color(0xFF003D28),
        gradientEnd = Color(0xFF001A12)
    ),
    RoleConfig(
        id = "worker",
        label = "Field Worker",
        subtitle = "Manage assigned tasks",
        icon = Icons.Filled.Build,
        accent = Sapphire,
        gradientStart = Color(0xFF002366),
        gradientEnd = Color(0xFF001133)
    ),
    RoleConfig(
        id = "admin",
        label = "Admin",
        subtitle = "Full system control",
        icon = Icons.Filled.Security,
        accent = Amber,
        gradientStart = Color(0xFF3D2800),
        gradientEnd = Color(0xFF1A1000)
    )
)

// ─── Screen ─────────────────────────────────────────────────────────────────
@Composable
fun LoginScreen(
    onNavigate: (String) -> Unit
) {
    val context = LocalContext.current
    var selectedRole by remember { mutableStateOf("citizen") }
    var isLoading    by remember { mutableStateOf(false) }

    // ── Google Sign-In client (LOGIC UNCHANGED) ──────────────────────────────
    val googleSignInClient = GoogleSignIn.getClient(
        context,
        GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
            .requestIdToken("287394813859-s11c7ao761m8pkiaf1bq51j203mulsul.apps.googleusercontent.com")
            .requestEmail()
            .build()
    )

    // ── Launcher (LOGIC UNCHANGED) ───────────────────────────────────────────
    val launcher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.StartActivityForResult()
    ) { result ->

        if (result.resultCode == Activity.RESULT_OK) {
            val task    = GoogleSignIn.getSignedInAccountFromIntent(result.data)
            val account = task.result
            val credential = GoogleAuthProvider.getCredential(account.idToken, null)

            FirebaseAuth.getInstance().signInWithCredential(credential)
                .addOnSuccessListener {
                    val user  = FirebaseAuth.getInstance().currentUser
                    val uid   = user?.uid ?: return@addOnSuccessListener
                    val email = user.email ?: ""

                    // 🔒 WORKER RESTRICTION
                    if (selectedRole == "worker" && email != "grameenlight308@gmail.com") {
                        Toast.makeText(context, "Not authorized as worker", Toast.LENGTH_SHORT).show()
                        FirebaseAuth.getInstance().signOut()
                        isLoading = false
                        return@addOnSuccessListener
                    }

                    // 🔥 ROLE LOGIC
                    val role = when {
                        selectedRole == "worker" && email == "grameenlight308@gmail.com" -> "worker"
                        selectedRole == "admin" -> "admin"
                        else -> "citizen"
                    }

                    val db      = FirebaseFirestore.getInstance()
                    val userRef = db.collection("users").document(uid)

                    userRef.get().addOnSuccessListener { snapshot ->
                        val newUser = mapOf(
                            "userId"      to uid,
                            "name"        to user.displayName,
                            "email"       to email,
                            "role"        to role,
                            // 🔥 PRESERVE OLD VALUES (IMPORTANT)
                            "phone"       to (snapshot.getString("phone") ?: if (role == "worker") "9876543210" else ""),
                            "address"     to (snapshot.getString("address") ?: if (role == "worker") "Bangalore" else ""),
                            "reportCount" to (snapshot.getLong("reportCount") ?: 0),
                            "badge"       to (snapshot.getString("badge") ?: "🌱 Beginner")
                        )
                        // ✅ ALWAYS UPDATE (THIS FIXES YOUR ISSUE)
                        userRef.set(newUser, SetOptions.merge())

                        // 🔥 NAVIGATION
                        when (role) {
                            "worker" -> onNavigate("worker")
                            "admin"  -> onNavigate("admin")
                            else     -> onNavigate("home")
                        }
                    }
                }
                .addOnFailureListener {
                    isLoading = false
                    Toast.makeText(context, "Sign-in failed: ${it.message}", Toast.LENGTH_SHORT).show()
                }
        } else {
            isLoading = false
        }
    }

    // ── Animated background orbs ─────────────────────────────────────────────
    val infiniteTransition = rememberInfiniteTransition(label = "bg")
    val orb1Phase by infiniteTransition.animateFloat(
        initialValue = 0f, targetValue = (2 * Math.PI).toFloat(),
        animationSpec = infiniteRepeatable(tween(12000, easing = LinearEasing)),
        label = "orb1"
    )
    val orb2Phase by infiniteTransition.animateFloat(
        initialValue = (Math.PI).toFloat(), targetValue = (3 * Math.PI).toFloat(),
        animationSpec = infiniteRepeatable(tween(16000, easing = LinearEasing)),
        label = "orb2"
    )
    val pulse by infiniteTransition.animateFloat(
        initialValue = 0.95f, targetValue = 1.05f,
        animationSpec = infiniteRepeatable(tween(2000, easing = FastOutSlowInEasing), RepeatMode.Reverse),
        label = "pulse"
    )

    // ── Root container ───────────────────────────────────────────────────────
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(BgDeep)
            .drawBehind {
                drawAnimatedOrbs(orb1Phase, orb2Phase, size.width, size.height)
            }
    ) {
        // Blur overlay for depth
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(
                    Brush.verticalGradient(
                        listOf(Color(0x00080E1A), Color(0xCC080E1A), Color(0xFF080E1A))
                    )
                )
        )

        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 24.dp),
            verticalArrangement = Arrangement.Center,
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Spacer(Modifier.height(48.dp))

            // ── Logo / Brand ─────────────────────────────────────────────────
            Box(
                modifier = Modifier
                    .size(96.dp)
                    .scale(pulse)
                    .clip(CircleShape)
                    .background(
                        Brush.radialGradient(listOf(Emerald.copy(alpha = 0.3f), Color.Transparent))
                    )
                    .border(1.5.dp, Emerald.copy(alpha = 0.5f), CircleShape),
                contentAlignment = Alignment.Center
            ) {
                Text("💡", fontSize = 40.sp)
            }

            Spacer(Modifier.height(20.dp))

            Text(
                text = "GrameenLight",
                fontSize = 30.sp,
                fontWeight = FontWeight.ExtraBold,
                color = TextPrimary,
                letterSpacing = 0.5.sp
            )
            Text(
                text = "Powering rural communities",
                fontSize = 13.sp,
                color = TextSub,
                letterSpacing = 1.2.sp
            )

            Spacer(Modifier.height(40.dp))

            // ── Section label ────────────────────────────────────────────────
            Text(
                text = "SELECT YOUR ROLE",
                fontSize = 11.sp,
                fontWeight = FontWeight.SemiBold,
                color = TextSub,
                letterSpacing = 2.sp,
                modifier = Modifier.fillMaxWidth()
            )

            Spacer(Modifier.height(14.dp))

            // ── Role Cards ───────────────────────────────────────────────────
            roles.forEach { role ->
                RoleCard(
                    role      = role,
                    isSelected = selectedRole == role.id,
                    isLoading = isLoading && selectedRole == role.id,
                    onClick = {
                        if (!isLoading) {
                            selectedRole = role.id
                            isLoading = true
                            googleSignInClient.signOut().addOnCompleteListener {
                                launcher.launch(googleSignInClient.signInIntent)
                            }
                        }
                    }
                )
                Spacer(Modifier.height(12.dp))
            }

            Spacer(Modifier.height(32.dp))

            // ── Google branding footer ────────────────────────────────────────
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.Center
            ) {
                Text("🔐", fontSize = 14.sp)
                Spacer(Modifier.width(6.dp))
                Text(
                    "Secured by Google Sign-In & Firebase",
                    fontSize = 12.sp,
                    color = TextSub,
                    textAlign = TextAlign.Center
                )
            }

            Spacer(Modifier.height(48.dp))
        }
    }
}

// ─── Role Card ───────────────────────────────────────────────────────────────
@Composable
private fun RoleCard(
    role: RoleConfig,
    isSelected: Boolean,
    isLoading: Boolean,
    onClick: () -> Unit
) {
    val scale by animateFloatAsState(
        targetValue = if (isSelected) 1.02f else 1f,
        animationSpec = spring(dampingRatio = Spring.DampingRatioMediumBouncy),
        label = "cardScale"
    )
    val borderAlpha by animateFloatAsState(
        targetValue = if (isSelected) 1f else 0.25f,
        animationSpec = tween(300),
        label = "borderAlpha"
    )
    val bgAlpha by animateFloatAsState(
        targetValue = if (isSelected) 1f else 0.5f,
        animationSpec = tween(300),
        label = "bgAlpha"
    )

    val infiniteTransition = rememberInfiniteTransition(label = "shimmer")
    val shimmerOffset by infiniteTransition.animateFloat(
        initialValue = -300f, targetValue = 300f,
        animationSpec = infiniteRepeatable(tween(1800, easing = LinearEasing)),
        label = "shimmer"
    )

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .scale(scale)
            .clip(RoundedCornerShape(18.dp))
            .background(
                Brush.linearGradient(
                    listOf(
                        role.gradientStart.copy(alpha = bgAlpha),
                        role.gradientEnd.copy(alpha = bgAlpha)
                    )
                )
            )
            .then(
                if (isSelected) Modifier.drawBehind {
                    // Shimmer sweep on selected card
                    drawRect(
                        brush = Brush.linearGradient(
                            listOf(
                                Color.Transparent,
                                Color.White.copy(alpha = 0.05f),
                                Color.Transparent
                            ),
                            start = Offset(shimmerOffset, 0f),
                            end   = Offset(shimmerOffset + 200f, size.height)
                        )
                    )
                } else Modifier
            )
            .border(
                width = 1.5.dp,
                brush = Brush.linearGradient(
                    listOf(
                        role.accent.copy(alpha = borderAlpha),
                        role.accent.copy(alpha = borderAlpha * 0.4f)
                    )
                ),
                shape = RoundedCornerShape(18.dp)
            )
            .clickable(enabled = true, onClick = onClick)
            .padding(horizontal = 20.dp, vertical = 18.dp)
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Icon bubble
            Box(
                modifier = Modifier
                    .size(52.dp)
                    .clip(RoundedCornerShape(14.dp))
                    .background(role.accent.copy(alpha = 0.15f))
                    .border(1.dp, role.accent.copy(alpha = 0.4f), RoundedCornerShape(14.dp)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = role.icon,
                    contentDescription = role.label,
                    tint = role.accent,
                    modifier = Modifier.size(26.dp)
                )
            }

            Spacer(Modifier.width(16.dp))

            // Text block
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = role.label,
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Bold,
                    color = TextPrimary
                )
                Text(
                    text = role.subtitle,
                    fontSize = 12.sp,
                    color = TextSub
                )
            }

            // Right side: loading or arrow
            AnimatedContent(
                targetState = isLoading,
                label = "rightIcon"
            ) { loading ->
                if (loading) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(22.dp),
                        color = role.accent,
                        strokeWidth = 2.dp
                    )
                } else {
                    Box(
                        modifier = Modifier
                            .size(32.dp)
                            .clip(CircleShape)
                            .background(
                                if (isSelected) role.accent.copy(alpha = 0.2f)
                                else Color.Transparent
                            ),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = "→",
                            color = if (isSelected) role.accent else TextSub,
                            fontSize = 18.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }
        }

        // Selected glow dot
        if (isSelected) {
            Box(
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .padding(10.dp)
                    .size(8.dp)
                    .clip(CircleShape)
                    .background(role.accent)
            )
        }
    }
}

// ─── Background orb painter ───────────────────────────────────────────────────
private fun DrawScope.drawAnimatedOrbs(
    phase1: Float,
    phase2: Float,
    width: Float,
    height: Float
) {
    // Orb 1 — emerald
    val x1 = width  * 0.5f + cos(phase1) * width  * 0.35f
    val y1 = height * 0.3f + sin(phase1) * height * 0.15f
    drawCircle(
        brush = Brush.radialGradient(
            listOf(Color(0x2200D68F), Color.Transparent),
            center = Offset(x1, y1),
            radius = 280f
        ),
        radius = 280f,
        center = Offset(x1, y1)
    )

    // Orb 2 — sapphire
    val x2 = width  * 0.5f + cos(phase2) * width  * 0.3f
    val y2 = height * 0.7f + sin(phase2) * height * 0.12f
    drawCircle(
        brush = Brush.radialGradient(
            listOf(Color(0x220D6EFD), Color.Transparent),
            center = Offset(x2, y2),
            radius = 240f
        ),
        radius = 240f,
        center = Offset(x2, y2)
    )

    // Orb 3 — amber (static top-right accent)
    drawCircle(
        brush = Brush.radialGradient(
            listOf(Color(0x18FFC400), Color.Transparent),
            center = Offset(width * 0.85f, height * 0.1f),
            radius = 160f
        ),
        radius = 160f,
        center = Offset(width * 0.85f, height * 0.1f)
    )
}
