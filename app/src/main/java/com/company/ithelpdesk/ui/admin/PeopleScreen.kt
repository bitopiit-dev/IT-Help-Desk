// © 2026 Khondaker Arifur Rahman. All Rights Reserved.
// BGL-IT Help — see COPYRIGHT.md. Unauthorized copying, modification or redistribution is prohibited.

package com.company.ithelpdesk.ui.admin

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
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.ScrollableTabRow
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Tab
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.company.ithelpdesk.domain.model.Role
import com.company.ithelpdesk.domain.model.UserProfile
import com.company.ithelpdesk.ui.components.AppCard
import com.company.ithelpdesk.ui.components.CallButton
import com.company.ithelpdesk.ui.components.rememberPhoneCaller
import com.company.ithelpdesk.ui.components.UserAvatar
import com.company.ithelpdesk.ui.components.EmptyState
import com.company.ithelpdesk.ui.components.ErrorBox
import com.company.ithelpdesk.ui.components.SkeletonList
import com.company.ithelpdesk.util.formatRelative
import com.company.ithelpdesk.util.rememberNow

/** Sign-up approvals and user management. Only admins can open this (rules enforce it too). */
@Composable
fun PeopleScreen(viewModel: PeopleViewModel) {
    val ui by viewModel.ui.collectAsStateWithLifecycle()
    val message by viewModel.message.collectAsStateWithLifecycle()
    val snackbar = remember { SnackbarHostState() }
    val now = rememberNow()
    var tab by rememberSaveable { mutableStateOf(PeopleTab.PENDING) }
    var confirmDecline by remember { mutableStateOf<UserProfile?>(null) }
    val call = rememberPhoneCaller()

    LaunchedEffect(message) {
        message?.let {
            snackbar.showSnackbar(it)
            viewModel.consumeMessage()
        }
    }

    Box(Modifier.fillMaxSize()) {
        Column(Modifier.fillMaxSize().statusBarsPadding()) {
            Row(Modifier.fillMaxWidth().padding(start = 16.dp, end = 8.dp, top = 12.dp), verticalAlignment = Alignment.CenterVertically) {
                Column(Modifier.weight(1f)) {
                    Text("People", style = MaterialTheme.typography.headlineMedium)
                    Text(
                        "${ui.all.count { it.isActive && it.isApproved }} active · ${ui.pendingCount} waiting",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
                if (tab == PeopleTab.PENDING && ui.pendingCount > 1) {
                    TextButton(onClick = viewModel::approveAll) { Text("Approve all") }
                }
            }
            ScrollableTabRow(selectedTabIndex = tab.ordinal, edgePadding = 12.dp, containerColor = MaterialTheme.colorScheme.background) {
                PeopleTab.entries.forEach { t ->
                    Tab(selected = tab == t, onClick = { tab = t }, text = { Text("${t.label} (${ui.list(t).size})") })
                }
            }
            val list = ui.list(tab)
            when {
                ui.loading -> SkeletonList()
                ui.error != null -> ErrorBox(ui.error!!, onRetry = null)
                list.isEmpty() -> EmptyState(
                    if (tab == PeopleTab.PENDING) "🎈" else "👥",
                    if (tab == PeopleTab.PENDING) "No requests waiting" else "Nobody here",
                    if (tab == PeopleTab.PENDING) "New sign-ups show up here instantly, and you get an alert." else "",
                )
                else -> LazyColumn(contentPadding = PaddingValues(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    items(list, key = { it.uid }) { u ->
                        PersonCard(
                            user = u,
                            isMe = u.uid == viewModel.me.uid,
                            now = now,
                            onApprove = { viewModel.approve(u) },
                            onDecline = { confirmDecline = u },
                            onRole = { viewModel.setRole(u, it) },
                            onActive = { viewModel.setActive(u, it) },
                            onCall = { call(u.name, u.phone) },
                        )
                    }
                }
            }
        }
        SnackbarHost(snackbar, Modifier.align(Alignment.BottomCenter))
    }

    confirmDecline?.let { u ->
        AlertDialog(
            onDismissRequest = { confirmDecline = null },
            title = { Text("Decline ${u.firstName}?") },
            text = { Text("${u.email} won't be able to use BGL-IT Help. You can approve them later from the Blocked tab.") },
            confirmButton = {
                TextButton(onClick = { viewModel.decline(u); confirmDecline = null }) {
                    Text("Decline", color = MaterialTheme.colorScheme.error)
                }
            },
            dismissButton = { TextButton(onClick = { confirmDecline = null }) { Text("Back") } },
        )
    }
}

@Composable
private fun PersonCard(
    user: UserProfile,
    isMe: Boolean,
    now: Long,
    onApprove: () -> Unit,
    onDecline: () -> Unit,
    onRole: (Role) -> Unit,
    onActive: (Boolean) -> Unit,
    onCall: () -> Unit,
) {
    var menu by remember { mutableStateOf(false) }
    AppCard(padding = 14.dp) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            UserAvatar(user.uid, user.initials, size = 46.dp, photoVersion = user.photoVersion)
            Spacer(Modifier.width(12.dp))
            Column(Modifier.weight(1f)) {
                Text(
                    user.name + if (isMe) " (you)" else "",
                    style = MaterialTheme.typography.titleSmall,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
                Text(user.email, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1, overflow = TextOverflow.Ellipsis)
                Text(
                    listOf(
                        if (user.role.isAdmin) "🛡️ Admin" else "",
                        user.department,
                        user.phone,
                        if (user.employeeId.isNotBlank()) "ID ${user.employeeId}" else "",
                    ).filter { it.isNotBlank() }.joinToString(" · "),
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            if (!isMe && user.phone.isNotBlank()) {
                CallButton(onClick = onCall, contentDescription = "Call ${user.name}")
            }
            if (!isMe && !user.isPending) {
                Box {
                    IconButton(onClick = { menu = true }) { Icon(Icons.Default.MoreVert, "Options for ${user.name}") }
                    DropdownMenu(expanded = menu, onDismissRequest = { menu = false }) {
                        if (user.isActive) {
                            if (user.role.isAdmin) {
                                DropdownMenuItem(text = { Text("Make regular user") }, onClick = { menu = false; onRole(Role.USER) })
                            } else {
                                DropdownMenuItem(text = { Text("Make admin 🛡️") }, onClick = { menu = false; onRole(Role.ADMIN) })
                            }
                            DropdownMenuItem(text = { Text("Block access") }, onClick = { menu = false; onActive(false) })
                        } else {
                            DropdownMenuItem(text = { Text("Restore & approve") }, onClick = { menu = false; onActive(true) })
                        }
                    }
                }
            }
        }
        if (user.isPending) {
            Spacer(Modifier.height(4.dp))
            Text("Requested ${formatRelative(user.createdAt, now)}", style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant)
            Spacer(Modifier.height(10.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Button(
                    onClick = onApprove,
                    shape = CircleShape,
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.secondary,
                        contentColor = MaterialTheme.colorScheme.onSecondary,
                    ),
                    modifier = Modifier.weight(1f),
                ) { Text("✓ Approve") }
                OutlinedButton(onClick = onDecline, shape = CircleShape, modifier = Modifier.weight(1f)) { Text("Decline") }
            }
        }
    }
}
