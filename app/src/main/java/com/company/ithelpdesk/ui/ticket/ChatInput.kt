// © 2026 Khondaker Arifur Rahman. All Rights Reserved.
// BGL-IT Help — see COPYRIGHT.md. Unauthorized copying, modification or redistribution is prohibited.

package com.company.ithelpdesk.ui.ticket

import android.net.Uri
import androidx.compose.animation.AnimatedVisibility
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.outlined.AddPhotoAlternate
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FilterChip
import androidx.compose.material3.SuggestionChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import com.company.ithelpdesk.ui.theme.LocalBrand

/**
 * Message box with photo attach; admins also get an "internal note" toggle and
 * one-tap [quickReplies] (label to text) shown while the box is empty.
 */
@Composable
fun ChatInput(
    busy: Boolean,
    allowInternal: Boolean,
    placeholder: String,
    quickReplies: List<Pair<String, String>> = emptyList(),
    onSend: (text: String, internal: Boolean, images: List<Uri>, clear: () -> Unit) -> Unit,
) {
    var text by rememberSaveable { mutableStateOf("") }
    var internal by rememberSaveable { mutableStateOf(false) }
    val images = remember { mutableStateListOf<Uri>() }
    val brand = LocalBrand.current
    val picker = rememberLauncherForActivityResult(ActivityResultContracts.PickMultipleVisualMedia(3)) { uris ->
        images.addAll(uris.filter { it !in images }.take(3 - images.size))
    }
    Surface(tonalElevation = 3.dp, color = MaterialTheme.colorScheme.surfaceContainer) {
        Column(Modifier.fillMaxWidth().navigationBarsPadding().imePadding().padding(horizontal = 12.dp, vertical = 8.dp)) {
            if (images.isNotEmpty()) {
                Row(Modifier.horizontalScroll(rememberScrollState()).padding(bottom = 6.dp), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    images.forEach { uri ->
                        Box(Modifier.size(56.dp)) {
                            AsyncImage(uri, null, contentScale = ContentScale.Crop, modifier = Modifier.fillMaxSize().clip(RoundedCornerShape(12.dp)))
                            Box(
                                Modifier.align(Alignment.TopEnd).size(20.dp).clip(CircleShape).background(Color.Black.copy(alpha = 0.6f))
                                    .clickable { images.remove(uri) },
                                contentAlignment = Alignment.Center,
                            ) { Icon(Icons.Default.Close, "Remove", tint = Color.White, modifier = Modifier.size(12.dp)) }
                        }
                    }
                }
            }
            AnimatedVisibility(visible = quickReplies.isNotEmpty() && text.isBlank() && !internal) {
                Row(
                    Modifier.horizontalScroll(rememberScrollState()).padding(bottom = 4.dp),
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                ) {
                    quickReplies.forEach { (label, body) ->
                        SuggestionChip(onClick = { text = body }, label = { Text(label) }, shape = CircleShape)
                    }
                }
            }
            if (allowInternal) {
                FilterChip(
                    selected = internal,
                    onClick = { internal = !internal },
                    label = { Text(if (internal) "🔒 Internal note (IT only)" else "Visible to requester") },
                    shape = CircleShape,
                )
            }
            Row(verticalAlignment = Alignment.CenterVertically) {
                IconButton(onClick = { picker.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)) }) {
                    Icon(Icons.Outlined.AddPhotoAlternate, contentDescription = "Attach photo")
                }
                OutlinedTextField(
                    value = text,
                    onValueChange = { text = it.take(2000) },
                    placeholder = { Text(placeholder) },
                    maxLines = 4,
                    shape = RoundedCornerShape(24.dp),
                    modifier = Modifier.weight(1f),
                )
                Box(
                    Modifier.padding(start = 8.dp).size(48.dp).clip(CircleShape).background(brand.accentBrush)
                        .clickable(enabled = !busy && (text.isNotBlank() || images.isNotEmpty())) {
                            onSend(text, internal, images.toList()) {
                                text = ""
                                images.clear()
                            }
                        },
                    contentAlignment = Alignment.Center,
                ) {
                    if (busy) CircularProgressIndicator(Modifier.size(20.dp), color = Color.White, strokeWidth = 2.dp)
                    else Icon(Icons.AutoMirrored.Filled.Send, contentDescription = "Send", tint = Color.White)
                }
            }
        }
    }
}
