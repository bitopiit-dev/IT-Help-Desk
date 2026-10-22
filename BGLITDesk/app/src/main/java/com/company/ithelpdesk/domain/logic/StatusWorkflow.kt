package com.company.ithelpdesk.domain.logic

import com.company.ithelpdesk.domain.model.Ticket
import com.company.ithelpdesk.domain.model.TicketStatus
import com.company.ithelpdesk.domain.model.TicketStatus.ASSIGNED
import com.company.ithelpdesk.domain.model.TicketStatus.CANCELLED
import com.company.ithelpdesk.domain.model.TicketStatus.CLOSED
import com.company.ithelpdesk.domain.model.TicketStatus.IN_PROGRESS
import com.company.ithelpdesk.domain.model.TicketStatus.NEW
import com.company.ithelpdesk.domain.model.TicketStatus.ON_HOLD_PARTS
import com.company.ithelpdesk.domain.model.TicketStatus.ON_HOLD_USER
import com.company.ithelpdesk.domain.model.TicketStatus.REOPENED
import com.company.ithelpdesk.domain.model.TicketStatus.RESOLVED
import com.company.ithelpdesk.domain.model.UserProfile

/**
 * Single source of truth for which status changes are allowed.
 *
 * New → Assigned → In Progress → On Hold (user/parts) → Resolved → Closed
 * Resolved/Closed → Reopened → (Assigned) → In Progress
 * Cancelled is reachable from New or Assigned.
 *
 * ASSIGNED is entered only through the "Pick up" / "Assign" actions, never as a plain transition.
 */
object StatusWorkflow {

    const val REOPEN_WINDOW_MS: Long = 7L * 24 * 60 * 60 * 1000

    val staffTransitions: Map<TicketStatus, Set<TicketStatus>> = mapOf(
        NEW to setOf(ASSIGNED, CANCELLED),
        ASSIGNED to setOf(IN_PROGRESS, CANCELLED),
        IN_PROGRESS to setOf(ON_HOLD_USER, ON_HOLD_PARTS, RESOLVED),
        ON_HOLD_USER to setOf(IN_PROGRESS, RESOLVED),
        ON_HOLD_PARTS to setOf(IN_PROGRESS, RESOLVED),
        RESOLVED to setOf(CLOSED, REOPENED),
        CLOSED to setOf(REOPENED),
        REOPENED to setOf(ASSIGNED, IN_PROGRESS),
        CANCELLED to emptySet(),
    )

    fun isValidStaffTransition(from: TicketStatus, to: TicketStatus): Boolean =
        to in staffTransitions[from].orEmpty()

    fun canReopen(ticket: Ticket, now: Long): Boolean {
        val finishedAt = ticket.resolvedAt ?: ticket.closedAt ?: return false
        return now - finishedAt <= REOPEN_WINDOW_MS
    }

    /** Status buttons to show to [actor] for [ticket]. */
    fun allowedTransitions(ticket: Ticket, actor: UserProfile, now: Long): List<TicketStatus> {
        val result = linkedSetOf<TicketStatus>()
        if (actor.role.isStaff) {
            result += staffTransitions[ticket.status].orEmpty()
            result -= ASSIGNED
            if (ticket.assigneeId == null) result -= IN_PROGRESS
        }
        if (ticket.requesterId == actor.uid) {
            if (ticket.status == NEW || ticket.status == ASSIGNED) result += CANCELLED
            if (ticket.status == RESOLVED) result += CLOSED
            if ((ticket.status == RESOLVED || ticket.status == CLOSED) && canReopen(ticket, now)) {
                result += REOPENED
            }
        }
        return result.toList()
    }

    /** Field changes that accompany a status change. Times use the device clock only for on-hold maths. */
    data class Effects(
        val onHoldSince: Long?,
        val addOnHoldMs: Long,
        val markFirstResponse: Boolean,
        val markResolved: Boolean,
        val clearResolved: Boolean,
        val markClosed: Boolean,
        val incrementReopen: Boolean,
        val touchesHold: Boolean,
    )

    fun effectsOf(ticket: Ticket, to: TicketStatus, byStaff: Boolean, now: Long): Effects {
        val from = ticket.status
        val leavingHold = from.isOnHold && !to.isOnHold
        val addHold = if (leavingHold && ticket.onHoldSince != null) {
            (now - ticket.onHoldSince).coerceAtLeast(0)
        } else {
            0L
        }
        val holdSince = when {
            to.isOnHold && from.isOnHold -> ticket.onHoldSince ?: now
            to.isOnHold -> now
            else -> null
        }
        return Effects(
            onHoldSince = holdSince,
            addOnHoldMs = addHold,
            markFirstResponse = byStaff && ticket.firstResponseAt == null,
            markResolved = to == RESOLVED,
            clearResolved = to == REOPENED,
            markClosed = to == CLOSED || to == CANCELLED,
            incrementReopen = to == REOPENED,
            touchesHold = from.isOnHold || to.isOnHold,
        )
    }
}
