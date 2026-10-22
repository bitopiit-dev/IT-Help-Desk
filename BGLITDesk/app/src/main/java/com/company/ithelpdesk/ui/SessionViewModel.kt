package com.company.ithelpdesk.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.company.ithelpdesk.data.SessionManager
import com.company.ithelpdesk.data.SessionState
import com.company.ithelpdesk.domain.repository.TicketRepository
import com.company.ithelpdesk.ui.components.ImageLoaderFn
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class SessionViewModel @Inject constructor(
    private val session: SessionManager,
    tickets: TicketRepository,
) : ViewModel() {
    val state: StateFlow<SessionState> = session.state

    /** Handed to every screen so photos stored in Firestore can be shown. */
    val imageLoader = ImageLoaderFn { ticketId, imageId -> tickets.loadImage(ticketId, imageId) }

    fun signOut() {
        viewModelScope.launch { session.signOut() }
    }
}
