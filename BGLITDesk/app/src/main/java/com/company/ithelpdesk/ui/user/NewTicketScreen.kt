package com.company.ithelpdesk.ui.user

import android.content.Context
import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.outlined.PhotoCamera
import androidx.compose.material.icons.outlined.PhotoLibrary
import androidx.compose.material.icons.outlined.QrCodeScanner
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.FileProvider
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import coil.compose.AsyncImage
import com.company.ithelpdesk.domain.model.ContactMethod
import com.company.ithelpdesk.domain.model.Priority
import com.company.ithelpdesk.ui.components.DropdownField
import com.company.ithelpdesk.ui.components.GradientButton
import com.company.ithelpdesk.ui.components.categoryEmoji
import com.company.ithelpdesk.ui.theme.LocalBrand
import com.google.mlkit.vision.codescanner.GmsBarcodeScanning
import java.io.File

private val priorityEmoji = mapOf(
    Priority.LOW to "🐢",
    Priority.MEDIUM to "🙂",
    Priority.HIGH to "😬",
    Priority.CRITICAL to "🚨",
)

private val priorityHint = mapOf(
    Priority.LOW to "Whenever you get a chance",
    Priority.MEDIUM to "Annoying, but I can still work",
    Priority.HIGH to "Blocking a big part of my work",
    Priority.CRITICAL to "Can't work at all / many people affected",
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun NewTicketScreen(onBack: () -> Unit, onCreated: (String) -> Unit, viewModel: CreateTicketViewModel = hiltViewModel()) {
    val form by viewModel.form.collectAsStateWithLifecycle()
    val categories by viewModel.categories.collectAsStateWithLifecycle()
    val event by viewModel.event.collectAsStateWithLifecycle()
    val context = LocalContext.current
    val snackbar = remember { SnackbarHostState() }
    val brand = LocalBrand.current
    var pendingCameraUri by rememberSaveable { mutableStateOf<Uri?>(null) }

    val gallery = rememberLauncherForActivityResult(
        ActivityResultContracts.PickMultipleVisualMedia(CreateTicketViewModel.MAX_PHOTOS),
    ) { uris -> viewModel.addPhotos(uris) }
    val camera = rememberLauncherForActivityResult(ActivityResultContracts.TakePicture()) { ok ->
        val uri = pendingCameraUri
        if (ok && uri != null) viewModel.addPhotos(listOf(uri))
    }

    LaunchedEffect(event) {
        when (val e = event) {
            is CreateTicketEvent.Created -> {
                viewModel.consumeEvent()
                onCreated(e.ticketId)
            }
            null -> Unit
        }
    }
    LaunchedEffect(form.error) { form.error?.let { snackbar.showSnackbar(it) } }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("New ticket") },
                navigationIcon = { IconButton(onClick = onBack) { Icon(Icons.AutoMirrored.Filled.ArrowBack, "Back") } },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = MaterialTheme.colorScheme.background),
            )
        },
        snackbarHost = { SnackbarHost(snackbar) },
    ) { padding ->
        Column(
            Modifier
                .fillMaxSize()
                .padding(padding)
                .imePadding()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 20.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp),
        ) {
            Text("1 · What kind of problem?", style = MaterialTheme.typography.titleMedium)
            Row(Modifier.horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                categories.forEach { c ->
                    val selected = form.category?.name == c.name
                    Column(
                        Modifier
                            .width(88.dp)
                            .clip(MaterialTheme.shapes.large)
                            .background(if (selected) brand.accentBrush else androidx.compose.ui.graphics.SolidColor(MaterialTheme.colorScheme.surfaceContainerHigh))
                            .clickable { viewModel.update { copy(category = c, subCategory = "") } }
                            .padding(vertical = 12.dp, horizontal = 6.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                    ) {
                        Text(c.emoji.ifBlank { categoryEmoji(c.name) }, fontSize = 24.sp)
                        Text(c.name, style = MaterialTheme.typography.labelSmall, maxLines = 2,
                            color = if (selected) Color.White else MaterialTheme.colorScheme.onSurface)
                    }
                }
            }
            if (form.showErrors && form.categoryError != null) {
                Text(form.categoryError!!, color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodySmall)
            }
            val subs = form.category?.subCategories.orEmpty()
            if (subs.isNotEmpty()) {
                DropdownField("Narrow it down (optional)", subs, form.subCategory.ifBlank { null }, { it },
                    { s -> viewModel.update { copy(subCategory = s) } }, Modifier.fillMaxWidth())
            }

            Text("2 · Tell us what happened", style = MaterialTheme.typography.titleMedium)
            OutlinedTextField(
                value = form.title,
                onValueChange = { v -> viewModel.update { copy(title = v.take(120)) } },
                label = { Text("Short title") },
                placeholder = { Text("e.g. Wi-Fi keeps dropping") },
                isError = form.showErrors && form.titleError != null,
                supportingText = if (form.showErrors) form.titleError?.let { { Text(it) } } else null,
                singleLine = true,
                shape = MaterialTheme.shapes.medium,
                modifier = Modifier.fillMaxWidth(),
            )
            OutlinedTextField(
                value = form.description,
                onValueChange = { v -> viewModel.update { copy(description = v.take(4000)) } },
                label = { Text("Details") },
                placeholder = { Text("What were you doing? Any error message?") },
                isError = form.showErrors && form.descriptionError != null,
                supportingText = if (form.showErrors) form.descriptionError?.let { { Text(it) } } else null,
                minLines = 4,
                shape = MaterialTheme.shapes.medium,
                modifier = Modifier.fillMaxWidth(),
            )

            Text("3 · How urgent?", style = MaterialTheme.typography.titleMedium)
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Priority.entries.forEach { p ->
                    val selected = form.priority == p
                    Row(
                        Modifier
                            .fillMaxWidth()
                            .clip(MaterialTheme.shapes.medium)
                            .border(
                                if (selected) 2.dp else 1.dp,
                                if (selected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outlineVariant,
                                MaterialTheme.shapes.medium,
                            )
                            .background(if (selected) MaterialTheme.colorScheme.primary.copy(alpha = 0.08f) else Color.Transparent)
                            .clickable { viewModel.update { copy(priority = p) } }
                            .padding(12.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Text(priorityEmoji.getValue(p), fontSize = 22.sp)
                        Spacer(Modifier.width(12.dp))
                        Column {
                            Text(p.label, style = MaterialTheme.typography.titleSmall)
                            Text(priorityHint.getValue(p), style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                    }
                }
            }

            Text("4 · Photos (optional)", style = MaterialTheme.typography.titleMedium)
            Row(Modifier.horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                form.photos.forEach { uri ->
                    Box(Modifier.size(84.dp)) {
                        AsyncImage(uri, contentDescription = "Selected photo", contentScale = ContentScale.Crop,
                            modifier = Modifier.fillMaxSize().clip(RoundedCornerShape(16.dp)))
                        Box(
                            Modifier.align(Alignment.TopEnd).padding(4.dp).size(24.dp).clip(CircleShape)
                                .background(Color.Black.copy(alpha = 0.6f)).clickable { viewModel.removePhoto(uri) },
                            contentAlignment = Alignment.Center,
                        ) { Icon(Icons.Default.Close, "Remove photo", tint = Color.White, modifier = Modifier.size(14.dp)) }
                    }
                }
            }
            if (form.photos.size < CreateTicketViewModel.MAX_PHOTOS) {
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedButton(onClick = {
                        val uri = newCameraUri(context)
                        pendingCameraUri = uri
                        camera.launch(uri)
                    }, shape = CircleShape) {
                        Icon(Icons.Outlined.PhotoCamera, null)
                        Spacer(Modifier.width(6.dp))
                        Text("Camera")
                    }
                    OutlinedButton(onClick = {
                        gallery.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly))
                    }, shape = CircleShape) {
                        Icon(Icons.Outlined.PhotoLibrary, null)
                        Spacer(Modifier.width(6.dp))
                        Text("Gallery")
                    }
                }
            }

            Text("5 · Extras", style = MaterialTheme.typography.titleMedium)
            OutlinedTextField(
                value = form.assetTag,
                onValueChange = { v -> viewModel.update { copy(assetTag = v.take(60)) } },
                label = { Text("Asset tag (optional)") },
                singleLine = true,
                shape = MaterialTheme.shapes.medium,
                trailingIcon = {
                    IconButton(onClick = {
                        GmsBarcodeScanning.getClient(context).startScan()
                            .addOnSuccessListener { code -> code.rawValue?.let { v -> viewModel.update { copy(assetTag = v) } } }
                    }) { Icon(Icons.Outlined.QrCodeScanner, "Scan barcode") }
                },
                modifier = Modifier.fillMaxWidth(),
            )
            Text("How should IT reach you?", style = MaterialTheme.typography.labelLarge)
            Row(Modifier.horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                ContactMethod.entries.forEach { m ->
                    FilterChip(selected = form.contactMethod == m, onClick = { viewModel.update { copy(contactMethod = m) } },
                        label = { Text(m.label) }, shape = CircleShape)
                }
            }

            Spacer(Modifier.height(4.dp))
            GradientButton(
                text = form.progress ?: "Send to IT",
                onClick = viewModel::submit,
                loading = form.submitting && form.progress == null,
                enabled = !form.submitting,
                icon = Icons.AutoMirrored.Filled.Send,
                modifier = Modifier.fillMaxWidth(),
            )
            Text(
                "Your phone model and Android version are attached automatically to help IT.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.navigationBarsPadding().padding(bottom = 16.dp),
            )
        }
    }
}

private fun newCameraUri(context: Context): Uri {
    val dir = File(context.cacheDir, "camera").apply { mkdirs() }
    val file = File(dir, "photo_${System.currentTimeMillis()}.jpg")
    return FileProvider.getUriForFile(context, "${context.packageName}.fileprovider", file)
}
