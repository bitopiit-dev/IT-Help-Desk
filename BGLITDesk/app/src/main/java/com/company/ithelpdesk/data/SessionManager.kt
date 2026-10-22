package com.company.ithelpdesk.data

import com.company.ithelpdesk.di.ApplicationScope
import com.company.ithelpdesk.domain.model.UserProfile
import com.company.ithelpdesk.domain.repository.AuthRepository
import com.company.ithelpdesk.domain.repository.UserRepository
import com.google.firebase.messaging.FirebaseMessaging
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.tasks.await
import javax.inject.Inject
import javax.inject.Singleton

sealed interface SessionState {
    data object Loading : SessionState
    data object SignedOut : SessionState
    data class NeedsProfile(val uid: String, val email: String) : SessionState
    data class PendingApproval(val profile: UserProfile) : SessionState
    data object Deactivated : SessionState
    data class Ready(val profile: UserProfile) : SessionState
    data class Error(val message: String) : SessionState
}

/** Holds who is signed in and their profile/role, shared by every screen. */
@OptIn(ExperimentalCoroutinesApi::class)
@Singleton
class SessionManager @Inject constructor(
    private val auth: AuthRepository,
    private val users: UserRepository,
    @ApplicationScope scope: CoroutineScope,
) {
    private var registeredTokenFor: String? = null

    val state: StateFlow<SessionState> = auth.observeAuthUser()
        .flatMapLatest { user ->
            if (user == null) {
                registeredTokenFor = null
                flowOf(SessionState.SignedOut)
            } else {
                users.observeProfile(user.uid)
                    .map { p ->
                        when {
                            p == null -> SessionState.NeedsProfile(user.uid, user.email)
                            !p.isActive -> SessionState.Deactivated
                            !p.isApproved -> SessionState.PendingApproval(p)
                            else -> SessionState.Ready(p)
                        }
                    }
                    .catch { emit(SessionState.Error(it.message ?: "Could not load your profile")) }
            }
        }
        .stateIn(scope, SharingStarted.Eagerly, SessionState.Loading)

    val profile: UserProfile? get() = (state.value as? SessionState.Ready)?.profile

    fun requireProfile(): UserProfile = checkNotNull(profile) { "No signed-in user" }

    init {
        state.onEach { s ->
            if (s is SessionState.Ready && registeredTokenFor != s.profile.uid) {
                registeredTokenFor = s.profile.uid
                runCatching {
                    val token = FirebaseMessaging.getInstance().token.await()
                    users.addFcmToken(s.profile.uid, token)
                }
            }
        }.launchIn(scope)
    }

    suspend fun signOut() {
        val uid = profile?.uid ?: auth.currentUser?.uid
        if (uid != null) {
            runCatching {
                val token = FirebaseMessaging.getInstance().token.await()
                users.removeFcmToken(uid, token)
            }
        }
        auth.signOut()
    }
}
