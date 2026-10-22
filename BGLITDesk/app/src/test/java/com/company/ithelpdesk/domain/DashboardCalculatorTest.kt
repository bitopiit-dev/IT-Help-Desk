package com.company.ithelpdesk.domain

import com.company.ithelpdesk.domain.logic.DashboardCalculator
import com.company.ithelpdesk.domain.logic.DashboardFilter
import com.company.ithelpdesk.domain.logic.DashboardSource
import com.company.ithelpdesk.domain.model.TicketStatus
import org.junit.Assert.assertEquals
import org.junit.Test
import java.time.ZoneOffset

class DashboardCalculatorTest {

    private val day = 24 * HOUR
    private val now = 10 * day + 12 * HOUR

    private val open1 = ticket(TicketStatus.IN_PROGRESS, createdAt = 9 * day, assigneeId = tech.uid) { copy(id = "a") }
    private val open2 = ticket(TicketStatus.NEW, createdAt = 10 * day) { copy(id = "b", department = "Sales") }
    private val done = ticket(TicketStatus.CLOSED, createdAt = 8 * day, assigneeId = tech.uid) {
        copy(id = "c", resolvedAt = 8 * day + 4 * HOUR, firstResponseAt = 8 * day + 30 * MIN, rating = 4, onHoldTotalMs = HOUR)
    }

    private val source = DashboardSource(
        createdInRange = listOf(open1, open2, done),
        resolvedInRange = listOf(done),
        open = listOf(open1, open2),
    )

    @Test fun `kpis are computed from the right sets`() {
        val d = DashboardCalculator.compute(source, listOf(tech, admin), DashboardFilter(), 4 * day, now, now, ZoneOffset.UTC)
        assertEquals(3, d.totalTickets)
        assertEquals(2, d.open)
        assertEquals(1, d.inProgress)
        assertEquals(1, d.resolvedInPeriod)
        assertEquals(0, d.resolvedToday)
        assertEquals(4 * HOUR, d.avgRepairMs)
        assertEquals(3 * HOUR, d.avgActiveRepairMs)
        assertEquals(30 * MIN, d.avgFirstResponseMs)
        assertEquals(4.0, d.csat!!, 0.0001)
        assertEquals(7, d.trend.size) // day 4..10
    }

    @Test fun `technician workload counts who handles what`() {
        val d = DashboardCalculator.compute(source, listOf(tech, admin), DashboardFilter(), 4 * day, now, now, ZoneOffset.UTC)
        val karim = d.workload.first { it.uid == tech.uid }
        assertEquals(1, karim.openAssigned)
        assertEquals(1, karim.inProgress)
        assertEquals(1, karim.resolvedInPeriod)
        assertEquals(listOf("a"), d.currentlyHandled.map { it.id })
    }

    @Test fun `department filter narrows everything`() {
        val d = DashboardCalculator.compute(source, listOf(tech), DashboardFilter(department = "Sales"), 4 * day, now, now, ZoneOffset.UTC)
        assertEquals(1, d.totalTickets)
        assertEquals(1, d.open)
        assertEquals(0, d.resolvedInPeriod)
    }
}
