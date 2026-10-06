package com.example.ui

import android.content.Context
import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.ai.AiComplaintAnalysis
import com.example.data.ai.GeminiAiService
import com.example.data.model.Complaint
import com.example.data.model.ComplaintStatus
import com.example.data.model.TimelineEvent
import com.example.data.model.UserProfile
import com.example.data.repository.AuthRepository
import com.example.data.repository.ComplaintRepository
import com.google.firebase.auth.FirebaseUser
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

private const val TAG = "MainViewModel"

sealed interface UiState {
    object Idle : UiState
    object Loading : UiState
    data class Success(val message: String) : UiState
    data class Error(val message: String) : UiState
}

class MainViewModel(
    val authRepository: AuthRepository,
    val complaintRepository: ComplaintRepository,
    private val aiService: GeminiAiService = GeminiAiService()
) : ViewModel() {

    val authUser: StateFlow<FirebaseUser?> = authRepository.authState
        .stateIn(viewModelScope, SharingStarted.Eagerly, null)

    val userProfile: StateFlow<UserProfile?> = authRepository.currentUserProfile

    val isDemoAuthority: StateFlow<Boolean> = authRepository.isDemoAuthority

    private val _citizenComplaints = MutableStateFlow<List<Complaint>>(emptyList())
    val citizenComplaints: StateFlow<List<Complaint>> = _citizenComplaints.asStateFlow()

    private val _authorityComplaints = MutableStateFlow<List<Complaint>>(emptyList())
    val authorityComplaints: StateFlow<List<Complaint>> = _authorityComplaints.asStateFlow()

    private val _selectedComplaintTimeline = MutableStateFlow<List<TimelineEvent>>(emptyList())
    val selectedComplaintTimeline: StateFlow<List<TimelineEvent>> = _selectedComplaintTimeline.asStateFlow()

    private val _uiState = MutableStateFlow<UiState>(UiState.Idle)
    val uiState: StateFlow<UiState> = _uiState.asStateFlow()

    // Live AI Assistant State (gemini-3.8-live)
    private val _liveChatMessages = MutableStateFlow<List<Pair<String, String>>>(
        listOf(
            "model" to "Vanakkam & Welcome to Civics Voice! I am your real-time civic assistant powered by Live API. How can I help you regarding civic issues, ward inquiries, or grievance redressal today?"
        )
    )
    val liveChatMessages: StateFlow<List<Pair<String, String>>> = _liveChatMessages.asStateFlow()

    private val _isLiveChatThinking = MutableStateFlow(false)
    val isLiveChatThinking: StateFlow<Boolean> = _isLiveChatThinking.asStateFlow()

    // High Thinking State (gemini-3.1-pro-preview)
    private val _thinkingResult = MutableStateFlow<String?>(null)
    val thinkingResult: StateFlow<String?> = _thinkingResult.asStateFlow()

    private val _isThinkingLoading = MutableStateFlow(false)
    val isThinkingLoading: StateFlow<Boolean> = _isThinkingLoading.asStateFlow()

    // Speech Transcription State (gemini-3.5-transcribe)
    private val _isTranscribing = MutableStateFlow(false)
    val isTranscribing: StateFlow<Boolean> = _isTranscribing.asStateFlow()

    init {
        viewModelScope.launch {
            authUser.collect { user ->
                if (user != null) {
                    authRepository.fetchOrCreateUserProfile(user)
                    complaintRepository.seedDemoComplaintsIfEmpty()
                    observeComplaints()
                }
            }
        }
        viewModelScope.launch {
            userProfile.collect { profile ->
                if (profile != null) {
                    complaintRepository.seedDemoComplaintsIfEmpty()
                    observeComplaints()
                } else if (authUser.value == null) {
                    _citizenComplaints.value = emptyList()
                    _authorityComplaints.value = emptyList()
                }
            }
        }
    }

    private fun observeComplaints() {
        viewModelScope.launch {
            complaintRepository.getCitizenComplaints().collect { list ->
                _citizenComplaints.value = list
            }
        }
        viewModelScope.launch {
            complaintRepository.getAuthorityComplaints().collect { list ->
                _authorityComplaints.value = list
            }
        }
    }

    fun loadComplaintTimeline(complaintId: String) {
        viewModelScope.launch {
            complaintRepository.getComplaintTimeline(complaintId).collect { events ->
                _selectedComplaintTimeline.value = events
            }
        }
    }

    fun loginPublic(email: String, password: String, onResult: (Boolean, String) -> Unit) {
        _uiState.value = UiState.Loading
        viewModelScope.launch {
            val result = authRepository.loginWithEmailPassword(email, password, requiredRole = "public")
            if (result.isSuccess) {
                val profile = result.getOrThrow()
                _uiState.value = UiState.Success("Welcome back, ${profile.name}!")
                onResult(true, "Signed in successfully")
            } else {
                val msg = result.exceptionOrNull()?.message ?: "Login failed"
                _uiState.value = UiState.Error(msg)
                onResult(false, msg)
            }
        }
    }

    fun registerPublic(
        name: String,
        email: String,
        password: String,
        onResult: (Boolean, String) -> Unit
    ) {
        _uiState.value = UiState.Loading
        viewModelScope.launch {
            val result = authRepository.registerPublicUser(name, email, password)
            if (result.isSuccess) {
                val profile = result.getOrThrow()
                _uiState.value = UiState.Success("Citizen account created! Welcome, ${profile.name}.")
                onResult(true, "Registration successful")
            } else {
                val msg = result.exceptionOrNull()?.message ?: "Registration failed"
                _uiState.value = UiState.Error(msg)
                onResult(false, msg)
            }
        }
    }

    fun loginAuthority(email: String, password: String, onResult: (Boolean, String) -> Unit) {
        _uiState.value = UiState.Loading
        viewModelScope.launch {
            val result = authRepository.loginWithEmailPassword(email, password, requiredRole = "authority")
            if (result.isSuccess) {
                val profile = result.getOrThrow()
                _uiState.value = UiState.Success("Authority authenticated: ${profile.name}")
                onResult(true, "Signed in as Authority")
            } else {
                val msg = result.exceptionOrNull()?.message ?: "Authority authentication failed"
                _uiState.value = UiState.Error(msg)
                onResult(false, msg)
            }
        }
    }

    fun sendPasswordReset(email: String, onResult: (Boolean, String) -> Unit) {
        _uiState.value = UiState.Loading
        viewModelScope.launch {
            val result = authRepository.sendPasswordReset(email)
            if (result.isSuccess) {
                _uiState.value = UiState.Success("Password reset email sent. Please check your inbox.")
                onResult(true, "Reset link sent")
            } else {
                val msg = result.exceptionOrNull()?.message ?: "Password reset failed"
                _uiState.value = UiState.Error(msg)
                onResult(false, msg)
            }
        }
    }

    fun signInWithGoogle(context: Context, onResult: (Boolean) -> Unit) {
        _uiState.value = UiState.Loading
        viewModelScope.launch {
            val result = authRepository.signInWithGoogle(context)
            if (result.isSuccess) {
                _uiState.value = UiState.Success("Signed in successfully")
                onResult(true)
            } else {
                val exception = result.exceptionOrNull()
                val isNoCred = exception is androidx.credentials.exceptions.NoCredentialException ||
                        exception?.message?.contains("No credentials", ignoreCase = true) == true
                if (isNoCred) {
                    // Emulator / unprovisioned device fallback to Citizen mode
                    authRepository.toggleDemoAuthority(false)
                    _uiState.value = UiState.Success("Welcome to Civics Voice! (Citizen Mode)")
                    onResult(true)
                } else {
                    val errorMsg = exception?.message ?: "Sign-in cancelled or failed"
                    _uiState.value = UiState.Error(errorMsg)
                    onResult(false)
                }
            }
        }
    }

    fun toggleDemoAuthority(enable: Boolean) {
        authRepository.toggleDemoAuthority(enable)
        if (enable) {
            _uiState.value = UiState.Success("Switched to Municipal Authority Mode (Ward 14)")
        } else {
            _uiState.value = UiState.Success("Switched to Public Citizen Mode")
        }
    }

    fun submitComplaint(
        description: String,
        category: String,
        department: String,
        latitude: Double,
        longitude: Double,
        address: String,
        imageUri: String,
        language: String = "en",
        onComplete: (Boolean, String) -> Unit
    ) {
        _uiState.value = UiState.Loading
        viewModelScope.launch {
            try {
                // Step 1: AI analysis
                val analysis = aiService.analyzeComplaint(description, address)

                // Step 2: Persist to Firestore
                val result = complaintRepository.createComplaint(
                    description = description,
                    category = category,
                    department = department,
                    latitude = latitude,
                    longitude = longitude,
                    address = address,
                    aiAnalysis = analysis,
                    imageUri = imageUri,
                    language = language
                )

                if (result.isSuccess) {
                    val complaintId = result.getOrThrow()
                    _uiState.value = UiState.Success("Complaint $complaintId lodged successfully!")
                    onComplete(true, complaintId)
                } else {
                    val msg = result.exceptionOrNull()?.message ?: "Failed to lodge complaint"
                    _uiState.value = UiState.Error(msg)
                    onComplete(false, msg)
                }
            } catch (e: Exception) {
                Log.e(TAG, "Error lodging complaint: ${e.message}", e)
                _uiState.value = UiState.Error(e.message ?: "Unknown error")
                onComplete(false, e.message ?: "Unknown error")
            }
        }
    }

    fun transcribeAudioAndApply(
        audioBase64: String,
        onTranscribed: (String) -> Unit
    ) {
        _isTranscribing.value = true
        viewModelScope.launch {
            try {
                val text = aiService.transcribeAudio(audioBase64)
                onTranscribed(text)
            } catch (e: Exception) {
                Log.e(TAG, "Transcription failed: ${e.message}")
            } finally {
                _isTranscribing.value = false
            }
        }
    }

    fun sendLiveChatMessage(userText: String) {
        if (userText.isBlank()) return
        val current = _liveChatMessages.value.toMutableList()
        current.add("user" to userText)
        _liveChatMessages.value = current
        _isLiveChatThinking.value = true

        viewModelScope.launch {
            try {
                val response = aiService.liveConversationTurn(
                    userMessage = userText,
                    conversationHistory = current
                )
                val updated = _liveChatMessages.value.toMutableList()
                updated.add("model" to response)
                _liveChatMessages.value = updated
            } catch (e: Exception) {
                val updated = _liveChatMessages.value.toMutableList()
                updated.add("model" to "Civic helpline is momentarily busy. Please try again or submit a complaint form.")
                _liveChatMessages.value = updated
            } finally {
                _isLiveChatThinking.value = false
            }
        }
    }

    fun runHighThinkingQuery(query: String) {
        if (query.isBlank()) return
        _isThinkingLoading.value = true
        _thinkingResult.value = null
        viewModelScope.launch {
            try {
                val answer = aiService.thinkDeeplyOnCivicQuery(query)
                _thinkingResult.value = answer
            } catch (e: Exception) {
                _thinkingResult.value = "Error executing high-thinking reasoning: ${e.message}"
            } finally {
                _isThinkingLoading.value = false
            }
        }
    }

    fun updateStatusByAuthority(
        complaintId: String,
        newStatus: ComplaintStatus,
        assignedOfficer: String,
        resolutionProofNote: String
    ) {
        if (userProfile.value?.role != "authority") {
            _uiState.value = UiState.Error("Access Denied: Municipal Authority role required.")
            return
        }
        _uiState.value = UiState.Loading
        viewModelScope.launch {
            val result = complaintRepository.updateComplaintStatus(
                complaintId = complaintId,
                newStatus = newStatus,
                assignedOfficer = assignedOfficer,
                resolutionProofNote = resolutionProofNote,
                actorName = userProfile.value?.name ?: "Municipal Authority"
            )
            if (result.isSuccess) {
                _uiState.value = UiState.Success("Status updated to ${newStatus.displayName}")
            } else {
                _uiState.value = UiState.Error(result.exceptionOrNull()?.message ?: "Failed to update status")
            }
        }
    }

    fun verifyResolution(complaintId: String, isSatisfied: Boolean, feedback: String) {
        _uiState.value = UiState.Loading
        viewModelScope.launch {
            val result = complaintRepository.verifyResolution(complaintId, isSatisfied, feedback)
            if (result.isSuccess) {
                val msg = if (isSatisfied) "Resolution confirmed! Grievance closed." else "Grievance reopened for rectification."
                _uiState.value = UiState.Success(msg)
            } else {
                _uiState.value = UiState.Error("Failed to submit verification")
            }
        }
    }

    fun supportComplaint(complaintId: String) {
        viewModelScope.launch {
            complaintRepository.supportComplaint(complaintId)
        }
    }

    fun clearUiState() {
        _uiState.value = UiState.Idle
    }

    fun signOut() {
        authRepository.signOut()
    }
}
