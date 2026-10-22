package com.company.ithelpdesk.ui.admin

import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.outlined.Search
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.ScrollableTabRow
import androidx.compose.material3.Tab
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.company.ithelpdesk.domain.model.Priority
import com.company.ithelpdesk.domain.model.SlaState
import com.company.ithelpdesk.domain.model.TicketStatus
import com.company.ithelpdesk.ui.components.AdminTicketCard
import com.company.ithelpdesk.ui.components.EmptyState
import com.company.ithelpdesk.ui.components.ErrorBox
import com.company.ithelpdesk.ui.components.FilterDropdownChip
import com.company.ithelpdesk.ui.components.LoadingBox

@Composable
fun AdminTicketsScreen(assignee: String?, onOpenTicket: (String) -> Unit, viewModel: QueueViewModel = hiltViewModel()) {
    val ui by viewModel.ui.collectAsStateWithLifecycle()
    LaunchedEffect(assignee) { if (assignee != null) viewModel.showAssignee(assignee) }

    Column(Modifier.fillMaxSize().statusBarsPadding()) {
        Text("Tickets", style = MaterialTheme.typography.headlineMedium, modifier = Modifier.padding(start = 16.dp, top = 12.dp))
        OutlinedTextField(
            value = ui.query,
            onValueChange = viewModel::setQuery,
            placeholder = { Text("Search number, title, person…") },
            leadingIcon = { Icon(Icons.Outlined.Search, null) },
            trailingIcon = {
                if (ui.query.isNotEmpty()) IconButton(onClick = { viewModel.setQuery("") }) { Icon(Icons.Default.Close, "Clear search") }
            },
            singleLine = true,
            shape = CircleShape,
            modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 8.dp),
        )
        ScrollableTabRow(selectedTabIndex = ui.tab.ordinal, edgePadding = 12.dp, containerColor = MaterialTheme.colorScheme.background) {
            QueueTab.entries.forEach { tab ->
                Tab(
                    selected = ui.tab == tab,
                    onClick = { viewModel.setTab(tab) },
                    text = { Text("${tab.label} (${ui.counts[tab] ?: 0})") },
                )
            }
        }
        Row(
            Modifier.horizontalScroll(rememberScrollState()).padding(horizontal = 12.dp, vertical = 8.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            FilterDropdownChip("Sort", QueueSort.entries, ui.sort, { it.label }) { viewModel.setSort(it ?: QueueSort.PRIORITY) }
            FilterDropdownChip("Priority", Priority.entries, ui.filters.priority, { it.label }) { p -> viewModel.setFilters { copy(priority = p) } }
            FilterDropdownChip("SLA", SlaState.entries, ui.filters.sla, { it.label }) { s -> viewModel.setFilters { copy(sla = s) } }
            FilterDropdownChip("Status", TicketStatus.entries, ui.filters.status, { it.label }) { s -> viewModel.setFilters { copy(status = s) } }
            FilterDropdownChip("Category", ui.categories, ui.filters.category, { it }) { c -> viewModel.setFilters { copy(category = c) } }
            FilterDropdownChip(
                "Assignee",
                ui.assignees.map { it.first },
                ui.filters.assignee,
                { id -> ui.assignees.firstOrNull { it.first == id }?.second ?: "?" },
            ) { a -> viewModel.setFilters { copy(assignee = a) } }
            if (!ui.filters.isEmpty) TextButton(onClick = viewModel::clearFilters) { Text("Clear") }
        }
        when {
            ui.loading -> LoadingBox()
            ui.error != null -> ErrorBox(ui.error!!, onRetry = viewModel::refresh)
            ui.tickets.isEmpty() -> EmptyState(
                if (ui.tab == QueueTab.UNASSIGNED) "🧘" else "📭",
                if (ui.tab == QueueTab.UNASSIGNED) "Queue is clear" else "No tickets here",
                if (ui.filters.isEmpty && ui.query.isBlank()) "New tickets land here in real time." else "Try clearing the search or filters.",
            )
            else -> LazyColumn(contentPadding = PaddingValues(start = 16.dp, end = 16.dp, bottom = 16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                items(ui.tickets, key = { it.id }) { t -> AdminTicketCard(t, ui.now, onClick = { onOpenTicket(t.id) }) }
            }
        }
    }
}
