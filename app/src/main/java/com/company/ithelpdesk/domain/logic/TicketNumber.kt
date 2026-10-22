// © 2026 Khondaker Arifur Rahman. All Rights Reserved.
// BGL-IT Help — see COPYRIGHT.md. Unauthorized copying, modification or redistribution is prohibited.

package com.company.ithelpdesk.domain.logic

import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import java.time.format.DateTimeFormatter

/**
 * Human-friendly ticket numbers: BGL-YYMMDD-XXXX, e.g. BGL-261022-0007 is the 7th ticket of 22 Oct 2026.
 * The sequence starts again at 0001 every day. It comes from counters/tickets-{YYMMDD}, incremented in the
 * same transaction that creates the ticket, so two people creating tickets at once never get the same number.
 * The date is Bangladesh time, so a ticket raised at 00:30 belongs to the new day for everyone.
 */
object TicketNumber {
    const val PREFIX = "BGL"
    val ZONE: ZoneId = ZoneId.of("Asia/Dhaka")
    private val DATE = DateTimeFormatter.ofPattern("yyMMdd")

    fun dateOf(epochMillis: Long): LocalDate = Instant.ofEpochMilli(epochMillis).atZone(ZONE).toLocalDate()

    fun datePart(date: LocalDate): String = date.format(DATE)

    fun counterId(date: LocalDate): String = "tickets-${datePart(date)}"

    /** Four digits, growing to five only past 9,999 tickets in one day. */
    fun format(date: LocalDate, sequence: Long): String =
        "$PREFIX-${datePart(date)}-" + sequence.toString().padStart(4, '0')
}
