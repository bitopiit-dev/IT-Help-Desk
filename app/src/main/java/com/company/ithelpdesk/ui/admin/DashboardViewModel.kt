// © 2026 Khondaker Arifur Rahman. All Rights Reserved.
// BGL-IT Help — see COPYRIGHT.md. Unauthorized copying, modification or redistribution is prohibited.

package com.company.ithelpdesk.ui.admin

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.company.ithelpdesk.domain.logic.DashboardCalculator
import com.company.ithelpdesk.domain.logic.DashboardData
import com.company.ithelpdesk.domain.logic.DashboardFilter
import com.company.ithelpdesk.domain.logic.DashboardSource
import com.company.ithelpdesk.domain.model.UserProfile
import com.company.ithelpdesk.domain.repository.SettingsRepository
import com.company.ithelpdesk.domain.repository.TicketRepository
import com.company.ithelpdesk.domain.repository.UserRepository
import com.company.ithelpdesk.util.formatDate
import com.company.ithelpdesk.util.userMessage
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.onStart
import kotlinx.coroutines.flow.stateIn
import java.time.LocalDate
import java.time.ZoneId
import javax.inject.Inject

enum class DashboardRange(val label: String) { TODAY("Today"), LAST_7("7 days"), LAST_30("30 days"), CUSTOM("Custom") }

data class RangeSelection(val range: DashboardRange = DashboardRange.LAST_7, val customStart: Long? = null, val customEnd: Long? = null)

data class DashboardUi(
    val loading: Boolean = true,
    val error: String? = null,
    val selection: RangeSelection = RangeSelection(),
    val rangeLabel: String = "",
    val filter: DashboardFilter = DashboardFilter(),
    val data: DashboardData? = null,
    val technicians: List<UserProfile> = emptyList(),
    val departments: List<String> = emptyList(),
    val categories: List<String> = emptyList(),
    val now: Long = System.currentTimeMillis(),
)

private sealed interface SourceLoad {
    data object Loading : SourceLoad
    data class Data(val source: DashboardSource) : SourceLoad
    data class Failed(val message: String) : SourceLoad
}

@OptIn(ExperimentalCoroutinesApi::class)
@HiltViewModel
class DashboardViewModel @Inject constructor(
    private val tickets: TicketRepository,
    users: UserRepository,
    settings: SettingsRepository,
) : ViewModel() {

    private val zone: ZoneId = ZoneId.systemDefault()
    private val selection = MutableStateFlow(RangeSelection())
    private val filter = MutableStateFlow(DashboardFilter())
    private val refresh = MutableStateFlow(0)

    private val clock = flow {
        while (true) {
            emit(System.currentTimeMillis())
            delay(60_000)
        }
    }

    private fun bounds(sel: RangeSelection, now: Long): Pair<Long, Long> {
        val today = LocalDate.now(zone)
        fun startOf(d: LocalDate) = d.atStartOfDay(zone).toInstant().toEpochMilli()
        return when (sel.range) {
            DashboardRange.TODAY -> startOf(today) to now
            DashboardRange.LAST_7 -> startOf(today.minusDays(6)) to now
            DashboardRange.LAST_30 -> startOf(today.minusDays(29)) to now
            DashboardRange.CUSTOM -> {
                // Date pickers return UTC midnight; convert to local day bounds.
                val s = sel.customStart?.let { LocalDate.ofEpochDay(it / 86_400_000) } ?: today.minusDays(6)
                val e = sel.customEnd?.let { LocalDate.ofEpochDay(it / 86_400_000) } ?: today
                startOf(s) to (startOf(e.plusDays(1)) - 1)
            }
        }
    }

    private val source = combine(selection, refresh) { sel, _ -> sel }
        .flatMapLatest { sel ->
            val now = System.currentTimeMillis()
            val (start, _) = bounds(sel, now)
            val todayStart = LocalDate.now(zone).atStartOfDay(zone).toInstant().toEpochMilli()
            tickets.observeDashboardSource(createdFrom = start, resolvedFrom = minOf(start, todayStart))
                .map<DashboardSource, SourceLoad> { SourceLoad.Data(it) }
                .onStart { emit(SourceLoad.Loading) }
                .catch { emit(SourceLoad.Failed(it.userMessage())) }
        }

    private val staff = users.observeAdmins().catch { emit(emptyList()) }
    private val departmentNames = settings.observeDepartments().map { list -> list.map { it.name } }.catch { emit(emptyList()) }

    val ui: StateFlow<DashboardUi> = combine(
        combine(source, selection, filter) { a, b, c -> Triple(a, b, c) },
        staff,
        departmentNames,
        clock,
    ) { (load, sel, f), techs, depts, now ->
        val (start, end) = bounds(sel, now)
        val label = when (sel.range) {
            DashboardRange.CUSTOM -> "${formatDate(start)} – ${formatDate(end)}"
            else -> sel.range.label
        }
        when (load) {
            SourceLoad.Loading -> DashboardUi(loading = true, selection = sel, rangeLabel = label, filter = f, technicians = techs, now = now)
            is SourceLoad.Failed -> DashboardUi(loading = false, error = load.message, selection = sel, rangeLabel = label, filter = f, now = now)
            is SourceLoad.Data -> {
                val all = load.source.createdInRange + load.source.open
                DashboardUi(
                    loading = false,
                    selection = sel,
                    rangeLabel = label,
                    filter = f,
                    data = DashboardCalculator.compute(load.source, techs, f, start, end, now, zone),
                    technicians = techs,
                    departments = (depts + all.map { it.department }).filter { it.isNotBlank() }.distinct().sorted(),
                    categories = all.map { it.category }.filter { it.isNotBlank() }.distinct().sorted(),
                    now = now,
                )
            }
        }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), DashboardUi())

    fun setRange(range: DashboardRange) { selection.value = RangeSelection(range) }
    fun setCustomRange(start: Long, end: Long) { selection.value = RangeSelection(DashboardRange.CUSTOM, start, end) }
    fun setFilter(f: DashboardFilter) { filter.value = f }
    fun refresh() { refresh.value++ }
}
