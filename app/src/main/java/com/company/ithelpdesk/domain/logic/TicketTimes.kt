// © 2026 Khondaker Arifur Rahman. All Rights Reserved.
// BGL-IT Help — see COPYRIGHT.md. Unauthorized copying, modification or redistribution is prohibited.

package com.company.ithelpdesk.domain.logic

import com.company.ithelpdesk.domain.model.Ticket

/**
 * Time definitions used everywhere (app, dashboard, CSV, Cloud Functions):
 *  - First response time = firstResponseAt − createdAt
 *    (first technician action: assign, comment or status change)
 *  - Repair time         = resolvedAt − createdAt
 *  - Active repair time  = repair time − total time in On Hold states
 */
object TicketTimes {
    fun firstResponseMs(t: Ticket): Long? =
        t.firstResponseAt?.let { (it - t.createdAt).coerceAtLeast(0) }

    fun repairMs(t: Ticket): Long? =
        t.resolvedAt?.let { (it - t.createdAt).coerceAtLeast(0) }

    fun activeRepairMs(t: Ticket): Long? =
        repairMs(t)?.let { (it - t.onHoldTotalMs).coerceAtLeast(0) }

    fun currentHoldMs(t: Ticket, now: Long): Long =
        if (t.status.isOnHold && t.onHoldSince != null) (now - t.onHoldSince).coerceAtLeast(0) else 0L

    fun elapsedMs(t: Ticket, now: Long): Long =
        ((t.resolvedAt ?: now) - t.createdAt).coerceAtLeast(0)
}
