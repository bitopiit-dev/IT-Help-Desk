// © 2026 Khondaker Arifur Rahman. All Rights Reserved.
// BGL-IT Help — see COPYRIGHT.md. Unauthorized copying, modification or redistribution is prohibited.

package com.company.ithelpdesk.ui.components

import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.compositionLocalOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.produceState
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage

/** Loads a profile photo's JPEG bytes. Provided once in AppRoot. */
fun interface AvatarLoaderFn {
    suspend fun load(uid: String, version: Long): ByteArray?
}

val LocalAvatarLoader = compositionLocalOf<AvatarLoaderFn> { AvatarLoaderFn { _, _ -> null } }

/**
 * Profile photo if the person has one, otherwise their initials on a gradient.
 * [photoVersion]: the profile's photoVersion when known (0 = no photo), or -1 when unknown.
 */
@Composable
fun UserAvatar(
    uid: String,
    initials: String,
    modifier: Modifier = Modifier,
    size: Dp = 44.dp,
    photoVersion: Long = -1L,
    ring: Color? = null,
) {
    val loader = LocalAvatarLoader.current
    val bytes by produceState<ByteArray?>(null, uid, photoVersion) {
        value = runCatching { loader.load(uid, photoVersion) }.getOrNull()
    }
    val ringed = if (ring != null) modifier.border(2.dp, ring, CircleShape) else modifier
    Box(ringed.size(size).clip(CircleShape)) {
        if (bytes != null) {
            AsyncImage(
                model = bytes,
                contentDescription = "Profile photo",
                contentScale = ContentScale.Crop,
                modifier = Modifier.fillMaxSize(),
            )
        } else {
            Avatar(initials, size = size)
        }
    }
}
