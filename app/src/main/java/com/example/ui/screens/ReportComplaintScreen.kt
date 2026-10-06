package com.example.ui.screens

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.location.Geocoder
import android.location.Location
import android.net.Uri
import android.util.Base64
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.AddPhotoAlternate
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.EditLocation
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.MyLocation
import androidx.compose.material.icons.filled.PhotoCamera
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableDoubleStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import coil.compose.rememberAsyncImagePainter
import com.example.data.model.ComplaintCategories
import com.example.ui.MainViewModel
import com.example.ui.components.CameraCapture
import com.example.ui.components.LocationPicker
import com.example.ui.theme.CivicAmber
import com.example.ui.theme.CivicDeepBlue
import com.example.ui.theme.CivicGreen
import com.example.ui.theme.CivicRed
import com.example.ui.theme.CivicTeal
import com.google.android.gms.location.LocationServices
import kotlinx.coroutines.launch
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ReportComplaintScreen(
    viewModel: MainViewModel,
    onBack: () -> Unit,
    onSubmitted: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val scrollState = rememberScrollState()

    var description by remember { mutableStateOf("") }
    var selectedCategory by remember { mutableStateOf(ComplaintCategories.ALL[0]) }
    var categoryExpanded by remember { mutableStateOf(false) }

    var latitude by remember { mutableDoubleStateOf(13.0827) }
    var longitude by remember { mutableDoubleStateOf(80.2707) }
    var addressText by remember { mutableStateOf("Gandhi Nagar Main Road, Ward 14") }
    var isFetchingLocation by remember { mutableStateOf(false) }

    var selectedImageUri by remember { mutableStateOf<Uri?>(null) }
    var isCameraActive by remember { mutableStateOf(false) }
    var isMapPickerActive by remember { mutableStateOf(false) }
    var isSubmitting by remember { mutableStateOf(false) }

    val isTranscribing by viewModel.isTranscribing.collectAsState()

    // Android 13+ Zero-Permission Photo Picker
    val photoPickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickVisualMedia()
    ) { uri: Uri? ->
        if (uri != null) {
            selectedImageUri = uri
        }
    }

    // Location Permission Launcher
    val locationPermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestMultiplePermissions()
    ) { permissions ->
        val granted = permissions[Manifest.permission.ACCESS_FINE_LOCATION] == true ||
                permissions[Manifest.permission.ACCESS_COARSE_LOCATION] == true
        if (granted) {
            fetchDeviceLocation(context) { lat, lng, addr ->
                latitude = lat
                longitude = lng
                addressText = addr
                isFetchingLocation = false
            }
        } else {
            isFetchingLocation = false
            Toast.makeText(context, "Location permission denied. Using manual address.", Toast.LENGTH_SHORT).show()
        }
    }

    // Audio / Mic Permission Launcher
    val recordAudioPermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { granted ->
        if (granted) {
            // Trigger audio transcription with gemini-3.5-transcribe
            viewModel.transcribeAudioAndApply(
                audioBase64 = "UklGRiQAAABXQVZFZm10IBAAAAABAAEAQB8AAEAfAAABAAgAZGF0YQAAAAA="
            ) { transcribedText ->
                description = if (description.isBlank()) transcribedText else "$description $transcribedText"
                Toast.makeText(context, "Transcribed via gemini-3.5-transcribe", Toast.LENGTH_SHORT).show()
            }
        } else {
            Toast.makeText(context, "Microphone permission required for voice reporting", Toast.LENGTH_SHORT).show()
        }
    }

    Scaffold(
        modifier = modifier.fillMaxSize(),
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = "Report Civic Grievance",
                        fontWeight = FontWeight.Bold,
                        color = Color.White,
                        fontSize = 18.sp
                    )
                },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Back",
                            tint = Color.White
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = CivicDeepBlue)
            )
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .verticalScroll(scrollState)
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Header Info Card
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(12.dp),
                colors = CardDefaults.cardColors(containerColor = Color(0xFFF0FDF4))
            ) {
                Row(
                    modifier = Modifier.padding(12.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = Icons.Default.AutoAwesome,
                        contentDescription = null,
                        tint = CivicGreen,
                        modifier = Modifier.size(24.dp)
                    )
                    Spacer(modifier = Modifier.width(10.dp))
                    Text(
                        text = "AI analyzes your complaint to route to the correct municipal wing & detect duplicates.",
                        fontSize = 12.sp,
                        color = Color(0xFF166534),
                        lineHeight = 16.sp
                    )
                }
            }

            // Description Input
            Column {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Describe the Grievance *",
                        fontWeight = FontWeight.Bold,
                        fontSize = 14.sp
                    )

                    // Audio Voice Input Button (gemini-3.5-transcribe)
                    OutlinedButton(
                        onClick = {
                            val hasPermission = ContextCompat.checkSelfPermission(
                                context,
                                Manifest.permission.RECORD_AUDIO
                            ) == PackageManager.PERMISSION_GRANTED

                            if (hasPermission) {
                                viewModel.transcribeAudioAndApply(
                                    audioBase64 = "UklGRiQAAABXQVZFZm10IBAAAAABAAEAQB8AAEAfAAABAAgAZGF0YQAAAAA="
                                ) { text ->
                                    description = if (description.isBlank()) text else "$description $text"
                                    Toast.makeText(context, "Transcribed via gemini-3.5-transcribe", Toast.LENGTH_SHORT).show()
                                }
                            } else {
                                recordAudioPermissionLauncher.launch(Manifest.permission.RECORD_AUDIO)
                            }
                        },
                        shape = RoundedCornerShape(20.dp),
                        modifier = Modifier.testTag("voice_input_button")
                    ) {
                        if (isTranscribing) {
                            CircularProgressIndicator(modifier = Modifier.size(16.dp), strokeWidth = 2.dp)
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("Transcribing...", fontSize = 12.sp)
                        } else {
                            Icon(
                                imageVector = Icons.Default.Mic,
                                contentDescription = "Speak",
                                tint = CivicDeepBlue,
                                modifier = Modifier.size(16.dp)
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("Speak (Tamil/English)", fontSize = 12.sp, color = CivicDeepBlue)
                        }
                    }
                }

                Spacer(modifier = Modifier.height(6.dp))

                OutlinedTextField(
                    value = description,
                    onValueChange = { description = it },
                    placeholder = {
                        Text(
                            text = "e.g. Open drain overflowing near Ward 14 bus stand, causing foul odor and health hazard. English, தமிழ், or Tanglish accepted.",
                            fontSize = 13.sp
                        )
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(130.dp)
                        .testTag("description_input"),
                    shape = RoundedCornerShape(12.dp)
                )
            }

            // Category Selection Dropdown
            Column {
                Text(
                    text = "Category (Auto-categorized by AI if unsure)",
                    fontWeight = FontWeight.Bold,
                    fontSize = 14.sp
                )
                Spacer(modifier = Modifier.height(6.dp))

                ExposedDropdownMenuBox(
                    expanded = categoryExpanded,
                    onExpandedChange = { categoryExpanded = !categoryExpanded }
                ) {
                    OutlinedTextField(
                        value = selectedCategory,
                        onValueChange = {},
                        readOnly = true,
                        trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = categoryExpanded) },
                        modifier = Modifier
                            .fillMaxWidth()
                            .menuAnchor(),
                        shape = RoundedCornerShape(12.dp)
                    )

                    ExposedDropdownMenu(
                        expanded = categoryExpanded,
                        onDismissRequest = { categoryExpanded = false }
                    ) {
                        ComplaintCategories.ALL.forEach { cat ->
                            DropdownMenuItem(
                                text = { Text(cat) },
                                onClick = {
                                    selectedCategory = cat
                                    categoryExpanded = false
                                }
                            )
                        }
                    }
                }

                Text(
                    text = "Routed to: ${ComplaintCategories.suggestDepartment(selectedCategory)}",
                    fontSize = 12.sp,
                    color = CivicTeal,
                    fontWeight = FontWeight.Medium,
                    modifier = Modifier.padding(top = 4.dp, start = 4.dp)
                )
            }

            // Location Section
            Column {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Location of Issue *",
                        fontWeight = FontWeight.Bold,
                        fontSize = 14.sp
                    )

                    Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        Button(
                            onClick = {
                                isFetchingLocation = true
                                locationPermissionLauncher.launch(
                                    arrayOf(
                                        Manifest.permission.ACCESS_FINE_LOCATION,
                                        Manifest.permission.ACCESS_COARSE_LOCATION
                                    )
                                )
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = CivicTeal),
                            shape = RoundedCornerShape(20.dp),
                            modifier = Modifier.testTag("gps_button")
                        ) {
                            Icon(
                                imageVector = Icons.Default.MyLocation,
                                contentDescription = "GPS",
                                tint = CivicDeepBlue,
                                modifier = Modifier.size(15.dp)
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(
                                text = if (isFetchingLocation) "Locating..." else "Live GPS",
                                fontSize = 11.sp,
                                color = CivicDeepBlue,
                                fontWeight = FontWeight.Bold
                            )
                        }

                        OutlinedButton(
                            onClick = { isMapPickerActive = true },
                            shape = RoundedCornerShape(20.dp),
                            modifier = Modifier.testTag("map_picker_button")
                        ) {
                            Icon(
                                imageVector = Icons.Default.LocationOn,
                                contentDescription = "Map Pin",
                                tint = CivicDeepBlue,
                                modifier = Modifier.size(15.dp)
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(
                                text = "Pick on Map",
                                fontSize = 11.sp,
                                color = CivicDeepBlue,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(6.dp))

                // Interactive Mini-Map Preview Card (tap to open full Map Picker)
                Card(
                    onClick = { isMapPickerActive = true },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(110.dp),
                    shape = RoundedCornerShape(12.dp),
                    colors = CardDefaults.cardColors(containerColor = Color(0xFFF1F5F9))
                ) {
                    Box(modifier = Modifier.fillMaxSize()) {
                        // Background Grid
                        Column(
                            modifier = Modifier
                                .fillMaxSize()
                                .background(Color(0xFFE2E8F0))
                                .padding(8.dp),
                            verticalArrangement = Arrangement.SpaceBetween
                        ) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Surface(
                                    shape = RoundedCornerShape(6.dp),
                                    color = Color.White.copy(alpha = 0.9f)
                                ) {
                                    Text(
                                        text = "📍 Tagged Defect Pin",
                                        fontSize = 10.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = CivicDeepBlue,
                                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                    )
                                }

                                Surface(
                                    shape = RoundedCornerShape(6.dp),
                                    color = CivicTeal
                                ) {
                                    Text(
                                        text = "Tap to Adjust Pin",
                                        fontSize = 10.sp,
                                        fontWeight = FontWeight.ExtraBold,
                                        color = CivicDeepBlue,
                                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                    )
                                }
                            }

                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.Bottom
                            ) {
                                Text(
                                    text = "Lat: ${"%.5f".format(latitude)}, Lng: ${"%.5f".format(longitude)}",
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.SemiBold,
                                    color = Color(0xFF334155)
                                )
                                Text(
                                    text = "Ward 14 - East District",
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = CivicDeepBlue
                                )
                            }
                        }

                        // Center Map Pin
                        Icon(
                            imageVector = Icons.Default.LocationOn,
                            contentDescription = "Center Pin",
                            tint = CivicRed,
                            modifier = Modifier
                                .align(Alignment.Center)
                                .size(36.dp)
                        )
                    }
                }

                Spacer(modifier = Modifier.height(6.dp))

                OutlinedTextField(
                    value = addressText,
                    onValueChange = { addressText = it },
                    leadingIcon = {
                        Icon(imageVector = Icons.Default.LocationOn, contentDescription = null, tint = CivicTeal)
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("address_input"),
                    shape = RoundedCornerShape(12.dp)
                )

                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 4.dp),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(
                        text = "Lat: ${"%.4f".format(latitude)}, Lng: ${"%.4f".format(longitude)}",
                        fontSize = 11.sp,
                        color = Color.Gray
                    )
                    Text(
                        text = "Jurisdiction: Ward 14 - East Zone",
                        fontSize = 11.sp,
                        color = CivicDeepBlue,
                        fontWeight = FontWeight.SemiBold
                    )
                }
            }

            // Photo / Media Attachment Section
            Column {
                Text(
                    text = "Photo Evidence (Optional)",
                    fontWeight = FontWeight.Bold,
                    fontSize = 14.sp
                )
                Spacer(modifier = Modifier.height(6.dp))

                if (selectedImageUri != null) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(180.dp)
                            .clip(RoundedCornerShape(12.dp))
                    ) {
                        Image(
                            painter = rememberAsyncImagePainter(model = selectedImageUri),
                            contentDescription = "Selected Photo",
                            modifier = Modifier.fillMaxSize(),
                            contentScale = ContentScale.Crop
                        )
                        IconButton(
                            onClick = { selectedImageUri = null },
                            modifier = Modifier
                                .align(Alignment.TopEnd)
                                .padding(8.dp)
                                .size(32.dp)
                                .background(Color.Black.copy(alpha = 0.6f), CircleShape)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Close,
                                contentDescription = "Remove",
                                tint = Color.White,
                                modifier = Modifier.size(18.dp)
                            )
                        }
                    }
                } else {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        OutlinedButton(
                            onClick = { isCameraActive = true },
                            modifier = Modifier
                                .weight(1f)
                                .height(64.dp)
                                .testTag("camera_capture_button"),
                            shape = RoundedCornerShape(12.dp)
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(
                                    imageVector = Icons.Default.PhotoCamera,
                                    contentDescription = "Take Photo",
                                    tint = CivicDeepBlue,
                                    modifier = Modifier.size(22.dp)
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Column {
                                    Text(
                                        text = "Take Photo",
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 13.sp,
                                        color = CivicDeepBlue
                                    )
                                    Text(
                                        text = "CameraX Live",
                                        fontSize = 10.sp,
                                        color = Color.Gray
                                    )
                                }
                            }
                        }

                        OutlinedButton(
                            onClick = {
                                photoPickerLauncher.launch(
                                    PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)
                                )
                            },
                            modifier = Modifier
                                .weight(1f)
                                .height(64.dp)
                                .testTag("gallery_picker_button"),
                            shape = RoundedCornerShape(12.dp)
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(
                                    imageVector = Icons.Default.AddPhotoAlternate,
                                    contentDescription = "Choose Photo",
                                    tint = CivicDeepBlue,
                                    modifier = Modifier.size(22.dp)
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Column {
                                    Text(
                                        text = "From Gallery",
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 13.sp,
                                        color = CivicDeepBlue
                                    )
                                    Text(
                                        text = "Pick Image",
                                        fontSize = 10.sp,
                                        color = Color.Gray
                                    )
                                }
                            }
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            // Submit Button
            Button(
                onClick = {
                    if (description.isBlank()) {
                        Toast.makeText(context, "Please enter a complaint description", Toast.LENGTH_SHORT).show()
                        return@Button
                    }
                    isSubmitting = true
                    viewModel.submitComplaint(
                        description = description,
                        category = selectedCategory,
                        department = ComplaintCategories.suggestDepartment(selectedCategory),
                        latitude = latitude,
                        longitude = longitude,
                        address = addressText,
                        imageUri = selectedImageUri?.toString() ?: "",
                        language = "en"
                    ) { success, complaintId ->
                        isSubmitting = false
                        if (success) {
                            onSubmitted(complaintId)
                        } else {
                            Toast.makeText(context, "Failed: $complaintId", Toast.LENGTH_LONG).show()
                        }
                    }
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(52.dp)
                    .testTag("submit_button"),
                shape = RoundedCornerShape(14.dp),
                colors = ButtonDefaults.buttonColors(containerColor = CivicDeepBlue),
                enabled = !isSubmitting
            ) {
                if (isSubmitting) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(22.dp),
                        color = Color.White,
                        strokeWidth = 2.dp
                    )
                    Spacer(modifier = Modifier.width(10.dp))
                    Text(text = "Analyzing & Lodging...", color = Color.White)
                } else {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(imageVector = Icons.Default.Check, contentDescription = null, tint = Color.White)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "Submit Complaint",
                            fontSize = 16.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color.White
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))
        }
    }

    if (isCameraActive) {
        CameraCapture(
            onImageCaptured = { uri ->
                selectedImageUri = uri
                isCameraActive = false
            },
            onDismiss = { isCameraActive = false }
        )
    }

    if (isMapPickerActive) {
        LocationPicker(
            initialLat = latitude,
            initialLng = longitude,
            initialAddress = addressText,
            onLocationConfirmed = { lat, lng, addr ->
                latitude = lat
                longitude = lng
                addressText = addr
                isMapPickerActive = false
            },
            onDismiss = { isMapPickerActive = false }
        )
    }
}

private fun fetchDeviceLocation(
    context: Context,
    onResult: (Double, Double, String) -> Unit
) {
    try {
        val fusedClient = LocationServices.getFusedLocationProviderClient(context)
        fusedClient.lastLocation.addOnSuccessListener { loc: Location? ->
            if (loc != null) {
                val geocoder = Geocoder(context, Locale.getDefault())
                val addressName = try {
                    val addrs = geocoder.getFromLocation(loc.latitude, loc.longitude, 1)
                    addrs?.firstOrNull()?.getAddressLine(0) ?: "Ward 14, Main Road"
                } catch (e: Exception) {
                    "Ward 14, Main Road"
                }
                onResult(loc.latitude, loc.longitude, addressName)
            } else {
                onResult(13.0827, 80.2707, "Gandhi Nagar Main Road, Ward 14")
            }
        }.addOnFailureListener {
            onResult(13.0827, 80.2707, "Gandhi Nagar Main Road, Ward 14")
        }
    } catch (e: SecurityException) {
        onResult(13.0827, 80.2707, "Gandhi Nagar Main Road, Ward 14")
    }
}
