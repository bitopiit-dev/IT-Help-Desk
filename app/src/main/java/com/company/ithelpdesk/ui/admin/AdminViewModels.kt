// © 2026 Khondaker Arifur Rahman. All Rights Reserved.
// BGL-IT Help — see COPYRIGHT.md. Unauthorized copying, modification or redistribution is prohibited.

package com.company.ithelpdesk.ui.admin

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.company.ithelpdesk.data.SessionManager
import com.company.ithelpdesk.domain.logic.SlaCalculator
import com.company.ithelpdesk.domain.model.Category
import com.company.ithelpdesk.domain.model.Department
import com.company.ithelpdesk.domain.model.Priority
import com.company.ithelpdesk.domain.model.Role
import com.company.ithelpdesk.domain.model.SlaSettings
import com.company.ithelpdesk.domain.model.SlaState
import com.company.ithelpdesk.domain.model.Ticket
import com.company.ithelpdesk.domain.model.TicketStatus
import com.company.ithelpdesk.domain.model.UserProfile
import com.company.ithelpdesk.domain.repository.SettingsRepository
import com.company.ithelpdesk.domain.repository.TicketRepository
import com.company.ithelpdesk.domain.repository.UserRepository
import com.company.ithelpdesk.util.userMessage
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.onStart
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

/** Shared message plumbing for the admin screens. */
abstract class AdminBaseViewModel : ViewModel() {
    private val _message = MutableStateFlow<String?>(null)
    val message: StateFlow<String?> = _message.asStateFlow()
    fun consumeMessage() { _message.value = null }
    protected fun setMessage(msg: String) { _message.value = msg }

    protected fun action(success: String? = null, block: suspend () -> Unit) {
        viewModelScope.launch {
            try {
                block()
                if (success != null) _message.value = success
            } catch (e: Exception) {
                _message.value = e.userMessage()
            }
        }
    }
}

// ---------- People & approvals ----------

enum class PeopleTab(val label: String) { PENDING("Pending"), USERS("Users"), ADMINS("Admins"), BLOCKED("Blocked") }

data class PeopleUi(
    val loading: Boolean = true,
    val error: String? = null,
    val all: List<UserProfile> = emptyList(),
) {
    fun list(tab: PeopleTab): List<UserProfile> = when (tab) {
        PeopleTab.PENDING -> all.filter { it.isPending }.sortedByDescending { it.createdAt }
        PeopleTab.USERS -> all.filter { it.isActive && it.isApproved && !it.role.isAdmin }
        PeopleTab.ADMINS -> all.filter { it.isActive && it.isApproved && it.role.isAdmin }
        PeopleTab.BLOCKED -> all.filter { it.isBlocked }
    }
    val pendingCount: Int get() = all.count { it.isPending }
}

@HiltViewModel
class PeopleViewModel @Inject constructor(
    private val users: UserRepository,
    session: SessionManager,
) : AdminBaseViewModel() {
    val me: UserProfile = session.requireProfile()

    val ui: StateFlow<PeopleUi> = users.observeAllUsers()
        .map { PeopleUi(loading = false, all = it) }
        .catch { emit(PeopleUi(loading = false, error = it.userMessage())) }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), PeopleUi())

    fun approve(u: UserProfile) = action("${u.firstName} approved 🎉") { users.approve(u, me) }
    fun decline(u: UserProfile) = action("Request from ${u.firstName} declined") { users.decline(u, me) }
    fun approveAll() {
        val pending = ui.value.list(PeopleTab.PENDING)
        action("${pending.size} people approved 🎉") { pending.forEach { users.approve(it, me) } }
    }
    fun setRole(u: UserProfile, role: Role) {
        if (u.isSuperAdmin) {
            setMessage("${u.firstName} is permanent Super Admin and cannot be demoted.")
            return
        }
        action("${u.firstName} is now ${role.label}") { users.setRole(u.uid, role) }
    }
    fun setActive(u: UserProfile, active: Boolean) {
        if (!active && u.isSuperAdmin) {
            setMessage("${u.firstName} is permanent Super Admin and cannot be blocked.")
            return
        }
        action(if (active) "${u.firstName} reactivated" else "${u.firstName} blocked") {
            if (active) users.approve(u, me) else users.setActive(u.uid, false)
        }
    }
}

// ---------- Ticket queue ----------

enum class QueueTab(val label: String) { UNASSIGNED("Unassigned"), MINE("Mine"), ALL("All open"), CLOSED("Closed") }

enum class QueueSort(val label: String) { PRIORITY("Priority"), SLA("SLA due"), OLDEST("Oldest"), NEWEST("Newest") }

data class QueueFilters(
    val priority: Priority? = null,
    val category: String? = null,
    val status: TicketStatus? = null,
    val sla: SlaState? = null,
    val assignee: String? = null,
) {
    val isEmpty get() = this == QueueFilters()
}

data class QueueUi(
    val loading: Boolean = true,
    val error: String? = null,
    val tab: QueueTab = QueueTab.UNASSIGNED,
    val sort: QueueSort = QueueSort.PRIORITY,
    val filters: QueueFilters = QueueFilters(),
    val query: String = "",
    val tickets: List<Ticket> = emptyList(),
    val counts: Map<QueueTab, Int> = emptyMap(),
    val categories: List<String> = emptyList(),
    val assignees: List<Pair<String, String>> = emptyList(),
    val now: Long = System.currentTimeMillis(),
)

private sealed interface QLoad {
    data object Loading : QLoad
    data class Data(val open: List<Ticket>, val closed: List<Ticket>) : QLoad
    data class Failed(val message: String) : QLoad
}

private data class QueueControls(val tab: QueueTab, val sort: QueueSort, val filters: QueueFilters, val query: String)

@OptIn(ExperimentalCoroutinesApi::class)
@HiltViewModel
class QueueViewModel @Inject constructor(
    repo: TicketRepository,
    session: SessionManager,
) : ViewModel() {
    private val myUid = session.requireProfile().uid
    private val refresh = MutableStateFlow(0)
    private val controls = MutableStateFlow(QueueControls(QueueTab.UNASSIGNED, QueueSort.PRIORITY, QueueFilters(), ""))

    private val clock = flow {
        while (true) {
            emit(System.currentTimeMillis())
            delay(30_000)
        }
    }

    private val load = refresh.flatMapLatest {
        combine(repo.observeOpenTickets(), repo.observeRecentClosed()) { o, c -> QLoad.Data(o, c) as QLoad }
            .onStart { emit(QLoad.Loading) }
            .catch { emit(QLoad.Failed(it.userMessage())) }
    }

    val ui: StateFlow<QueueUi> = combine(load, controls, clock) { l, c, now ->
        when (l) {
            QLoad.Loading -> QueueUi(loading = true, tab = c.tab, sort = c.sort, filters = c.filters, query = c.query)
            is QLoad.Failed -> QueueUi(loading = false, error = l.message, tab = c.tab, sort = c.sort, filters = c.filters, query = c.query)
            is QLoad.Data -> {
                fun source(tab: QueueTab) = when (tab) {
                    QueueTab.UNASSIGNED -> l.open.filter { it.assigneeId == null }
                    QueueTab.MINE -> l.open.filter { it.assigneeId == myUid }
                    QueueTab.ALL -> l.open
                    QueueTab.CLOSED -> l.closed
                }
                val f = c.filters
                val q = c.query.trim()
                val filtered = source(c.tab)
                    .filter { f.priority == null || it.priority == f.priority }
                    .filter { f.category == null || it.category == f.category }
                    .filter { f.status == null || it.status == f.status }
                    .filter { f.sla == null || SlaCalculator.evaluate(it, now) == f.sla }
                    .filter { f.assignee == null || it.assigneeId == f.assignee }
                    .filter {
                        q.isEmpty() || it.title.contains(q, true) || it.displayNumber.contains(q, true) ||
                            it.requesterName.contains(q, true) || it.department.contains(q, true)
                    }
                val sorted = when (c.sort) {
                    QueueSort.PRIORITY -> filtered.sortedWith(compareByDescending<Ticket> { it.priority.rank }.thenBy { it.createdAt })
                    QueueSort.SLA -> filtered.sortedBy { SlaCalculator.remainingMs(it, now) ?: Long.MAX_VALUE }
                    QueueSort.OLDEST -> filtered.sortedBy { it.createdAt }
                    QueueSort.NEWEST -> filtered.sortedByDescending { it.createdAt }
                }
                val everything = l.open + l.closed
                QueueUi(
                    loading = false,
                    tab = c.tab,
                    sort = c.sort,
                    filters = c.filters,
                    query = c.query,
                    tickets = sorted,
                    counts = QueueTab.entries.associateWith { source(it).size },
                    categories = everything.map { it.category }.filter { it.isNotBlank() }.distinct().sorted(),
                    assignees = everything.mapNotNull { t -> t.assigneeId?.let { it to (t.assigneeName ?: "?") } }.distinctBy { it.first },
                    now = now,
                )
            }
        }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), QueueUi())

    fun setTab(tab: QueueTab) = controls.update { it.copy(tab = tab) }
    fun setSort(sort: QueueSort) = controls.update { it.copy(sort = sort) }
    fun setQuery(q: String) = controls.update { it.copy(query = q) }
    fun setFilters(block: QueueFilters.() -> QueueFilters) = controls.update { it.copy(filters = it.filters.block()) }
    fun clearFilters() = controls.update { it.copy(filters = QueueFilters()) }
    fun showAssignee(uid: String) = controls.update { it.copy(tab = QueueTab.ALL, filters = QueueFilters(assignee = uid)) }
    fun refresh() { refresh.value++ }
}

// ---------- Settings ----------

@HiltViewModel
class AdminSettingsViewModel @Inject constructor(
    private val settings: SettingsRepository,
) : AdminBaseViewModel() {
    val categories: StateFlow<List<Category>?> = settings.observeCategories()
        .catch { emit(emptyList()) }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), null)

    val departments: StateFlow<List<Department>?> = settings.observeDepartments()
        .catch { emit(emptyList()) }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), null)

    val sla: StateFlow<SlaSettings?> = settings.observeSla()
        .catch { emit(SlaSettings.DEFAULT) }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), null)

    fun saveCategory(c: Category) = action("Saved ${c.name}") { settings.saveCategory(c) }
    fun deleteCategory(c: Category) = action("Deleted ${c.name}") { settings.deleteCategory(c.id) }
    fun importDefaults() = action("Default categories added") { settings.addDefaultCategories() }
    fun addItemRequest() = action("Added Item Request") {
        settings.saveCategory(Category.DEFAULTS.first { it.id == Category.ITEM_REQUEST_ID })
    }

    fun addDepartment(name: String) {
        if (name.isBlank()) return
        action("Added ${name.trim()}") { settings.saveDepartment(name) }
    }
    fun deleteDepartment(d: Department) = action("Deleted ${d.name}") { settings.deleteDepartment(d.id) }

    fun saveSla(s: SlaSettings) = action("SLA targets saved. They apply to new tickets.") { settings.saveSla(s) }
}
