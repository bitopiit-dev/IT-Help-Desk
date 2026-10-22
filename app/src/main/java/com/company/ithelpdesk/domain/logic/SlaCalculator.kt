// © 2026 Khondaker Arifur Rahman. All Rights Reserved.
// BGL-IT Help — see COPYRIGHT.md. Unauthorized copying, modification or redistribution is prohibited.

package com.company.ithelpdesk.domain.logic

import com.company.ithelpdesk.domain.model.Priority
import com.company.ithelpdesk.domain.model.SlaSettings
import com.company.ithelpdesk.domain.model.SlaState
import com.company.ithelpdesk.domain.model.Ticket
import com.company.ithelpdesk.domain.model.TicketStatus

/**
 * SLA rules (mirrored in functions/src/sla.ts):
 *  - Response and resolve due times are set from createdAt + the priority's targets.
 *  - The resolve clock pauses while the ticket is On Hold (due time shifts by time on hold).
 *  - "At risk" once 75 % of the allowed time is used; "Breached" at 100 %.
 *  - A late first response breaches the SLA even if the fix is on time.
 */
object SlaCalculator {
    const val AT_RISK_FRACTION = 0.75

    fun dueDates(createdAt: Long, priority: Priority, settings: SlaSettings): Pair<Long, Long> {
        val target = settings.target(priority)
        return (createdAt + target.respondMinutes * 60_000L) to (createdAt + target.resolveMinutes * 60_000L)
    }

    fun pausedMs(t: Ticket, now: Long): Long = t.onHoldTotalMs + TicketTimes.currentHoldMs(t, now)

    private fun responseLate(t: Ticket): Boolean {
        val due = t.slaResponseDueAt ?: return false
        val first = t.firstResponseAt ?: return false
        return first > due
    }

    fun evaluate(t: Ticket, now: Long): SlaState {
        val resolveDue = t.slaResolveDueAt ?: return t.slaState
        if (t.status == TicketStatus.CANCELLED) return t.slaState
        val effectiveResolveDue = resolveDue + pausedMs(t, now)

        val resolvedAt = t.resolvedAt
        if (!t.status.isOpen && resolvedAt != null) {
            return if (resolvedAt <= effectiveResolveDue && !responseLate(t)) SlaState.MET else SlaState.BREACHED
        }
        if (responseLate(t)) return SlaState.BREACHED

        var worst = fraction(t.createdAt, effectiveResolveDue, now)
        val respondDue = t.slaResponseDueAt
        if (t.firstResponseAt == null && respondDue != null) {
            worst = maxOf(worst, fraction(t.createdAt, respondDue, now))
        }
        return when {
            worst >= 1.0 -> SlaState.BREACHED
            worst >= AT_RISK_FRACTION -> SlaState.AT_RISK
            else -> SlaState.ON_TRACK
        }
    }

    /** Time left until the resolve deadline (negative when overdue). */
    fun remainingMs(t: Ticket, now: Long): Long? {
        val due = t.slaResolveDueAt ?: return null
        return due + pausedMs(t, now) - now
    }

    private fun fraction(start: Long, due: Long, now: Long): Double {
        val total = (due - start).coerceAtLeast(1)
        return (now - start).toDouble() / total
    }
}
