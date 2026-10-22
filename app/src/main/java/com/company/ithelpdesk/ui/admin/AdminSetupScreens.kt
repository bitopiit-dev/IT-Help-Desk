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
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.outlined.Delete
import androidx.compose.material.icons.outlined.Edit
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.company.ithelpdesk.domain.model.Category
import com.company.ithelpdesk.domain.model.Priority
import com.company.ithelpdesk.domain.model.SlaSettings
import com.company.ithelpdesk.domain.model.SlaTarget
import com.company.ithelpdesk.ui.components.AppCard
import com.company.ithelpdesk.ui.components.EmptyState
import com.company.ithelpdesk.ui.components.GradientButton
import com.company.ithelpdesk.ui.components.LoadingBox
import com.company.ithelpdesk.ui.components.ShareAppDialog
import com.company.ithelpdesk.ui.components.categoryEmoji

/** "Setup" tab: links to the configuration pages and the admin's own profile. */
@Composable
fun AdminMoreScreen(onCategories: () -> Unit, onDepartments: () -> Unit, onSla: () -> Unit, onProfile: () -> Unit) {
    var showShareDialog by remember { mutableStateOf(false) }
    if (showShareDialog) {
        ShareAppDialog(onDismiss = { showShareDialog = false })
    }

    Column(Modifier.fillMaxSize().statusBarsPadding().verticalScroll(rememberScrollState()).padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp)) {
        Text("Setup", style = MaterialTheme.typography.headlineMedium)
        Text("Shape how BGL-IT Help works for everyone.", color = MaterialTheme.colorScheme.onSurfaceVariant)
        Spacer(Modifier.height(6.dp))
        SetupRow("🗂️", "Categories", "What users can report and the sub-types", onCategories)
        SetupRow("🏢", "Departments", "Shown on the sign-up form", onDepartments)
        SetupRow("⏱️", "SLA targets", "Response and fix times per priority", onSla)
        SetupRow("📲", "Share App & QR Code", "Scan QR code or share download link for BGL-IT Help") { showShareDialog = true }
        SetupRow("🧑‍💻", "My profile", "Your details and sign out", onProfile)
        Spacer(Modifier.height(8.dp))
        AppCard {
            Text("💡 Tips", style = MaterialTheme.typography.titleMedium)
            Spacer(Modifier.height(6.dp))
            Text(
                "• New sign-ups appear in People › Pending, with an alert.\n" +
                    "• Make a teammate an admin from People › Users (⋮ menu).\n" +
                    "• Everything here runs on the free Firebase plan, from any network.",
                style = MaterialTheme.typography.bodyMedium,
            )
        }
    }
}

@Composable
private fun SetupRow(emoji: String, title: String, subtitle: String, onClick: () -> Unit) {
    AppCard(onClick = onClick, padding = 14.dp) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(emoji, fontSize = 26.sp)
            Spacer(Modifier.width(14.dp))
            Column(Modifier.weight(1f)) {
                Text(title, style = MaterialTheme.typography.titleSmall)
                Text(subtitle, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            Icon(Icons.AutoMirrored.Filled.KeyboardArrowRight, null)
        }
    }
}

/** Page frame with a back arrow and snackbar, used by the setup pages. */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AdminSubPage(
    title: String,
    onBack: () -> Unit,
    snackbar: SnackbarHostState = remember { SnackbarHostState() },
    fab: (@Composable () -> Unit)? = null,
    content: @Composable (PaddingValues) -> Unit,
) {
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(title) },
                navigationIcon = { IconButton(onClick = onBack) { Icon(Icons.AutoMirrored.Filled.ArrowBack, "Back") } },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = MaterialTheme.colorScheme.background),
            )
        },
        snackbarHost = { SnackbarHost(snackbar) },
        floatingActionButton = { fab?.invoke() },
        content = content,
    )
}

@Composable
private fun MessageEffect(vm: AdminBaseViewModel, snackbar: SnackbarHostState) {
    val message by vm.message.collectAsStateWithLifecycle()
    LaunchedEffect(message) {
        message?.let {
            snackbar.showSnackbar(it)
            vm.consumeMessage()
        }
    }
}

// ---------- Categories ----------

@Composable
fun CategoriesScreen(onBack: () -> Unit, viewModel: AdminSettingsViewModel = hiltViewModel()) {
    val categories by viewModel.categories.collectAsStateWithLifecycle()
    val snackbar = remember { SnackbarHostState() }
    var editing by remember { mutableStateOf<Category?>(null) }
    var deleting by remember { mutableStateOf<Category?>(null) }
    MessageEffect(viewModel, snackbar)

    AdminSubPage(
        "Categories",
        onBack,
        snackbar,
        fab = { FloatingActionButton(onClick = { editing = Category(id = "", name = "") }) { Icon(Icons.Default.Add, "Add category") } },
    ) { padding ->
        val list = categories
        when {
            list == null -> LoadingBox(Modifier.padding(padding))
            list.isEmpty() -> EmptyState(
                "🗂️", "No categories yet",
                "Users currently see the built-in list. Add it here so you can edit it.",
                Modifier.padding(padding),
            ) { GradientButton("Add default categories", onClick = viewModel::importDefaults) }
            else -> LazyColumn(Modifier.padding(padding), contentPadding = PaddingValues(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                // Installs set up before 2.1 do not have "Item Request" yet; offer it once instead of overwriting anything.
                val hasItemRequest = list.any { it.id == Category.ITEM_REQUEST_ID || it.name.equals("Item Request", ignoreCase = true) }
                if (!hasItemRequest) {
                    item(key = "add-item-request") {
                        AppCard(padding = 14.dp) {
                            Text("📦 New category: Item Request", style = MaterialTheme.typography.titleSmall)
                            Spacer(Modifier.height(4.dp))
                            Text(
                                "Lets people ask for a mouse, cable, charger, laptop bag and more. Shows in Quick start for everyone.",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                            Spacer(Modifier.height(10.dp))
                            GradientButton("Add Item Request", onClick = viewModel::addItemRequest)
                        }
                    }
                }
                items(list, key = { it.id }) { c ->
                    AppCard(padding = 12.dp) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(c.emoji.ifBlank { categoryEmoji(c.name) }, fontSize = 24.sp)
                            Spacer(Modifier.width(12.dp))
                            Column(Modifier.weight(1f)) {
                                Text(c.name, style = MaterialTheme.typography.titleSmall)
                                Text(
                                    c.subCategories.joinToString(", ").ifBlank { "No sub-categories" },
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                )
                            }
                            IconButton(onClick = { editing = c }) { Icon(Icons.Outlined.Edit, "Edit ${c.name}") }
                            IconButton(onClick = { deleting = c }) { Icon(Icons.Outlined.Delete, "Delete ${c.name}") }
                        }
                    }
                }
                item { Spacer(Modifier.height(72.dp)) }
            }
        }
    }

    editing?.let { c ->
        CategoryDialog(c, onDismiss = { editing = null }) {
            viewModel.saveCategory(it)
            editing = null
        }
    }
    deleting?.let { c ->
        AlertDialog(
            onDismissRequest = { deleting = null },
            title = { Text("Delete ${c.name}?") },
            text = { Text("Existing tickets keep their category name.") },
            confirmButton = { TextButton(onClick = { viewModel.deleteCategory(c); deleting = null }) { Text("Delete") } },
            dismissButton = { TextButton(onClick = { deleting = null }) { Text("Cancel") } },
        )
    }
}

@Composable
private fun CategoryDialog(initial: Category, onDismiss: () -> Unit, onSave: (Category) -> Unit) {
    var name by rememberSaveable { mutableStateOf(initial.name) }
    var emoji by rememberSaveable { mutableStateOf(initial.emoji) }
    var subs by rememberSaveable { mutableStateOf(initial.subCategories.joinToString(", ")) }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(if (initial.id.isBlank()) "New category" else "Edit category") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedTextField(name, { name = it.take(40) }, label = { Text("Name") }, singleLine = true, modifier = Modifier.fillMaxWidth())
                OutlinedTextField(emoji, { emoji = it.take(4) }, label = { Text("Emoji (optional)") }, singleLine = true, modifier = Modifier.fillMaxWidth())
                OutlinedTextField(subs, { subs = it }, label = { Text("Sub-categories, comma separated") }, minLines = 2, modifier = Modifier.fillMaxWidth())
            }
        },
        confirmButton = {
            TextButton(enabled = name.isNotBlank(), onClick = {
                onSave(
                    initial.copy(
                        name = name.trim(),
                        emoji = emoji.trim(),
                        subCategories = subs.split(",").map { it.trim() }.filter { it.isNotEmpty() }.distinct(),
                    ),
                )
            }) { Text("Save") }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancel") } },
    )
}

// ---------- Departments ----------

@Composable
fun DepartmentsScreen(onBack: () -> Unit, viewModel: AdminSettingsViewModel = hiltViewModel()) {
    val departments by viewModel.departments.collectAsStateWithLifecycle()
    val snackbar = remember { SnackbarHostState() }
    var name by rememberSaveable { mutableStateOf("") }
    MessageEffect(viewModel, snackbar)

    AdminSubPage("Departments", onBack, snackbar) { padding ->
        Column(Modifier.fillMaxSize().padding(padding).imePadding()) {
            Row(Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
                OutlinedTextField(name, { name = it.take(60) }, label = { Text("New department") }, singleLine = true,
                    modifier = Modifier.weight(1f))
                Spacer(Modifier.width(8.dp))
                OutlinedButton(enabled = name.isNotBlank(), onClick = { viewModel.addDepartment(name); name = "" }) { Text("Add") }
            }
            val list = departments
            when {
                list == null -> LoadingBox()
                list.isEmpty() -> EmptyState("🏢", "No departments", "Without a list, people type their department when signing up.")
                else -> LazyColumn(contentPadding = PaddingValues(horizontal = 16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    items(list, key = { it.id }) { d ->
                        AppCard(padding = 12.dp) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Text(d.name, style = MaterialTheme.typography.titleSmall, modifier = Modifier.weight(1f))
                                IconButton(onClick = { viewModel.deleteDepartment(d) }) { Icon(Icons.Outlined.Delete, "Delete ${d.name}") }
                            }
                        }
                    }
                }
            }
        }
    }
}

// ---------- SLA ----------

@Composable
fun SlaScreen(onBack: () -> Unit, viewModel: AdminSettingsViewModel = hiltViewModel()) {
    val sla by viewModel.sla.collectAsStateWithLifecycle()
    val snackbar = remember { SnackbarHostState() }
    MessageEffect(viewModel, snackbar)

    AdminSubPage("SLA targets", onBack, snackbar) { padding ->
        val current = sla
        if (current == null) {
            LoadingBox(Modifier.padding(padding))
            return@AdminSubPage
        }
        val respond = remember(current) {
            mutableStateMapOf<Priority, String>().apply { Priority.entries.forEach { put(it, current.target(it).respondMinutes.toString()) } }
        }
        val resolve = remember(current) {
            mutableStateMapOf<Priority, String>().apply { Priority.entries.forEach { put(it, (current.target(it).resolveMinutes / 60.0).let(::trimHours)) } }
        }
        Column(
            Modifier.fillMaxSize().padding(padding).imePadding().verticalScroll(rememberScrollState()).padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            Text("A ticket is “at risk” at 75 % of its time and “breached” at 100 %. Time on hold doesn't count.",
                style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
            Priority.entries.reversed().forEach { p ->
                AppCard(padding = 12.dp) {
                    Text(p.label, style = MaterialTheme.typography.titleSmall)
                    Spacer(Modifier.height(6.dp))
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        OutlinedTextField(
                            respond[p].orEmpty(), { v -> respond[p] = v.filter { it.isDigit() }.take(5) },
                            label = { Text("Respond (min)") }, singleLine = true,
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number), modifier = Modifier.weight(1f),
                        )
                        OutlinedTextField(
                            resolve[p].orEmpty(), { v -> resolve[p] = v.filter { it.isDigit() || it == '.' }.take(6) },
                            label = { Text("Fix (hours)") }, singleLine = true,
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal), modifier = Modifier.weight(1f),
                        )
                    }
                }
            }
            Box(Modifier.fillMaxWidth()) {
                GradientButton("Save targets", modifier = Modifier.fillMaxWidth(), onClick = {
                    val targets = Priority.entries.associateWith { p ->
                        val d = current.target(p)
                        SlaTarget(
                            respondMinutes = respond[p]?.toIntOrNull()?.coerceAtLeast(1) ?: d.respondMinutes,
                            resolveMinutes = resolve[p]?.toDoubleOrNull()?.let { (it * 60).toInt() }?.coerceAtLeast(1) ?: d.resolveMinutes,
                        )
                    }
                    viewModel.saveSla(SlaSettings(targets))
                })
            }
        }
    }
}

private fun trimHours(h: Double): String = if (h % 1.0 == 0.0) h.toInt().toString() else String.format(java.util.Locale.US, "%.1f", h)
