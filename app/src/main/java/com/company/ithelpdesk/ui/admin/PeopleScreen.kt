// © 2026 Khondaker Arifur Rahman. All Rights Reserved.
// BGL-IT Help — see COPYRIGHT.md. Unauthorized copying, modification or redistribution is prohibited.

package com.company.ithelpdesk.ui.admin

import androidx.compose.foundation.horizontalScroll
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Surface
import androidx.compose.ui.text.font.FontWeight
import com.company.ithelpdesk.domain.model.UserPriority
import com.company.ithelpdesk.ui.components.StatusChip
import com.company.ithelpdesk.util.formatDate
import androidx.compose.foundation.clickable
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.outlined.Delete
import androidx.compose.material3.OutlinedTextField
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
fun PeopleScreen(viewModel: PeopleViewModel, onOpenTicket: (String) -> Unit = {}) {
    val ui by viewModel.ui.collectAsStateWithLifecycle()
    val message by viewModel.message.collectAsStateWithLifecycle()
    val snackbar = remember { SnackbarHostState() }
    val now = rememberNow()
    var tab by rememberSaveable { mutableStateOf(PeopleTab.PENDING) }
    var priorityFilter by rememberSaveable { mutableStateOf<UserPriority?>(null) }
    var confirmDecline by remember { mutableStateOf<UserProfile?>(null) }
    var confirmResetPassword by remember { mutableStateOf<UserProfile?>(null) }
    var editUser by remember { mutableStateOf<UserProfile?>(null) }
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

            if (viewModel.me.isSuperAdmin && tab != PeopleTab.PENDING) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .horizontalScroll(rememberScrollState())
                        .padding(horizontal = 12.dp, vertical = 6.dp),
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                ) {
                    FilterChip(
                        selected = priorityFilter == null,
                        onClick = { priorityFilter = null },
                        label = { Text("All Priority") },
                        shape = CircleShape,
                    )
                    UserPriority.entries.forEach { p ->
                        FilterChip(
                            selected = priorityFilter == p,
                            onClick = { priorityFilter = if (priorityFilter == p) null else p },
                            label = { Text("${p.emoji} ${p.label}") },
                            shape = CircleShape,
                        )
                    }
                }
            }

            val list = ui.list(tab).filter { priorityFilter == null || it.userPriority == priorityFilter }
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
                            showPriorityTag = viewModel.me.isSuperAdmin,
                            now = now,
                            onApprove = { viewModel.approve(u) },
                            onDecline = { confirmDecline = u },
                            onRole = { viewModel.setRole(u, it) },
                            onActive = { viewModel.setActive(u, it) },
                            onResetPassword = { confirmResetPassword = u },
                            onEditProfile = { editUser = u },
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

    confirmResetPassword?.let { u ->
        AlertDialog(
            onDismissRequest = { confirmResetPassword = null },
            title = { Text("Reset password for ${u.firstName}?") },
            text = { Text("A password reset link will be sent to ${u.email}. They can click the link in their email inbox to set a new password.") },
            confirmButton = {
                Button(onClick = { viewModel.resetPassword(u); confirmResetPassword = null }) {
                    Text("Send reset link")
                }
            },
            dismissButton = { TextButton(onClick = { confirmResetPassword = null }) { Text("Cancel") } },
        )
    }

    editUser?.let { u ->
        EditUserDialog(
            user = u,
            viewModel = viewModel,
            onOpenTicket = onOpenTicket,
            onDismiss = { editUser = null },
            onSave = { name, employeeId, department, phone, location, extraFields ->
                viewModel.updateUserProfile(u, name, employeeId, department, phone, location, extraFields)
                editUser = null
            },
        )
    }
}

@Composable
private fun PersonCard(
    user: UserProfile,
    isMe: Boolean,
    showPriorityTag: Boolean,
    now: Long,
    onApprove: () -> Unit,
    onDecline: () -> Unit,
    onRole: (Role) -> Unit,
    onActive: (Boolean) -> Unit,
    onResetPassword: () -> Unit,
    onEditProfile: () -> Unit,
    onCall: () -> Unit,
) {
    var menu by remember { mutableStateOf(false) }
    AppCard(
        padding = 14.dp,
        modifier = Modifier.clickable(enabled = !user.isPending) { onEditProfile() },
    ) {
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
                Text(
                    user.email,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
                Text(
                    listOf(
                        if (user.isSuperAdmin) "👑 Super Admin" else if (user.role.isAdmin) "🛡️ Admin" else "",
                        if (showPriorityTag || user.userPriority != UserPriority.STANDARD) "${user.userPriority.emoji} ${user.userPriority.label}" else "",
                        user.department,
                        user.phone,
                        if (user.employeeId.isNotBlank()) "ID ${user.employeeId}" else "",
                        if (user.location.isNotBlank()) user.location else "",
                    ).filter { it.isNotBlank() }.joinToString(" · "),
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                if (user.extraFields.isNotEmpty()) {
                    Spacer(Modifier.height(2.dp))
                    Text(
                        user.extraFields.entries.joinToString(" · ") { "${it.key}: ${it.value}" },
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.primary,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis,
                    )
                }
            }
            if (!isMe && user.phone.isNotBlank()) {
                CallButton(onClick = onCall, contentDescription = "Call ${user.name}")
            }
            if (!user.isPending) {
                Box {
                    IconButton(onClick = { menu = true }) { Icon(Icons.Default.MoreVert, "Options for ${user.name}") }
                    DropdownMenu(expanded = menu, onDismissRequest = { menu = false }) {
                        DropdownMenuItem(text = { Text("✏️ Edit info & sub-sections") }, onClick = { menu = false; onEditProfile() })
                        if (!isMe && !user.isSuperAdmin) {
                            if (user.isActive) {
                                if (user.role.isAdmin) {
                                    DropdownMenuItem(text = { Text("Make regular user") }, onClick = { menu = false; onRole(Role.USER) })
                                } else {
                                    DropdownMenuItem(text = { Text("Make admin 🛡️") }, onClick = { menu = false; onRole(Role.ADMIN) })
                                }
                                DropdownMenuItem(text = { Text("Reset password 🔑") }, onClick = { menu = false; onResetPassword() })
                                DropdownMenuItem(text = { Text("Block access") }, onClick = { menu = false; onActive(false) })
                            } else {
                                DropdownMenuItem(text = { Text("Restore & approve") }, onClick = { menu = false; onActive(true) })
                            }
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

@Composable
private fun EditUserDialog(
    user: UserProfile,
    viewModel: PeopleViewModel,
    onOpenTicket: (String) -> Unit,
    onDismiss: () -> Unit,
    onSave: (name: String, employeeId: String, department: String, phone: String, location: String, extraFields: Map<String, String>) -> Unit,
) {
    var name by rememberSaveable { mutableStateOf(user.name) }
    var employeeId by rememberSaveable { mutableStateOf(user.employeeId) }
    var department by rememberSaveable { mutableStateOf(user.department) }
    var phone by rememberSaveable { mutableStateOf(user.phone) }
    var location by rememberSaveable { mutableStateOf(user.location) }

    var extraMap by remember { mutableStateOf(user.extraFields.toMutableMap()) }
    var showAddFieldDialog by remember { mutableStateOf(false) }

    val userTickets by viewModel.observeUserTickets(user.uid).collectAsStateWithLifecycle(emptyList())

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Profile & Details: ${user.firstName}") },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                // Super Admin Priority Setting (only visible to Super Admin)
                if (viewModel.me.isSuperAdmin) {
                    Text(
                        "⭐ User Priority (Super Admin Only)",
                        style = MaterialTheme.typography.titleSmall,
                        color = MaterialTheme.colorScheme.primary,
                    )
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .horizontalScroll(rememberScrollState()),
                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                    ) {
                        UserPriority.entries.forEach { p ->
                            FilterChip(
                                selected = user.userPriority == p,
                                onClick = { viewModel.setUserPriority(user, p) },
                                label = { Text("${p.emoji} ${p.label}") },
                                shape = CircleShape,
                            )
                        }
                    }
                }

                // Ticket History Section
                Text(
                    "🎫 Ticket History (${userTickets.size} raised since joining)",
                    style = MaterialTheme.typography.titleSmall,
                    color = MaterialTheme.colorScheme.primary,
                )
                if (userTickets.isEmpty()) {
                    Text(
                        "No tickets raised by ${user.firstName} yet.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                } else {
                    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                        userTickets.forEach { t ->
                            Surface(
                                color = MaterialTheme.colorScheme.surfaceContainerHigh,
                                shape = MaterialTheme.shapes.medium,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable {
                                        onDismiss()
                                        onOpenTicket(t.id)
                                    },
                            ) {
                                Row(
                                    modifier = Modifier.padding(10.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                ) {
                                    Column(modifier = Modifier.weight(1f)) {
                                        Text(
                                            "${t.displayNumber} · ${t.title}",
                                            style = MaterialTheme.typography.bodyMedium,
                                            fontWeight = FontWeight.SemiBold,
                                            maxLines = 1,
                                            overflow = TextOverflow.Ellipsis,
                                        )
                                        Text(
                                            "${t.category} · ${formatDate(t.createdAt)}",
                                            style = MaterialTheme.typography.labelSmall,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                                        )
                                    }
                                    Spacer(Modifier.width(8.dp))
                                    StatusChip(t.status)
                                }
                            }
                        }
                    }
                }

                Text(
                    "Standard Info",
                    style = MaterialTheme.typography.titleSmall,
                    color = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.padding(top = 4.dp),
                )

                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it },
                    label = { Text("Full Name") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                )
                OutlinedTextField(
                    value = employeeId,
                    onValueChange = { employeeId = it },
                    label = { Text("Employee ID") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                )
                OutlinedTextField(
                    value = department,
                    onValueChange = { department = it },
                    label = { Text("Department") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                )
                OutlinedTextField(
                    value = phone,
                    onValueChange = { phone = it },
                    label = { Text("Phone Number") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                )
                OutlinedTextField(
                    value = location,
                    onValueChange = { location = it },
                    label = { Text("Location / Desk") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                )

                Text(
                    "Custom Sub-sections / Extra Info",
                    style = MaterialTheme.typography.titleSmall,
                    color = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.padding(top = 4.dp),
                )

                extraMap.forEach { (key, valStr) ->
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                        modifier = Modifier.fillMaxWidth(),
                    ) {
                        OutlinedTextField(
                            value = valStr,
                            onValueChange = { newV ->
                                extraMap = extraMap.toMutableMap().apply { put(key, newV) }
                            },
                            label = { Text(key) },
                            singleLine = true,
                            modifier = Modifier.weight(1f),
                        )
                        IconButton(onClick = {
                            extraMap = extraMap.toMutableMap().apply { remove(key) }
                        }) {
                            Icon(Icons.Outlined.Delete, contentDescription = "Delete $key", tint = MaterialTheme.colorScheme.error)
                        }
                    }
                }

                OutlinedButton(
                    onClick = { showAddFieldDialog = true },
                    modifier = Modifier.fillMaxWidth(),
                    shape = CircleShape,
                ) {
                    Icon(Icons.Default.Add, contentDescription = null)
                    Spacer(Modifier.width(6.dp))
                    Text("Add custom sub-section")
                }
            }
        },
        confirmButton = {
            Button(onClick = {
                onSave(name, employeeId, department, phone, location, extraMap)
            }) {
                Text("Save changes")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Cancel") }
        },
    )

    if (showAddFieldDialog) {
        var newKey by rememberSaveable { mutableStateOf("") }
        var newVal by rememberSaveable { mutableStateOf("") }

        AlertDialog(
            onDismissRequest = { showAddFieldDialog = false },
            title = { Text("Add Custom Sub-section") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Text(
                        "Enter a sub-section title (e.g. Designation, Shift, Emergency Contact, Laptop Serial) and value:",
                        style = MaterialTheme.typography.bodyMedium,
                    )
                    OutlinedTextField(
                        value = newKey,
                        onValueChange = { newKey = it },
                        label = { Text("Sub-section Title (e.g. Designation)") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth(),
                    )
                    OutlinedTextField(
                        value = newVal,
                        onValueChange = { newVal = it },
                        label = { Text("Value (e.g. Sr. Executive)") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth(),
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        if (newKey.isNotBlank()) {
                            extraMap = extraMap.toMutableMap().apply { put(newKey.trim(), newVal.trim()) }
                        }
                        showAddFieldDialog = false
                    },
                    enabled = newKey.isNotBlank(),
                ) {
                    Text("Add")
                }
            },
            dismissButton = {
                TextButton(onClick = { showAddFieldDialog = false }) { Text("Cancel") }
            },
        )
    }
}
