package com.company.ithelpdesk.domain

import com.company.ithelpdesk.domain.logic.StatusWorkflow
import com.company.ithelpdesk.domain.model.TicketStatus.ASSIGNED
import com.company.ithelpdesk.domain.model.TicketStatus.CANCELLED
import com.company.ithelpdesk.domain.model.TicketStatus.CLOSED
import com.company.ithelpdesk.domain.model.TicketStatus.IN_PROGRESS
import com.company.ithelpdesk.domain.model.TicketStatus.NEW
import com.company.ithelpdesk.domain.model.TicketStatus.ON_HOLD_PARTS
import com.company.ithelpdesk.domain.model.TicketStatus.ON_HOLD_USER
import com.company.ithelpdesk.domain.model.TicketStatus.REOPENED
import com.company.ithelpdesk.domain.model.TicketStatus.RESOLVED
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class StatusWorkflowTest {

    @Test fun `happy path transitions are valid for staff`() {
        val path = listOf(NEW, ASSIGNED, IN_PROGRESS, ON_HOLD_USER, IN_PROGRESS, RESOLVED, CLOSED, REOPENED, IN_PROGRESS)
        path.zipWithNext().forEach { (a, b) -> assertTrue("$a -> $b", StatusWorkflow.isValidStaffTransition(a, b)) }
    }

    @Test fun `invalid jumps are rejected`() {
        assertFalse(StatusWorkflow.isValidStaffTransition(NEW, RESOLVED))
        assertFalse(StatusWorkflow.isValidStaffTransition(NEW, IN_PROGRESS))
        assertFalse(StatusWorkflow.isValidStaffTransition(CANCELLED, IN_PROGRESS))
        assertFalse(StatusWorkflow.isValidStaffTransition(IN_PROGRESS, CANCELLED))
        assertFalse(StatusWorkflow.isValidStaffTransition(CLOSED, IN_PROGRESS))
    }

    @Test fun `technician cannot start work on unassigned ticket and never sees ASSIGNED`() {
        val allowed = StatusWorkflow.allowedTransitions(ticket(NEW), tech, now = 0)
        assertEquals(listOf(CANCELLED), allowed)
        val reopened = StatusWorkflow.allowedTransitions(ticket(REOPENED), tech, now = 0)
        assertFalse(IN_PROGRESS in reopened)
    }

    @Test fun `assigned technician can start work`() {
        val allowed = StatusWorkflow.allowedTransitions(ticket(ASSIGNED, assigneeId = tech.uid), tech, now = 0)
        assertTrue(IN_PROGRESS in allowed)
        assertTrue(CANCELLED in allowed)
    }

    @Test fun `requester can cancel only before work starts`() {
        assertEquals(listOf(CANCELLED), StatusWorkflow.allowedTransitions(ticket(NEW), employee, 0))
        assertEquals(listOf(CANCELLED), StatusWorkflow.allowedTransitions(ticket(ASSIGNED, assigneeId = "tech"), employee, 0))
        assertTrue(StatusWorkflow.allowedTransitions(ticket(IN_PROGRESS, assigneeId = "tech"), employee, 0).isEmpty())
        assertTrue(StatusWorkflow.allowedTransitions(ticket(ON_HOLD_PARTS, assigneeId = "tech"), employee, 0).isEmpty())
    }

    @Test fun `requester can confirm or reopen within 7 days only`() {
        val resolvedAt = 10 * HOUR
        val t = ticket(RESOLVED) { copy(resolvedAt = resolvedAt) }
        assertEquals(listOf(CLOSED, REOPENED), StatusWorkflow.allowedTransitions(t, employee, resolvedAt + HOUR))
        val late = resolvedAt + StatusWorkflow.REOPEN_WINDOW_MS + 1
        assertEquals(listOf(CLOSED), StatusWorkflow.allowedTransitions(t, employee, late))
        val closed = ticket(CLOSED) { copy(resolvedAt = resolvedAt) }
        assertTrue(StatusWorkflow.allowedTransitions(closed, employee, late).isEmpty())
    }

    @Test fun `going on hold records start, leaving adds elapsed hold time`() {
        val start = StatusWorkflow.effectsOf(ticket(IN_PROGRESS, assigneeId = "tech"), ON_HOLD_PARTS, byStaff = true, now = 5 * HOUR)
        assertEquals(5 * HOUR, start.onHoldSince)
        assertEquals(0L, start.addOnHoldMs)

        val onHold = ticket(ON_HOLD_PARTS, assigneeId = "tech") { copy(onHoldSince = 5 * HOUR) }
        val resume = StatusWorkflow.effectsOf(onHold, IN_PROGRESS, byStaff = true, now = 7 * HOUR)
        assertEquals(null, resume.onHoldSince)
        assertEquals(2 * HOUR, resume.addOnHoldMs)
        assertTrue(resume.touchesHold)
    }

    @Test fun `first staff action marks first response, requester actions do not`() {
        assertTrue(StatusWorkflow.effectsOf(ticket(ASSIGNED, assigneeId = "tech"), IN_PROGRESS, true, 0).markFirstResponse)
        assertFalse(StatusWorkflow.effectsOf(ticket(NEW), CANCELLED, false, 0).markFirstResponse)
        val responded = ticket(ASSIGNED, assigneeId = "tech") { copy(firstResponseAt = 1L) }
        assertFalse(StatusWorkflow.effectsOf(responded, IN_PROGRESS, true, 0).markFirstResponse)
    }

    @Test fun `reopen clears resolution time and counts reopen`() {
        val fx = StatusWorkflow.effectsOf(ticket(RESOLVED) { copy(resolvedAt = 1L) }, REOPENED, false, 2L)
        assertTrue(fx.clearResolved)
        assertTrue(fx.incrementReopen)
        assertFalse(fx.markResolved)
    }
}
