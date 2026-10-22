package com.company.ithelpdesk.ui.shared

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Logout
import androidx.compose.material.icons.outlined.Save
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.company.ithelpdesk.BuildConfig
import com.company.ithelpdesk.ui.auth.ProfileFields
import com.company.ithelpdesk.ui.components.AppCard
import com.company.ithelpdesk.ui.components.Avatar
import com.company.ithelpdesk.ui.components.GradientButton
import com.company.ithelpdesk.ui.components.GradientHero
import com.company.ithelpdesk.util.formatDate

/** Profile editor used by both apps (each wraps it in its own theme). */
@Composable
fun ProfileContent(viewModel: ProfileViewModel, extra: (@Composable () -> Unit)? = null) {
    val profile by viewModel.profile.collectAsStateWithLifecycle()
    val departments by viewModel.departments.collectAsStateWithLifecycle()
    val saving by viewModel.saving.collectAsStateWithLifecycle()
    val message by viewModel.message.collectAsStateWithLifecycle()
    val snackbar = remember { SnackbarHostState() }
    LaunchedEffect(message) {
        message?.let {
            snackbar.showSnackbar(it)
            viewModel.consumeMessage()
        }
    }
    Box(Modifier.fillMaxSize()) {
        Column(Modifier.fillMaxSize().imePadding().verticalScroll(rememberScrollState())) {
            GradientHero {
                Column(
                    Modifier.fillMaxWidth().statusBarsPadding().padding(24.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                ) {
                    Avatar(profile.initials, size = 84.dp)
                    Spacer(Modifier.height(10.dp))
                    Text(profile.name, style = MaterialTheme.typography.headlineSmall, color = Color.White)
                    Text(profile.email, style = MaterialTheme.typography.bodyMedium, color = Color.White.copy(alpha = 0.85f))
                    Spacer(Modifier.height(6.dp))
                    Text(
                        "${if (profile.role.isAdmin) "🛡️ Admin" else "🙂 User"} · member since ${formatDate(profile.createdAt)}",
                        style = MaterialTheme.typography.labelMedium,
                        color = Color.White.copy(alpha = 0.85f),
                    )
                }
            }
            Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                extra?.invoke()
                AppCard {
                    Text("Your details", style = MaterialTheme.typography.titleMedium)
                    Spacer(Modifier.height(8.dp))
                    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                        ProfileFields(viewModel.form, departments.map { it.name })
                    }
                    Spacer(Modifier.height(12.dp))
                    GradientButton("Save changes", onClick = viewModel::save, loading = saving, icon = Icons.Outlined.Save,
                        modifier = Modifier.fillMaxWidth())
                }
                OutlinedButton(
                    onClick = viewModel::signOut,
                    shape = RoundedCornerShape(18.dp),
                    modifier = Modifier.fillMaxWidth().height(52.dp),
                ) {
                    Icon(Icons.AutoMirrored.Filled.Logout, contentDescription = null)
                    Spacer(Modifier.padding(4.dp))
                    Text("Sign out")
                }
                Text(
                    "BGLITDesk ${BuildConfig.VERSION_NAME} · BGL IT",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.align(Alignment.CenterHorizontally).padding(bottom = 16.dp),
                )
            }
        }
        SnackbarHost(snackbar, Modifier.align(Alignment.BottomCenter))
    }
}
