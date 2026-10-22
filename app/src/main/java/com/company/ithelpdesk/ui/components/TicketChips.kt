// © 2026 Khondaker Arifur Rahman. All Rights Reserved.
// BGL-IT Help — see COPYRIGHT.md. Unauthorized copying, modification or redistribution is prohibited.

package com.company.ithelpdesk.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Error
import androidx.compose.material.icons.filled.KeyboardDoubleArrowUp
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.material.icons.filled.Remove
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.dp
import com.company.ithelpdesk.domain.model.Priority
import com.company.ithelpdesk.domain.model.SlaState
import com.company.ithelpdesk.domain.model.TicketStatus
import com.company.ithelpdesk.ui.theme.LocalStatusColors

@Composable
fun Pill(text: String, container: Color, content: Color, icon: ImageVector? = null, modifier: Modifier = Modifier) {
    Row(
        modifier
            .background(container, RoundedCornerShape(50))
            .padding(horizontal = 10.dp, vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(4.dp),
    ) {
        if (icon != null) Icon(icon, contentDescription = null, tint = content, modifier = Modifier.size(14.dp))
        Text(text, color = content, style = MaterialTheme.typography.labelMedium)
    }
}

@Composable
fun statusColor(status: TicketStatus): Pair<Color, Color> {
    val c = LocalStatusColors.current
    return when (status) {
        TicketStatus.NEW, TicketStatus.REOPENED -> c.info to c.onInfo
        TicketStatus.ASSIGNED, TicketStatus.IN_PROGRESS ->
            MaterialTheme.colorScheme.primaryContainer to MaterialTheme.colorScheme.onPrimaryContainer
        TicketStatus.ON_HOLD_USER, TicketStatus.ON_HOLD_PARTS -> c.warn to c.onWarn
        TicketStatus.RESOLVED, TicketStatus.CLOSED -> c.ok to c.onOk
        TicketStatus.CANCELLED -> c.mute to c.onMute
    }
}

@Composable
fun StatusChip(status: TicketStatus, modifier: Modifier = Modifier) {
    val (bg, fg) = statusColor(status)
    Pill(status.label, bg, fg, modifier = modifier)
}

@Composable
fun PriorityChip(priority: Priority, modifier: Modifier = Modifier) {
    val c = LocalStatusColors.current
    val (bg, fg, icon) = when (priority) {
        Priority.LOW -> Triple(c.mute, c.onMute, Icons.Default.KeyboardArrowDown)
        Priority.MEDIUM -> Triple(c.info, c.onInfo, Icons.Default.Remove)
        Priority.HIGH -> Triple(c.warn, c.onWarn, Icons.Default.KeyboardArrowUp)
        Priority.CRITICAL -> Triple(c.bad, c.onBad, Icons.Default.KeyboardDoubleArrowUp)
    }
    Pill(priority.label, bg, fg, icon, modifier)
}

@Composable
fun SlaBadge(state: SlaState, modifier: Modifier = Modifier) {
    val c = LocalStatusColors.current
    val (bg, fg, icon) = when (state) {
        SlaState.ON_TRACK -> Triple(c.mute, c.onMute, Icons.Default.Schedule)
        SlaState.AT_RISK -> Triple(c.warn, c.onWarn, Icons.Default.Warning)
        SlaState.BREACHED -> Triple(c.bad, c.onBad, Icons.Default.Error)
        SlaState.MET -> Triple(c.ok, c.onOk, Icons.Default.CheckCircle)
    }
    Pill("SLA ${state.label.lowercase()}", bg, fg, icon, modifier)
}
