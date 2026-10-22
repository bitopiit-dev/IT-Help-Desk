// © 2026 Khondaker Arifur Rahman. All Rights Reserved.
// BGL-IT Help — see COPYRIGHT.md. Unauthorized copying, modification or redistribution is prohibited.

package com.company.ithelpdesk.ui.shared

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.company.ithelpdesk.data.notify.SystemNotifier
import com.company.ithelpdesk.domain.model.AppNotification
import androidx.compose.runtime.setValue
import com.company.ithelpdesk.ui.components.EmptyState
import com.company.ithelpdesk.ui.components.ErrorBox
import com.company.ithelpdesk.ui.components.SkeletonList
import com.company.ithelpdesk.util.formatRelative
import com.company.ithelpdesk.util.rememberNow

private fun kindEmoji(kind: String) = when (kind) {
    AppNotification.KIND_SIGNUP -> "🙋"
    AppNotification.KIND_ACCOUNT -> "🎉"
    AppNotification.KIND_TICKET -> "🎫"
    AppNotification.KIND_ASSIGN -> "🧑‍🔧"
    AppNotification.KIND_MESSAGE -> "💬"
    else -> "🔔"
}

/** Notification centre; looks native in both apps because it only uses theme colours. */
@Composable
fun NotificationsScreen(
    viewModel: NotificationsViewModel,
    onOpenTicket: (String) -> Unit,
    onOpenSignups: (() -> Unit)? = null,
) {
    val ui by viewModel.ui.collectAsStateWithLifecycle()
    val now = rememberNow()
    val context = androidx.compose.ui.platform.LocalContext.current
    // Re-checked every time the screen is shown (e.g. after coming back from phone settings).
    val lifecycle = androidx.lifecycle.compose.LocalLifecycleOwner.current
    var enabled by androidx.compose.runtime.remember { androidx.compose.runtime.mutableStateOf(SystemNotifier.isEnabled(context)) }
    androidx.compose.runtime.DisposableEffect(lifecycle) {
        val observer = androidx.lifecycle.LifecycleEventObserver { _, e ->
            if (e == androidx.lifecycle.Lifecycle.Event.ON_RESUME) enabled = SystemNotifier.isEnabled(context)
        }
        lifecycle.lifecycle.addObserver(observer)
        onDispose { lifecycle.lifecycle.removeObserver(observer) }
    }
    Column(Modifier.fillMaxSize().statusBarsPadding()) {
        Row(Modifier.fillMaxWidth().padding(start = 20.dp, end = 8.dp, top = 12.dp), verticalAlignment = Alignment.CenterVertically) {
            Column(Modifier.weight(1f)) {
                Text("Alerts", style = MaterialTheme.typography.headlineMedium)
                Text(
                    if (ui.unread == 0) "You're all caught up" else "${ui.unread} new",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            if (ui.unread > 0) TextButton(onClick = viewModel::markAllRead) { Text("Mark all read") }
        }
        if (!enabled) {
            com.company.ithelpdesk.ui.components.Banner(
                "Notifications are off, so you won't hear the alert tone. Tap to turn them on.",
                "🔕",
                MaterialTheme.colorScheme.error,
                modifier = Modifier.padding(horizontal = 16.dp, vertical = 6.dp),
                onClick = { SystemNotifier.openSettings(context) },
            )
        }
        when {
            ui.loading -> SkeletonList()
            ui.error != null -> ErrorBox(ui.error!!, onRetry = null)
            ui.items.isEmpty() -> EmptyState("🔕", "Nothing yet", "Updates about your tickets will pop up here.")
            else -> LazyColumn(contentPadding = PaddingValues(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                items(ui.items, key = { it.id }) { n ->
                    val unread = !n.isReadBy(viewModel.me.uid)
                    Row(
                        Modifier
                            .fillMaxWidth()
                            .clip(MaterialTheme.shapes.large)
                            .background(
                                if (unread) MaterialTheme.colorScheme.primary.copy(alpha = 0.10f)
                                else MaterialTheme.colorScheme.surfaceContainerLow,
                            )
                            .clickable {
                                viewModel.markRead(n)
                                when {
                                    n.ticketId != null -> onOpenTicket(n.ticketId)
                                    n.kind == AppNotification.KIND_SIGNUP && onOpenSignups != null -> onOpenSignups()
                                }
                            }
                            .padding(14.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Box(
                            Modifier.size(42.dp).clip(CircleShape).background(MaterialTheme.colorScheme.surfaceContainerHigh),
                            contentAlignment = Alignment.Center,
                        ) { Text(kindEmoji(n.kind), fontSize = 20.sp) }
                        Spacer(Modifier.width(12.dp))
                        Column(Modifier.weight(1f)) {
                            Text(n.title, style = MaterialTheme.typography.titleSmall)
                            Text(n.body, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            Text(formatRelative(n.createdAt, now), style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                        if (unread) Box(Modifier.size(10.dp).clip(CircleShape).background(MaterialTheme.colorScheme.secondary))
                    }
                }
            }
        }
    }
}
