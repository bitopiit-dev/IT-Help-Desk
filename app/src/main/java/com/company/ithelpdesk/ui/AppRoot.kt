// © 2026 Khondaker Arifur Rahman. All Rights Reserved.
// BGL-IT Help — see COPYRIGHT.md. Unauthorized copying, modification or redistribution is prohibited.

package com.company.ithelpdesk.ui

import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.HasDefaultViewModelProviderFactory
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.ViewModelStore
import androidx.lifecycle.ViewModelStoreOwner
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.CreationExtras
import androidx.lifecycle.viewmodel.compose.LocalViewModelStoreOwner
import com.company.ithelpdesk.data.SessionState
import com.company.ithelpdesk.ui.admin.AdminShell
import com.company.ithelpdesk.ui.admin.AppLockGate
import com.company.ithelpdesk.ui.auth.AuthFlow
import com.company.ithelpdesk.ui.auth.BlockedScreen
import com.company.ithelpdesk.ui.auth.CompleteProfileScreen
import com.company.ithelpdesk.ui.auth.PendingApprovalScreen
import com.company.ithelpdesk.ui.components.ErrorBox
import com.company.ithelpdesk.ui.components.LoadingBox
import com.company.ithelpdesk.ui.components.LocalAvatarLoader
import com.company.ithelpdesk.ui.components.LocalImageLoader
import com.company.ithelpdesk.ui.theme.AdminTheme
import com.company.ithelpdesk.ui.theme.UserTheme
import com.company.ithelpdesk.ui.user.UserShell

/**
 * Picks the app to show from who is signed in:
 * signed out → sign in / sign up; waiting → pending screen; USER → user app; ADMIN → admin app.
 * The two apps are completely separate (own theme, navigation and screens).
 */
@Composable
fun AppRoot(
    pendingTicketId: String?,
    onPendingTicketHandled: () -> Unit,
    viewModel: SessionViewModel = hiltViewModel(),
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    CompositionLocalProvider(
        LocalImageLoader provides viewModel.imageLoader,
        LocalAvatarLoader provides viewModel.avatarLoader,
    ) {
        when (val s = state) {
            is SessionState.Ready -> SessionScope(key = s.profile.uid + s.profile.role.name) {
                if (s.profile.role.isAdmin) {
                    AdminTheme {
                        Surface(Modifier.fillMaxSize(), color = MaterialTheme.colorScheme.background) {
                            AppLockGate { AdminShell(pendingTicketId, onPendingTicketHandled) }
                        }
                    }
                } else {
                    UserTheme {
                        Surface(Modifier.fillMaxSize(), color = MaterialTheme.colorScheme.background) {
                            UserShell(pendingTicketId, onPendingTicketHandled)
                        }
                    }
                }
            }
            else -> UserTheme {
                Surface(Modifier.fillMaxSize(), color = MaterialTheme.colorScheme.background) {
                    when (s) {
                        SessionState.Loading -> LoadingBox()
                        SessionState.SignedOut -> AuthFlow()
                        is SessionState.NeedsProfile -> CompleteProfileScreen(email = s.email, onSignOut = viewModel::signOut)
                        is SessionState.PendingApproval -> PendingApprovalScreen(s.profile, onSignOut = viewModel::signOut)
                        SessionState.Deactivated -> BlockedScreen(onSignOut = viewModel::signOut)
                        is SessionState.Error -> ErrorBox(message = s.message, onRetry = viewModel::signOut)
                        is SessionState.Ready -> Unit
                    }
                }
            }
        }
    }
}

/**
 * Gives each signed-in person (and role) a fresh set of ViewModels, so nothing from a previous
 * account or from the other app is ever reused after sign-out, sign-in or a role change.
 */
@Composable
private fun SessionScope(key: String, content: @Composable () -> Unit) {
    val parent = checkNotNull(LocalViewModelStoreOwner.current)
    val owner = remember(key) { ScopedOwner(parent) }
    DisposableEffect(owner) { onDispose { owner.viewModelStore.clear() } }
    CompositionLocalProvider(LocalViewModelStoreOwner provides owner) { content() }
}

private class ScopedOwner(private val parent: ViewModelStoreOwner) : ViewModelStoreOwner, HasDefaultViewModelProviderFactory {
    override val viewModelStore = ViewModelStore()
    override val defaultViewModelProviderFactory: ViewModelProvider.Factory
        get() = (parent as HasDefaultViewModelProviderFactory).defaultViewModelProviderFactory
    override val defaultViewModelCreationExtras: CreationExtras
        get() = (parent as HasDefaultViewModelProviderFactory).defaultViewModelCreationExtras
}
