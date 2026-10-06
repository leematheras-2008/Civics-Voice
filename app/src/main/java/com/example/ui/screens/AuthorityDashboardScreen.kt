package com.example.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.AssignmentTurnedIn
import androidx.compose.material.icons.filled.Build
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.FilterList
import androidx.compose.material.icons.filled.HourglassEmpty
import androidx.compose.material.icons.filled.Logout
import androidx.compose.material.icons.filled.PendingActions
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
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
fun AuthorityDashboardScreen(
    viewModel: MainViewModel,
    onBack: () -> Unit,
    onSelectComplaint: (Complaint) -> Unit,
    modifier: Modifier = Modifier
) {
    val authorityComplaints by viewModel.authorityComplaints.collectAsState()

    var searchQuery by remember { mutableStateOf("") }
    var selectedFilterStatus by remember { mutableStateOf<ComplaintStatus?>(null) }

    val filteredComplaints = authorityComplaints.filter { complaint ->
        val matchesSearch = complaint.complaintId.contains(searchQuery, ignoreCase = true) ||
                complaint.description.contains(searchQuery, ignoreCase = true) ||
                complaint.category.contains(searchQuery, ignoreCase = true) ||
                complaint.address.contains(searchQuery, ignoreCase = true)
        val matchesStatus = selectedFilterStatus == null || complaint.statusEnum == selectedFilterStatus
        matchesSearch && matchesStatus
    }

    // Real metrics
    val submittedCount = authorityComplaints.count { it.statusEnum == ComplaintStatus.SUBMITTED }
    val underReviewCount = authorityComplaints.count { it.statusEnum == ComplaintStatus.UNDER_REVIEW }
    val inProgressCount = authorityComplaints.count { it.statusEnum == ComplaintStatus.WORK_IN_PROGRESS }
    val verificationCount = authorityComplaints.count { it.statusEnum == ComplaintStatus.CITIZEN_VERIFICATION }
    val closedCount = authorityComplaints.count { it.statusEnum == ComplaintStatus.CLOSED }
    val reopenedCount = authorityComplaints.count { it.statusEnum == ComplaintStatus.REOPENED }

    Scaffold(
        modifier = modifier.fillMaxSize(),
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text(
                            text = "Authority Command Hub",
                            fontWeight = FontWeight.Bold,
                            color = Color.White,
                            fontSize = 18.sp
                        )
                        Text(
                            text = "Ward 14 Grievance Redressal Desk",
                            color = CivicTeal,
                            fontSize = 12.sp
                        )
                    }
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
                actions = {
                    IconButton(onClick = { viewModel.signOut() }) {
                        Icon(
                            imageVector = Icons.Default.Logout,
                            contentDescription = "Sign Out",
                            tint = Color.White
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = CivicDeepBlue)
            )
        }
    ) { paddingValues ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            // Metrics KPI Grid
            item {
                Text(
                    text = "Live Redressal Metrics",
                    fontWeight = FontWeight.Bold,
                    fontSize = 15.sp,
                    color = MaterialTheme.colorScheme.onBackground
                )
            }

            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    MetricCard(
                        count = submittedCount,
                        label = "Submitted",
                        color = Color(0xFF64748B),
                        icon = Icons.Default.PendingActions,
                        modifier = Modifier.weight(1f)
                    )
                    MetricCard(
                        count = inProgressCount,
                        label = "In Progress",
                        color = CivicAmber,
                        icon = Icons.Default.Build,
                        modifier = Modifier.weight(1f)
                    )
                    MetricCard(
                        count = verificationCount,
                        label = "Verification",
                        color = CivicTeal,
                        icon = Icons.Default.HourglassEmpty,
                        modifier = Modifier.weight(1f)
                    )
                    MetricCard(
                        count = closedCount,
                        label = "Closed",
                        color = CivicGreen,
                        icon = Icons.Default.CheckCircle,
                        modifier = Modifier.weight(1f)
                    )
                }
            }

            // Search Bar
            item {
                OutlinedTextField(
                    value = searchQuery,
                    onValueChange = { searchQuery = it },
                    placeholder = { Text("Search by ID, area, or description...") },
                    leadingIcon = {
                        Icon(imageVector = Icons.Default.Search, contentDescription = null, tint = CivicDeepBlue)
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("authority_search_bar"),
                    shape = RoundedCornerShape(12.dp),
                    singleLine = true
                )
            }

            // Status Filter Chips
            item {
                LazyRow(
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    item {
                        FilterChip(
                            selected = selectedFilterStatus == null,
                            onClick = { selectedFilterStatus = null },
                            label = { Text("All (${authorityComplaints.size})") }
                        )
                    }
                    ComplaintStatus.values().forEach { status ->
                        val count = authorityComplaints.count { it.statusEnum == status }
                        item {
                            FilterChip(
                                selected = selectedFilterStatus == status,
                                onClick = {
                                    selectedFilterStatus = if (selectedFilterStatus == status) null else status
                                },
                                label = { Text("${status.displayName} ($count)") }
                            )
                        }
                    }
                }
            }

            // Register Items
            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Filtered Grievances (${filteredComplaints.size})",
                        fontWeight = FontWeight.Bold,
                        fontSize = 15.sp
                    )
                }
            }

            if (filteredComplaints.isEmpty()) {
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
                                imageVector = Icons.Default.AssignmentTurnedIn,
                                contentDescription = null,
                                tint = CivicGreen,
                                modifier = Modifier.size(36.dp)
                            )
                            Spacer(modifier = Modifier.height(8.dp))
                            Text(text = "No grievances match current filters", fontWeight = FontWeight.Bold)
                        }
                    }
                }
            } else {
                items(filteredComplaints) { complaint ->
                    ComplaintCardItem(
                        complaint = complaint,
                        onClick = { onSelectComplaint(complaint) }
                    )
                }
            }
        }
    }
}

@Composable
private fun MetricCard(
    count: Int,
    label: String,
    color: Color,
    icon: ImageVector,
    modifier: Modifier = Modifier
) {
    ElevatedCard(
        modifier = modifier.height(88.dp),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.elevatedCardColors(containerColor = MaterialTheme.colorScheme.surface)
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(8.dp),
            verticalArrangement = Arrangement.Center,
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Icon(imageVector = icon, contentDescription = null, tint = color, modifier = Modifier.size(18.dp))
            Spacer(modifier = Modifier.height(2.dp))
            Text(
                text = count.toString(),
                fontWeight = FontWeight.ExtraBold,
                fontSize = 16.sp,
                color = color
            )
            Text(
                text = label,
                fontSize = 9.sp,
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}
