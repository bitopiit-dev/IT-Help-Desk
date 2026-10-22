package com.company.ithelpdesk.domain.logic

import com.company.ithelpdesk.domain.model.Priority
import com.company.ithelpdesk.domain.model.SlaState
import com.company.ithelpdesk.domain.model.Ticket
import com.company.ithelpdesk.domain.model.TicketStatus
import com.company.ithelpdesk.domain.model.UserProfile
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import java.time.format.DateTimeFormatter

data class DashboardFilter(
    val department: String? = null,
    val category: String? = null,
    val technicianId: String? = null,
)

/** Raw data for one dashboard view. */
data class DashboardSource(
    val createdInRange: List<Ticket>,
    val resolvedInRange: List<Ticket>,
    val open: List<Ticket>,
)

data class TrendPoint(val label: String, val created: Int, val resolved: Int)

data class TechnicianRow(
    val uid: String,
    val name: String,
    val openAssigned: Int,
    val inProgress: Int,
    val resolvedInPeriod: Int,
    val avgRepairMs: Long?,
    val avgRating: Double?,
    val slaCompliancePct: Int?,
)

data class DashboardData(
    val totalTickets: Int,
    val open: Int,
    val inProgress: Int,
    val onHold: Int,
    val resolvedToday: Int,
    val resolvedInPeriod: Int,
    val overdue: Int,
    val avgFirstResponseMs: Long?,
    val avgRepairMs: Long?,
    val avgActiveRepairMs: Long?,
    val csat: Double?,
    val ratedCount: Int,
    val byStatus: Map<TicketStatus, Int>,
    val trend: List<TrendPoint>,
    val byCategory: List<Pair<String, Int>>,
    val byDepartment: List<Pair<String, Int>>,
    val avgRepairByCategory: List<Pair<String, Long>>,
    val avgRepairByPriority: List<Pair<Priority, Long>>,
    val workload: List<TechnicianRow>,
    val currentlyHandled: List<Ticket>,
    val breaches: List<Ticket>,
    val ticketsInPeriod: List<Ticket>,
)

object DashboardCalculator {

    private const val MAX_TREND_DAYS = 92L

    fun compute(
        source: DashboardSource,
        technicians: List<UserProfile>,
        filter: DashboardFilter,
        rangeStart: Long,
        rangeEnd: Long,
        now: Long,
        zone: ZoneId = ZoneId.systemDefault(),
    ): DashboardData {
        fun Ticket.matches(): Boolean =
            (filter.department == null || department == filter.department) &&
                (filter.category == null || category == filter.category) &&
                (filter.technicianId == null || assigneeId == filter.technicianId)

        val created = source.createdInRange.filter { it.matches() && it.createdAt in rangeStart..rangeEnd }
        val resolvedAll = source.resolvedInRange.filter {
            it.matches() && it.resolvedAt != null && !it.status.isOpen && it.status != TicketStatus.CANCELLED
        }
        val resolved = resolvedAll.filter { it.resolvedAt!! in rangeStart..rangeEnd }
        val open = source.open.filter { it.matches() && it.status.isOpen }

        val todayStart = LocalDate.ofInstant(Instant.ofEpochMilli(now), zone)
            .atStartOfDay(zone).toInstant().toEpochMilli()

        val breaches = open.filter { SlaCalculator.evaluate(it, now) == SlaState.BREACHED }
            .sortedBy { SlaCalculator.remainingMs(it, now) ?: Long.MAX_VALUE }

        val staff = technicians.filter { it.role.isStaff && it.isActive }
            .filter { filter.technicianId == null || it.uid == filter.technicianId }

        val workload = staff.map { tech ->
            val mineOpen = open.filter { it.assigneeId == tech.uid }
            val mineResolved = resolved.filter { it.assigneeId == tech.uid }
            val rated = mineResolved.mapNotNull { it.rating }
            val slaEvaluated = mineResolved.map { SlaCalculator.evaluate(it, now) }
                .filter { it == SlaState.MET || it == SlaState.BREACHED }
            TechnicianRow(
                uid = tech.uid,
                name = tech.name,
                openAssigned = mineOpen.size,
                inProgress = mineOpen.count { it.status == TicketStatus.IN_PROGRESS },
                resolvedInPeriod = mineResolved.size,
                avgRepairMs = mineResolved.mapNotNull { TicketTimes.repairMs(it) }.averageOrNull(),
                avgRating = if (rated.isEmpty()) null else rated.average(),
                slaCompliancePct = if (slaEvaluated.isEmpty()) {
                    null
                } else {
                    (slaEvaluated.count { it == SlaState.MET } * 100) / slaEvaluated.size
                },
            )
        }.sortedWith(compareByDescending<TechnicianRow> { it.openAssigned }.thenBy { it.name })

        val ratings = resolved.mapNotNull { it.rating }

        return DashboardData(
            totalTickets = created.size,
            open = open.size,
            inProgress = open.count { it.status == TicketStatus.IN_PROGRESS },
            onHold = open.count { it.status.isOnHold },
            resolvedToday = resolvedAll.count { it.resolvedAt!! >= todayStart },
            resolvedInPeriod = resolved.size,
            overdue = breaches.size,
            avgFirstResponseMs = created.mapNotNull { TicketTimes.firstResponseMs(it) }.averageOrNull(),
            avgRepairMs = resolved.mapNotNull { TicketTimes.repairMs(it) }.averageOrNull(),
            avgActiveRepairMs = resolved.mapNotNull { TicketTimes.activeRepairMs(it) }.averageOrNull(),
            csat = if (ratings.isEmpty()) null else ratings.average(),
            ratedCount = ratings.size,
            byStatus = created.groupingBy { it.status }.eachCount(),
            trend = trend(created, resolved, rangeStart, rangeEnd, zone),
            byCategory = created.groupingBy { it.category.ifBlank { "Uncategorised" } }.eachCount()
                .toList().sortedByDescending { it.second },
            byDepartment = created.groupingBy { it.department.ifBlank { "Unknown" } }.eachCount()
                .toList().sortedByDescending { it.second },
            avgRepairByCategory = resolved.groupBy { it.category.ifBlank { "Uncategorised" } }
                .mapNotNull { (k, v) -> v.mapNotNull { TicketTimes.repairMs(it) }.averageOrNull()?.let { k to it } }
                .sortedByDescending { it.second },
            avgRepairByPriority = resolved.groupBy { it.priority }
                .mapNotNull { (k, v) -> v.mapNotNull { TicketTimes.repairMs(it) }.averageOrNull()?.let { k to it } }
                .sortedByDescending { it.first.rank },
            workload = workload,
            currentlyHandled = open.filter { it.assigneeId != null }.sortedBy { it.createdAt },
            breaches = breaches,
            ticketsInPeriod = created.sortedByDescending { it.createdAt },
        )
    }

    private fun trend(
        created: List<Ticket>,
        resolved: List<Ticket>,
        rangeStart: Long,
        rangeEnd: Long,
        zone: ZoneId,
    ): List<TrendPoint> {
        val fmt = DateTimeFormatter.ofPattern("dd MMM")
        val startDay = LocalDate.ofInstant(Instant.ofEpochMilli(rangeStart), zone)
        val endDay = LocalDate.ofInstant(Instant.ofEpochMilli(rangeEnd), zone)
        val days = generateSequence(startDay) { it.plusDays(1) }
            .takeWhile { !it.isAfter(endDay) }
            .take(MAX_TREND_DAYS.toInt())
            .toList()
        val createdByDay = created.groupingBy { LocalDate.ofInstant(Instant.ofEpochMilli(it.createdAt), zone) }.eachCount()
        val resolvedByDay = resolved.groupingBy { LocalDate.ofInstant(Instant.ofEpochMilli(it.resolvedAt!!), zone) }.eachCount()
        return days.map { d -> TrendPoint(d.format(fmt), createdByDay[d] ?: 0, resolvedByDay[d] ?: 0) }
    }

    private fun List<Long>.averageOrNull(): Long? = if (isEmpty()) null else (sum() / size)
}
