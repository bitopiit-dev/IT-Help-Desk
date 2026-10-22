package com.company.ithelpdesk.ui.ticket

import android.net.Uri
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.company.ithelpdesk.data.SessionManager
import com.company.ithelpdesk.domain.logic.StatusWorkflow
import com.company.ithelpdesk.domain.model.Comment
import com.company.ithelpdesk.domain.model.Resolution
import com.company.ithelpdesk.domain.model.StatusChange
import com.company.ithelpdesk.domain.model.Ticket
import com.company.ithelpdesk.domain.model.TicketStatus
import com.company.ithelpdesk.domain.model.UserProfile
import com.company.ithelpdesk.domain.repository.TicketRepository
import com.company.ithelpdesk.domain.repository.UserRepository
import com.company.ithelpdesk.domain.repository.WriteResult
import com.company.ithelpdesk.util.userMessage
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

sealed interface TicketLoad {
    data object Loading : TicketLoad
    data object NotFound : TicketLoad
    data class Loaded(val ticket: Ticket) : TicketLoad
    data class Failed(val message: String) : TicketLoad
}

/** Shared by the user and admin ticket screens; what each can do is decided by role and StatusWorkflow. */
@HiltViewModel
class TicketDetailViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    private val repo: TicketRepository,
    users: UserRepository,
    session: SessionManager,
) : ViewModel() {

    val ticketId: String = checkNotNull(savedStateHandle["ticketId"])
    val me: UserProfile = session.requireProfile()

    val ticket: StateFlow<TicketLoad> = repo.observeTicket(ticketId)
        .map { t -> if (t == null) TicketLoad.NotFound else TicketLoad.Loaded(t) }
        .catch { emit(TicketLoad.Failed(it.userMessage())) }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), TicketLoad.Loading)

    val comments: StateFlow<List<Comment>> = repo.observeComments(ticketId, includeInternal = me.role.isAdmin)
        .catch { emit(emptyList()) }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    val history: StateFlow<List<StatusChange>> = repo.observeHistory(ticketId)
        .catch { emit(emptyList()) }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    val admins: StateFlow<List<UserProfile>> =
        (if (me.role.isAdmin) users.observeAdmins() else flowOf(emptyList()))
            .catch { emit(emptyList()) }
            .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    private val _busy = MutableStateFlow(false)
    val busy: StateFlow<Boolean> = _busy.asStateFlow()

    private val _message = MutableStateFlow<String?>(null)
    val message: StateFlow<String?> = _message.asStateFlow()

    fun consumeMessage() { _message.value = null }

    private fun current(): Ticket? = (ticket.value as? TicketLoad.Loaded)?.ticket

    fun allowedTransitions(t: Ticket, now: Long): List<TicketStatus> = StatusWorkflow.allowedTransitions(t, me, now)

    fun changeStatus(to: TicketStatus, note: String = "", resolution: Resolution? = null) {
        val t = current() ?: return
        perform(successText = "Status changed to ${to.label}") { repo.changeStatus(t, to, me, note, resolution) }
    }

    fun pickUp() {
        val t = current() ?: return
        perform(successText = "You're on it 💪") { repo.assign(t, me, me) }
    }

    fun assign(user: UserProfile) {
        val t = current() ?: return
        perform(successText = "Assigned to ${user.name}") { repo.assign(t, user, me) }
    }

    fun sendComment(text: String, internal: Boolean, images: List<Uri>, onSent: () -> Unit) {
        val t = current() ?: return
        if (text.isBlank() && images.isEmpty()) return
        perform(successText = null) {
            val saved = images.map { repo.saveImage(t.id, it, me) }
            val result = repo.addComment(t, me, text, saved, internal)
            onSent()
            result
        }
    }

    fun rate(stars: Int, comment: String) {
        val t = current() ?: return
        perform(successText = "Thanks for the feedback! 🌟") { repo.rate(t, me, stars, comment) }
    }

    private fun perform(successText: String?, block: suspend () -> WriteResult) {
        viewModelScope.launch {
            _busy.value = true
            try {
                val result = block()
                _message.value = when (result) {
                    WriteResult.QUEUED -> "Saved offline. It will sync when you're back online."
                    WriteResult.SYNCED -> successText
                }
            } catch (e: Exception) {
                _message.value = e.userMessage()
            } finally {
                _busy.value = false
            }
        }
    }
}
