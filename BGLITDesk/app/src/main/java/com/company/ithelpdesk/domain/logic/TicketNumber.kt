package com.company.ithelpdesk.domain.logic

/**
 * Human-friendly ticket numbers such as BGL-2026-0042.
 * The sequence comes from counters/tickets-{year}, incremented in the same transaction that creates
 * the ticket, so two people creating tickets at once never get the same number.
 */
object TicketNumber {
    const val PREFIX = "BGL"

    fun counterId(year: Int): String = "tickets-$year"

    fun format(year: Int, sequence: Long): String = "$PREFIX-$year-" + sequence.toString().padStart(4, '0')
}
