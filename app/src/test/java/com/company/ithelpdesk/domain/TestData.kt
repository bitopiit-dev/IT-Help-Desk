// © 2026 Khondaker Arifur Rahman. All Rights Reserved.
// BGL-IT Help — see COPYRIGHT.md. Unauthorized copying, modification or redistribution is prohibited.

package com.company.ithelpdesk.domain

import com.company.ithelpdesk.domain.model.Priority
import com.company.ithelpdesk.domain.model.Role
import com.company.ithelpdesk.domain.model.Ticket
import com.company.ithelpdesk.domain.model.TicketStatus
import com.company.ithelpdesk.domain.model.UserProfile

const val MIN = 60_000L
const val HOUR = 60 * MIN

val employee = UserProfile(uid = "emp", name = "Rahim Uddin", email = "rahim@gmail.com", role = Role.USER, isApproved = true)
/** A second admin who works tickets (there is no separate technician role any more). */
val tech = UserProfile(uid = "tech", name = "Karim", email = "karim@bitopibd.com", role = Role.ADMIN, isApproved = true)
val admin = UserProfile(uid = "adm", name = "Nadia", email = "nadia@bitopibd.com", role = Role.ADMIN, isApproved = true)

fun ticket(
    status: TicketStatus = TicketStatus.NEW,
    priority: Priority = Priority.MEDIUM,
    createdAt: Long = 0L,
    assigneeId: String? = null,
    block: Ticket.() -> Ticket = { this },
): Ticket = Ticket(
    id = "t1",
    title = "Printer jam",
    description = "Paper stuck in tray 2",
    category = "Printer",
    priority = priority,
    status = status,
    requesterId = employee.uid,
    requesterName = employee.name,
    department = "Finance",
    assigneeId = assigneeId,
    assigneeName = assigneeId?.let { "Karim" },
    createdAt = createdAt,
).block()
