package com.company.ithelpdesk.util

import android.content.Context
import android.content.Intent
import androidx.core.content.FileProvider
import com.company.ithelpdesk.domain.logic.DashboardData
import com.company.ithelpdesk.domain.logic.SlaCalculator
import com.company.ithelpdesk.domain.logic.TicketTimes
import java.io.File
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter

object CsvExporter {

    private fun esc(v: Any?): String {
        val s = v?.toString() ?: ""
        return if (s.any { it == ',' || it == '"' || it == '\n' }) "\"" + s.replace("\"", "\"\"") + "\"" else s
    }

    private fun hours(ms: Long?): String = ms?.let { String.format(java.util.Locale.US, "%.2f", it / 3_600_000.0) } ?: ""

    fun build(data: DashboardData, rangeLabel: String, now: Long): String = buildString {
        appendLine("BGLITDesk report,${esc(rangeLabel)}")
        appendLine("Generated,${esc(formatDateTime(now))}")
        appendLine()
        appendLine("Metric,Value")
        appendLine("Tickets created,${data.totalTickets}")
        appendLine("Open now,${data.open}")
        appendLine("In progress now,${data.inProgress}")
        appendLine("On hold now,${data.onHold}")
        appendLine("Resolved in period,${data.resolvedInPeriod}")
        appendLine("Overdue (SLA breached),${data.overdue}")
        appendLine("Avg first response (h),${hours(data.avgFirstResponseMs)}")
        appendLine("Avg repair time (h),${hours(data.avgRepairMs)}")
        appendLine("Avg active repair time (h),${hours(data.avgActiveRepairMs)}")
        appendLine("Satisfaction (1-5),${data.csat?.let { String.format(java.util.Locale.US, "%.2f", it) } ?: ""}")
        appendLine()
        appendLine("Admin,Open assigned,In progress,Resolved in period,Avg repair (h),Avg rating,SLA compliance %")
        data.workload.forEach { r ->
            appendLine(listOf(r.name, r.openAssigned, r.inProgress, r.resolvedInPeriod, hours(r.avgRepairMs),
                r.avgRating?.let { String.format(java.util.Locale.US, "%.2f", it) } ?: "", r.slaCompliancePct ?: "").joinToString(",") { esc(it) })
        }
        appendLine()
        appendLine("Ticket,Title,Category,Sub-category,Priority,Status,Requester,Department,Assignee,Created,First response (h),Resolved,Repair (h),Active repair (h),SLA,Rating")
        data.ticketsInPeriod.forEach { t ->
            appendLine(
                listOf(
                    t.displayNumber, t.title, t.category, t.subCategory, t.priority.label, t.status.label,
                    t.requesterName, t.department, t.assigneeName ?: "", formatDateTime(t.createdAt),
                    hours(TicketTimes.firstResponseMs(t)), formatDateTime(t.resolvedAt), hours(TicketTimes.repairMs(t)),
                    hours(TicketTimes.activeRepairMs(t)), SlaCalculator.evaluate(t, now).label, t.rating ?: "",
                ).joinToString(",") { esc(it) },
            )
        }
    }

    /** Writes the CSV to cache and returns a share-sheet intent. */
    fun shareIntent(context: Context, csv: String): Intent {
        val dir = File(context.cacheDir, "exports").apply { mkdirs() }
        val stamp = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMdd_HHmm"))
        val file = File(dir, "bglitdesk_report_$stamp.csv").apply { writeText(csv) }
        val uri = FileProvider.getUriForFile(context, "${context.packageName}.fileprovider", file)
        val send = Intent(Intent.ACTION_SEND).apply {
            type = "text/csv"
            putExtra(Intent.EXTRA_STREAM, uri)
            putExtra(Intent.EXTRA_SUBJECT, "BGLITDesk report")
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }
        return Intent.createChooser(send, "Export report")
    }
}
