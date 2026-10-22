// © 2026 Khondaker Arifur Rahman. All Rights Reserved.
// BGL-IT Help — see COPYRIGHT.md. Unauthorized copying, modification or redistribution is prohibited.

package com.company.ithelpdesk.ui.shared

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.filled.PhotoCamera
import androidx.compose.material.icons.outlined.Delete
import androidx.compose.material.icons.outlined.PhotoCamera
import androidx.compose.material.icons.outlined.PhotoLibrary
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.core.content.FileProvider
import java.io.File
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
import com.company.ithelpdesk.ui.components.UserAvatar
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
    val photoBusy by viewModel.photoBusy.collectAsStateWithLifecycle()
    val snackbar = remember { SnackbarHostState() }
    val context = LocalContext.current
    var photoMenu by remember { mutableStateOf(false) }
    var showAbout by remember { mutableStateOf(false) }
    if (showAbout) AboutDialog(onDismiss = { showAbout = false })
    var cameraUri by rememberSaveable { mutableStateOf<Uri?>(null) }
    val gallery = rememberLauncherForActivityResult(ActivityResultContracts.PickVisualMedia()) { uri ->
        if (uri != null) viewModel.setPhoto(uri)
    }
    val camera = rememberLauncherForActivityResult(ActivityResultContracts.TakePicture()) { ok ->
        val uri = cameraUri
        if (ok && uri != null) viewModel.setPhoto(uri)
    }
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
                    Box {
                        Box(
                            Modifier.clip(CircleShape).clickable(enabled = !photoBusy) { photoMenu = true },
                            contentAlignment = Alignment.Center,
                        ) {
                            UserAvatar(profile.uid, profile.initials, size = 104.dp, photoVersion = profile.photoVersion, ring = Color.White)
                            if (photoBusy) {
                                Box(Modifier.size(104.dp).background(Color.Black.copy(alpha = 0.45f)), contentAlignment = Alignment.Center) {
                                    CircularProgressIndicator(Modifier.size(28.dp), color = Color.White, strokeWidth = 3.dp)
                                }
                            }
                        }
                        Box(
                            Modifier.align(Alignment.BottomEnd).size(34.dp).clip(CircleShape).background(Color.White)
                                .clickable(enabled = !photoBusy) { photoMenu = true },
                            contentAlignment = Alignment.Center,
                        ) { Icon(Icons.Filled.PhotoCamera, contentDescription = "Change profile photo", tint = Color(0xFF7C3AED), modifier = Modifier.size(18.dp)) }
                        DropdownMenu(expanded = photoMenu, onDismissRequest = { photoMenu = false }) {
                            DropdownMenuItem(
                                text = { Text("Choose from gallery") },
                                leadingIcon = { Icon(Icons.Outlined.PhotoLibrary, null) },
                                onClick = {
                                    photoMenu = false
                                    gallery.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly))
                                },
                            )
                            DropdownMenuItem(
                                text = { Text("Take a selfie") },
                                leadingIcon = { Icon(Icons.Outlined.PhotoCamera, null) },
                                onClick = {
                                    photoMenu = false
                                    val dir = File(context.cacheDir, "camera").apply { mkdirs() }
                                    val uri = FileProvider.getUriForFile(
                                        context, "${context.packageName}.fileprovider", File(dir, "avatar_${System.currentTimeMillis()}.jpg"),
                                    )
                                    cameraUri = uri
                                    camera.launch(uri)
                                },
                            )
                            if (profile.photoVersion > 0) {
                                DropdownMenuItem(
                                    text = { Text("Remove photo") },
                                    leadingIcon = { Icon(Icons.Outlined.Delete, null) },
                                    onClick = { photoMenu = false; viewModel.removePhoto() },
                                )
                            }
                        }
                    }
                    if (profile.photoVersion == 0L) {
                        Spacer(Modifier.height(6.dp))
                        Text("Tap to add a photo", style = MaterialTheme.typography.labelMedium, color = Color.White.copy(alpha = 0.85f))
                    }
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
                androidx.compose.material3.TextButton(
                    onClick = { showAbout = true },
                    modifier = Modifier.align(Alignment.CenterHorizontally),
                ) { Text("About & copyright") }
                Text(
                    "BGL-IT Help ${BuildConfig.VERSION_NAME}\n${com.company.ithelpdesk.Copyright.SHORT}",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    textAlign = androidx.compose.ui.text.style.TextAlign.Center,
                    modifier = Modifier.align(Alignment.CenterHorizontally).padding(bottom = 16.dp),
                )
            }
        }
        SnackbarHost(snackbar, Modifier.align(Alignment.BottomCenter))
    }
}
