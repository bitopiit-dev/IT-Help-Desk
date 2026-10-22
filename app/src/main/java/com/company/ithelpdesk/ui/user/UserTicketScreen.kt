// © 2026 Khondaker Arifur Rahman. All Rights Reserved.
// BGL-IT Help — see COPYRIGHT.md. Unauthorized copying, modification or redistribution is prohibited.

package com.company.ithelpdesk.ui.user

import androidx.compose.foundation.background
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.company.ithelpdesk.domain.logic.NotificationRules
import com.company.ithelpdesk.domain.model.Ticket
import com.company.ithelpdesk.domain.model.TicketStatus
import com.company.ithelpdesk.ui.components.AppCard
import com.company.ithelpdesk.ui.components.CallButton
import com.company.ithelpdesk.ui.components.rememberPhoneCaller
import com.company.ithelpdesk.ui.components.ChatBubble
import com.company.ithelpdesk.ui.components.EmptyState
import com.company.ithelpdesk.ui.components.ErrorBox
import com.company.ithelpdesk.ui.components.GradientButton
import com.company.ithelpdesk.ui.components.InfoRow
import com.company.ithelpdesk.ui.components.LoadingBox
import com.company.ithelpdesk.ui.components.SectionTitle
import com.company.ithelpdesk.ui.components.StatusTimeline
import com.company.ithelpdesk.ui.components.TicketPhoto
import com.company.ithelpdesk.ui.components.friendlyEta
import com.company.ithelpdesk.ui.theme.LocalBrand
import com.company.ithelpdesk.ui.ticket.ChatInput
import com.company.ithelpdesk.ui.ticket.ImageDialog
import com.company.ithelpdesk.ui.ticket.NoteDialog
import com.company.ithelpdesk.ui.ticket.RateDialog
import com.company.ithelpdesk.ui.ticket.TicketDetailViewModel
import com.company.ithelpdesk.ui.ticket.TicketLoad
import com.company.ithelpdesk.util.formatDateTime
import com.company.ithelpdesk.util.rememberNow

/** The requester's view of a ticket: friendly progress, chat with IT, confirm / reopen / rate. */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun UserTicketScreen(onBack: () -> Unit, viewModel: TicketDetailViewModel = hiltViewModel()) {
    val load by viewModel.ticket.collectAsStateWithLifecycle()
    val comments by viewModel.comments.collectAsStateWithLifecycle()
    val history by viewModel.history.collectAsStateWithLifecycle()
    val busy by viewModel.busy.collectAsStateWithLifecycle()
    val message by viewModel.message.collectAsStateWithLifecycle()
    val snackbar = remember { SnackbarHostState() }
    val now = rememberNow()
    var dialog by rememberSaveable { mutableStateOf<String?>(null) }
    var openImage by remember { mutableStateOf<Any?>(null) }
    val call = rememberPhoneCaller()

    LaunchedEffect(message) {
        message?.let {
            snackbar.showSnackbar(it)
            viewModel.consumeMessage()
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text((load as? TicketLoad.Loaded)?.ticket?.displayNumber ?: "Ticket") },
                navigationIcon = { IconButton(onClick = onBack) { Icon(Icons.AutoMirrored.Filled.ArrowBack, "Back") } },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = MaterialTheme.colorScheme.background),
            )
        },
        snackbarHost = { SnackbarHost(snackbar) },
        bottomBar = {
            val t = (load as? TicketLoad.Loaded)?.ticket
            if (t != null && t.status != TicketStatus.CANCELLED) {
                ChatInput(busy = busy, allowInternal = false, placeholder = "Message IT…") { text, _, images, clear ->
                    viewModel.sendComment(text, false, images, clear)
                }
            }
        },
    ) { padding ->
        when (val l = load) {
            TicketLoad.Loading -> LoadingBox(Modifier.padding(padding))
            TicketLoad.NotFound -> EmptyState("🕵️", "Ticket not found", "It may have been removed.", Modifier.padding(padding))
            is TicketLoad.Failed -> ErrorBox(l.message, onRetry = null, modifier = Modifier.padding(padding))
            is TicketLoad.Loaded -> {
                val t = l.ticket
                val actions = viewModel.allowedTransitions(t, now)
                LazyColumn(
                    Modifier.fillMaxSize().padding(padding),
                    contentPadding = PaddingValues(16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp),
                ) {
                    item { HeroCard(t, now) }
                    item { ProgressSteps(t.status) }
                    if (t.status.isOpen && t.assigneeName != null && t.assigneePhone.isNotBlank()) {
                        item {
                            AppCard(padding = 14.dp) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    com.company.ithelpdesk.ui.components.UserAvatar(
                                        t.assigneeId.orEmpty(),
                                        t.assigneeName.split(" ").mapNotNull { it.firstOrNull() }.take(2).joinToString(""),
                                        size = 44.dp,
                                    )
                                    Spacer(Modifier.width(12.dp))
                                    Column(Modifier.weight(1f)) {
                                        Text("${t.assigneeName} is handling this", style = MaterialTheme.typography.titleSmall)
                                        Text("Tap to call IT directly", style = MaterialTheme.typography.bodySmall,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant)
                                    }
                                    CallButton(onClick = { call(t.assigneeName, t.assigneePhone) }, contentDescription = "Call ${t.assigneeName}")
                                }
                            }
                        }
                    }
                    if (actions.isNotEmpty() || (t.status in listOf(TicketStatus.RESOLVED, TicketStatus.CLOSED) && t.rating == null)) {
                        item {
                            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                                if (TicketStatus.CLOSED in actions) {
                                    GradientButton("Yes, it's fixed! ✅", onClick = { viewModel.changeStatus(TicketStatus.CLOSED, "Confirmed by requester") },
                                        enabled = !busy, modifier = Modifier.fillMaxWidth())
                                }
                                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                    if (TicketStatus.REOPENED in actions) {
                                        OutlinedButton(onClick = { dialog = "reopen" }, enabled = !busy, shape = CircleShape) { Text("Still broken 😕") }
                                    }
                                    if (TicketStatus.CANCELLED in actions) {
                                        OutlinedButton(onClick = { dialog = "cancel" }, enabled = !busy, shape = CircleShape) { Text("Cancel ticket") }
                                    }
                                    if ((t.status == TicketStatus.RESOLVED || t.status == TicketStatus.CLOSED) && t.rating == null) {
                                        OutlinedButton(onClick = { dialog = "rate" }, enabled = !busy, shape = CircleShape) { Text("Rate ⭐") }
                                    }
                                }
                            }
                        }
                    }
                    t.resolution?.let { r ->
                        item {
                            AppCard {
                                Text("🛠️ What IT did", style = MaterialTheme.typography.titleMedium)
                                Spacer(Modifier.height(6.dp))
                                Text(r.actionTaken, style = MaterialTheme.typography.bodyMedium)
                                if (r.rootCause.isNotBlank()) {
                                    Text("Cause: ${r.rootCause}", style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant)
                                }
                            }
                        }
                    }
                    item {
                        AppCard {
                            Text("Details", style = MaterialTheme.typography.titleMedium)
                            Spacer(Modifier.height(6.dp))
                            Text(t.description, style = MaterialTheme.typography.bodyMedium)
                            Spacer(Modifier.height(8.dp))
                            InfoRow("Category", listOf(t.category, t.subCategory).filter { it.isNotBlank() }.joinToString(" › "))
                            InfoRow("Urgency", t.priority.label)
                            InfoRow("Handled by", t.assigneeName ?: "Waiting for IT")
                            InfoRow("Raised", formatDateTime(t.createdAt))
                            if (t.rating != null) InfoRow("Your rating", "★".repeat(t.rating))
                            if (t.attachments.isNotEmpty()) {
                                Spacer(Modifier.height(8.dp))
                                Row(Modifier.horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                    t.attachments.forEach { a -> TicketPhoto(t.id, a, onOpen = { openImage = it }) }
                                }
                            }
                        }
                    }
                    if (history.isNotEmpty()) {
                        item {
                            AppCard {
                                Text("Timeline", style = MaterialTheme.typography.titleMedium)
                                Spacer(Modifier.height(10.dp))
                                StatusTimeline(history, friendly = true)
                            }
                        }
                    }
                    item { SectionTitle("Chat with IT 💬") }
                    if (comments.isEmpty()) {
                        item {
                            Text("No messages yet. Add anything that might help — IT will reply here.",
                                style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                    }
                    items(comments, key = { it.id }) { c ->
                        ChatBubble(t.id, c, mine = c.authorId == viewModel.me.uid, onOpenImage = { openImage = it })
                    }
                }
            }
        }
    }

    when (dialog) {
        "reopen" -> NoteDialog("Reopen ticket", "What is still wrong? (required)", required = true, onDismiss = { dialog = null }) {
            viewModel.changeStatus(TicketStatus.REOPENED, it)
            dialog = null
        }
        "cancel" -> NoteDialog("Cancel this ticket?", "Reason (optional)", required = false, onDismiss = { dialog = null }) {
            viewModel.changeStatus(TicketStatus.CANCELLED, it)
            dialog = null
        }
        "rate" -> RateDialog(onDismiss = { dialog = null }) { stars, comment ->
            viewModel.rate(stars, comment)
            dialog = null
        }
    }
    openImage?.let { ImageDialog(it) { openImage = null } }
}

@Composable
private fun HeroCard(t: Ticket, now: Long) {
    val brand = LocalBrand.current
    Box(Modifier.fillMaxWidth().clip(RoundedCornerShape(28.dp)).background(brand.heroBrush).padding(20.dp)) {
        Column {
            Box(Modifier.clip(CircleShape).background(Color.White.copy(alpha = 0.22f)).padding(horizontal = 12.dp, vertical = 4.dp)) {
                Text(NotificationRules.userFacingStatus(t.status), color = Color.White, style = MaterialTheme.typography.labelLarge)
            }
            Spacer(Modifier.height(10.dp))
            Text(t.title, color = Color.White, style = MaterialTheme.typography.headlineSmall)
            friendlyEta(t, now)?.let {
                Spacer(Modifier.height(6.dp))
                Text("⏱️ $it", color = Color.White.copy(alpha = 0.9f), style = MaterialTheme.typography.bodyMedium)
            }
        }
    }
}

/** Five friendly steps; on-hold and reopened map onto "Working on it". */
@Composable
private fun ProgressSteps(status: TicketStatus) {
    val steps = listOf("Received", "Assigned", "Working", "Fixed", "Closed")
    val current = when (status) {
        TicketStatus.NEW -> 0
        TicketStatus.ASSIGNED -> 1
        TicketStatus.IN_PROGRESS, TicketStatus.ON_HOLD_USER, TicketStatus.ON_HOLD_PARTS, TicketStatus.REOPENED -> 2
        TicketStatus.RESOLVED -> 3
        TicketStatus.CLOSED -> 4
        TicketStatus.CANCELLED -> -1
    }
    if (current < 0) {
        Text("This ticket was cancelled.", color = MaterialTheme.colorScheme.onSurfaceVariant)
        return
    }
    val brand = LocalBrand.current
    Column {
    Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
        steps.forEachIndexed { i, label ->
            Column(Modifier.weight(1f), horizontalAlignment = Alignment.CenterHorizontally) {
                Box(
                    Modifier.size(28.dp).clip(CircleShape)
                        .background(if (i <= current) brand.accentBrush else androidx.compose.ui.graphics.SolidColor(MaterialTheme.colorScheme.surfaceVariant)),
                    contentAlignment = Alignment.Center,
                ) {
                    Text(if (i < current) "✓" else "${i + 1}", color = if (i <= current) Color.White else MaterialTheme.colorScheme.onSurfaceVariant,
                        style = MaterialTheme.typography.labelMedium)
                }
                Spacer(Modifier.height(4.dp))
                Text(label, style = MaterialTheme.typography.labelSmall,
                    color = if (i <= current) MaterialTheme.colorScheme.onSurface else MaterialTheme.colorScheme.onSurfaceVariant)
            }
            if (i < steps.lastIndex) Spacer(Modifier.width(2.dp))
        }
    }
    if (status == TicketStatus.ON_HOLD_USER) {
        Spacer(Modifier.height(4.dp))
        Text("👉 IT is waiting for a reply from you — check the chat below.", color = MaterialTheme.colorScheme.tertiary,
            style = MaterialTheme.typography.bodyMedium)
    }
    }
}
