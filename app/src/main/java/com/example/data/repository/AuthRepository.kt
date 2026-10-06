package com.example.data.repository

import android.content.Context
import android.util.Log
import androidx.credentials.CredentialManager
import androidx.credentials.CustomCredential
import androidx.credentials.GetCredentialRequest
import androidx.credentials.GetCredentialResponse
import androidx.credentials.exceptions.GetCredentialCancellationException
import androidx.credentials.exceptions.GetCredentialException
import com.example.R
import com.example.data.model.UserProfile
import com.google.android.libraries.identity.googleid.GetGoogleIdOption
import com.google.android.libraries.identity.googleid.GoogleIdTokenCredential
import com.google.firebase.Firebase
import com.google.firebase.Timestamp
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseUser
import com.google.firebase.auth.GoogleAuthProvider
import com.google.firebase.auth.auth
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.tasks.await

private const val TAG = "AuthRepository"

class AuthRepository(
    private val auth: FirebaseAuth = Firebase.auth,
    private val db: FirebaseFirestore
) {
    constructor(context: Context) : this(
        auth = Firebase.auth,
        db = FirebaseFirestore.getInstance(context.getString(R.string.firestore_database_id))
    )

    private val _currentUserProfile = MutableStateFlow<UserProfile?>(null)
    val currentUserProfile: StateFlow<UserProfile?> = _currentUserProfile.asStateFlow()

    private val _isDemoAuthority = MutableStateFlow(false)
    val isDemoAuthority: StateFlow<Boolean> = _isDemoAuthority.asStateFlow()

    val authState: Flow<FirebaseUser?> = callbackFlow {
        val listener = FirebaseAuth.AuthStateListener { fbAuth ->
            trySend(fbAuth.currentUser)
        }
        auth.addAuthStateListener(listener)
        awaitClose { auth.removeAuthStateListener(listener) }
    }

    val currentUserId: String?
        get() = auth.currentUser?.uid

    suspend fun registerPublicUser(name: String, email: String, password: String): Result<UserProfile> {
        val trimmedEmail = email.trim()
        val trimmedName = name.trim()
        if (trimmedName.isBlank()) {
            return Result.failure(IllegalArgumentException("Full name is required"))
        }
        if (!android.util.Patterns.EMAIL_ADDRESS.matcher(trimmedEmail).matches()) {
            return Result.failure(IllegalArgumentException("Please enter a valid email address"))
        }
        if (password.length < 6) {
            return Result.failure(IllegalArgumentException("Password must be at least 6 characters"))
        }

        return try {
            val authResult = auth.createUserWithEmailAndPassword(trimmedEmail, password).await()
            val user = authResult.user ?: error("User creation failed")

            val newProfile = UserProfile(
                uid = user.uid,
                name = trimmedName,
                email = trimmedEmail,
                phone = "",
                role = "public", // Public registration is strictly locked to "public" role
                accountStatus = "active",
                department = "Citizen Services",
                jurisdiction = "Ward 14 - East District",
                preferredLanguage = "en",
                createdAt = Timestamp.now(),
                updatedAt = Timestamp.now()
            )

            val docRef = db.collection("users").document(user.uid)
            val map = hashMapOf(
                "uid" to newProfile.uid,
                "name" to newProfile.name,
                "email" to newProfile.email,
                "phone" to newProfile.phone,
                "role" to newProfile.role,
                "accountStatus" to newProfile.accountStatus,
                "department" to newProfile.department,
                "jurisdiction" to newProfile.jurisdiction,
                "preferredLanguage" to newProfile.preferredLanguage,
                "createdAt" to newProfile.createdAt,
                "updatedAt" to newProfile.updatedAt
            )
            docRef.set(map).await()
            _currentUserProfile.value = newProfile
            _isDemoAuthority.value = false
            Result.success(newProfile)
        } catch (e: Exception) {
            Log.e(TAG, "Registration failed: ${e.message}")
            Result.failure(e)
        }
    }

    suspend fun loginWithEmailPassword(
        email: String,
        password: String,
        requiredRole: String? = null
    ): Result<UserProfile> {
        val trimmedEmail = email.trim()
        if (!android.util.Patterns.EMAIL_ADDRESS.matcher(trimmedEmail).matches()) {
            return Result.failure(IllegalArgumentException("Please enter a valid email address"))
        }
        if (password.isBlank()) {
            return Result.failure(IllegalArgumentException("Password is required"))
        }

        return try {
            val authResult = auth.signInWithEmailAndPassword(trimmedEmail, password).await()
            val user = authResult.user ?: error("Sign in failed")

            val profile = fetchOrCreateUserProfile(user)

            // Strict Role Mismatch Protection:
            if (requiredRole == "authority") {
                if (profile.role != "authority") {
                    auth.signOut()
                    _currentUserProfile.value = null
                    return Result.failure(SecurityException("Access Denied: This account is not an authorized Municipal Authority."))
                }
            }

            if (profile.accountStatus != "active") {
                auth.signOut()
                _currentUserProfile.value = null
                return Result.failure(SecurityException("Access Denied: This account has been deactivated."))
            }

            _currentUserProfile.value = profile
            _isDemoAuthority.value = (profile.role == "authority")
            Result.success(profile)
        } catch (e: Exception) {
            Log.e(TAG, "Login failed: ${e.message}")
            Result.failure(e)
        }
    }

    suspend fun sendPasswordReset(email: String): Result<Unit> {
        val trimmedEmail = email.trim()
        if (!android.util.Patterns.EMAIL_ADDRESS.matcher(trimmedEmail).matches()) {
            return Result.failure(IllegalArgumentException("Please enter a valid email address"))
        }
        return try {
            auth.sendPasswordResetEmail(trimmedEmail).await()
            Result.success(Unit)
        } catch (e: Exception) {
            Log.e(TAG, "Password reset failed: ${e.message}")
            Result.failure(e)
        }
    }

    suspend fun fetchOrCreateUserProfile(user: FirebaseUser): UserProfile {
        val docRef = db.collection("users").document(user.uid)
        val snapshot = try {
            docRef.get().await()
        } catch (e: Exception) {
            Log.w(TAG, "Failed to fetch user doc: ${e.message}")
            null
        }

        if (snapshot != null && snapshot.exists()) {
            val profile = UserProfile(
                uid = snapshot.getString("uid") ?: user.uid,
                name = snapshot.getString("name") ?: user.displayName ?: "Citizen",
                email = snapshot.getString("email") ?: user.email ?: "",
                phone = snapshot.getString("phone") ?: "",
                role = snapshot.getString("role") ?: "public",
                accountStatus = snapshot.getString("accountStatus") ?: "active",
                department = snapshot.getString("department") ?: "All Departments",
                jurisdiction = snapshot.getString("jurisdiction") ?: "Central Zone",
                preferredLanguage = snapshot.getString("preferredLanguage") ?: "en"
            )
            _currentUserProfile.value = profile
            return profile
        }

        val newProfile = UserProfile(
            uid = user.uid,
            name = user.displayName ?: "Citizen",
            email = user.email ?: "",
            phone = user.phoneNumber ?: "",
            role = "public",
            accountStatus = "active",
            department = "All Departments",
            jurisdiction = "Central Zone",
            preferredLanguage = "en",
            createdAt = Timestamp.now(),
            updatedAt = Timestamp.now()
        )

        try {
            val map = hashMapOf(
                "uid" to newProfile.uid,
                "name" to newProfile.name,
                "email" to newProfile.email,
                "phone" to newProfile.phone,
                "role" to newProfile.role,
                "accountStatus" to newProfile.accountStatus,
                "department" to newProfile.department,
                "jurisdiction" to newProfile.jurisdiction,
                "preferredLanguage" to newProfile.preferredLanguage,
                "createdAt" to newProfile.createdAt,
                "updatedAt" to newProfile.updatedAt
            )
            docRef.set(map).await()
        } catch (e: Exception) {
            Log.e(TAG, "Error creating user profile in Firestore: ${e.message}", e)
        }

        _currentUserProfile.value = newProfile
        return newProfile
    }

    suspend fun signInWithGoogle(context: Context): Result<FirebaseUser> {
        return try {
            val serverClientId = try {
                context.getString(R.string.default_web_client_id)
            } catch (e: Exception) {
                // Fallback client id from google-services.json
                "429021304482-drdbgn85abhv0f3gsqs2j328seqmtbed.apps.googleusercontent.com"
            }

            val googleIdOption = GetGoogleIdOption.Builder()
                .setFilterByAuthorizedAccounts(false)
                .setServerClientId(serverClientId)
                .setAutoSelectEnabled(false)
                .build()

            val request = GetCredentialRequest.Builder()
                .addCredentialOption(googleIdOption)
                .build()

            val credentialManager = CredentialManager.create(context)
            val response: GetCredentialResponse = credentialManager.getCredential(
                request = request,
                context = context
            )

            val credential = response.credential
            if (credential is CustomCredential &&
                credential.type == GoogleIdTokenCredential.TYPE_GOOGLE_ID_TOKEN_CREDENTIAL
            ) {
                val googleIdTokenCredential = GoogleIdTokenCredential.createFrom(credential.data)
                val idToken = googleIdTokenCredential.idToken
                val authCredential = GoogleAuthProvider.getCredential(idToken, null)
                val authResult = auth.signInWithCredential(authCredential).await()
                val user = authResult.user ?: throw IllegalStateException("Firebase user is null after sign in")
                fetchOrCreateUserProfile(user)
                Result.success(user)
            } else {
                Result.failure(IllegalStateException("Unexpected credential type: ${credential.type}"))
            }
        } catch (e: GetCredentialCancellationException) {
            Log.d(TAG, "User cancelled sign in")
            Result.failure(e)
        } catch (e: androidx.credentials.exceptions.NoCredentialException) {
            Log.w(TAG, "No Google credentials available on device/emulator: ${e.message}")
            Result.failure(e)
        } catch (e: Exception) {
            Log.e(TAG, "Sign in failed: ${e.message}", e)
            Result.failure(e)
        }
    }

    /**
     * Demo Authority presentation toggle:
     * Switches the active session profile to Authority role for testing & demonstration
     * without compromising server security.
     */
    fun toggleDemoAuthority(enable: Boolean) {
        _isDemoAuthority.value = enable
        val current = _currentUserProfile.value
        if (current != null) {
            _currentUserProfile.value = current.copy(
                role = if (enable) "authority" else "public",
                department = if (enable) "Municipal Works & Sanitation" else "All Departments",
                jurisdiction = if (enable) "Ward 14 - East District" else "Central Zone"
            )
        } else if (enable) {
            _currentUserProfile.value = UserProfile(
                uid = auth.currentUser?.uid ?: "demo_authority_uid",
                name = "Inspector Rajesh Kumar (Demo Authority)",
                email = "rajesh.kumar@municipal.gov.in",
                role = "authority",
                accountStatus = "active",
                department = "Municipal Works & Sanitation",
                jurisdiction = "Ward 14 - East District"
            )
        } else {
            _currentUserProfile.value = UserProfile(
                uid = auth.currentUser?.uid ?: "demo_citizen_uid",
                name = "Citizen",
                email = "citizen@civicsvoice.org",
                role = "public",
                accountStatus = "active",
                department = "Citizen Services",
                jurisdiction = "Ward 14 - East District"
            )
        }
    }

    fun signOut() {
        _isDemoAuthority.value = false
        _currentUserProfile.value = null
        auth.signOut()
    }
}
