package com.company.ithelpdesk.domain

import com.company.ithelpdesk.domain.logic.NotificationRules
import com.company.ithelpdesk.domain.logic.TicketNumber
import com.company.ithelpdesk.domain.model.AppNotification
import com.company.ithelpdesk.domain.model.Role
import com.company.ithelpdesk.domain.model.TicketStatus
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class RolesAndNotificationsTest {

    @Test fun `only ADMIN maps to admin, legacy roles become user`() {
        assertEquals(Role.ADMIN, Role.from("ADMIN"))
        assertEquals(Role.USER, Role.from("USER"))
        assertEquals(Role.USER, Role.from("EMPLOYEE"))
        assertEquals(Role.USER, Role.from("TECHNICIAN"))
        assertEquals(Role.USER, Role.from(null))
    }

    @Test fun `initials and first name`() {
        assertEquals("RU", employee.initials)
        assertEquals("Rahim", employee.firstName)
    }

    @Test fun `ticket numbers are zero padded per year`() {
        assertEquals("BGL-2026-0007", TicketNumber.format(2026, 7))
        assertEquals("BGL-2026-12345", TicketNumber.format(2026, 12345))
        assertEquals("tickets-2026", TicketNumber.counterId(2026))
    }

    @Test fun `new ticket goes to all admins`() {
        val d = NotificationRules.ticketCreated(ticket())
        assertEquals(AppNotification.AUDIENCE_ADMINS, d.audience)
    }

    @Test fun `admin status change notifies the requester only`() {
        val drafts = NotificationRules.statusChanged(ticket(TicketStatus.IN_PROGRESS, assigneeId = admin.uid), TicketStatus.RESOLVED, admin)
        assertEquals(listOf(employee.uid), drafts.map { it.audience })
    }

    @Test fun `requester action goes to the assignee, or every admin when unassigned`() {
        val assigned = NotificationRules.statusChanged(ticket(TicketStatus.ASSIGNED, assigneeId = tech.uid), TicketStatus.CANCELLED, employee)
        assertEquals(listOf(tech.uid), assigned.map { it.audience })
        val unassigned = NotificationRules.statusChanged(ticket(TicketStatus.NEW), TicketStatus.CANCELLED, employee)
        assertEquals(listOf(AppNotification.AUDIENCE_ADMINS), unassigned.map { it.audience })
    }

    @Test fun `picking up a ticket yourself does not notify yourself`() {
        val drafts = NotificationRules.assigned(ticket(), admin, admin)
        assertEquals(listOf(employee.uid), drafts.map { it.audience })
        val byOther = NotificationRules.assigned(ticket(), tech, admin)
        assertEquals(listOf(employee.uid, tech.uid), byOther.map { it.audience })
    }

    @Test fun `internal notes never reach the requester`() {
        val t = ticket(TicketStatus.IN_PROGRESS, assigneeId = tech.uid)
        assertEquals(listOf(tech.uid), NotificationRules.messagePosted(t, admin, "check driver", internal = true).map { it.audience })
        assertTrue(NotificationRules.messagePosted(t, tech, "my own note", internal = true).isEmpty())
        assertEquals(listOf(employee.uid), NotificationRules.messagePosted(t, tech, "on my way", internal = false).map { it.audience })
        assertEquals(listOf(tech.uid), NotificationRules.messagePosted(t, employee, "thanks", internal = false).map { it.audience })
    }
}
