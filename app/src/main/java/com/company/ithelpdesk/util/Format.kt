// © 2026 Khondaker Arifur Rahman. All Rights Reserved.
// BGL-IT Help — see COPYRIGHT.md. Unauthorized copying, modification or redistribution is prohibited.

package com.company.ithelpdesk.util

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import kotlinx.coroutines.delay
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import kotlin.math.abs

private val dateTimeFmt = DateTimeFormatter.ofPattern("dd MMM yyyy, h:mm a")
private val dateFmt = DateTimeFormatter.ofPattern("dd MMM yyyy")

fun formatDateTime(ms: Long?): String =
    if (ms == null || ms <= 0) "—" else Instant.ofEpochMilli(ms).atZone(ZoneId.systemDefault()).format(dateTimeFmt)

fun formatDate(ms: Long?): String =
    if (ms == null || ms <= 0) "—" else Instant.ofEpochMilli(ms).atZone(ZoneId.systemDefault()).format(dateFmt)

/** 45m, 3h 20m, 2d 4h */
fun formatDuration(ms: Long?): String {
    if (ms == null) return "—"
    val sign = if (ms < 0) "-" else ""
    val totalMin = abs(ms) / 60_000
    val days = totalMin / (60 * 24)
    val hours = (totalMin / 60) % 24
    val minutes = totalMin % 60
    return sign + when {
        days > 0 -> "${days}d ${hours}h"
        hours > 0 -> "${hours}h ${minutes}m"
        else -> "${minutes}m"
    }
}

fun formatRelative(ms: Long, now: Long): String {
    if (ms <= 0) return "just now"
    val diff = now - ms
    return if (diff < 60_000) "just now" else "${formatDuration(diff)} ago"
}

/** Current time that refreshes periodically so elapsed times and SLA badges stay live. */
@Composable
fun rememberNow(intervalMs: Long = 30_000): Long {
    var now by remember { mutableLongStateOf(System.currentTimeMillis()) }
    LaunchedEffect(intervalMs) {
        while (true) {
            delay(intervalMs)
            now = System.currentTimeMillis()
        }
    }
    return now
}

fun Throwable.userMessage(): String {
    val msg = message.orEmpty()
    return when {
        msg.contains("PERMISSION_DENIED", true) -> "You don't have permission to do that."
        msg.contains("network", true) || msg.contains("UNAVAILABLE") -> "No connection. Check your internet and try again."
        msg.contains("password is invalid", true) || msg.contains("INVALID_LOGIN_CREDENTIALS") ||
            msg.contains("credential is incorrect", true) -> "Wrong email or password."
        msg.contains("no user record", true) -> "No account found for this email."
        msg.contains("already in use", true) -> "An account with this email already exists. Try signing in."
        msg.isNotBlank() -> msg
        else -> "Something went wrong. Please try again."
    }
}
