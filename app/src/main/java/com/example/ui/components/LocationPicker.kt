package com.example.ui.components

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.location.Geocoder
import android.location.Location
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.MyLocation
import androidx.compose.material.icons.filled.Remove
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableDoubleStateOf
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import com.example.ui.theme.CivicAmber
import com.example.ui.theme.CivicDeepBlue
import com.example.ui.theme.CivicGreen
import com.example.ui.theme.CivicRed
import com.example.ui.theme.CivicTeal
import com.google.android.gms.location.LocationServices
import com.google.android.gms.location.Priority
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.util.Locale

data class CivicLandmark(
    val name: String,
    val lat: Double,
    val lng: Double,
    val ward: String
)

val MUNICIPAL_LANDMARKS = listOf(
    CivicLandmark("Ward 14 Main Junction", 13.0827, 80.2707, "Ward 14 - Central"),
    CivicLandmark("Gandhi Road Bus Terminal", 13.0845, 80.2730, "Ward 14 - North"),
    CivicLandmark("Community Health Centre", 13.0810, 80.2685, "Ward 14 - South"),
    CivicLandmark("Metro Water Pumping Station", 13.0862, 80.2690, "Ward 14 - West"),
    CivicLandmark("Model School Cross", 13.0838, 80.2755, "Ward 14 - East")
)

/**
 * Interactive LocationPicker utilizing play-services-location
 * for high-accuracy GPS geotagging of civic complaints.
 */
@Composable
fun LocationPicker(
    initialLat: Double,
    initialLng: Double,
    initialAddress: String,
    onLocationConfirmed: (lat: Double, lng: Double, address: String) -> Unit,
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()

    var currentLat by remember { mutableDoubleStateOf(if (initialLat != 0.0) initialLat else 13.0827) }
    var currentLng by remember { mutableDoubleStateOf(if (initialLng != 0.0) initialLng else 80.2707) }
    var addressText by remember { mutableStateOf(initialAddress.ifBlank { "Ward 14 Main Road, Chennai" }) }
    var accuracyMeters by remember { mutableFloatStateOf(8.5f) }
    var isLocating by remember { mutableStateOf(false) }
    var zoomLevel by remember { mutableFloatStateOf(16f) }

    // Offset in map coordinates for pan gesture
    var panOffsetX by remember { mutableFloatStateOf(0f) }
    var panOffsetY by remember { mutableFloatStateOf(0f) }

    val fusedLocationClient = remember {
        LocationServices.getFusedLocationProviderClient(context)
    }

    fun reverseGeocode(lat: Double, lng: Double) {
        scope.launch(Dispatchers.IO) {
            val resolved = try {
                val geocoder = Geocoder(context, Locale.getDefault())
                val results = geocoder.getFromLocation(lat, lng, 1)
                results?.firstOrNull()?.getAddressLine(0) ?: "Lat: ${"%.5f".format(lat)}, Lng: ${"%.5f".format(lng)}"
            } catch (e: Exception) {
                "Ward 14, Near Coordinates (${"%.4f".format(lat)}, ${"%.4f".format(lng)})"
            }
            withContext(Dispatchers.Main) {
                addressText = resolved
            }
        }
    }

    fun requestGpsLocation() {
        val hasFine = ContextCompat.checkSelfPermission(
            context,
            Manifest.permission.ACCESS_FINE_LOCATION
        ) == PackageManager.PERMISSION_GRANTED

        val hasCoarse = ContextCompat.checkSelfPermission(
            context,
            Manifest.permission.ACCESS_COARSE_LOCATION
        ) == PackageManager.PERMISSION_GRANTED

        if (!hasFine && !hasCoarse) {
            return
        }

        isLocating = true
        try {
            fusedLocationClient.getCurrentLocation(Priority.PRIORITY_HIGH_ACCURACY, null)
                .addOnSuccessListener { loc: Location? ->
                    isLocating = false
                    if (loc != null) {
                        currentLat = loc.latitude
                        currentLng = loc.longitude
                        accuracyMeters = loc.accuracy
                        panOffsetX = 0f
                        panOffsetY = 0f
                        reverseGeocode(loc.latitude, loc.longitude)
                    }
                }
                .addOnFailureListener {
                    isLocating = false
                }
        } catch (e: SecurityException) {
            isLocating = false
        }
    }

    val permissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestMultiplePermissions()
    ) { permissions ->
        val granted = permissions[Manifest.permission.ACCESS_FINE_LOCATION] == true ||
                permissions[Manifest.permission.ACCESS_COARSE_LOCATION] == true
        if (granted) {
            requestGpsLocation()
        }
    }

    LaunchedEffect(Unit) {
        val hasPermission = ContextCompat.checkSelfPermission(
            context,
            Manifest.permission.ACCESS_FINE_LOCATION
        ) == PackageManager.PERMISSION_GRANTED
        if (!hasPermission) {
            permissionLauncher.launch(
                arrayOf(
                    Manifest.permission.ACCESS_FINE_LOCATION,
                    Manifest.permission.ACCESS_COARSE_LOCATION
                )
            )
        } else {
            requestGpsLocation()
        }
    }

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(Color.Black.copy(alpha = 0.75f)),
        contentAlignment = Alignment.Center
    ) {
        Card(
            modifier = Modifier
                .fillMaxSize()
                .padding(12.dp),
            shape = RoundedCornerShape(20.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
        ) {
            Column(
                modifier = Modifier.fillMaxSize()
            ) {
                // Header Bar
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(CivicDeepBlue)
                        .padding(horizontal = 16.dp, vertical = 14.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = Icons.Default.LocationOn,
                                contentDescription = null,
                                tint = CivicTeal,
                                modifier = Modifier.size(20.dp)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = "Tag Defect Location",
                                fontWeight = FontWeight.Bold,
                                color = Color.White,
                                fontSize = 16.sp
                            )
                        }
                        Text(
                            text = "Interactive GPS Tagging & Municipal Ward Mapping",
                            color = Color.White.copy(alpha = 0.8f),
                            fontSize = 11.sp
                        )
                    }

                    IconButton(
                        onClick = onDismiss,
                        modifier = Modifier
                            .size(36.dp)
                            .clip(CircleShape)
                            .background(Color.White.copy(alpha = 0.2f))
                    ) {
                        Icon(
                            imageVector = Icons.Default.Close,
                            contentDescription = "Close",
                            tint = Color.White,
                            modifier = Modifier.size(18.dp)
                        )
                    }
                }

                // Interactive Map Canvas
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f)
                        .background(Color(0xFFE2E8F0))
                ) {
                    // Stylized Civic Road & Grid Canvas
                    Canvas(
                        modifier = Modifier
                            .fillMaxSize()
                            .pointerInput(Unit) {
                                detectDragGestures { change, dragAmount ->
                                    change.consume()
                                    panOffsetX += dragAmount.x
                                    panOffsetY += dragAmount.y

                                    // 1 pixel roughly corresponds to geographic delta scaled by zoom
                                    val deltaLat = -(dragAmount.y / (1000f * zoomLevel))
                                    val deltaLng = (dragAmount.x / (1000f * zoomLevel))
                                    currentLat += deltaLat
                                    currentLng += deltaLng
                                }
                            }
                            .pointerInput(Unit) {
                                detectTapGestures { tapOffset ->
                                    // Move pin towards tapped position
                                    val centerX = size.width / 2f
                                    val centerY = size.height / 2f
                                    val diffX = tapOffset.x - centerX
                                    val diffY = tapOffset.y - centerY

                                    currentLat -= (diffY / (1000f * zoomLevel))
                                    currentLng += (diffX / (1000f * zoomLevel))
                                    reverseGeocode(currentLat, currentLng)
                                }
                            }
                    ) {
                        val width = size.width
                        val height = size.height

                        // Background ground tint
                        drawRect(Color(0xFFE8EEF5))

                        // Grid lines (Blocks / Streets)
                        val gridSpacing = 40.dp.toPx()
                        var x = (panOffsetX % gridSpacing)
                        while (x < width) {
                            drawLine(
                                color = Color(0xFFCBD5E1),
                                start = Offset(x, 0f),
                                end = Offset(x, height),
                                strokeWidth = 1.5f
                            )
                            x += gridSpacing
                        }

                        var y = (panOffsetY % gridSpacing)
                        while (y < height) {
                            drawLine(
                                color = Color(0xFFCBD5E1),
                                start = Offset(0f, y),
                                end = Offset(width, y),
                                strokeWidth = 1.5f
                            )
                            y += gridSpacing
                        }

                        // Simulated Major Arterial Roads
                        val roadPath1 = Path().apply {
                            moveTo(0f, height * 0.45f + (panOffsetY * 0.5f))
                            lineTo(width, height * 0.45f + (panOffsetY * 0.5f))
                        }
                        drawPath(
                            path = roadPath1,
                            color = Color(0xFFFED7AA),
                            style = Stroke(width = 24.dp.toPx(), cap = StrokeCap.Round)
                        )

                        val roadPath2 = Path().apply {
                            moveTo(width * 0.5f + (panOffsetX * 0.5f), 0f)
                            lineTo(width * 0.5f + (panOffsetX * 0.5f), height)
                        }
                        drawPath(
                            path = roadPath2,
                            color = Color(0xFFE0E7FF),
                            style = Stroke(width = 20.dp.toPx(), cap = StrokeCap.Round)
                        )

                        // GPS Accuracy Circle
                        val center = Offset(width / 2f, height / 2f)
                        val accuracyRadius = (accuracyMeters * 2.5f).coerceIn(25f, 90f)
                        drawCircle(
                            color = CivicTeal.copy(alpha = 0.2f),
                            radius = accuracyRadius,
                            center = center
                        )
                        drawCircle(
                            color = CivicTeal,
                            radius = accuracyRadius,
                            center = center,
                            style = Stroke(width = 1.5.dp.toPx())
                        )
                    }

                    // Center Defect Pin Icon
                    Column(
                        modifier = Modifier.align(Alignment.Center),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Icon(
                            imageVector = Icons.Default.LocationOn,
                            contentDescription = "Pin Location",
                            tint = CivicRed,
                            modifier = Modifier
                                .size(44.dp)
                                .testTag("map_center_pin")
                        )
                        Box(
                            modifier = Modifier
                                .size(8.dp)
                                .clip(CircleShape)
                                .background(Color.Black.copy(alpha = 0.5f))
                        )
                    }

                    // Floating Map Controls: Zoom in/out, Re-center GPS
                    Column(
                        modifier = Modifier
                            .align(Alignment.TopEnd)
                            .padding(12.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        IconButton(
                            onClick = { zoomLevel = (zoomLevel + 2f).coerceAtMost(25f) },
                            modifier = Modifier
                                .size(40.dp)
                                .clip(CircleShape)
                                .background(Color.White)
                                .border(1.dp, Color(0xFFCBD5E1), CircleShape)
                        ) {
                            Icon(imageVector = Icons.Default.Add, contentDescription = "Zoom In", tint = CivicDeepBlue)
                        }

                        IconButton(
                            onClick = { zoomLevel = (zoomLevel - 2f).coerceAtLeast(8f) },
                            modifier = Modifier
                                .size(40.dp)
                                .clip(CircleShape)
                                .background(Color.White)
                                .border(1.dp, Color(0xFFCBD5E1), CircleShape)
                        ) {
                            Icon(imageVector = Icons.Default.Remove, contentDescription = "Zoom Out", tint = CivicDeepBlue)
                        }

                        IconButton(
                            onClick = { requestGpsLocation() },
                            modifier = Modifier
                                .size(40.dp)
                                .clip(CircleShape)
                                .background(CivicTeal)
                                .border(1.dp, CivicDeepBlue, CircleShape)
                                .testTag("recenter_gps_button")
                        ) {
                            if (isLocating) {
                                CircularProgressIndicator(
                                    modifier = Modifier.size(18.dp),
                                    strokeWidth = 2.dp,
                                    color = CivicDeepBlue
                                )
                            } else {
                                Icon(
                                    imageVector = Icons.Default.MyLocation,
                                    contentDescription = "Current GPS",
                                    tint = CivicDeepBlue,
                                    modifier = Modifier.size(20.dp)
                                )
                            }
                        }
                    }

                    // Coordinate Badge Overlay
                    Surface(
                        modifier = Modifier
                            .align(Alignment.BottomStart)
                            .padding(12.dp),
                        shape = RoundedCornerShape(8.dp),
                        color = Color.Black.copy(alpha = 0.7f)
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "GPS: ${"%.5f".format(currentLat)}, ${"%.5f".format(currentLng)} (±${accuracyMeters.toInt()}m)",
                                color = Color.White,
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Medium
                            )
                        }
                    }
                }

                // Municipal Landmark Shortcuts
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 14.dp, vertical = 8.dp)
                ) {
                    Text(
                        text = "Quick Municipal Ward Points:",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    LazyRow(
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        items(MUNICIPAL_LANDMARKS) { lm ->
                            FilterChip(
                                selected = (currentLat == lm.lat && currentLng == lm.lng),
                                onClick = {
                                    currentLat = lm.lat
                                    currentLng = lm.lng
                                    panOffsetX = 0f
                                    panOffsetY = 0f
                                    addressText = "${lm.name}, ${lm.ward}"
                                },
                                label = { Text(lm.name, fontSize = 11.sp) }
                            )
                        }
                    }
                }

                // Address & Confirmation Bottom Sheet
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 14.dp, vertical = 8.dp)
                ) {
                    OutlinedTextField(
                        value = addressText,
                        onValueChange = { addressText = it },
                        label = { Text("Confirmed Street Address / Landmark") },
                        leadingIcon = {
                            Icon(imageVector = Icons.Default.LocationOn, contentDescription = null, tint = CivicTeal)
                        },
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("location_picker_address_input"),
                        shape = RoundedCornerShape(10.dp)
                    )

                    Spacer(modifier = Modifier.height(10.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Button(
                            onClick = onDismiss,
                            modifier = Modifier
                                .weight(1f)
                                .height(48.dp),
                            shape = RoundedCornerShape(10.dp),
                            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFE2E8F0))
                        ) {
                            Text("Cancel", color = Color(0xFF334155), fontWeight = FontWeight.SemiBold)
                        }

                        Button(
                            onClick = {
                                onLocationConfirmed(currentLat, currentLng, addressText)
                            },
                            modifier = Modifier
                                .weight(1.5f)
                                .height(48.dp)
                                .testTag("confirm_location_pin_button"),
                            shape = RoundedCornerShape(10.dp),
                            colors = ButtonDefaults.buttonColors(containerColor = CivicDeepBlue)
                        ) {
                            Icon(imageVector = Icons.Default.Check, contentDescription = null, tint = Color.White)
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("Confirm Pin", color = Color.White, fontWeight = FontWeight.Bold)
                        }
                    }
                    Spacer(modifier = Modifier.height(6.dp))
                }
            }
        }
    }
}
