package com.company.ithelpdesk.ui.admin

import android.content.Intent
import android.net.Uri
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.outlined.Call
import androidx.compose.material.icons.outlined.Email
import androidx.compose.material3.AssistChip
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
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.company.ithelpdesk.domain.logic.SlaCalculator
import com.company.ithelpdesk.domain.logic.TicketTimes
import com.company.ithelpdesk.domain.model.TicketStatus
import com.company.ithelpdesk.ui.components.AppCard
import com.company.ithelpdesk.ui.components.ChatBubble
import com.company.ithelpdesk.ui.components.EmptyState
import com.company.ithelpdesk.ui.components.ErrorBox
import com.company.ithelpdesk.ui.components.GradientButton
import com.company.ithelpdesk.ui.components.InfoRow
import com.company.ithelpdesk.ui.components.LoadingBox
import com.company.ithelpdesk.ui.components.PriorityChip
import com.company.ithelpdesk.ui.components.SectionTitle
import com.company.ithelpdesk.ui.components.SlaBadge
import com.company.ithelpdesk.ui.components.StatusChip
import com.company.ithelpdesk.ui.components.StatusTimeline
import com.company.ithelpdesk.ui.components.TicketPhoto
import com.company.ithelpdesk.ui.ticket.AssignDialog
import com.company.ithelpdesk.ui.ticket.ChatInput
import com.company.ithelpdesk.ui.ticket.ImageDialog
import com.company.ithelpdesk.ui.ticket.NoteDialog
import com.company.ithelpdesk.ui.ticket.ResolveDialog
import com.company.ithelpdesk.ui.ticket.TicketDetailViewModel
import com.company.ithelpdesk.ui.ticket.TicketLoad
import com.company.ithelpdesk.util.formatDateTime
import com.company.ithelpdesk.util.formatDuration
import com.company.ithelpdesk.util.rememberNow

/** The admin's working view of a ticket: pick up, assign, move through the workflow, internal notes. */
@OptIn(ExperimentalMaterial3Api::class, androidx.compose.foundation.layout.ExperimentalLayoutApi::class)
@Composable
fun AdminTicketScreen(onBack: () -> Unit, viewModel: TicketDetailViewModel = hiltViewModel()) {
    val load by viewModel.ticket.collectAsStateWithLifecycle()
    val comments by viewModel.comments.collectAsStateWithLifecycle()
    val history by viewModel.history.collectAsStateWithLifecycle()
    val admins by viewModel.admins.collectAsStateWithLifecycle()
    val busy by viewModel.busy.collectAsStateWithLifecycle()
    val message by viewModel.message.collectAsStateWithLifecycle()
    val snackbar = remember { SnackbarHostState() }
    val context = LocalContext.current
    val now = rememberNow()
    var dialog by rememberSaveable { mutableStateOf<String?>(null) }
    var openImage by remember { mutableStateOf<Any?>(null) }

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
            if (load is TicketLoad.Loaded) {
                ChatInput(busy = busy, allowInternal = true, placeholder = "Reply or add a note…") { text, internal, images, clear ->
                    viewModel.sendComment(text, internal, images, clear)
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
                val transitions = viewModel.allowedTransitions(t, now)
                val sla = SlaCalculator.evaluate(t, now)
                LazyColumn(
                    Modifier.fillMaxSize().padding(padding),
                    contentPadding = PaddingValues(16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp),
                ) {
                    item {
                        Column {
                            Text(t.title, style = MaterialTheme.typography.headlineSmall)
                            Spacer(Modifier.height(8.dp))
                            Row(Modifier.horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                StatusChip(t.status)
                                PriorityChip(t.priority)
                                if (t.slaResolveDueAt != null) SlaBadge(sla)
                            }
                            SlaCalculator.remainingMs(t, now)?.takeIf { t.status.isOpen }?.let { left ->
                                Spacer(Modifier.height(6.dp))
                                Text(
                                    if (left >= 0) "⏳ ${formatDuration(left)} left to resolve" else "⛔ Overdue by ${formatDuration(-left)}",
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = if (left >= 0) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.error,
                                )
                            }
                        }
                    }
                    item {
                        AppCard {
                            Text("Actions", style = MaterialTheme.typography.titleMedium)
                            Spacer(Modifier.height(10.dp))
                            if (t.status.isOpen && t.assigneeId != viewModel.me.uid) {
                                GradientButton(
                                    if (t.assigneeId == null) "Pick up this ticket" else "Take over from ${t.assigneeName?.substringBefore(' ')}",
                                    onClick = viewModel::pickUp,
                                    enabled = !busy,
                                    modifier = Modifier.fillMaxWidth(),
                                )
                                Spacer(Modifier.height(8.dp))
                            }
                            FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                                if (t.status.isOpen) {
                                    OutlinedButton(onClick = { dialog = "assign" }, enabled = !busy, shape = CircleShape) { Text("Assign…") }
                                }
                                transitions.forEach { to ->
                                    OutlinedButton(
                                        onClick = {
                                            when (to) {
                                                TicketStatus.RESOLVED -> dialog = "resolve"
                                                TicketStatus.CANCELLED -> dialog = "cancel"
                                                TicketStatus.ON_HOLD_USER, TicketStatus.ON_HOLD_PARTS, TicketStatus.REOPENED -> dialog = "note:${to.name}"
                                                else -> viewModel.changeStatus(to)
                                            }
                                        },
                                        enabled = !busy,
                                        shape = CircleShape,
                                    ) { Text(actionLabel(to)) }
                                }
                            }
                        }
                    }
                    item {
                        AppCard {
                            Text("Requester", style = MaterialTheme.typography.titleMedium)
                            Spacer(Modifier.height(6.dp))
                            InfoRow("Name", t.requesterName)
                            InfoRow("Department", t.department)
                            InfoRow("Location", t.location)
                            InfoRow("Prefers", t.contactMethod.label)
                            Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.padding(top = 6.dp)) {
                                if (t.requesterPhone.isNotBlank()) {
                                    AssistChip(
                                        onClick = { context.startActivity(Intent(Intent.ACTION_DIAL, Uri.parse("tel:${t.requesterPhone}"))) },
                                        label = { Text(t.requesterPhone) },
                                        leadingIcon = { Icon(Icons.Outlined.Call, null) },
                                    )
                                }
                                if (t.requesterEmail.isNotBlank()) {
                                    AssistChip(
                                        onClick = {
                                            context.startActivity(
                                                Intent(Intent.ACTION_SENDTO, Uri.parse("mailto:${t.requesterEmail}"))
                                                    .putExtra(Intent.EXTRA_SUBJECT, "${t.displayNumber}: ${t.title}"),
                                            )
                                        },
                                        label = { Text("Email") },
                                        leadingIcon = { Icon(Icons.Outlined.Email, null) },
                                    )
                                }
                            }
                        }
                    }
                    item {
                        AppCard {
                            Text("Problem", style = MaterialTheme.typography.titleMedium)
                            Spacer(Modifier.height(6.dp))
                            Text(t.description, style = MaterialTheme.typography.bodyMedium)
                            Spacer(Modifier.height(8.dp))
                            InfoRow("Category", listOf(t.category, t.subCategory).filter { it.isNotBlank() }.joinToString(" › "))
                            InfoRow("Assignee", t.assigneeName ?: "Unassigned")
                            InfoRow("Asset tag", t.assetTag)
                            InfoRow("Device", t.deviceInfo?.summary ?: "")
                            InfoRow("Created", formatDateTime(t.createdAt))
                            InfoRow("1st response", formatDuration(TicketTimes.firstResponseMs(t)))
                            InfoRow("Repair time", formatDuration(TicketTimes.repairMs(t)))
                            InfoRow("On hold total", formatDuration(t.onHoldTotalMs + TicketTimes.currentHoldMs(t, now)))
                            if (t.reopenCount > 0) InfoRow("Reopened", "${t.reopenCount}×")
                            if (t.rating != null) InfoRow("Rating", "★".repeat(t.rating) + " ${t.ratingComment}")
                            if (t.attachments.isNotEmpty()) {
                                Spacer(Modifier.height(8.dp))
                                Row(Modifier.horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                    t.attachments.forEach { a -> TicketPhoto(t.id, a, onOpen = { openImage = it }) }
                                }
                            }
                        }
                    }
                    t.resolution?.let { r ->
                        item {
                            AppCard {
                                Text("Resolution", style = MaterialTheme.typography.titleMedium)
                                InfoRow("Type", r.category)
                                InfoRow("Root cause", r.rootCause)
                                InfoRow("Action", r.actionTaken)
                                if (r.partsUsed.isNotBlank()) InfoRow("Parts", r.partsUsed)
                            }
                        }
                    }
                    if (history.isNotEmpty()) {
                        item {
                            AppCard {
                                Text("History", style = MaterialTheme.typography.titleMedium)
                                Spacer(Modifier.height(10.dp))
                                StatusTimeline(history, friendly = false)
                            }
                        }
                    }
                    item { SectionTitle("Conversation") }
                    if (comments.isEmpty()) {
                        item {
                            Text("No messages yet.", style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                    }
                    items(comments, key = { it.id }) { c ->
                        ChatBubble(t.id, c, mine = c.authorId == viewModel.me.uid, onOpenImage = { openImage = it })
                    }
                    item { Spacer(Modifier.width(1.dp).height(8.dp)) }
                }

                when (val d = dialog) {
                    "assign" -> AssignDialog(admins, t.assigneeId, onDismiss = { dialog = null }) {
                        viewModel.assign(it)
                        dialog = null
                    }
                    "resolve" -> ResolveDialog(onDismiss = { dialog = null }) {
                        viewModel.changeStatus(TicketStatus.RESOLVED, resolution = it)
                        dialog = null
                    }
                    "cancel" -> NoteDialog("Cancel ticket", "Reason (required)", required = true, onDismiss = { dialog = null }) {
                        viewModel.changeStatus(TicketStatus.CANCELLED, it)
                        dialog = null
                    }
                    null -> Unit
                    else -> if (d.startsWith("note:")) {
                        val to = TicketStatus.from(d.removePrefix("note:"))
                        NoteDialog(actionLabel(to), "Note for the requester", required = to == TicketStatus.ON_HOLD_USER,
                            onDismiss = { dialog = null }) {
                            viewModel.changeStatus(to, it)
                            dialog = null
                        }
                    }
                }
            }
        }
    }
    openImage?.let { ImageDialog(it) { openImage = null } }
}

private fun actionLabel(to: TicketStatus): String = when (to) {
    TicketStatus.IN_PROGRESS -> "▶ Start work"
    TicketStatus.ON_HOLD_USER -> "⏸ Wait for user"
    TicketStatus.ON_HOLD_PARTS -> "📦 Wait for parts"
    TicketStatus.RESOLVED -> "✅ Resolve"
    TicketStatus.CLOSED -> "🔒 Close"
    TicketStatus.REOPENED -> "↩ Reopen"
    TicketStatus.CANCELLED -> "✖ Cancel"
    else -> to.label
}
