package com.company.ithelpdesk.ui.ticket

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.outlined.StarOutline
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.ListItem
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import coil.compose.AsyncImage
import com.company.ithelpdesk.domain.model.Resolution
import com.company.ithelpdesk.domain.model.UserProfile
import com.company.ithelpdesk.ui.components.DropdownField

val RESOLUTION_TYPES = listOf(
    "Fixed / repaired",
    "Configuration change",
    "Software installed / updated",
    "Hardware replaced",
    "Password / access granted",
    "User guidance",
    "Workaround provided",
    "Escalated to vendor",
    "Not reproducible",
)

@Composable
fun NoteDialog(title: String, noteLabel: String, required: Boolean, onDismiss: () -> Unit, onConfirm: (String) -> Unit) {
    var note by rememberSaveable { mutableStateOf("") }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(title) },
        text = {
            OutlinedTextField(note, { note = it }, label = { Text(noteLabel) }, minLines = 2, modifier = Modifier.fillMaxWidth())
        },
        confirmButton = {
            TextButton(onClick = { onConfirm(note) }, enabled = !required || note.isNotBlank()) { Text("Confirm") }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Back") } },
    )
}

@Composable
fun ResolveDialog(onDismiss: () -> Unit, onConfirm: (Resolution) -> Unit) {
    var rootCause by rememberSaveable { mutableStateOf("") }
    var action by rememberSaveable { mutableStateOf("") }
    var type by rememberSaveable { mutableStateOf<String?>(null) }
    var parts by rememberSaveable { mutableStateOf("") }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Resolve ticket") },
        text = {
            Column(Modifier.verticalScroll(rememberScrollState()), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedTextField(rootCause, { rootCause = it }, label = { Text("Root cause") }, minLines = 2, modifier = Modifier.fillMaxWidth())
                OutlinedTextField(action, { action = it }, label = { Text("Action taken") }, minLines = 2, modifier = Modifier.fillMaxWidth())
                DropdownField("Resolution type", RESOLUTION_TYPES, type, { it }, { type = it }, Modifier.fillMaxWidth())
                OutlinedTextField(parts, { parts = it }, label = { Text("Parts used (optional)") }, singleLine = true, modifier = Modifier.fillMaxWidth())
                Text("The requester will be asked to confirm the fix.", style = MaterialTheme.typography.bodySmall)
            }
        },
        confirmButton = {
            TextButton(
                enabled = rootCause.isNotBlank() && action.isNotBlank() && type != null,
                onClick = { onConfirm(Resolution(rootCause.trim(), action.trim(), type!!, parts.trim())) },
            ) { Text("Resolve") }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Back") } },
    )
}

@Composable
fun AssignDialog(
    technicians: List<UserProfile>,
    currentAssigneeId: String?,
    onDismiss: () -> Unit,
    onAssign: (UserProfile) -> Unit,
) {
    var selected by rememberSaveable { mutableStateOf(currentAssigneeId) }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Assign to") },
        text = {
            if (technicians.isEmpty()) {
                Text("No active admins found. Promote someone to admin in People.")
            } else {
                LazyColumn(Modifier.heightIn(max = 360.dp)) {
                    items(technicians, key = { it.uid }) { u ->
                        ListItem(
                            headlineContent = { Text(u.name) },
                            supportingContent = { Text(u.role.label) },
                            leadingContent = { RadioButton(selected = selected == u.uid, onClick = { selected = u.uid }) },
                            modifier = Modifier.clickable { selected = u.uid },
                        )
                    }
                }
            }
        },
        confirmButton = {
            TextButton(
                enabled = selected != null && selected != currentAssigneeId,
                onClick = { technicians.firstOrNull { it.uid == selected }?.let(onAssign) },
            ) { Text("Assign") }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancel") } },
    )
}

@Composable
fun RateDialog(onDismiss: () -> Unit, onConfirm: (Int, String) -> Unit) {
    var stars by rememberSaveable { mutableIntStateOf(0) }
    var comment by rememberSaveable { mutableStateOf("") }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("How did IT do? ✨") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Row {
                    (1..5).forEach { i ->
                        IconButton(onClick = { stars = i }) {
                            Icon(
                                if (i <= stars) Icons.Filled.Star else Icons.Outlined.StarOutline,
                                contentDescription = "$i star${if (i > 1) "s" else ""}",
                                tint = if (i <= stars) Color(0xFFFFB020) else MaterialTheme.colorScheme.outline,
                                modifier = Modifier.size(32.dp),
                            )
                        }
                    }
                }
                OutlinedTextField(comment, { comment = it }, label = { Text("Comment (optional)") }, minLines = 2, modifier = Modifier.fillMaxWidth())
            }
        },
        confirmButton = { TextButton(enabled = stars > 0, onClick = { onConfirm(stars, comment) }) { Text("Submit") } },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Later") } },
    )
}

@Composable
fun ImageDialog(model: Any?, onDismiss: () -> Unit) {
    Dialog(onDismissRequest = onDismiss, properties = DialogProperties(usePlatformDefaultWidth = false)) {
        Box(Modifier.fillMaxSize().background(Color.Black)) {
            AsyncImage(model = model, contentDescription = "Full size image", contentScale = ContentScale.Fit, modifier = Modifier.fillMaxSize())
            IconButton(onClick = onDismiss, modifier = Modifier.align(Alignment.TopEnd).padding(16.dp)) {
                Icon(Icons.Default.Close, contentDescription = "Close", tint = Color.White)
            }
        }
    }
}
