package com.company.ithelpdesk.ui.shared

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.company.ithelpdesk.data.SessionManager
import com.company.ithelpdesk.data.SessionState
import com.company.ithelpdesk.domain.model.Department
import com.company.ithelpdesk.domain.model.UserProfile
import com.company.ithelpdesk.domain.repository.SettingsRepository
import com.company.ithelpdesk.domain.repository.UserRepository
import com.company.ithelpdesk.ui.auth.ProfileFormState
import com.company.ithelpdesk.util.userMessage
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.filterIsInstance
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class ProfileViewModel @Inject constructor(
    private val session: SessionManager,
    private val users: UserRepository,
    settings: SettingsRepository,
) : ViewModel() {

    val profile: StateFlow<UserProfile> = session.state
        .filterIsInstance<SessionState.Ready>()
        .map { it.profile }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), session.requireProfile())

    val form = ProfileFormState(session.requireProfile())

    val departments: StateFlow<List<Department>> = settings.observeDepartments()
        .catch { emit(emptyList()) }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    private val _message = MutableStateFlow<String?>(null)
    val message: StateFlow<String?> = _message.asStateFlow()
    private val _saving = MutableStateFlow(false)
    val saving: StateFlow<Boolean> = _saving.asStateFlow()

    fun consumeMessage() { _message.value = null }

    fun save() {
        val error = form.validate()
        if (error != null) {
            _message.value = error
            return
        }
        viewModelScope.launch {
            _saving.value = true
            try {
                users.updateOwnProfile(profile.value.uid, form.name, form.department, form.phone, form.location, form.employeeId)
                _message.value = "Profile saved ✅"
            } catch (e: Exception) {
                _message.value = e.userMessage()
            } finally {
                _saving.value = false
            }
        }
    }

    fun signOut() {
        viewModelScope.launch { session.signOut() }
    }
}
