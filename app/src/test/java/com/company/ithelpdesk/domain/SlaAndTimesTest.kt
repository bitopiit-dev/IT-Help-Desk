// © 2026 Khondaker Arifur Rahman. All Rights Reserved.
// BGL-IT Help — see COPYRIGHT.md. Unauthorized copying, modification or redistribution is prohibited.

package com.company.ithelpdesk.domain

import com.company.ithelpdesk.domain.logic.SlaCalculator
import com.company.ithelpdesk.domain.logic.TicketTimes
import com.company.ithelpdesk.domain.model.Priority
import com.company.ithelpdesk.domain.model.SlaSettings
import com.company.ithelpdesk.domain.model.SlaState
import com.company.ithelpdesk.domain.model.TicketStatus
import org.junit.Assert.assertEquals
import org.junit.Test

class SlaAndTimesTest {

    // HIGH default: respond 1h, resolve 8h
    private fun highTicket(status: TicketStatus = TicketStatus.IN_PROGRESS) =
        ticket(status, Priority.HIGH, createdAt = 0, assigneeId = "tech") {
            copy(slaResponseDueAt = 1 * HOUR, slaResolveDueAt = 8 * HOUR, firstResponseAt = 10 * MIN)
        }

    @Test fun `due dates come from priority targets`() {
        val (respond, resolve) = SlaCalculator.dueDates(0, Priority.CRITICAL, SlaSettings.DEFAULT)
        assertEquals(15 * MIN, respond)
        assertEquals(4 * HOUR, resolve)
    }

    @Test fun `on track, at risk and breached thresholds`() {
        val t = highTicket()
        assertEquals(SlaState.ON_TRACK, SlaCalculator.evaluate(t, 5 * HOUR))
        assertEquals(SlaState.AT_RISK, SlaCalculator.evaluate(t, 6 * HOUR)) // 75 %
        assertEquals(SlaState.BREACHED, SlaCalculator.evaluate(t, 8 * HOUR))
    }

    @Test fun `missing first response breaches the response target`() {
        val t = highTicket(TicketStatus.NEW).copy(firstResponseAt = null, assigneeId = null)
        assertEquals(SlaState.AT_RISK, SlaCalculator.evaluate(t, 50 * MIN))
        assertEquals(SlaState.BREACHED, SlaCalculator.evaluate(t, 61 * MIN))
    }

    @Test fun `late first response breaches even if resolved in time`() {
        val t = highTicket(TicketStatus.RESOLVED).copy(firstResponseAt = 2 * HOUR, resolvedAt = 3 * HOUR)
        assertEquals(SlaState.BREACHED, SlaCalculator.evaluate(t, 3 * HOUR))
    }

    @Test fun `time on hold pauses the resolve clock`() {
        val held = highTicket().copy(onHoldTotalMs = 4 * HOUR)
        assertEquals(SlaState.ON_TRACK, SlaCalculator.evaluate(held, 8 * HOUR)) // 8h elapsed, due moved to 12h
        assertEquals(SlaState.BREACHED, SlaCalculator.evaluate(highTicket(), 8 * HOUR)) // same time without hold
        val currentlyHeld = highTicket(TicketStatus.ON_HOLD_PARTS).copy(onHoldSince = 2 * HOUR)
        assertEquals(SlaState.ON_TRACK, SlaCalculator.evaluate(currentlyHeld, 8 * HOUR)) // on hold 6h so far, due moved to 14h
    }

    @Test fun `resolved tickets are MET or BREACHED`() {
        assertEquals(SlaState.MET, SlaCalculator.evaluate(highTicket(TicketStatus.RESOLVED).copy(resolvedAt = 7 * HOUR), 100 * HOUR))
        assertEquals(SlaState.BREACHED, SlaCalculator.evaluate(highTicket(TicketStatus.CLOSED).copy(resolvedAt = 9 * HOUR), 100 * HOUR))
    }

    @Test fun `repair and active repair time`() {
        val t = highTicket(TicketStatus.RESOLVED).copy(createdAt = 1 * HOUR, resolvedAt = 6 * HOUR, onHoldTotalMs = 2 * HOUR)
        assertEquals(5 * HOUR, TicketTimes.repairMs(t))
        assertEquals(3 * HOUR, TicketTimes.activeRepairMs(t))
        assertEquals(9 * MIN, TicketTimes.firstResponseMs(t.copy(firstResponseAt = 1 * HOUR + 9 * MIN)))
    }

    @Test fun `open ticket has no repair time`() {
        assertEquals(null, TicketTimes.repairMs(highTicket()))
    }
}
