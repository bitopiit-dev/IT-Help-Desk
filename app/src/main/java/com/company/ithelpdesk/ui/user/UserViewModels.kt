// © 2026 Khondaker Arifur Rahman. All Rights Reserved.
// BGL-IT Help — see COPYRIGHT.md. Unauthorized copying, modification or redistribution is prohibited.

package com.company.ithelpdesk.ui.user

import android.net.Uri
import android.os.Build
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.company.ithelpdesk.BuildConfig
import com.company.ithelpdesk.data.SessionManager
import com.company.ithelpdesk.domain.model.Category
import com.company.ithelpdesk.domain.model.ContactMethod
import com.company.ithelpdesk.domain.model.DeviceInfo
import com.company.ithelpdesk.domain.model.Priority
import com.company.ithelpdesk.domain.model.SlaSettings
import com.company.ithelpdesk.domain.model.Ticket
import com.company.ithelpdesk.domain.model.TicketStatus
import com.company.ithelpdesk.domain.model.UserProfile
import com.company.ithelpdesk.domain.repository.SettingsRepository
import com.company.ithelpdesk.domain.repository.TicketRepository
import com.company.ithelpdesk.domain.repository.WriteResult
import com.company.ithelpdesk.util.userMessage
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

data class MyTicketsUi(
    val loading: Boolean = true,
    val error: String? = null,
    val tickets: List<Ticket> = emptyList(),
) {
    val open get() = tickets.filter { it.status.isOpen }
    val done get() = tickets.filter { !it.status.isOpen }
    val needsMe get() = tickets.filter { it.status == TicketStatus.RESOLVED || it.status == TicketStatus.ON_HOLD_USER }
}

@HiltViewModel
class MyTicketsViewModel @Inject constructor(
    repo: TicketRepository,
    settings: SettingsRepository,
    session: SessionManager,
) : ViewModel() {
    val me: UserProfile = session.requireProfile()

    val ui: StateFlow<MyTicketsUi> = repo.observeMyTickets(me.uid)
        .map { MyTicketsUi(loading = false, tickets = it) }
        .catch { emit(MyTicketsUi(loading = false, error = it.userMessage())) }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), MyTicketsUi())

    val categories: StateFlow<List<Category>> = settings.observeCategories()
        .map { it.ifEmpty { Category.DEFAULTS } }
        .catch { emit(Category.DEFAULTS) }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), Category.DEFAULTS)
}

data class CreateTicketForm(
    val title: String = "",
    val description: String = "",
    val category: Category? = null,
    val subCategory: String = "",
    val priority: Priority = Priority.MEDIUM,
    val contactMethod: ContactMethod = ContactMethod.IN_APP,
    val assetTag: String = "",
    val photos: List<Uri> = emptyList(),
    val submitting: Boolean = false,
    val progress: String? = null,
    val error: String? = null,
    val showErrors: Boolean = false,
) {
    val titleError get() = if (title.isBlank()) "Give the problem a short title" else null
    val descriptionError get() = if (description.trim().length < 10) "Describe the problem (at least 10 characters)" else null
    val categoryError get() = if (category == null) "Choose a category" else null
    val isValid get() = titleError == null && descriptionError == null && categoryError == null
}

sealed interface CreateTicketEvent {
    data class Created(val ticketId: String, val queued: Boolean) : CreateTicketEvent
}

@HiltViewModel
class CreateTicketViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    private val repo: TicketRepository,
    settings: SettingsRepository,
    private val session: SessionManager,
) : ViewModel() {

    companion object {
        const val MAX_PHOTOS = 4
    }

    private val preselect: String? = savedStateHandle.get<String>("category")?.takeIf { it.isNotBlank() }

    private val _form = MutableStateFlow(CreateTicketForm())
    val form: StateFlow<CreateTicketForm> = _form.asStateFlow()

    private val _event = MutableStateFlow<CreateTicketEvent?>(null)
    val event: StateFlow<CreateTicketEvent?> = _event.asStateFlow()

    val categories: StateFlow<List<Category>> = settings.observeCategories()
        .map { it.ifEmpty { Category.DEFAULTS } }
        .catch { emit(Category.DEFAULTS) }
        .stateIn(viewModelScope, SharingStarted.Eagerly, Category.DEFAULTS)

    private val sla: StateFlow<SlaSettings> = settings.observeSla()
        .catch { emit(SlaSettings.DEFAULT) }
        .stateIn(viewModelScope, SharingStarted.Eagerly, SlaSettings.DEFAULT)

    init {
        if (preselect != null) {
            viewModelScope.launch {
                categories.collect { list ->
                    if (_form.value.category == null) {
                        list.firstOrNull { it.id == preselect || it.name == preselect }?.let { c ->
                            _form.update { it.copy(category = c) }
                        }
                    }
                }
            }
        }
    }

    fun update(block: CreateTicketForm.() -> CreateTicketForm) = _form.update { it.block().copy(error = null) }

    fun addPhotos(uris: List<Uri>) = _form.update { f ->
        f.copy(photos = (f.photos + uris).distinct().take(MAX_PHOTOS))
    }

    fun removePhoto(uri: Uri) = _form.update { it.copy(photos = it.photos - uri) }

    fun consumeEvent() { _event.value = null }

    fun submit() {
        val f = _form.value
        if (!f.isValid) {
            _form.update { it.copy(showErrors = true) }
            return
        }
        val me = session.requireProfile()
        viewModelScope.launch {
            _form.update { it.copy(submitting = true, error = null) }
            try {
                val id = repo.newTicketId()
                val attachments = f.photos.mapIndexed { index, uri ->
                    _form.update { it.copy(progress = "Adding photo ${index + 1} of ${f.photos.size}…") }
                    repo.saveImage(id, uri, me)
                }
                _form.update { it.copy(progress = "Sending to IT…") }
                val ticket = Ticket(
                    id = id,
                    title = f.title.trim(),
                    description = f.description.trim(),
                    category = f.category!!.name,
                    subCategory = f.subCategory,
                    priority = f.priority,
                    requesterId = me.uid,
                    requesterName = me.name,
                    requesterPhone = me.phone,
                    requesterEmail = me.email,
                    department = me.department,
                    location = me.location,
                    attachments = attachments,
                    deviceInfo = DeviceInfo(
                        manufacturer = Build.MANUFACTURER.replaceFirstChar { it.uppercase() },
                        model = Build.MODEL,
                        androidVersion = Build.VERSION.RELEASE,
                        appVersion = BuildConfig.VERSION_NAME,
                    ),
                    assetTag = f.assetTag.trim(),
                    contactMethod = f.contactMethod,
                )
                val result = repo.createTicket(ticket, sla.value)
                _form.update { it.copy(submitting = false, progress = null) }
                _event.value = CreateTicketEvent.Created(id, queued = result == WriteResult.QUEUED)
            } catch (e: Exception) {
                _form.update { it.copy(submitting = false, progress = null, error = e.userMessage()) }
            }
        }
    }
}
