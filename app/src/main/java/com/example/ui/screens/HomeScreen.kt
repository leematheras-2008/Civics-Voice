package com.example.ui.screens

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.AdminPanelSettings
import androidx.compose.material.icons.filled.Campaign
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Dashboard
import androidx.compose.material.icons.filled.Forum
import androidx.compose.material.icons.filled.Hearing
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.Logout
import androidx.compose.material.icons.filled.Psychology
import androidx.compose.material.icons.filled.ReportProblem
import androidx.compose.material.icons.filled.Shield
import androidx.compose.material.icons.filled.SwapHoriz
import androidx.compose.material3.Badge
import androidx.compose.material3.BadgedBox
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.R
import com.example.data.model.Complaint
import com.example.data.model.ComplaintStatus
import com.example.ui.MainViewModel
import com.example.ui.theme.CivicAmber
import com.example.ui.theme.CivicDeepBlue
import com.example.ui.theme.CivicGreen
import com.example.ui.theme.CivicRed
import com.example.ui.theme.CivicTeal

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen(
    viewModel: MainViewModel,
    onNavigateToReport: () -> Unit,
    onNavigateToMyComplaints: () -> Unit,
    onNavigateToTracking: (String?) -> Unit,
    onNavigateToLiveAi: () -> Unit,
    onNavigateToThinking: () -> Unit,
    onNavigateToAuthorityDashboard: () -> Unit,
    onComplaintSelected: (Complaint) -> Unit,
    modifier: Modifier = Modifier
) {
    val userProfile by viewModel.userProfile.collectAsState()
    val isDemoAuthority by viewModel.isDemoAuthority.collectAsState()
    val citizenComplaints by viewModel.citizenComplaints.collectAsState()
    val authorityComplaints by viewModel.authorityComplaints.collectAsState()

    val displayComplaints = if (isDemoAuthority) authorityComplaints else citizenComplaints
    val pendingCount = displayComplaints.count {
        it.statusEnum != ComplaintStatus.CLOSED
    }

    Scaffold(
        modifier = modifier.fillMaxSize(),
        topBar = {
            TopAppBar(
                title = {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Surface(
                            modifier = Modifier.size(36.dp),
                            shape = CircleShape,
                            color = CivicTeal.copy(alpha = 0.2f),
                            border = androidx.compose.foundation.BorderStroke(1.5.dp, CivicTeal)
                        ) {
                            Image(
                                painter = painterResource(id = R.drawable.civic_pulse_icon_1791280313191),
                                contentDescription = "Logo",
                                modifier = Modifier
                                    .fillMaxSize()
                                    .clip(CircleShape),
                                contentScale = ContentScale.Crop
                            )
                        }
                        Spacer(modifier = Modifier.width(10.dp))
                        Column {
                            Text(
                                text = "Civics Voice",
                                fontSize = 18.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color.White
                            )
                            Text(
                                text = if (isDemoAuthority) "Municipal Authority Portal" else "Citizen Portal",
                                fontSize = 11.sp,
                                color = CivicTeal
                            )
                        }
                    }
                },
                actions = {
                    // Sign Out
                    IconButton(onClick = { viewModel.signOut() }) {
                        Icon(
                            imageVector = Icons.Default.Logout,
                            contentDescription = "Sign Out",
                            tint = Color.White
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = CivicDeepBlue
                )
            )
        },
        floatingActionButton = {
            FloatingActionButton(
                onClick = onNavigateToReport,
                containerColor = CivicTeal,
                contentColor = CivicDeepBlue,
                modifier = Modifier.testTag("report_fab")
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 16.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(imageVector = Icons.Default.Add, contentDescription = "Report")
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(text = "Report", fontWeight = FontWeight.Bold)
                }
            }
        }
    ) { paddingValues ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Role & Greeting Banner
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = if (isDemoAuthority) Color(0xFF1E293B) else Color(0xFF0F3A70)
                    )
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "Welcome, ${userProfile?.name ?: "Citizen"}",
                                fontSize = 18.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color.White
                            )
                            Surface(
                                shape = RoundedCornerShape(12.dp),
                                color = if (isDemoAuthority) CivicGreen else CivicTeal
                            ) {
                                Text(
                                    text = if (isDemoAuthority) "AUTHORITY" else "CITIZEN",
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.ExtraBold,
                                    color = if (isDemoAuthority) Color.White else CivicDeepBlue,
                                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(6.dp))
                        Text(
                            text = if (isDemoAuthority)
                                "Ward 14 Grievance Desk • Active Issues: $pendingCount"
                            else
                                "Your voice shapes our neighborhood • $pendingCount Active Grievances",
                            fontSize = 13.sp,
                            color = Color.White.copy(alpha = 0.85f)
                        )
                    }
                }
            }

            // Hero Banner Image
            item {
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(140.dp),
                    shape = RoundedCornerShape(16.dp),
                    elevation = CardDefaults.cardElevation(4.dp)
                ) {
                    Box(modifier = Modifier.fillMaxSize()) {
                        Image(
                            painter = painterResource(id = R.drawable.civic_hero_banner_1791280351738),
                            contentDescription = "Smart City Banner",
                            modifier = Modifier.fillMaxSize(),
                            contentScale = ContentScale.Crop
                        )
                        Box(
                            modifier = Modifier
                                .fillMaxSize()
                                .background(
                                    Brush.verticalGradient(
                                        colors = listOf(Color.Transparent, Color(0xCC0B3C8C))
                                    )
                                )
                        )
                        Column(
                            modifier = Modifier
                                .align(Alignment.BottomStart)
                                .padding(12.dp)
                        ) {
                            Text(
                                text = "\"The Voice and Needs of the People\"",
                                color = Color.White,
                                fontWeight = FontWeight.Bold,
                                fontSize = 14.sp
                            )
                            Text(
                                text = "Direct connection to municipal departments with AI triage",
                                color = CivicTeal,
                                fontSize = 12.sp
                            )
                        }
                    }
                }
            }

            // 2-Column Grid Action Cards
            item {
                Text(
                    text = "Civic Services",
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onBackground
                )
            }

            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    ActionCard(
                        title = "Report Issue",
                        subtitle = "Voice, GPS, Photo",
                        icon = Icons.Default.ReportProblem,
                        iconTint = CivicTeal,
                        modifier = Modifier.weight(1f),
                        onClick = onNavigateToReport
                    )
                    ActionCard(
                        title = "Live Tracking",
                        subtitle = "Real-time timeline",
                        icon = Icons.Default.LocationOn,
                        iconTint = CivicAmber,
                        modifier = Modifier.weight(1f),
                        onClick = { onNavigateToTracking(displayComplaints.firstOrNull()?.complaintId) }
                    )
                }
            }

            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    ActionCard(
                        title = "Live Voice AI",
                        subtitle = "gemini-3.8-live",
                        icon = Icons.Default.Hearing,
                        iconTint = Color(0xFF6366F1),
                        modifier = Modifier.weight(1f),
                        onClick = onNavigateToLiveAi
                    )
                    ActionCard(
                        title = "Deep Thinker",
                        subtitle = "gemini-3.1-pro HIGH",
                        icon = Icons.Default.Psychology,
                        iconTint = Color(0xFFEC4899),
                        modifier = Modifier.weight(1f),
                        onClick = onNavigateToThinking
                    )
                }
            }

            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    ActionCard(
                        title = "My Grievances",
                        subtitle = "${displayComplaints.size} logged issues",
                        icon = Icons.Default.History,
                        iconTint = CivicGreen,
                        modifier = Modifier.weight(1f),
                        onClick = onNavigateToMyComplaints
                    )
                    if (userProfile?.role == "authority") {
                        ActionCard(
                            title = "Authority Hub",
                            subtitle = "Municipal triage",
                            icon = Icons.Default.AdminPanelSettings,
                            iconTint = CivicDeepBlue,
                            modifier = Modifier.weight(1f),
                            onClick = onNavigateToAuthorityDashboard
                        )
                    } else {
                        ActionCard(
                            title = "Civic Directory",
                            subtitle = "Ward 14 emergency",
                            icon = Icons.Default.Shield,
                            iconTint = CivicDeepBlue,
                            modifier = Modifier.weight(1f),
                            onClick = onNavigateToLiveAi
                        )
                    }
                }
            }

            // Recent Grievances Header
            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = if (isDemoAuthority) "Ward 14 Registry" else "Recent Community Reports",
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onBackground
                    )
                    Text(
                        text = "View All",
                        color = CivicDeepBlue,
                        fontSize = 13.sp,
                        fontWeight = FontWeight.SemiBold,
                        modifier = Modifier.clickable { onNavigateToMyComplaints() }
                    )
                }
            }

            // Grievances items
            if (displayComplaints.isEmpty()) {
                item {
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(24.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Icon(
                                imageVector = Icons.Default.CheckCircle,
                                contentDescription = null,
                                tint = CivicGreen,
                                modifier = Modifier.size(40.dp)
                            )
                            Spacer(modifier = Modifier.height(8.dp))
                            Text(
                                text = "No open complaints found",
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Text(
                                text = "Tap 'Report Issue' to lodge a municipal grievance",
                                fontSize = 12.sp,
                                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.8f)
                            )
                        }
                    }
                }
            } else {
                items(displayComplaints.take(5)) { complaint ->
                    ComplaintCardItem(
                        complaint = complaint,
                        onClick = { onComplaintSelected(complaint) }
                    )
                }
            }
        }
    }
}

@Composable
fun ActionCard(
    title: String,
    subtitle: String,
    icon: ImageVector,
    iconTint: Color,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    ElevatedCard(
        onClick = onClick,
        modifier = modifier.height(105.dp),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.elevatedCardColors(
            containerColor = MaterialTheme.colorScheme.surface
        ),
        elevation = CardDefaults.elevatedCardElevation(2.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(12.dp),
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            Box(
                modifier = Modifier
                    .size(36.dp)
                    .clip(RoundedCornerShape(8.dp))
                    .background(iconTint.copy(alpha = 0.15f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = title,
                    tint = iconTint,
                    modifier = Modifier.size(22.dp)
                )
            }
            Column {
                Text(
                    text = title,
                    fontWeight = FontWeight.Bold,
                    fontSize = 14.sp,
                    color = MaterialTheme.colorScheme.onSurface,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Text(
                    text = subtitle,
                    fontSize = 11.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
        }
    }
}

@Composable
fun ComplaintCardItem(
    complaint: Complaint,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Card(
        onClick = onClick,
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(2.dp)
    ) {
        Column(modifier = Modifier.padding(14.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = complaint.complaintId,
                        fontWeight = FontWeight.Bold,
                        fontSize = 13.sp,
                        color = CivicDeepBlue
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    PriorityBadge(priority = complaint.priority)
                }

                StatusBadge(status = complaint.statusEnum)
            }

            Spacer(modifier = Modifier.height(6.dp))

            Text(
                text = complaint.category,
                fontWeight = FontWeight.SemiBold,
                fontSize = 14.sp,
                color = MaterialTheme.colorScheme.onSurface
            )

            Text(
                text = complaint.description,
                fontSize = 12.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis
            )

            Spacer(modifier = Modifier.height(8.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Default.LocationOn,
                        contentDescription = null,
                        tint = CivicTeal,
                        modifier = Modifier.size(14.dp)
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        text = complaint.address.take(30),
                        fontSize = 11.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = "${complaint.supportCount} citizens supported",
                        fontSize = 11.sp,
                        color = CivicGreen,
                        fontWeight = FontWeight.SemiBold
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.ArrowForward,
                        contentDescription = "Open",
                        modifier = Modifier.size(14.dp),
                        tint = CivicDeepBlue
                    )
                }
            }
        }
    }
}

@Composable
fun StatusBadge(status: ComplaintStatus) {
    val (bgColor, textColor) = when (status) {
        ComplaintStatus.SUBMITTED -> Color(0xFFE2E8F0) to Color(0xFF475569)
        ComplaintStatus.UNDER_REVIEW -> Color(0xFFFEF3C7) to Color(0xFFB45309)
        ComplaintStatus.ASSIGNED -> Color(0xFFE0E7FF) to Color(0xFF4338CA)
        ComplaintStatus.WORK_IN_PROGRESS -> Color(0xFFDBEAFE) to Color(0xFF1D4ED8)
        ComplaintStatus.RESOLVED -> Color(0xFFDCFCE7) to Color(0xFF15803D)
        ComplaintStatus.CITIZEN_VERIFICATION -> Color(0xFFFEE2E2) to Color(0xFFB91C1C)
        ComplaintStatus.CLOSED -> Color(0xFFF1F5F9) to Color(0xFF64748B)
        ComplaintStatus.REOPENED -> Color(0xFFFFEDD5) to Color(0xFFC2410C)
    }

    Surface(
        shape = RoundedCornerShape(8.dp),
        color = bgColor
    ) {
        Text(
            text = status.displayName,
            fontSize = 11.sp,
            fontWeight = FontWeight.Bold,
            color = textColor,
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
        )
    }
}

@Composable
fun PriorityBadge(priority: String) {
    val (color, text) = when (priority.uppercase()) {
        "CRITICAL" -> CivicRed to "CRITICAL"
        "HIGH" -> CivicAmber to "HIGH"
        "MEDIUM" -> CivicTeal to "MEDIUM"
        else -> CivicGreen to "LOW"
    }

    Surface(
        shape = RoundedCornerShape(4.dp),
        color = color.copy(alpha = 0.15f)
    ) {
        Text(
            text = text,
            fontSize = 9.sp,
            fontWeight = FontWeight.Bold,
            color = color,
            modifier = Modifier.padding(horizontal = 5.dp, vertical = 2.dp)
        )
    }
}
