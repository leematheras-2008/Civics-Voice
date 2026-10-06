package com.example

import com.example.data.model.Complaint
import com.example.data.model.ComplaintCategories
import com.example.data.model.ComplaintPriority
import com.example.data.model.ComplaintStatus
import com.example.data.model.UserProfile
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class CivicPulseUnitTest {

    @Test
    fun testDepartmentMappingForCategories() {
        assertEquals(
            "Electricity Board (EB)",
            ComplaintCategories.suggestDepartment("Electricity / EB")
        )
        assertEquals(
            "Water Supply & Drainage Dept",
            ComplaintCategories.suggestDepartment("Water Supply & Leakage")
        )
        assertEquals(
            "Sanitation & Solid Waste Dept",
            ComplaintCategories.suggestDepartment("Garbage & Solid Waste")
        )
        assertEquals(
            "Highways & Public Works (PWD)",
            ComplaintCategories.suggestDepartment("Roads & Potholes")
        )
    }

    @Test
    fun testComplaintStatusEnumTransitions() {
        val submitted = ComplaintStatus.SUBMITTED
        assertEquals("Submitted", submitted.displayName)
        assertEquals(0, submitted.stepIndex)

        val resolved = ComplaintStatus.RESOLVED
        assertEquals("Resolved", resolved.displayName)
        assertEquals(4, resolved.stepIndex)

        val verification = ComplaintStatus.CITIZEN_VERIFICATION
        assertEquals("Citizen Verification", verification.displayName)
        assertEquals(5, verification.stepIndex)
    }

    @Test
    fun testUserProfileAuthorityVerification() {
        val citizen = UserProfile(role = "public", accountStatus = "active")
        assertFalse(citizen.isAuthority)

        val authority = UserProfile(role = "authority", accountStatus = "active")
        assertTrue(authority.isAuthority)

        val disabledAuthority = UserProfile(role = "authority", accountStatus = "disabled")
        assertFalse(disabledAuthority.isAuthority)
    }

    @Test
    fun testComplaintPriorityParsing() {
        val criticalComplaint = Complaint(priority = "CRITICAL")
        assertEquals(ComplaintPriority.CRITICAL, criticalComplaint.priorityEnum)

        val invalidPriorityComplaint = Complaint(priority = "UNKNOWN")
        assertEquals(ComplaintPriority.MEDIUM, invalidPriorityComplaint.priorityEnum)
    }
}
