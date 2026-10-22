// © 2026 Khondaker Arifur Rahman. All Rights Reserved.
// BGL-IT Help — see COPYRIGHT.md. Unauthorized copying, modification or redistribution is prohibited.

package com.company.ithelpdesk.domain.model

/**
 * In-app notification stored in Firestore `notifications/{id}`.
 * [audience] is a user's uid, or [AUDIENCE_ADMINS] for every admin.
 * Works on the free Spark plan: the app writes these itself, no Cloud Functions needed.
 */
data class AppNotification(
    val id: String,
    val audience: String,
    val title: String,
    val body: String,
    val ticketId: String?,
    val kind: String,
    val createdBy: String,
    val createdAt: Long,
    val readBy: List<String> = emptyList(),
) {
    fun isReadBy(uid: String): Boolean = uid in readBy || createdBy == uid

    companion object {
        const val AUDIENCE_ADMINS = "ADMINS"

        const val KIND_SIGNUP = "SIGNUP"
        const val KIND_ACCOUNT = "ACCOUNT"
        const val KIND_TICKET = "TICKET"
        const val KIND_STATUS = "STATUS"
        const val KIND_ASSIGN = "ASSIGN"
        const val KIND_MESSAGE = "MESSAGE"
    }
}
