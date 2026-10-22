// © 2026 Khondaker Arifur Rahman. All Rights Reserved.
// BGL-IT Help — see COPYRIGHT.md. Unauthorized copying, modification or redistribution is prohibited.

package com.company.ithelpdesk.ui.admin

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.outlined.Image
import androidx.compose.material.icons.outlined.IosShare
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import com.company.ithelpdesk.util.ReportImage
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.DateRange
import androidx.compose.material.icons.outlined.FileDownload
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.DateRangePicker
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberDateRangePickerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.company.ithelpdesk.domain.model.TicketStatus
import com.company.ithelpdesk.ui.components.AdminTicketCard
import com.company.ithelpdesk.ui.components.AppCard
import com.company.ithelpdesk.ui.components.UserAvatar
import com.company.ithelpdesk.ui.components.Banner
import com.company.ithelpdesk.ui.components.BarList
import com.company.ithelpdesk.ui.components.ChartSlice
import com.company.ithelpdesk.ui.components.DonutChart
import com.company.ithelpdesk.ui.components.ErrorBox
import com.company.ithelpdesk.ui.components.LoadingBox
import com.company.ithelpdesk.ui.components.SectionTitle
import com.company.ithelpdesk.ui.components.StatTile
import com.company.ithelpdesk.ui.components.TrendChart
import com.company.ithelpdesk.ui.theme.LocalBrand
import com.company.ithelpdesk.util.CsvExporter
import com.company.ithelpdesk.util.formatDuration
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AdminOverviewScreen(
    pendingSignups: Int,
    onOpenTicket: (String) -> Unit,
    onOpenPeople: () -> Unit,
    onOpenQueue: () -> Unit,
    onOpenAssignee: (String) -> Unit,
    viewModel: DashboardViewModel = hiltViewModel(),
) {
    val ui by viewModel.ui.collectAsStateWithLifecycle()
    val context = LocalContext.current
    val tints = LocalBrand.current.cardTints
    var showPicker by rememberSaveable { mutableStateOf(false) }
    var exportMenu by remember { mutableStateOf(false) }
    var exporting by remember { mutableStateOf(false) }
    val scope = rememberCoroutineScope()
    var preview by remember { mutableStateOf<Pair<android.graphics.Bitmap, String>?>(null) }

    LazyColumn(Modifier.fillMaxSize().statusBarsPadding(), contentPadding = PaddingValues(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
        item {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Column(Modifier.weight(1f)) {
                    Text("Command centre", style = MaterialTheme.typography.headlineMedium)
                    Text("Live · ${ui.rangeLabel}", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.primary)
                }
                Box {
                    IconButton(enabled = ui.data != null && !exporting, onClick = { exportMenu = true }) {
                        if (exporting) CircularProgressIndicator(Modifier.size(22.dp), strokeWidth = 2.dp)
                        else Icon(Icons.Outlined.IosShare, contentDescription = "Share report")
                    }
                    DropdownMenu(expanded = exportMenu, onDismissRequest = { exportMenu = false }) {
                        DropdownMenuItem(
                            text = { Text("Chart report (JPG)") },
                            leadingIcon = { Icon(Icons.Outlined.Image, null) },
                            onClick = {
                                exportMenu = false
                                val d = ui.data ?: return@DropdownMenuItem
                                val label = ui.rangeLabel
                                exporting = true
                                scope.launch {
                                    try {
                                        val bmp = withContext(Dispatchers.Default) { ReportImage.render(context, d, label) }
                                        preview = bmp to label
                                    } catch (e: Exception) {
                                        android.widget.Toast.makeText(context, "Couldn't create the report: ${e.message}", android.widget.Toast.LENGTH_LONG).show()
                                    } finally {
                                        exporting = false
                                    }
                                }
                            },
                        )
                        DropdownMenuItem(
                            text = { Text("Raw data (CSV / Excel)") },
                            leadingIcon = { Icon(Icons.Outlined.FileDownload, null) },
                            onClick = {
                                exportMenu = false
                                ui.data?.let { d ->
                                    context.startActivity(CsvExporter.shareIntent(context, CsvExporter.build(d, ui.rangeLabel, ui.now)))
                                }
                            },
                        )
                    }
                }
            }
        }
        item { com.company.ithelpdesk.ui.components.LiveClock(onGradient = false) }
        item {
            LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                items(DashboardRange.entries) { r ->
                    FilterChip(
                        selected = ui.selection.range == r,
                        onClick = { if (r == DashboardRange.CUSTOM) showPicker = true else viewModel.setRange(r) },
                        label = { Text(r.label) },
                        leadingIcon = if (r == DashboardRange.CUSTOM) { { Icon(Icons.Outlined.DateRange, null) } } else null,
                        shape = CircleShape,
                    )
                }
            }
        }
        if (pendingSignups > 0) {
            item {
                Banner(
                    "$pendingSignups sign-up request${if (pendingSignups > 1) "s" else ""} waiting for approval",
                    "🙋",
                    MaterialTheme.colorScheme.secondary,
                    onClick = onOpenPeople,
                )
            }
        }
        val d = ui.data
        when {
            ui.loading && d == null -> item { LoadingBox(Modifier.height(240.dp)) }
            ui.error != null -> item { ErrorBox(ui.error!!, onRetry = viewModel::refresh) }
            d != null -> {
                item {
                    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                        Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                            StatTile("Open now", d.open.toString(), "📥", tints[0], Modifier.weight(1f), onClick = onOpenQueue)
                            StatTile("In progress", d.inProgress.toString(), "⚙️", tints[4], Modifier.weight(1f), onClick = onOpenQueue)
                            StatTile("Overdue", d.overdue.toString(), "🚨", tints[5], Modifier.weight(1f), onClick = onOpenQueue)
                        }
                        Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                            StatTile("Created", d.totalTickets.toString(), "🎫", tints[2], Modifier.weight(1f))
                            StatTile("Resolved", d.resolvedInPeriod.toString(), "✅", tints[1], Modifier.weight(1f))
                            StatTile("Fixed today", d.resolvedToday.toString(), "⚡", tints[3], Modifier.weight(1f))
                        }
                        Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                            StatTile("1st response", formatDuration(d.avgFirstResponseMs), "⏱️", tints[0], Modifier.weight(1f))
                            StatTile("Avg repair", formatDuration(d.avgRepairMs), "🛠️", tints[4], Modifier.weight(1f))
                            StatTile(
                                "CSAT (${d.ratedCount})",
                                d.csat?.let { String.format(java.util.Locale.US, "%.1f★", it) } ?: "—",
                                "⭐",
                                tints[3],
                                Modifier.weight(1f),
                            )
                        }
                    }
                }
                item {
                    AppCard {
                        val totalTickets = d.byStatus.values.sum()
                        val resolvedAndClosed = (d.byStatus[TicketStatus.RESOLVED] ?: 0) + (d.byStatus[TicketStatus.CLOSED] ?: 0)
                        val resolutionRate = if (totalTickets > 0) (resolvedAndClosed * 100f / totalTickets) else 100f

                        // Dynamic status color rule: 80%-100% Green, 60%-79% Lime, <50% Red
                        val statusColor = when {
                            resolutionRate >= 80f -> Color(0xFF22C55E) // Green
                            resolutionRate >= 60f -> Color(0xFF84CC16) // Lime
                            else -> Color(0xFFEF4444)                  // Red
                        }

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            Text("Tickets by status", style = MaterialTheme.typography.titleMedium)
                            if (totalTickets > 0) {
                                Surface(
                                    color = statusColor.copy(alpha = 0.15f),
                                    shape = CircleShape,
                                ) {
                                    Text(
                                        text = String.format(Locale.US, "%.0f%% resolved", resolutionRate),
                                        style = MaterialTheme.typography.labelSmall,
                                        color = statusColor,
                                        fontWeight = FontWeight.Bold,
                                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp),
                                    )
                                }
                            }
                        }
                        Spacer(Modifier.height(10.dp))
                        val palette = mapOf(
                            TicketStatus.NEW to tints[0],
                            TicketStatus.ASSIGNED to tints[4],
                            TicketStatus.IN_PROGRESS to tints[1],
                            TicketStatus.ON_HOLD_USER to tints[3],
                            TicketStatus.ON_HOLD_PARTS to Color(0xFFFB923C),
                            TicketStatus.RESOLVED to statusColor,
                            TicketStatus.CLOSED to statusColor,
                            TicketStatus.REOPENED to tints[2],
                            TicketStatus.CANCELLED to Color(0xFF475569),
                        )
                        DonutChart(TicketStatus.entries.map { ChartSlice(it.label, d.byStatus[it] ?: 0, palette.getValue(it)) })
                    }
                }
                item {
                    AppCard {
                        Text("Created vs resolved", style = MaterialTheme.typography.titleMedium)
                        Spacer(Modifier.height(10.dp))
                        TrendChart(d.trend)
                    }
                }
                if (d.byCategory.isNotEmpty()) {
                    item {
                        AppCard {
                            Text("Top categories", style = MaterialTheme.typography.titleMedium)
                            Spacer(Modifier.height(10.dp))
                            BarList(d.byCategory.map { it.first to it.second.toFloat() }, { it.toInt().toString() }, color = tints[1])
                        }
                    }
                }
                if (d.byDepartment.isNotEmpty()) {
                    item {
                        AppCard {
                            Text("Departments", style = MaterialTheme.typography.titleMedium)
                            Spacer(Modifier.height(10.dp))
                            BarList(d.byDepartment.map { it.first to it.second.toFloat() }, { it.toInt().toString() }, color = tints[2])
                        }
                    }
                }
                if (d.workload.isNotEmpty()) {
                    item { SectionTitle("Team workload") }
                    items(d.workload, key = { it.uid }) { r ->
                        AppCard(onClick = { onOpenAssignee(r.uid) }, padding = 12.dp) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                UserAvatar(r.uid, r.name.split(" ").mapNotNull { it.firstOrNull() }.take(2).joinToString(""), size = 40.dp)
                                Spacer(Modifier.width(12.dp))
                                Column(Modifier.weight(1f)) {
                                    Text(r.name, style = MaterialTheme.typography.titleSmall, maxLines = 1, overflow = TextOverflow.Ellipsis)
                                    Text(
                                        "${r.openAssigned} open · ${r.inProgress} in progress · ${r.resolvedInPeriod} resolved",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    )
                                }
                                Column(horizontalAlignment = Alignment.End) {
                                    Text(formatDuration(r.avgRepairMs), style = MaterialTheme.typography.labelLarge)
                                    Text(
                                        listOfNotNull(
                                            r.avgRating?.let { String.format(java.util.Locale.US, "%.1f★", it) },
                                            r.slaCompliancePct?.let { "SLA $it%" },
                                        ).joinToString(" · ").ifBlank { "—" },
                                        style = MaterialTheme.typography.labelSmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    )
                                }
                            }
                        }
                    }
                }
                if (d.breaches.isNotEmpty()) {
                    item { SectionTitle("🚨 SLA breached (${d.breaches.size})") }
                    items(d.breaches.take(10), key = { "b" + it.id }) { t ->
                        AdminTicketCard(t, ui.now, onClick = { onOpenTicket(t.id) })
                    }
                }
                if (d.currentlyHandled.isNotEmpty()) {
                    item {
                        SectionTitle("Being handled right now") {
                            Text("${d.currentlyHandled.size}", style = MaterialTheme.typography.labelLarge,
                                color = MaterialTheme.colorScheme.primary, modifier = Modifier.clickable(onClick = onOpenQueue))
                        }
                    }
                    items(d.currentlyHandled.take(8), key = { "h" + it.id }) { t ->
                        AdminTicketCard(t, ui.now, onClick = { onOpenTicket(t.id) })
                    }
                }
                item { Spacer(Modifier.fillMaxWidth().height(8.dp)) }
            }
        }
    }

    preview?.let { (bmp, label) -> ReportPreviewDialog(bmp, label, onDismiss = { preview = null }) }

    if (showPicker) {
        val state = rememberDateRangePickerState()
        DatePickerDialog(
            onDismissRequest = { showPicker = false },
            confirmButton = {
                TextButton(
                    enabled = state.selectedStartDateMillis != null,
                    onClick = {
                        val s = state.selectedStartDateMillis
                        if (s != null) viewModel.setCustomRange(s, state.selectedEndDateMillis ?: s)
                        showPicker = false
                    },
                ) { Text("Apply") }
            },
            dismissButton = { TextButton(onClick = { showPicker = false }) { Text("Cancel") } },
        ) {
            DateRangePicker(state = state, modifier = Modifier.weight(1f))
        }
    }
}
