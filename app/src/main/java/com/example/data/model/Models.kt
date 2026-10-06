package com.example.data.model

import com.google.firebase.Timestamp

enum class UserRole {
    PUBLIC,
    AUTHORITY
}

enum class AccountStatus {
    ACTIVE,
    DISABLED
}

data class UserProfile(
    val uid: String = "",
    val name: String = "",
    val email: String = "",
    val phone: String = "",
    val role: String = "public",
    val accountStatus: String = "active",
    val department: String = "All Departments",
    val jurisdiction: String = "Central Zone",
    val preferredLanguage: String = "en",
    val createdAt: Timestamp = Timestamp.now(),
    val updatedAt: Timestamp = Timestamp.now()
) {
    val isAuthority: Boolean
        get() = role.equals("authority", ignoreCase = true) && accountStatus.equals("active", ignoreCase = true)
}

enum class ComplaintPriority {
    LOW,
    MEDIUM,
    HIGH,
    CRITICAL
}

enum class ComplaintStatus(val displayName: String, val stepIndex: Int) {
    SUBMITTED("Submitted", 0),
    UNDER_REVIEW("Under Review", 1),
    ASSIGNED("Assigned", 2),
    WORK_IN_PROGRESS("Work in Progress", 3),
    RESOLVED("Resolved", 4),
    CITIZEN_VERIFICATION("Citizen Verification", 5),
    CLOSED("Closed", 6),
    REOPENED("Reopened", 1)
}

data class Complaint(
    val complaintId: String = "",
    val citizenId: String = "",
    val citizenName: String = "",
    val description: String = "",
    val language: String = "en",
    val category: String = "General",
    val department: String = "Municipal Corporation",
    val jurisdiction: String = "Ward 12",
    val priority: String = "MEDIUM",
    val status: String = "SUBMITTED",
    val latitude: Double = 0.0,
    val longitude: Double = 0.0,
    val address: String = "",
    val aiSummary: String = "",
    val aiConfidence: Double = 0.95,
    val aiKeywords: List<String> = emptyList(),
    val aiProcessingStatus: String = "completed",
    val supportCount: Int = 1,
    val assignedAuthorityId: String = "",
    val assignedAuthorityName: String = "",
    val resolutionProofNote: String = "",
    val citizenFeedback: String = "",
    val imageUri: String = "",
    val createdAt: Timestamp = Timestamp.now(),
    val updatedAt: Timestamp = Timestamp.now()
) {
    val statusEnum: ComplaintStatus
        get() = try {
            ComplaintStatus.valueOf(status)
        } catch (e: Exception) {
            ComplaintStatus.SUBMITTED
        }

    val priorityEnum: ComplaintPriority
        get() = try {
            ComplaintPriority.valueOf(priority)
        } catch (e: Exception) {
            ComplaintPriority.MEDIUM
        }
}

data class TimelineEvent(
    val updateId: String = "",
    val status: String = "",
    val title: String = "",
    val description: String = "",
    val actorId: String = "",
    val actorRole: String = "",
    val timestamp: Timestamp = Timestamp.now()
)

object ComplaintCategories {
    val ALL = listOf(
        "Electricity / EB",
        "Water Supply & Leakage",
        "Drainage & Sewage Overflow",
        "Garbage & Solid Waste",
        "Roads & Potholes",
        "Streetlights & Dark Spots",
        "Flooding & Waterlogging",
        "Health & Sanitation",
        "Domestic & Stray Animals",
        "Fallen Trees & Hazardous Branches",
        "Traffic & Road Safety",
        "Police & Public Safety",
        "Illegal Construction & Encroachment",
        "Public Toilets & Hygiene",
        "Public Transport & Bus Shelters",
        "Parks & Open Spaces"
    )

    fun suggestDepartment(category: String): String {
        return when {
            category.contains("Electricity", ignoreCase = true) || category.contains("Streetlight", ignoreCase = true) -> "Electricity Board (EB)"
            category.contains("Water", ignoreCase = true) || category.contains("Drainage", ignoreCase = true) || category.contains("Flooding", ignoreCase = true) -> "Water Supply & Drainage Dept"
            category.contains("Garbage", ignoreCase = true) || category.contains("Sanitation", ignoreCase = true) || category.contains("Toilet", ignoreCase = true) -> "Sanitation & Solid Waste Dept"
            category.contains("Road", ignoreCase = true) -> "Highways & Public Works (PWD)"
            category.contains("Animal", ignoreCase = true) -> "Veterinary & Animal Welfare"
            category.contains("Tree", ignoreCase = true) -> "Parks & Urban Forestry Dept"
            category.contains("Traffic", ignoreCase = true) || category.contains("Police", ignoreCase = true) -> "Traffic & Public Safety Dept"
            category.contains("Construction", ignoreCase = true) -> "Town Planning & Building Dept"
            else -> "Municipal Administration"
        }
    }
}
