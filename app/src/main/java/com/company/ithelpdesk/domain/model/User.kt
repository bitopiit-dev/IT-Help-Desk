// © 2026 Khondaker Arifur Rahman. All Rights Reserved.
// BGL-IT Help — see COPYRIGHT.md. Unauthorized copying, modification or redistribution is prohibited.

package com.company.ithelpdesk.domain.model

/**
 * BGL-IT Help has two roles with two completely separate apps inside one APK:
 *  - USER  : raises tickets, chats with IT, follows progress.
 *  - ADMIN : approves sign-ups, works every ticket, manages people and settings.
 * Legacy values (EMPLOYEE, TECHNICIAN) from earlier builds read as USER.
 */
enum class Role(val label: String) {
    USER("User"),
    ADMIN("Admin"),
    SUPER_ADMIN("Super Admin");

    val isAdmin: Boolean get() = this == ADMIN || this == SUPER_ADMIN
    val isSuperAdmin: Boolean get() = this == SUPER_ADMIN

    /** Kept for the shared workflow/dashboard logic: staff == admin. */
    val isStaff: Boolean get() = isAdmin

    companion object {
        const val SUPER_ADMIN_EMAIL = "bgl.it@bitopibd.com"

        fun from(value: String?): Role = when (value?.uppercase()) {
            "SUPER_ADMIN" -> SUPER_ADMIN
            "ADMIN" -> ADMIN
            else -> USER
        }
    }
}

enum class UserPriority(val label: String, val emoji: String) {
    STANDARD("Standard", "👤"),
    VIP("VIP", "⭐"),
    HIGH("High Priority", "⚡"),
    LOW("Low Priority", "🐢");

    companion object {
        fun from(value: String?): UserPriority = entries.firstOrNull { it.name.equals(value, ignoreCase = true) } ?: STANDARD
    }
}

data class UserProfile(
    val uid: String,
    val name: String,
    val email: String,
    val employeeId: String = "",
    val department: String = "",
    val phone: String = "",
    val location: String = "",
    val role: Role = Role.USER,
    val isApproved: Boolean = false,
    val isActive: Boolean = true,
    val createdAt: Long = 0L,
    val approvedAt: Long? = null,
    val approvedByName: String = "",
    /** When the profile photo last changed (0 = no photo). Photos live in `avatars/{uid}`. */
    val photoVersion: Long = 0L,
    /** Custom key-value sub-sections added by admins (e.g. "Designation" -> "Sr. Executive"). */
    val extraFields: Map<String, String> = emptyMap(),
    /** User Priority / VIP status, managed and visible exclusively to Super Admin. */
    val userPriority: UserPriority = UserPriority.STANDARD,
) {
    /** Permanent Super Admin who can grant admin roles and cannot be demoted or blocked. */
    val isSuperAdmin: Boolean get() = role == Role.SUPER_ADMIN || email.equals(Role.SUPER_ADMIN_EMAIL, ignoreCase = true)

    val firstName: String get() = name.trim().split(Regex("\\s+")).firstOrNull().orEmpty().ifBlank { email.substringBefore('@') }

    val initials: String
        get() {
            val parts = name.trim().split(Regex("\\s+")).filter { it.isNotBlank() }
            val letters = when {
                parts.size >= 2 -> "${parts.first().first()}${parts.last().first()}"
                parts.size == 1 -> parts.first().take(2)
                else -> email.take(2)
            }
            return letters.uppercase()
        }

    /** Sign-up waiting for an admin decision. */
    val isPending: Boolean get() = isActive && !isApproved

    /** Declined sign-up or deactivated account. */
    val isBlocked: Boolean get() = !isActive
}

data class AuthUser(val uid: String, val email: String)
