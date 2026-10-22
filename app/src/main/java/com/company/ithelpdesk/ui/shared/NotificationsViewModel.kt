// © 2026 Khondaker Arifur Rahman. All Rights Reserved.
// BGL-IT Help — see COPYRIGHT.md. Unauthorized copying, modification or redistribution is prohibited.

package com.company.ithelpdesk.ui.shared

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.company.ithelpdesk.data.SessionManager
import com.company.ithelpdesk.domain.model.AppNotification
import com.company.ithelpdesk.domain.repository.NotificationRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

data class NotificationsUi(
    val loading: Boolean = true,
    val items: List<AppNotification> = emptyList(),
    val unread: Int = 0,
    val error: String? = null,
)

@HiltViewModel
class NotificationsViewModel @Inject constructor(
    private val repo: NotificationRepository,
    session: SessionManager,
) : ViewModel() {
    val me = session.requireProfile()

    val ui: StateFlow<NotificationsUi> = repo.observeFor(me)
        .map { list -> NotificationsUi(loading = false, items = list, unread = list.count { !it.isReadBy(me.uid) }) }
        .catch { emit(NotificationsUi(loading = false, error = "Couldn't load notifications. ${it.message.orEmpty()}")) }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), NotificationsUi())

    fun markRead(n: AppNotification) {
        if (n.isReadBy(me.uid)) return
        viewModelScope.launch { runCatching { repo.markRead(listOf(n.id), me.uid) } }
    }

    fun markAllRead() {
        val ids = ui.value.items.filter { !it.isReadBy(me.uid) }.map { it.id }
        viewModelScope.launch { runCatching { repo.markRead(ids, me.uid) } }
    }
}
