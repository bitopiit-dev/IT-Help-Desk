package com.company.ithelpdesk.domain.model

/**
 * BGLITDesk has two roles with two completely separate apps inside one APK:
 *  - USER  : raises tickets, chats with IT, follows progress.
 *  - ADMIN : approves sign-ups, works every ticket, manages people and settings.
 * Legacy values (EMPLOYEE, TECHNICIAN) from earlier builds read as USER.
 */
enum class Role(val label: String) {
    USER("User"),
    ADMIN("Admin");

    val isAdmin: Boolean get() = this == ADMIN

    /** Kept for the shared workflow/dashboard logic: staff == admin. */
    val isStaff: Boolean get() = isAdmin

    companion object {
        fun from(value: String?): Role = if (value == ADMIN.name) ADMIN else USER
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
) {
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
