package com.example.ui

import android.widget.Toast
import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedContent
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import com.example.data.model.Complaint
import com.example.ui.screens.AuthorityDashboardScreen
import com.example.ui.screens.ComplaintDetailScreen
import com.example.ui.screens.DeepThinkingScreen
import com.example.ui.screens.HomeScreen
import com.example.ui.screens.LiveVoiceAssistantScreen
import com.example.ui.screens.MyComplaintsScreen
import com.example.ui.screens.ReportComplaintScreen
import com.example.ui.screens.SignInScreen
import kotlinx.coroutines.launch

enum class Screen {
    HOME,
    REPORT,
    DETAIL,
    MY_COMPLAINTS,
    LIVE_AI,
    THINKING,
    AUTHORITY_DASHBOARD
}

@Composable
fun CivicPulseApp(
    viewModel: MainViewModel,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val snackbarHostState = remember { SnackbarHostState() }

    val authUser by viewModel.authUser.collectAsState()
    val isDemoAuthority by viewModel.isDemoAuthority.collectAsState()
    val userProfile by viewModel.userProfile.collectAsState()
    val uiState by viewModel.uiState.collectAsState()

    var currentScreen by remember { mutableStateOf(Screen.HOME) }
    var selectedComplaintId by remember { mutableStateOf<String?>(null) }

    LaunchedEffect(uiState) {
        when (uiState) {
            is UiState.Success -> {
                scope.launch {
                    snackbarHostState.showSnackbar((uiState as UiState.Success).message)
                    viewModel.clearUiState()
                }
            }
            is UiState.Error -> {
                scope.launch {
                    snackbarHostState.showSnackbar((uiState as UiState.Error).message)
                    viewModel.clearUiState()
                }
            }
            else -> {}
        }
    }

    val isAuthority = userProfile?.role == "authority"

    // Strict Login Routing
    LaunchedEffect(userProfile) {
        if (userProfile != null) {
            if (userProfile?.role == "authority") {
                currentScreen = Screen.AUTHORITY_DASHBOARD
            } else {
                currentScreen = Screen.HOME
            }
        }
    }

    val isSignedIn = authUser != null || userProfile != null

    if (!isSignedIn) {
        SignInScreen(
            viewModel = viewModel,
            modifier = modifier
        )
    } else {
        Scaffold(
            modifier = modifier.fillMaxSize(),
            snackbarHost = { SnackbarHost(snackbarHostState) }
        ) { paddingValues ->
            Box(modifier = Modifier.padding(paddingValues)) {
                when (currentScreen) {
                    Screen.HOME -> {
                        HomeScreen(
                            viewModel = viewModel,
                            onNavigateToReport = { currentScreen = Screen.REPORT },
                            onNavigateToMyComplaints = { currentScreen = Screen.MY_COMPLAINTS },
                            onNavigateToTracking = { complaintId ->
                                if (complaintId != null) {
                                    selectedComplaintId = complaintId
                                    currentScreen = Screen.DETAIL
                                } else {
                                    currentScreen = Screen.MY_COMPLAINTS
                                }
                            },
                            onNavigateToLiveAi = { currentScreen = Screen.LIVE_AI },
                            onNavigateToThinking = { currentScreen = Screen.THINKING },
                            onNavigateToAuthorityDashboard = {
                                if (isAuthority) {
                                    currentScreen = Screen.AUTHORITY_DASHBOARD
                                } else {
                                    scope.launch {
                                        snackbarHostState.showSnackbar("Access Denied: Municipal Authority privileges required.")
                                    }
                                }
                            },
                            onComplaintSelected = { complaint ->
                                selectedComplaintId = complaint.complaintId
                                currentScreen = Screen.DETAIL
                            }
                        )
                    }

                    Screen.REPORT -> {
                        BackHandler { currentScreen = Screen.HOME }
                        ReportComplaintScreen(
                            viewModel = viewModel,
                            onBack = { currentScreen = Screen.HOME },
                            onSubmitted = { newId ->
                                selectedComplaintId = newId
                                currentScreen = Screen.DETAIL
                            }
                        )
                    }

                    Screen.DETAIL -> {
                        BackHandler { currentScreen = if (isAuthority) Screen.AUTHORITY_DASHBOARD else Screen.HOME }
                        ComplaintDetailScreen(
                            complaintId = selectedComplaintId ?: "",
                            viewModel = viewModel,
                            onBack = { currentScreen = if (isAuthority) Screen.AUTHORITY_DASHBOARD else Screen.HOME }
                        )
                    }

                    Screen.MY_COMPLAINTS -> {
                        BackHandler { currentScreen = Screen.HOME }
                        MyComplaintsScreen(
                            viewModel = viewModel,
                            onBack = { currentScreen = Screen.HOME },
                            onSelectComplaint = { complaint ->
                                selectedComplaintId = complaint.complaintId
                                currentScreen = Screen.DETAIL
                            }
                        )
                    }

                    Screen.LIVE_AI -> {
                        BackHandler { currentScreen = Screen.HOME }
                        LiveVoiceAssistantScreen(
                            viewModel = viewModel,
                            onBack = { currentScreen = Screen.HOME }
                        )
                    }

                    Screen.THINKING -> {
                        BackHandler { currentScreen = Screen.HOME }
                        DeepThinkingScreen(
                            viewModel = viewModel,
                            onBack = { currentScreen = Screen.HOME }
                        )
                    }

                    Screen.AUTHORITY_DASHBOARD -> {
                        BackHandler { currentScreen = Screen.HOME }
                        if (isAuthority) {
                            AuthorityDashboardScreen(
                                viewModel = viewModel,
                                onBack = { currentScreen = Screen.HOME },
                                onSelectComplaint = { complaint ->
                                    selectedComplaintId = complaint.complaintId
                                    currentScreen = Screen.DETAIL
                                }
                            )
                        } else {
                            // Strict Role Mismatch Protection: Unauthorized access fallback
                            LaunchedEffect(Unit) {
                                snackbarHostState.showSnackbar("Access Denied: Municipal Authority privileges required.")
                                currentScreen = Screen.HOME
                            }
                        }
                    }
                }
            }
        }
    }
}
