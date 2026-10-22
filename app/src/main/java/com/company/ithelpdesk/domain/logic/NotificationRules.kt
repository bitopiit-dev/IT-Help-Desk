// © 2026 Khondaker Arifur Rahman. All Rights Reserved.
// BGL-IT Help — see COPYRIGHT.md. Unauthorized copying, modification or redistribution is prohibited.

package com.company.ithelpdesk.domain.logic

import com.company.ithelpdesk.domain.model.AppNotification
import com.company.ithelpdesk.domain.model.Ticket
import com.company.ithelpdesk.domain.model.TicketStatus
import com.company.ithelpdesk.domain.model.UserProfile

/** A notification to write; the repository adds ids and timestamps. */
data class NotificationDraft(
    val audience: String,
    val title: String,
    val body: String,
    val ticketId: String?,
    val kind: String,
)

/**
 * Who hears about what. Written by the app itself (no Cloud Functions on the Spark plan):
 * the person who acts writes a notification document for the people who should know.
 * Nobody is notified about their own action.
 */
object NotificationRules {
    private const val ADMINS = AppNotification.AUDIENCE_ADMINS

    fun signupRequested(profile: UserProfile): NotificationDraft = NotificationDraft(
        audience = ADMINS,
        title = "New sign-up request",
        body = "${profile.name} (${profile.email}) wants to join BGL-IT Help.",
        ticketId = null,
        kind = AppNotification.KIND_SIGNUP,
    )

    fun accountApproved(uid: String): NotificationDraft = NotificationDraft(
        audience = uid,
        title = "You're in!",
        body = "Your BGL-IT Help account was approved. Raise your first ticket any time.",
        ticketId = null,
        kind = AppNotification.KIND_ACCOUNT,
    )

    fun ticketCreated(t: Ticket): NotificationDraft = NotificationDraft(
        audience = ADMINS,
        title = "New ${t.priority.label.lowercase()} ticket ${t.displayNumber}",
        body = "${t.requesterName}: ${t.title}",
        ticketId = t.id,
        kind = AppNotification.KIND_TICKET,
    )

    fun statusChanged(t: Ticket, to: TicketStatus, actor: UserProfile): List<NotificationDraft> {
        val drafts = mutableListOf<NotificationDraft>()
        if (actor.uid != t.requesterId) {
            drafts += NotificationDraft(
                audience = t.requesterId,
                title = "${t.displayNumber}: ${userFacingStatus(to)}",
                body = when (to) {
                    TicketStatus.RESOLVED -> "IT marked \"${t.title}\" as fixed. Please confirm it works."
                    TicketStatus.ON_HOLD_USER -> "IT needs something from you on \"${t.title}\"."
                    else -> t.title
                },
                ticketId = t.id,
                kind = AppNotification.KIND_STATUS,
            )
        } else {
            drafts += NotificationDraft(
                audience = t.assigneeId ?: ADMINS,
                title = "${t.displayNumber}: ${to.label} by requester",
                body = "${actor.name}: ${t.title}",
                ticketId = t.id,
                kind = AppNotification.KIND_STATUS,
            )
        }
        return drafts.filter { it.audience != actor.uid }
    }

    fun assigned(t: Ticket, assignee: UserProfile, actor: UserProfile): List<NotificationDraft> = listOfNotNull(
        NotificationDraft(
            audience = t.requesterId,
            title = "${t.displayNumber}: ${assignee.firstName} is on it",
            body = t.title,
            ticketId = t.id,
            kind = AppNotification.KIND_ASSIGN,
        ),
        if (assignee.uid != actor.uid) {
            NotificationDraft(
                audience = assignee.uid,
                title = "Assigned to you: ${t.displayNumber}",
                body = "${t.requesterName}: ${t.title}",
                ticketId = t.id,
                kind = AppNotification.KIND_ASSIGN,
            )
        } else {
            null
        },
    ).filter { it.audience != actor.uid }

    fun messagePosted(t: Ticket, actor: UserProfile, text: String, internal: Boolean): List<NotificationDraft> {
        val preview = text.trim().ifBlank { "Sent a photo" }.take(140)
        val audience = when {
            internal -> t.assigneeId ?: return emptyList()
            actor.uid == t.requesterId -> t.assigneeId ?: ADMINS
            else -> t.requesterId
        }
        if (audience == actor.uid) return emptyList()
        return listOf(
            NotificationDraft(
                audience = audience,
                title = "${actor.firstName} · ${t.displayNumber}",
                body = preview,
                ticketId = t.id,
                kind = AppNotification.KIND_MESSAGE,
            ),
        )
    }

    fun rated(t: Ticket, actor: UserProfile, stars: Int): List<NotificationDraft> {
        val audience = t.assigneeId ?: ADMINS
        if (audience == actor.uid) return emptyList()
        return listOf(
            NotificationDraft(
                audience = audience,
                title = "${"★".repeat(stars.coerceIn(1, 5))} for ${t.displayNumber}",
                body = "${actor.firstName} rated \"${t.title}\"",
                ticketId = t.id,
                kind = AppNotification.KIND_STATUS,
            ),
        )
    }

    /** Status wording shown to the person who raised the ticket. */
    fun userFacingStatus(s: TicketStatus): String = when (s) {
        TicketStatus.NEW -> "Received"
        TicketStatus.ASSIGNED -> "Assigned"
        TicketStatus.IN_PROGRESS -> "Work started"
        TicketStatus.ON_HOLD_USER -> "Waiting for you"
        TicketStatus.ON_HOLD_PARTS -> "Waiting for parts"
        TicketStatus.RESOLVED -> "Fixed"
        TicketStatus.CLOSED -> "Closed"
        TicketStatus.REOPENED -> "Reopened"
        TicketStatus.CANCELLED -> "Cancelled"
    }
}
