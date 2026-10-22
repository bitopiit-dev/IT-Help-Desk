package com.company.ithelpdesk.ui.auth

import android.util.Patterns
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.company.ithelpdesk.domain.model.Department
import com.company.ithelpdesk.domain.model.UserProfile
import com.company.ithelpdesk.domain.repository.AuthRepository
import com.company.ithelpdesk.domain.repository.SettingsRepository
import com.company.ithelpdesk.domain.repository.UserRepository
import com.company.ithelpdesk.util.userMessage
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

/** Profile fields shared by registration, profile completion and the profile screens. */
class ProfileFormState(initial: UserProfile? = null) {
    var name by mutableStateOf(initial?.name ?: "")
    var employeeId by mutableStateOf(initial?.employeeId ?: "")
    var department by mutableStateOf(initial?.department ?: "")
    var phone by mutableStateOf(initial?.phone ?: "")
    var location by mutableStateOf(initial?.location ?: "")

    fun validate(): String? = when {
        name.isBlank() -> "Please enter your name."
        department.isBlank() -> "Please choose or type your department."
        phone.isBlank() -> "Please add a phone number so IT can reach you."
        else -> null
    }
}

@HiltViewModel
class AuthViewModel @Inject constructor(
    private val auth: AuthRepository,
    private val users: UserRepository,
    settings: SettingsRepository,
) : ViewModel() {

    data class UiState(val loading: Boolean = false, val error: String? = null, val info: String? = null)

    private val _state = MutableStateFlow(UiState())
    val state: StateFlow<UiState> = _state.asStateFlow()

    val departments: StateFlow<List<Department>> = settings.observeDepartments()
        .catch { emit(emptyList()) }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    fun clearMessages() = _state.update { it.copy(error = null, info = null) }

    private fun isEmail(email: String) = Patterns.EMAIL_ADDRESS.matcher(email.trim()).matches()

    fun signIn(email: String, password: String) {
        if (email.isBlank() || password.isBlank()) {
            _state.update { it.copy(error = "Enter your email and password.") }
            return
        }
        launchAction { auth.signIn(email, password) }
    }

    fun resetPassword(email: String) {
        if (!isEmail(email)) {
            _state.update { it.copy(error = "Type your email above first, then tap \"Forgot password\".") }
            return
        }
        launchAction {
            auth.sendPasswordReset(email)
            _state.update { it.copy(info = "Reset link sent to ${email.trim()}. Check your inbox (and spam).") }
        }
    }

    /** Anyone can sign up with any email address; an admin approves the request before they get in. */
    fun register(email: String, password: String, confirm: String, form: ProfileFormState) {
        val error = when {
            !isEmail(email) -> "That doesn't look like an email address."
            password.length < 6 -> "Password needs at least 6 characters."
            password != confirm -> "Passwords don't match."
            else -> form.validate()
        }
        if (error != null) {
            _state.update { it.copy(error = error) }
            return
        }
        launchAction {
            val user = auth.register(email, password)
            users.createProfile(form.toProfile(user.uid, user.email))
        }
    }

    fun completeProfile(form: ProfileFormState) {
        val error = form.validate()
        if (error != null) {
            _state.update { it.copy(error = error) }
            return
        }
        val user = auth.currentUser ?: return
        launchAction { users.createProfile(form.toProfile(user.uid, user.email)) }
    }

    private fun ProfileFormState.toProfile(uid: String, email: String) = UserProfile(
        uid = uid,
        name = name.trim(),
        email = email.trim(),
        employeeId = employeeId.trim(),
        department = department.trim(),
        phone = phone.trim(),
        location = location.trim(),
    )

    private fun launchAction(block: suspend () -> Unit) {
        viewModelScope.launch {
            _state.update { it.copy(loading = true, error = null, info = null) }
            try {
                block()
                _state.update { it.copy(loading = false) }
            } catch (e: Exception) {
                _state.update { it.copy(loading = false, error = e.userMessage()) }
            }
        }
    }
}
