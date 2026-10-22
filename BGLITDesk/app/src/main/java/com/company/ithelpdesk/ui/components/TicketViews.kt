package com.company.ithelpdesk.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Lock
import androidx.compose.material.icons.outlined.Person
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.company.ithelpdesk.domain.logic.NotificationRules
import com.company.ithelpdesk.domain.logic.SlaCalculator
import com.company.ithelpdesk.domain.model.Comment
import com.company.ithelpdesk.domain.model.Role
import com.company.ithelpdesk.domain.model.StatusChange
import com.company.ithelpdesk.domain.model.Ticket
import com.company.ithelpdesk.domain.model.TicketStatus
import com.company.ithelpdesk.util.formatDateTime
import com.company.ithelpdesk.util.formatDuration
import com.company.ithelpdesk.util.formatRelative

/** Friendly status chip for the user app ("Work started", "Fixed"...). */
@Composable
fun UserStatusChip(status: TicketStatus, modifier: Modifier = Modifier) {
    val (bg, fg) = statusColor(status)
    Pill(NotificationRules.userFacingStatus(status), bg, fg, modifier = modifier)
}

fun categoryEmoji(category: String): String = when {
    category.contains("hard", true) -> "💻"
    category.contains("soft", true) -> "🧩"
    category.contains("net", true) || category.contains("wi", true) -> "📶"
    category.contains("mail", true) -> "✉️"
    category.contains("print", true) -> "🖨️"
    category.contains("access", true) || category.contains("pass", true) -> "🔐"
    category.contains("erp", true) -> "📊"
    else -> "✨"
}

/** Card used in the user app's ticket lists. */
@Composable
fun UserTicketCard(ticket: Ticket, now: Long, onClick: () -> Unit, modifier: Modifier = Modifier) {
    AppCard(modifier, onClick = onClick) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Box(
                Modifier.size(46.dp).clip(RoundedCornerShape(14.dp)).background(MaterialTheme.colorScheme.primaryContainer),
                contentAlignment = Alignment.Center,
            ) { Text(categoryEmoji(ticket.category)) }
            Spacer(Modifier.width(12.dp))
            Column(Modifier.weight(1f)) {
                Text(ticket.title, style = MaterialTheme.typography.titleSmall, maxLines = 1, overflow = TextOverflow.Ellipsis)
                Text(
                    "${ticket.displayNumber} · ${formatRelative(ticket.createdAt, now)}",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
        Spacer(Modifier.height(10.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(6.dp), verticalAlignment = Alignment.CenterVertically) {
            UserStatusChip(ticket.status)
            if (ticket.assigneeName != null && ticket.status.isOpen) {
                Text(
                    "👤 ${ticket.assigneeName.substringBefore(' ')}",
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            Spacer(Modifier.weight(1f))
            if (ticket.status == TicketStatus.RESOLVED) Text("Tap to confirm ✅", style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.primary)
        }
    }
}

/** Dense card used in the admin app's queue. */
@Composable
fun AdminTicketCard(ticket: Ticket, now: Long, onClick: () -> Unit, modifier: Modifier = Modifier) {
    val sla = SlaCalculator.evaluate(ticket, now)
    AppCard(modifier, onClick = onClick, padding = 14.dp) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(ticket.displayNumber, style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.primary)
            Spacer(Modifier.width(8.dp))
            Text(
                "${categoryEmoji(ticket.category)} ${ticket.category}",
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.weight(1f),
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            Text(formatRelative(ticket.createdAt, now), style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
        Spacer(Modifier.height(6.dp))
        Text(ticket.title, style = MaterialTheme.typography.titleSmall, maxLines = 2, overflow = TextOverflow.Ellipsis)
        Spacer(Modifier.height(4.dp))
        Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(Icons.Outlined.Person, null, Modifier.size(14.dp), tint = MaterialTheme.colorScheme.onSurfaceVariant)
            Spacer(Modifier.width(4.dp))
            Text(
                listOf(ticket.requesterName, ticket.department).filter { it.isNotBlank() }.joinToString(" · "),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
        }
        Spacer(Modifier.height(10.dp))
        Row(
            Modifier.horizontalScroll(rememberScrollState()),
            horizontalArrangement = Arrangement.spacedBy(6.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            StatusChip(ticket.status)
            PriorityChip(ticket.priority)
            if (ticket.status.isOpen && ticket.slaResolveDueAt != null) SlaBadge(sla)
            Text(
                ticket.assigneeName?.let { "→ ${it.substringBefore(' ')}" } ?: "Unassigned",
                style = MaterialTheme.typography.labelMedium,
                color = if (ticket.assigneeName == null) MaterialTheme.colorScheme.secondary else MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

/** Vertical status timeline. */
@Composable
fun StatusTimeline(history: List<StatusChange>, friendly: Boolean, modifier: Modifier = Modifier) {
    Column(modifier) {
        history.forEachIndexed { i, h ->
            Row {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    val (bg, _) = statusColor(h.to)
                    Box(Modifier.size(14.dp).clip(CircleShape).background(MaterialTheme.colorScheme.primary))
                    if (i < history.lastIndex) {
                        Box(Modifier.width(2.dp).height(44.dp).background(bg))
                    }
                }
                Spacer(Modifier.width(12.dp))
                Column(Modifier.padding(bottom = 12.dp)) {
                    Text(
                        if (friendly) NotificationRules.userFacingStatus(h.to) else h.to.label,
                        style = MaterialTheme.typography.titleSmall,
                    )
                    Text(
                        "${formatDateTime(h.changedAt)} · ${h.changedByName}",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                    if (h.note.isNotBlank()) Text(h.note, style = MaterialTheme.typography.bodySmall)
                }
            }
        }
    }
}

/** Chat bubble; my messages on the right in the accent gradient. */
@Composable
fun ChatBubble(
    ticketId: String,
    comment: Comment,
    mine: Boolean,
    onOpenImage: (Any) -> Unit,
    modifier: Modifier = Modifier,
) {
    val brand = com.company.ithelpdesk.ui.theme.LocalBrand.current
    val shape = RoundedCornerShape(
        topStart = 20.dp, topEnd = 20.dp,
        bottomStart = if (mine) 20.dp else 6.dp, bottomEnd = if (mine) 6.dp else 20.dp,
    )
    val bubbleModifier = when {
        comment.isInternal -> Modifier.background(Color(0x33FBBF24), shape)
        mine -> Modifier.background(brand.accentBrush, shape)
        else -> Modifier.background(MaterialTheme.colorScheme.surfaceContainerHigh, shape)
    }
    val textColor = if (mine && !comment.isInternal) Color.White else MaterialTheme.colorScheme.onSurface
    Row(modifier.fillMaxWidth(), horizontalArrangement = if (mine) Arrangement.End else Arrangement.Start) {
        Column(horizontalAlignment = if (mine) Alignment.End else Alignment.Start) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                if (comment.isInternal) {
                    Icon(Icons.Outlined.Lock, null, Modifier.size(12.dp), tint = Color(0xFFFBBF24))
                    Spacer(Modifier.width(4.dp))
                }
                Text(
                    buildString {
                        append(if (mine) "You" else comment.authorName)
                        if (!mine && comment.authorRole == Role.ADMIN) append(" · IT")
                        if (comment.isInternal) append(" · internal note")
                    },
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            Spacer(Modifier.height(2.dp))
            Column(bubbleModifier.widthIn(max = 290.dp).padding(horizontal = 14.dp, vertical = 10.dp)) {
                if (comment.text.isNotBlank()) Text(comment.text, color = textColor, style = MaterialTheme.typography.bodyMedium)
                if (comment.images.isNotEmpty()) {
                    Spacer(Modifier.height(6.dp))
                    Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        comment.images.forEach { TicketPhoto(ticketId, it, size = 96.dp, onOpen = onOpenImage) }
                    }
                }
            }
            Text(formatDateTime(comment.createdAt), style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.padding(top = 2.dp))
        }
    }
}

/** "Fix within 3h 20m" style text for the requester. */
fun friendlyEta(ticket: Ticket, now: Long): String? {
    if (!ticket.status.isOpen) return null
    val left = SlaCalculator.remainingMs(ticket, now) ?: return null
    return if (left > 0) "Target fix in ${formatDuration(left)}" else "Taking longer than planned — IT has been alerted"
}
