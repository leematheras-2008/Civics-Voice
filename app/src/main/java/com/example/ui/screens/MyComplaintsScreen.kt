package com.example.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.AssignmentTurnedIn
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.Complaint
import com.example.data.model.ComplaintStatus
import com.example.ui.MainViewModel
import com.example.ui.theme.CivicDeepBlue
import com.example.ui.theme.CivicGreen
import com.example.ui.theme.CivicTeal

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MyComplaintsScreen(
    viewModel: MainViewModel,
    onBack: () -> Unit,
    onSelectComplaint: (Complaint) -> Unit,
    modifier: Modifier = Modifier
) {
    val citizenComplaints by viewModel.citizenComplaints.collectAsState()
    val authorityComplaints by viewModel.authorityComplaints.collectAsState()
    val isDemoAuthority by viewModel.isDemoAuthority.collectAsState()

    val displayComplaints = if (isDemoAuthority) authorityComplaints else citizenComplaints

    var selectedTabIndex by remember { mutableIntStateOf(0) }
    val tabTitles = listOf("All", "In Progress", "Needs Verification", "Closed")

    val filteredList = when (selectedTabIndex) {
        1 -> displayComplaints.filter {
            it.statusEnum == ComplaintStatus.SUBMITTED ||
                    it.statusEnum == ComplaintStatus.UNDER_REVIEW ||
                    it.statusEnum == ComplaintStatus.ASSIGNED ||
                    it.statusEnum == ComplaintStatus.WORK_IN_PROGRESS ||
                    it.statusEnum == ComplaintStatus.REOPENED
        }
        2 -> displayComplaints.filter {
            it.statusEnum == ComplaintStatus.CITIZEN_VERIFICATION ||
                    it.statusEnum == ComplaintStatus.RESOLVED
        }
        3 -> displayComplaints.filter { it.statusEnum == ComplaintStatus.CLOSED }
        else -> displayComplaints
    }

    Scaffold(
        modifier = modifier.fillMaxSize(),
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = if (isDemoAuthority) "Municipal Registry" else "My Grievances",
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
        ) {
            TabRow(
                selectedTabIndex = selectedTabIndex,
                containerColor = MaterialTheme.colorScheme.surface,
                contentColor = CivicDeepBlue
            ) {
                tabTitles.forEachIndexed { index, title ->
                    Tab(
                        selected = selectedTabIndex == index,
                        onClick = { selectedTabIndex = index },
                        text = {
                            Text(
                                text = title,
                                fontSize = 12.sp,
                                fontWeight = if (selectedTabIndex == index) FontWeight.Bold else FontWeight.Normal
                            )
                        }
                    )
                }
            }

            if (filteredList.isEmpty()) {
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(32.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.AssignmentTurnedIn,
                        contentDescription = null,
                        tint = CivicGreen,
                        modifier = Modifier.size(48.dp)
                    )
                    Text(
                        text = "No complaints in this category",
                        fontWeight = FontWeight.Bold,
                        fontSize = 15.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(top = 8.dp)
                    )
                }
            } else {
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    items(filteredList) { complaint ->
                        ComplaintCardItem(
                            complaint = complaint,
                            onClick = { onSelectComplaint(complaint) }
                        )
                    }
                }
            }
        }
    }
}
