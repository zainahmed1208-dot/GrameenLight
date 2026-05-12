package com.example.grameenlight.ui.screens

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import com.google.firebase.auth.FirebaseAuth
import android.widget.Toast
import com.google.firebase.firestore.FirebaseFirestore

import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.background
import androidx.navigation.NavController
import androidx.compose.material.icons.automirrored.rounded.Logout
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.*
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.content.ContextCompat
import com.example.grameenlight.data.LocationData
import com.example.grameenlight.data.firebase.FirebaseRepository
import com.example.grameenlight.data.firebase.PoleDto
import com.example.grameenlight.data.model.User
import org.osmdroid.config.Configuration
import org.osmdroid.events.MapEventsReceiver
import org.osmdroid.views.overlay.MapEventsOverlay
import org.osmdroid.tileprovider.tilesource.TileSourceFactory
import org.osmdroid.util.GeoPoint
import org.osmdroid.views.MapView
import org.osmdroid.views.overlay.Marker
import java.util.UUID

// ─── Map Color Scheme (UNCHANGED) ─────────────────────────────────────────────
private val AppDarkColorScheme = darkColorScheme(
    primary          = Color(0xFF60A5FA),
    onPrimary        = Color(0xFF0F172A),
    primaryContainer = Color(0xFF1E3A5F),
    surface          = Color(0xFF1E293B),
    surfaceVariant   = Color(0xFF334155),
    onSurface        = Color(0xFFF1F5F9),
    onSurfaceVariant = Color(0xFF94A3B8),
    background       = Color(0xFF0F172A),
    onBackground     = Color(0xFFF1F5F9),
    error            = Color(0xFFF87171),
    outline          = Color(0xFF475569),
)

private val AppLightColorScheme = lightColorScheme(
    primary          = Color(0xFF1D4ED8),
    onPrimary        = Color(0xFFFFFFFF),
    primaryContainer = Color(0xFFDBEAFE),
    surface          = Color(0xFFFFFFFF),
    surfaceVariant   = Color(0xFFF1F5F9),
    onSurface        = Color(0xFF0F172A),
    onSurfaceVariant = Color(0xFF64748B),
    background       = Color(0xFFF1F5F9),
    onBackground     = Color(0xFF0F172A),
    error            = Color(0xFFDC2626),
    outline          = Color(0xFFCBD5E1),
)

private val ColorGreen = Color(0xFF4ADE80)
private val ColorRed   = Color(0xFFF87171)
private val ColorBlue  = Color(0xFF60A5FA)
private val ColorAmber = Color(0xFFFBBF24)

private const val DELETE_RADIUS_METRES = 60.0

// ─── GrameenLight Profile Theme ───────────────────────────────────────────────
private val GlBgDeep       = Color(0xFF070D18)
private val GlBgCard       = Color(0xFF0D1421)
private val GlEmerald      = Color(0xFF00D68F)
private val GlSapphire     = Color(0xFF4D9FFF)
private val GlAmberAccent  = Color(0xFFFFC400)
private val GlTextPrimary  = Color(0xFFEEF3FF)
private val GlTextSub      = Color(0xFF566880)
private val GlGlass        = Color(0x14FFFFFF)
private val GlGlassBorder  = Color(0x20FFFFFF)
private val GlDivider      = Color(0x18FFFFFF)

// ─────────────────────────────────────────────────────────────────────────────

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun OSMMapScreen(
    navController: NavController,
    isWorker: Boolean = false
) {
    var editMode     by remember { mutableStateOf(false) }
    var phoneInput   by remember { mutableStateOf("") }
    var addressInput by remember { mutableStateOf("") }
    var isDarkMode   by remember { mutableStateOf(true) }
    val colorScheme  = if (isDarkMode) AppDarkColorScheme else AppLightColorScheme

    MaterialTheme(colorScheme = colorScheme) {

        val context = LocalContext.current
        val userId  = "USER_001"

        val poles = remember { mutableStateListOf<PoleDto>() }

        var selectedPole  by remember { mutableStateOf<PoleDto?>(null) }
        var showPoleSheet by remember { mutableStateOf(false) }

        var pendingGeoPoint   by remember { mutableStateOf<GeoPoint?>(null) }
        var showAddPoleSheet  by remember { mutableStateOf(false) }
        var newPoleStatusPick by remember { mutableStateOf("working") }

        var poleToDelete    by remember { mutableStateOf<PoleDto?>(null) }
        var showDeleteSheet by remember { mutableStateOf(false) }

        var showLocationSheet by remember { mutableStateOf(false) }
        var pickerStep        by remember { mutableStateOf(0) }
        var pickedState       by remember { mutableStateOf("") }
        var selectedCityLabel by remember { mutableStateOf("") }

        var user             by remember { mutableStateOf<User?>(null) }
        var showProfileSheet by remember { mutableStateOf(false) }
        var isExpanded       by remember { mutableStateOf(false) }

        // ── MAP ───────────────────────────────────────────────────────────────
        val mapView = remember {
            MapView(context).apply {
                Configuration.getInstance().load(context,
                    context.getSharedPreferences("osm", 0))
                setTileSource(TileSourceFactory.MAPNIK)
                setMultiTouchControls(true)
                controller.setZoom(16.0)
                controller.setCenter(GeoPoint(12.9716, 77.5946))
            }
        }

        // ── LONG PRESS (worker only) ──────────────────────────────────────────
        DisposableEffect(isWorker) {
            val receiver = object : MapEventsReceiver {
                override fun singleTapConfirmedHelper(p: GeoPoint?): Boolean = false
                override fun longPressHelper(p: GeoPoint?): Boolean {
                    if (!isWorker || p == null) return false
                    val nearest = poles.minByOrNull { pole ->
                        GeoPoint(pole.lat, pole.lon).distanceToAsDouble(p)
                    }
                    if (nearest != null &&
                        GeoPoint(nearest.lat, nearest.lon).distanceToAsDouble(p) <= DELETE_RADIUS_METRES
                    ) {
                        poleToDelete    = nearest
                        showDeleteSheet = true
                    } else {
                        pendingGeoPoint   = p
                        newPoleStatusPick = "working"
                        showAddPoleSheet  = true
                    }
                    return true
                }
            }
            val overlay = MapEventsOverlay(receiver)
            mapView.overlays.add(0, overlay)
            mapView.invalidate()
            onDispose { mapView.overlays.remove(overlay) }
        }

        // ── FIREBASE ──────────────────────────────────────────────────────────
        DisposableEffect(Unit) {
            val poleListener = FirebaseRepository.listenPoles { poles.clear(); poles.addAll(it) }
            val userListener = FirebaseRepository.listenUser(userId) { user = it }
            onDispose { poleListener.remove(); userListener.remove() }
        }

        // ── MARKERS ───────────────────────────────────────────────────────────
        LaunchedEffect(poles.toList()) {
            mapView.overlays.removeIf { it is Marker }
            poles.forEach { pole ->
                val marker = Marker(mapView)
                marker.position = GeoPoint(pole.lat, pole.lon)
                marker.title    = pole.id
                val iconRes: Int = when (pole.status) {
                    "working" -> android.R.drawable.presence_online
                    "fused"   -> android.R.drawable.presence_busy
                    "burning" -> android.R.drawable.presence_away
                    else      -> android.R.drawable.presence_invisible
                }
                marker.icon = ContextCompat.getDrawable(context, iconRes)
                marker.setOnMarkerClickListener { _, _ ->
                    selectedPole  = pole
                    showPoleSheet = true
                    true
                }
                mapView.overlays.add(marker)
            }
            mapView.invalidate()
        }

        // ── METRICS ───────────────────────────────────────────────────────────
        val solved   = poles.count { it.status == "working" }
        val faulty   = poles.count { it.status == "fused" || it.status == "burning" }
        val saved    = solved * 2
        val consumed = faulty * 3

        // ── UI ────────────────────────────────────────────────────────────────
        Surface(modifier = Modifier.fillMaxSize(), color = MaterialTheme.colorScheme.background) {
            Column(modifier = Modifier.fillMaxSize().statusBarsPadding()) {

                // ── TOP BAR ───────────────────────────────────────────────────
                Surface(color = MaterialTheme.colorScheme.surface, tonalElevation = 3.dp) {
                    Row(
                        modifier          = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp, vertical = 10.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Surface(
                            shape    = CircleShape,
                            color    = MaterialTheme.colorScheme.primaryContainer,
                            modifier = Modifier.size(40.dp).clickable { showProfileSheet = true }
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Icon(Icons.Rounded.Person, "Profile",
                                    tint     = MaterialTheme.colorScheme.primary,
                                    modifier = Modifier.size(22.dp))
                            }
                        }

                        Spacer(Modifier.width(12.dp))

                        Column(modifier = Modifier.weight(1f)) {
                            Text("Rural Energy Map",
                                style      = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.SemiBold,
                                color      = MaterialTheme.colorScheme.onSurface)
                            Text(
                                if (selectedCityLabel.isNotEmpty()) selectedCityLabel
                                else "${poles.size} poles tracked",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }

                        Surface(
                            shape    = RoundedCornerShape(10.dp),
                            color    = MaterialTheme.colorScheme.surfaceVariant,
                            modifier = Modifier.clickable {
                                pickerStep        = 0
                                showLocationSheet = true
                            }
                        ) {
                            Row(
                                modifier              = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                                verticalAlignment     = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(4.dp)
                            ) {
                                Icon(Icons.Rounded.LocationOn, "Location",
                                    tint     = MaterialTheme.colorScheme.primary,
                                    modifier = Modifier.size(16.dp))
                                Text(
                                    if (pickedState.isNotEmpty()) pickedState.take(8) else "Location",
                                    style      = MaterialTheme.typography.labelSmall,
                                    color      = MaterialTheme.colorScheme.onSurfaceVariant,
                                    fontWeight = FontWeight.Medium)
                            }
                        }

                        Spacer(Modifier.width(8.dp))

                        Surface(
                            shape    = RoundedCornerShape(10.dp),
                            color    = MaterialTheme.colorScheme.surfaceVariant,
                            modifier = Modifier.clickable { isDarkMode = !isDarkMode }
                        ) {
                            Row(
                                modifier              = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                                verticalAlignment     = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                Icon(
                                    if (isDarkMode) Icons.Rounded.DarkMode else Icons.Rounded.LightMode,
                                    "Theme",
                                    tint     = MaterialTheme.colorScheme.primary,
                                    modifier = Modifier.size(18.dp))
                                Text(
                                    if (isDarkMode) "Dark" else "Light",
                                    style      = MaterialTheme.typography.labelSmall,
                                    color      = MaterialTheme.colorScheme.onSurfaceVariant,
                                    fontWeight = FontWeight.Medium)
                            }
                        }
                    }
                }

                // ── MAP AREA ──────────────────────────────────────────────────
                Box(modifier = Modifier
                    .fillMaxWidth()
                    .weight(if (isExpanded) 1f else 0.62f)
                ) {
                    AndroidView(factory = { mapView }, modifier = Modifier.fillMaxSize())

                    if (isWorker) {
                        Surface(
                            shape    = RoundedCornerShape(8.dp),
                            color    = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.90f),
                            modifier = Modifier.align(Alignment.TopCenter).padding(top = 10.dp)
                        ) {
                            Row(
                                modifier              = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
                                verticalAlignment     = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                Icon(Icons.Rounded.TouchApp, null,
                                    tint     = MaterialTheme.colorScheme.primary,
                                    modifier = Modifier.size(14.dp))
                                Text("Long press map to add  •  Long press pole to delete",
                                    style      = MaterialTheme.typography.labelSmall,
                                    color      = MaterialTheme.colorScheme.primary,
                                    fontWeight = FontWeight.Medium)
                            }
                        }
                    }

                    Surface(
                        shape          = RoundedCornerShape(8.dp),
                        color          = MaterialTheme.colorScheme.surface.copy(alpha = 0.88f),
                        tonalElevation = 4.dp,
                        modifier       = Modifier.align(Alignment.BottomStart).padding(12.dp)
                    ) {
                        Column(modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
                            verticalArrangement = Arrangement.spacedBy(4.dp)) {
                            LegendDot(ColorGreen, "Working")
                            LegendDot(ColorAmber, "Burning")
                            LegendDot(ColorRed,   "Fused")
                        }
                    }

                    Surface(
                        shape          = CircleShape,
                        color          = MaterialTheme.colorScheme.surface.copy(alpha = 0.92f),
                        tonalElevation = 6.dp,
                        modifier       = Modifier
                            .align(Alignment.BottomEnd)
                            .padding(12.dp)
                            .size(42.dp)
                            .clickable { isExpanded = !isExpanded }
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Icon(
                                if (isExpanded) Icons.Rounded.FullscreenExit
                                else            Icons.Rounded.Fullscreen,
                                "Fullscreen",
                                tint     = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(22.dp))
                        }
                    }
                }

                // ── METRICS ───────────────────────────────────────────────────
                AnimatedVisibility(
                    visible = !isExpanded,
                    enter   = slideInVertically(tween(300)) { it } + fadeIn(tween(300)),
                    exit    = slideOutVertically(tween(300)) { it } + fadeOut(tween(300))
                ) {
                    Surface(color = MaterialTheme.colorScheme.surface, tonalElevation = 2.dp,
                        modifier = Modifier.fillMaxWidth()) {
                        Column(modifier = Modifier.padding(20.dp)) {
                            Text("Live Metrics",
                                style    = MaterialTheme.typography.labelMedium,
                                color    = MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.padding(bottom = 16.dp))
                            Row(modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                                MetricCard("Energy Saved", "$saved kWh",   Icons.Rounded.Star,        ColorGreen, Modifier.weight(1f))
                                MetricCard("Consumed",     "$consumed kWh", Icons.Rounded.PowerOff,   ColorRed,   Modifier.weight(1f))
                                MetricCard("Resolved",     "$solved",       Icons.Rounded.CheckCircle, ColorBlue,  Modifier.weight(1f))
                            }
                        }
                    }
                }

                Spacer(Modifier.navigationBarsPadding())
            }
        }

        // ══════════════════════════════════════════════════════════════════════
        // SHEETS
        // ══════════════════════════════════════════════════════════════════════

        // ── LOCATION PICKER ───────────────────────────────────────────────────
        if (showLocationSheet) {
            ModalBottomSheet(
                onDismissRequest = { showLocationSheet = false },
                containerColor   = MaterialTheme.colorScheme.surface,
                tonalElevation   = 8.dp,
                shape            = RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp)
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .fillMaxHeight(0.75f)
                        .padding(horizontal = 24.dp)
                        .padding(bottom = 16.dp)
                ) {
                    Row(modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically) {
                        if (pickerStep == 1) {
                            IconButton(onClick = { pickerStep = 0 }) {
                                Icon(Icons.Rounded.ArrowBack, "Back",
                                    tint = MaterialTheme.colorScheme.onSurface)
                            }
                        }
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                if (pickerStep == 0) "Select State" else pickedState,
                                style      = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold,
                                color      = MaterialTheme.colorScheme.onSurface)
                            if (pickerStep == 1) {
                                Text("Choose a city",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant)
                            }
                        }
                    }

                    Spacer(Modifier.height(8.dp))
                    HorizontalDivider(color = MaterialTheme.colorScheme.outline)
                    Spacer(Modifier.height(8.dp))

                    if (pickerStep == 0) {
                        val stateList: List<String> = LocationData.data.keys.sorted()
                        LazyColumn(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                            items(stateList) { state ->
                                val cityCount: Int = LocationData.data[state]?.size ?: 0
                                Surface(
                                    shape    = RoundedCornerShape(10.dp),
                                    color    = MaterialTheme.colorScheme.surfaceVariant,
                                    modifier = Modifier.fillMaxWidth()
                                        .clickable { pickedState = state; pickerStep = 1 }
                                ) {
                                    Row(
                                        modifier = Modifier.fillMaxWidth()
                                            .padding(horizontal = 16.dp, vertical = 14.dp),
                                        verticalAlignment     = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.SpaceBetween
                                    ) {
                                        Row(verticalAlignment = Alignment.CenterVertically,
                                            horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                                            Icon(Icons.Rounded.LocationOn, null,
                                                tint     = MaterialTheme.colorScheme.primary,
                                                modifier = Modifier.size(18.dp))
                                            Text(state,
                                                style      = MaterialTheme.typography.bodyMedium,
                                                fontWeight = FontWeight.Medium,
                                                color      = MaterialTheme.colorScheme.onSurface)
                                        }
                                        Row(verticalAlignment = Alignment.CenterVertically,
                                            horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                                            Text("$cityCount cities",
                                                style = MaterialTheme.typography.labelSmall,
                                                color = MaterialTheme.colorScheme.onSurfaceVariant)
                                            Icon(Icons.Rounded.ChevronRight, null,
                                                tint     = MaterialTheme.colorScheme.onSurfaceVariant,
                                                modifier = Modifier.size(16.dp))
                                        }
                                    }
                                }
                            }
                        }
                    }

                    if (pickerStep == 1) {
                        val cities: Map<String, Pair<Double, Double>> =
                            LocationData.data[pickedState] ?: emptyMap()
                        val cityList: List<String> = cities.keys.sorted()
                        LazyColumn(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                            items(cityList) { city ->
                                val coords: Pair<Double, Double> = cities[city]!!
                                Surface(
                                    shape    = RoundedCornerShape(10.dp),
                                    color    = MaterialTheme.colorScheme.surfaceVariant,
                                    modifier = Modifier.fillMaxWidth().clickable {
                                        mapView.controller.animateTo(
                                            GeoPoint(coords.first, coords.second))
                                        mapView.controller.setZoom(14.0)
                                        selectedCityLabel = "$city, $pickedState"
                                        showLocationSheet  = false
                                    }
                                ) {
                                    Row(
                                        modifier = Modifier.fillMaxWidth()
                                            .padding(horizontal = 16.dp, vertical = 14.dp),
                                        verticalAlignment     = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.SpaceBetween
                                    ) {
                                        Row(verticalAlignment = Alignment.CenterVertically,
                                            horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                                            Icon(Icons.Rounded.Place, null,
                                                tint     = ColorBlue,
                                                modifier = Modifier.size(18.dp))
                                            Column {
                                                Text(city,
                                                    style      = MaterialTheme.typography.bodyMedium,
                                                    fontWeight = FontWeight.Medium,
                                                    color      = MaterialTheme.colorScheme.onSurface)
                                                Text("%.4f, %.4f".format(coords.first, coords.second),
                                                    style = MaterialTheme.typography.labelSmall,
                                                    color = MaterialTheme.colorScheme.onSurfaceVariant)
                                            }
                                        }
                                        Icon(Icons.Rounded.MyLocation, null,
                                            tint     = MaterialTheme.colorScheme.primary,
                                            modifier = Modifier.size(18.dp))
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }

        // ── DELETE SHEET ──────────────────────────────────────────────────────
        if (showDeleteSheet && poleToDelete != null) {
            val pole = poleToDelete!!
            val statusColor = when (pole.status) {
                "working" -> ColorGreen
                "burning" -> ColorAmber
                "fused"   -> ColorRed
                else      -> MaterialTheme.colorScheme.onSurfaceVariant
            }
            ModalBottomSheet(
                onDismissRequest = { showDeleteSheet = false; poleToDelete = null },
                containerColor   = MaterialTheme.colorScheme.surface,
                tonalElevation   = 8.dp,
                shape            = RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp)
            ) {
                Column(modifier = Modifier.fillMaxWidth()
                    .padding(horizontal = 24.dp).padding(bottom = 32.dp),
                    verticalArrangement = Arrangement.spacedBy(16.dp)) {

                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Surface(shape = RoundedCornerShape(10.dp),
                            color    = ColorRed.copy(alpha = 0.15f),
                            modifier = Modifier.size(48.dp)) {
                            Box(contentAlignment = Alignment.Center) {
                                Icon(Icons.Rounded.DeleteForever, null,
                                    tint     = ColorRed,
                                    modifier = Modifier.size(24.dp))
                            }
                        }
                        Spacer(Modifier.width(14.dp))
                        Column {
                            Text("Delete Pole",
                                style      = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold,
                                color      = MaterialTheme.colorScheme.onSurface)
                            Text("This action cannot be undone",
                                style = MaterialTheme.typography.labelSmall,
                                color = ColorRed.copy(alpha = 0.80f))
                        }
                    }

                    HorizontalDivider(color = MaterialTheme.colorScheme.outline)

                    Surface(shape = RoundedCornerShape(12.dp),
                        color = MaterialTheme.colorScheme.surfaceVariant) {
                        Row(modifier = Modifier.fillMaxWidth().padding(16.dp),
                            verticalAlignment     = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween) {
                            Column {
                                Text(pole.id,
                                    style      = MaterialTheme.typography.titleSmall,
                                    fontWeight = FontWeight.SemiBold,
                                    color      = MaterialTheme.colorScheme.onSurface)
                                Text("%.5f, %.5f".format(pole.lat, pole.lon),
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant)
                            }
                            Surface(shape = RoundedCornerShape(6.dp),
                                color = statusColor.copy(alpha = 0.15f)) {
                                Text(pole.status.replaceFirstChar { it.uppercase() },
                                    modifier   = Modifier.padding(horizontal = 10.dp, vertical = 4.dp),
                                    style      = MaterialTheme.typography.labelMedium,
                                    color      = statusColor,
                                    fontWeight = FontWeight.SemiBold)
                            }
                        }
                    }

                    Row(modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                        OutlinedButton(
                            onClick  = { showDeleteSheet = false; poleToDelete = null },
                            modifier = Modifier.weight(1f),
                            shape    = RoundedCornerShape(12.dp),
                            colors   = ButtonDefaults.outlinedButtonColors(
                                contentColor = MaterialTheme.colorScheme.onSurfaceVariant)
                        ) { Text("Cancel") }

                        Button(
                            onClick = {
                                FirebaseRepository.deletePole(pole.id)
                                showDeleteSheet = false
                                poleToDelete    = null
                            },
                            modifier = Modifier.weight(1f),
                            shape    = RoundedCornerShape(12.dp),
                            colors   = ButtonDefaults.buttonColors(
                                containerColor = ColorRed.copy(alpha = 0.18f),
                                contentColor   = ColorRed)
                        ) {
                            Icon(Icons.Rounded.DeleteForever, null, modifier = Modifier.size(18.dp))
                            Spacer(Modifier.width(6.dp))
                            Text("Delete", fontWeight = FontWeight.SemiBold)
                        }
                    }
                }
            }
        }

        // ── ADD POLE SHEET ────────────────────────────────────────────────────
        if (showAddPoleSheet && pendingGeoPoint != null) {
            val gp = pendingGeoPoint!!
            ModalBottomSheet(
                onDismissRequest = { showAddPoleSheet = false; pendingGeoPoint = null },
                containerColor   = MaterialTheme.colorScheme.surface,
                tonalElevation   = 8.dp,
                shape            = RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp)
            ) {
                Column(modifier = Modifier.fillMaxWidth()
                    .padding(horizontal = 24.dp).padding(bottom = 32.dp),
                    verticalArrangement = Arrangement.spacedBy(16.dp)) {

                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Surface(shape = RoundedCornerShape(10.dp),
                            color    = ColorGreen.copy(alpha = 0.15f),
                            modifier = Modifier.size(48.dp)) {
                            Box(contentAlignment = Alignment.Center) {
                                Icon(Icons.Rounded.AddLocationAlt, null,
                                    tint     = ColorGreen,
                                    modifier = Modifier.size(24.dp))
                            }
                        }
                        Spacer(Modifier.width(14.dp))
                        Column {
                            Text("Add New Pole",
                                style      = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold,
                                color      = MaterialTheme.colorScheme.onSurface)
                            Text("%.5f, %.5f".format(gp.latitude, gp.longitude),
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                    }

                    HorizontalDivider(color = MaterialTheme.colorScheme.outline)

                    Text("Initial Status",
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant)

                    Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                        listOf(
                            Triple("working", "Working", ColorGreen),
                            Triple("fused",   "Fused",   ColorRed),
                            Triple("burning", "Burning", ColorAmber)
                        ).forEach { (value, label, color) ->
                            val selected = newPoleStatusPick == value
                            Surface(
                                shape    = RoundedCornerShape(10.dp),
                                color    = if (selected) color.copy(alpha = 0.20f)
                                else MaterialTheme.colorScheme.surfaceVariant,
                                modifier = Modifier.weight(1f).clickable { newPoleStatusPick = value }
                            ) {
                                Box(contentAlignment = Alignment.Center,
                                    modifier = Modifier.padding(vertical = 10.dp)) {
                                    Text(label,
                                        style      = MaterialTheme.typography.labelMedium,
                                        fontWeight = if (selected) FontWeight.Bold else FontWeight.Normal,
                                        color      = if (selected) color
                                        else MaterialTheme.colorScheme.onSurfaceVariant)
                                }
                            }
                        }
                    }

                    Row(modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                        OutlinedButton(
                            onClick  = { showAddPoleSheet = false; pendingGeoPoint = null },
                            modifier = Modifier.weight(1f),
                            shape    = RoundedCornerShape(12.dp),
                            colors   = ButtonDefaults.outlinedButtonColors(
                                contentColor = MaterialTheme.colorScheme.onSurfaceVariant)
                        ) { Text("Cancel") }

                        Button(
                            onClick = {
                                val newId = "POLE_${UUID.randomUUID().toString().take(8).uppercase()}"
                                FirebaseRepository.addPole(
                                    PoleDto(id     = newId,
                                        lat    = gp.latitude,
                                        lon    = gp.longitude,
                                        status = newPoleStatusPick)
                                )
                                showAddPoleSheet = false
                                pendingGeoPoint  = null
                            },
                            modifier = Modifier.weight(1f),
                            shape    = RoundedCornerShape(12.dp),
                            colors   = ButtonDefaults.buttonColors(
                                containerColor = ColorGreen.copy(alpha = 0.18f),
                                contentColor   = ColorGreen)
                        ) {
                            Icon(Icons.Rounded.AddLocationAlt, null, modifier = Modifier.size(18.dp))
                            Spacer(Modifier.width(6.dp))
                            Text("Add Pole", fontWeight = FontWeight.SemiBold)
                        }
                    }
                }
            }
        }

        // ══════════════════════════════════════════════════════════════════════
        // ── PROFILE SHEET  ·  GrameenLight Dark Theme ─────────────────────────
        // ══════════════════════════════════════════════════════════════════════
        if (showProfileSheet) {

            // Role-based accent colour (same logic as login page)
            val roleAccent = when (user?.role) {
                "worker" -> GlSapphire
                "admin"  -> GlAmberAccent
                else     -> GlEmerald           // citizen (default)
            }
            val roleEmoji = when (user?.role) {
                "worker" -> "🔧"
                "admin"  -> "🛡️"
                else     -> "👤"
            }

            ModalBottomSheet(
                onDismissRequest = { showProfileSheet = false },
                // ── GL deep-space background
                containerColor   = GlBgDeep,
                tonalElevation   = 0.dp,
                shape            = RoundedCornerShape(topStart = 28.dp, topEnd = 28.dp)
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 22.dp)
                        .padding(bottom = 32.dp)
                ) {

                    // ── Header: avatar + name + role ──────────────────────────
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(20.dp))
                            .background(
                                Brush.linearGradient(
                                    listOf(
                                        roleAccent.copy(alpha = 0.15f),
                                        GlBgCard.copy(alpha = 0.80f)
                                    )
                                )
                            )
                            .border(1.dp, roleAccent.copy(alpha = 0.25f), RoundedCornerShape(20.dp))
                            .padding(18.dp)
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            // Glowing avatar circle
                            Box(
                                modifier = Modifier
                                    .size(64.dp)
                                    .clip(CircleShape)
                                    .background(roleAccent.copy(alpha = 0.15f))
                                    .border(
                                        width = 2.dp,
                                        brush = Brush.linearGradient(
                                            listOf(roleAccent, roleAccent.copy(alpha = 0.3f))
                                        ),
                                        shape = CircleShape
                                    ),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(roleEmoji, fontSize = 28.sp)
                            }

                            Spacer(Modifier.width(16.dp))

                            Column {
                                Text(
                                    user?.name ?: "Loading…",
                                    fontSize   = 18.sp,
                                    fontWeight = FontWeight.Bold,
                                    color      = GlTextPrimary
                                )
                                Spacer(Modifier.height(4.dp))
                                // Role pill
                                Box(
                                    modifier = Modifier
                                        .clip(RoundedCornerShape(20.dp))
                                        .background(roleAccent.copy(alpha = 0.18f))
                                        .border(0.5.dp, roleAccent.copy(alpha = 0.4f), RoundedCornerShape(20.dp))
                                        .padding(horizontal = 10.dp, vertical = 3.dp)
                                ) {
                                    Text(
                                        user?.role?.replaceFirstChar { it.uppercase() } ?: "",
                                        fontSize   = 11.sp,
                                        fontWeight = FontWeight.SemiBold,
                                        color      = roleAccent,
                                        letterSpacing = 0.5.sp
                                    )
                                }
                            }
                        }
                    }

                    Spacer(Modifier.height(16.dp))

                    // ── Section label ─────────────────────────────────────────
                    Text(
                        "ACCOUNT INFO",
                        fontSize      = 10.sp,
                        fontWeight    = FontWeight.SemiBold,
                        color         = GlTextSub,
                        letterSpacing = 2.sp,
                        modifier      = Modifier.padding(bottom = 8.dp, start = 2.dp)
                    )

                    // ── Profile rows / edit fields ────────────────────────────
                    if (user == null) {
                        Box(
                            modifier         = Modifier.fillMaxWidth().height(100.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            CircularProgressIndicator(color = roleAccent, strokeWidth = 2.dp)
                        }
                    } else {
                        val u = user!!

                        // Always-visible rows
                        GlProfileRow(Icons.Rounded.Person,   "Name",  u.name,  roleAccent)
                        GlProfileRow(Icons.Rounded.Email,    "Email", u.email, roleAccent)
                        GlProfileRow(Icons.Rounded.Star,     "Role",  u.role.replaceFirstChar { it.uppercase() }, roleAccent)

                        Spacer(Modifier.height(4.dp))

                        // Edit mode: text fields; view mode: rows
                        if (editMode) {
                            OutlinedTextField(
                                value         = phoneInput,
                                onValueChange = { phoneInput = it },
                                label         = { Text("Phone", color = GlTextSub) },
                                modifier      = Modifier.fillMaxWidth(),
                                shape         = RoundedCornerShape(14.dp),
                                colors        = OutlinedTextFieldDefaults.colors(
                                    focusedBorderColor   = roleAccent,
                                    unfocusedBorderColor = GlGlassBorder,
                                    focusedTextColor     = GlTextPrimary,
                                    unfocusedTextColor   = GlTextPrimary,
                                    cursorColor          = roleAccent,
                                    focusedLabelColor    = roleAccent,
                                    unfocusedContainerColor = GlGlass,
                                    focusedContainerColor   = GlGlass
                                )
                            )
                            Spacer(Modifier.height(10.dp))
                            OutlinedTextField(
                                value         = addressInput,
                                onValueChange = { addressInput = it },
                                label         = { Text("Address", color = GlTextSub) },
                                modifier      = Modifier.fillMaxWidth(),
                                shape         = RoundedCornerShape(14.dp),
                                colors        = OutlinedTextFieldDefaults.colors(
                                    focusedBorderColor   = roleAccent,
                                    unfocusedBorderColor = GlGlassBorder,
                                    focusedTextColor     = GlTextPrimary,
                                    unfocusedTextColor   = GlTextPrimary,
                                    cursorColor          = roleAccent,
                                    focusedLabelColor    = roleAccent,
                                    unfocusedContainerColor = GlGlass,
                                    focusedContainerColor   = GlGlass
                                )
                            )
                        } else {
                            GlProfileRow(Icons.Rounded.Phone,     "Phone",   u.phone,   roleAccent)
                            GlProfileRow(Icons.Rounded.LocationOn,"Address", u.address, roleAccent)
                        }

                        GlProfileRow(Icons.Rounded.Flag,  "Reports", u.reportCount.toString(), roleAccent)
                        GlProfileRow(Icons.Rounded.Star,  "Badge",   u.badge, roleAccent)
                    }

                    Spacer(Modifier.height(20.dp))

                    // ── Button row ────────────────────────────────────────────
                    Row(
                        modifier              = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        if (editMode) {
                            // Save
                            Box(
                                modifier = Modifier
                                    .weight(1f)
                                    .clip(RoundedCornerShape(14.dp))
                                    .background(roleAccent.copy(alpha = 0.18f))
                                    .border(1.dp, roleAccent.copy(alpha = 0.5f), RoundedCornerShape(14.dp))
                                    .clickable {
                                        val uid = FirebaseAuth.getInstance().currentUser?.uid ?: return@clickable
                                        try {
                                            FirebaseFirestore.getInstance()
                                                .collection("users").document(uid)
                                                .update(mapOf("phone" to phoneInput, "address" to addressInput))
                                            editMode = false
                                            Toast.makeText(context, "Profile updated", Toast.LENGTH_SHORT).show()
                                        } catch (e: Exception) {
                                            Toast.makeText(context, "Update failed", Toast.LENGTH_SHORT).show()
                                        }
                                    }
                                    .padding(vertical = 13.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                Text("Save", fontWeight = FontWeight.Bold, color = roleAccent, fontSize = 14.sp)
                            }
                        } else {
                            // Edit
                            Box(
                                modifier = Modifier
                                    .weight(1f)
                                    .clip(RoundedCornerShape(14.dp))
                                    .background(GlGlass)
                                    .border(1.dp, GlGlassBorder, RoundedCornerShape(14.dp))
                                    .clickable {
                                        phoneInput   = user?.phone   ?: ""
                                        addressInput = user?.address ?: ""
                                        editMode     = true
                                    }
                                    .padding(vertical = 13.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                Text("Edit", fontWeight = FontWeight.SemiBold, color = GlTextPrimary, fontSize = 14.sp)
                            }
                        }

                        // Close
                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .clip(RoundedCornerShape(14.dp))
                                .background(GlGlass)
                                .border(1.dp, GlGlassBorder, RoundedCornerShape(14.dp))
                                .clickable { showProfileSheet = false }
                                .padding(vertical = 13.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Text("Close", fontWeight = FontWeight.SemiBold, color = GlTextPrimary, fontSize = 14.sp)
                        }

                        // Logout
                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .clip(RoundedCornerShape(14.dp))
                                .background(ColorRed.copy(alpha = 0.15f))
                                .border(1.dp, ColorRed.copy(alpha = 0.4f), RoundedCornerShape(14.dp))
                                .clickable {
                                    FirebaseAuth.getInstance().signOut()
                                    showProfileSheet = false
                                    navController.navigate("login") { popUpTo(0) }
                                }
                                .padding(vertical = 13.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Row(
                                verticalAlignment     = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(4.dp)
                            ) {
                                Icon(Icons.AutoMirrored.Rounded.Logout, null,
                                    tint     = ColorRed,
                                    modifier = Modifier.size(16.dp))
                                Text("Logout", fontWeight = FontWeight.Bold, color = ColorRed, fontSize = 14.sp)
                            }
                        }
                    }
                }
            }
        }

        // ── POLE DETAIL SHEET ─────────────────────────────────────────────────
        if (showPoleSheet && selectedPole != null) {
            val pole = selectedPole!!
            val statusColor = when (pole.status) {
                "working" -> ColorGreen
                "burning" -> ColorAmber
                "fused"   -> ColorRed
                else      -> MaterialTheme.colorScheme.onSurfaceVariant
            }
            ModalBottomSheet(
                onDismissRequest = { showPoleSheet = false },
                containerColor   = MaterialTheme.colorScheme.surface,
                tonalElevation   = 8.dp,
                shape            = RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp)
            ) {
                Column(modifier = Modifier.fillMaxWidth()
                    .padding(horizontal = 24.dp).padding(bottom = 32.dp),
                    verticalArrangement = Arrangement.spacedBy(16.dp)) {

                    Column {
                        Text("Pole ${pole.id}",
                            style      = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.Bold,
                            color      = MaterialTheme.colorScheme.onSurface)
                        Spacer(Modifier.height(6.dp))
                        Surface(shape = RoundedCornerShape(6.dp),
                            color = statusColor.copy(alpha = 0.15f)) {
                            Text(pole.status.replaceFirstChar { it.uppercase() },
                                modifier   = Modifier.padding(horizontal = 10.dp, vertical = 4.dp),
                                style      = MaterialTheme.typography.labelMedium,
                                color      = statusColor,
                                fontWeight = FontWeight.SemiBold)
                        }
                    }

                    HorizontalDivider(color = MaterialTheme.colorScheme.outline)

                    Row(modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                        Button(
                            onClick = {
                                FirebaseRepository.updatePoleStatus(pole.id, "fused")
                                showPoleSheet = false
                            },
                            modifier = Modifier.weight(1f),
                            shape    = RoundedCornerShape(12.dp),
                            colors   = ButtonDefaults.buttonColors(
                                containerColor = ColorRed.copy(alpha = 0.18f),
                                contentColor   = ColorRed)
                        ) {
                            Icon(Icons.Rounded.ReportProblem, null, modifier = Modifier.size(18.dp))
                            Spacer(Modifier.width(6.dp))
                            Text("Report Issue", fontWeight = FontWeight.SemiBold)
                        }
                        Button(
                            onClick = {
                                FirebaseRepository.updatePoleStatus(pole.id, "working")
                                showPoleSheet = false
                            },
                            modifier = Modifier.weight(1f),
                            shape    = RoundedCornerShape(12.dp),
                            colors   = ButtonDefaults.buttonColors(
                                containerColor = ColorGreen.copy(alpha = 0.18f),
                                contentColor   = ColorGreen)
                        ) {
                            Icon(Icons.Rounded.CheckCircle, null, modifier = Modifier.size(18.dp))
                            Spacer(Modifier.width(6.dp))
                            Text("Mark Fixed", fontWeight = FontWeight.SemiBold)
                        }
                    }
                }
            }
        }
    }
}

// ─── Helpers ──────────────────────────────────────────────────────────────────

@Composable
private fun MetricCard(
    label    : String,
    value    : String,
    icon     : androidx.compose.ui.graphics.vector.ImageVector,
    color    : Color,
    modifier : Modifier = Modifier
) {
    Surface(modifier = modifier, shape = RoundedCornerShape(16.dp),
        color = color.copy(alpha = 0.10f), tonalElevation = 0.dp) {
        Column(modifier = Modifier.padding(14.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Box(modifier = Modifier
                .size(36.dp)
                .background(color.copy(alpha = 0.15f), CircleShape),
                contentAlignment = Alignment.Center) {
                Icon(icon, null, tint = color, modifier = Modifier.size(20.dp))
            }
            Text(value, style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.Bold, color = color)
            Text(label, style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}

// ── GL-themed profile row (replaces old ProfileRow in the profile sheet) ──────
@Composable
private fun GlProfileRow(
    icon   : androidx.compose.ui.graphics.vector.ImageVector,
    label  : String,
    value  : String,
    accent : Color
) {
    Row(
        modifier          = Modifier
            .fillMaxWidth()
            .padding(vertical = 5.dp)
            .clip(RoundedCornerShape(12.dp))
            .background(GlGlass)
            .border(0.5.dp, GlGlassBorder, RoundedCornerShape(12.dp))
            .padding(horizontal = 14.dp, vertical = 11.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        // Icon bubble
        Box(
            modifier = Modifier
                .size(36.dp)
                .clip(RoundedCornerShape(10.dp))
                .background(accent.copy(alpha = 0.14f))
                .border(0.5.dp, accent.copy(alpha = 0.35f), RoundedCornerShape(10.dp)),
            contentAlignment = Alignment.Center
        ) {
            Icon(icon, null, tint = accent, modifier = Modifier.size(18.dp))
        }
        Spacer(Modifier.width(14.dp))
        Column {
            Text(label,
                fontSize      = 10.sp,
                letterSpacing = 0.8.sp,
                color         = GlTextSub,
                fontWeight    = FontWeight.Medium)
            Text(value,
                fontSize   = 14.sp,
                fontWeight = FontWeight.SemiBold,
                color      = GlTextPrimary)
        }
    }
}

// ── Original ProfileRow kept for any non-profile-sheet usage ──────────────────
@Composable
private fun ProfileRow(
    icon  : androidx.compose.ui.graphics.vector.ImageVector,
    label : String,
    value : String
) {
    Row(modifier = Modifier.fillMaxWidth().padding(vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically) {
        Surface(shape = RoundedCornerShape(8.dp),
            color    = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.5f),
            modifier = Modifier.size(36.dp)) {
            Box(contentAlignment = Alignment.Center) {
                Icon(icon, null,
                    tint     = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(18.dp))
            }
        }
        Spacer(Modifier.width(16.dp))
        Column {
            Text(label,
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant)
            Text(value,
                style      = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.Medium,
                color      = MaterialTheme.colorScheme.onSurface)
        }
    }
    HorizontalDivider(
        color     = MaterialTheme.colorScheme.outline.copy(alpha = 0.4f),
        thickness = 0.5.dp)
}

@Composable
private fun LegendDot(color: Color, label: String) {
    Row(verticalAlignment     = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(6.dp)) {
        Box(modifier = Modifier.size(8.dp).background(color, CircleShape))
        Text(label,
            style    = MaterialTheme.typography.labelSmall,
            color    = MaterialTheme.colorScheme.onSurface,
            fontSize = 11.sp)
    }
}
