// © 2026 Khondaker Arifur Rahman. All Rights Reserved.
// BGL-IT Help — see COPYRIGHT.md. Unauthorized copying, modification or redistribution is prohibited.

package com.company.ithelpdesk.ui.shared

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.company.ithelpdesk.BuildConfig
import com.company.ithelpdesk.data.SessionManager
import com.company.ithelpdesk.data.SessionState
import com.company.ithelpdesk.domain.model.AppUpdateInfo
import com.company.ithelpdesk.domain.model.Department
import com.company.ithelpdesk.domain.model.UserProfile
import com.company.ithelpdesk.domain.repository.AuthRepository
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
    private val auth: AuthRepository,
    private val settings: SettingsRepository,
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

    fun changePassword(newPass: String, confirmPass: String) {
        when {
            newPass.length < 6 -> {
                _message.value = "Password needs at least 6 characters."
                return
            }
            newPass != confirmPass -> {
                _message.value = "Passwords don't match."
                return
            }
        }
        viewModelScope.launch {
            _saving.value = true
            try {
                auth.updatePassword(newPass)
                _message.value = "Password updated successfully 🔒"
            } catch (e: Exception) {
                _message.value = e.userMessage()
            } finally {
                _saving.value = false
            }
        }
    }

    fun sendSelfPasswordReset() {
        viewModelScope.launch {
            try {
                auth.sendPasswordReset(profile.value.email)
                _message.value = "Password reset link sent to ${profile.value.email} 📬"
            } catch (e: Exception) {
                _message.value = e.userMessage()
            }
        }
    }

    sealed interface UpdateCheckState {
        data object Checking : UpdateCheckState
        data class UpToDate(val version: String) : UpdateCheckState
        data class UpdateAvailable(val info: AppUpdateInfo) : UpdateCheckState
        data class Error(val error: String) : UpdateCheckState
    }

    private val _updateCheck = MutableStateFlow<UpdateCheckState?>(null)
    val updateCheck: StateFlow<UpdateCheckState?> = _updateCheck.asStateFlow()

    fun consumeUpdateCheck() { _updateCheck.value = null }

    fun checkForUpdates() {
        viewModelScope.launch {
            _updateCheck.value = UpdateCheckState.Checking
            try {
                val info = settings.getAppUpdateInfo()
                val currentCode = BuildConfig.VERSION_CODE
                val currentName = BuildConfig.VERSION_NAME

                if (info.latestVersionCode > currentCode) {
                    _updateCheck.value = UpdateCheckState.UpdateAvailable(info)
                } else {
                    _updateCheck.value = UpdateCheckState.UpToDate(currentName)
                }
            } catch (e: Exception) {
                _updateCheck.value = UpdateCheckState.Error(e.userMessage())
            }
        }
    }

    private val _photoBusy = MutableStateFlow(false)
    val photoBusy: StateFlow<Boolean> = _photoBusy.asStateFlow()

    fun setPhoto(uri: android.net.Uri) = photoAction("Profile photo updated 📸") { users.setPhoto(profile.value.uid, uri) }
    fun removePhoto() = photoAction("Profile photo removed") { users.removePhoto(profile.value.uid) }

    private fun photoAction(success: String, block: suspend () -> Unit) {
        viewModelScope.launch {
            _photoBusy.value = true
            try {
                block()
                _message.value = success
            } catch (e: Exception) {
                _message.value = e.userMessage()
            } finally {
                _photoBusy.value = false
            }
        }
    }

    fun signOut() {
        viewModelScope.launch { session.signOut() }
    }
}
