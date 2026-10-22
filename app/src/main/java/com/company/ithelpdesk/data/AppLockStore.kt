// © 2026 Khondaker Arifur Rahman. All Rights Reserved.
// BGL-IT Help — see COPYRIGHT.md. Unauthorized copying, modification or redistribution is prohibited.

package com.company.ithelpdesk.data

import android.content.Context
import android.os.SystemClock
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import javax.inject.Inject
import javax.inject.Singleton

/**
 * On-device setting for the admin app lock (fingerprint, face or the phone's screen lock).
 * It is per device on purpose: it protects this phone, not the account.
 * Unlock times live in memory, so killing the app always asks again.
 */
@Singleton
class AppLockStore @Inject constructor(@ApplicationContext context: Context) {
    private val prefs = context.getSharedPreferences("app_lock", Context.MODE_PRIVATE)
    private val _enabled = MutableStateFlow(prefs.getBoolean(KEY_ENABLED, false))
    val enabled: StateFlow<Boolean> = _enabled.asStateFlow()

    private var unlockedAt = 0L
    private var backgroundedAt = 0L

    fun setEnabled(value: Boolean) {
        prefs.edit().putBoolean(KEY_ENABLED, value).apply()
        // Turning it on from inside the app counts as unlocked, so it does not lock immediately.
        if (value) markUnlocked()
        _enabled.value = value
    }

    fun markUnlocked() {
        unlockedAt = SystemClock.elapsedRealtime()
        backgroundedAt = 0L
    }

    fun markBackgrounded() {
        if (unlockedAt > 0L) backgroundedAt = SystemClock.elapsedRealtime()
    }

    /** Locked on a fresh start, and after the app was in the background longer than [GRACE_MS]. */
    fun needsUnlock(): Boolean {
        if (!_enabled.value) return false
        if (unlockedAt == 0L) return true
        return backgroundedAt > 0L && SystemClock.elapsedRealtime() - backgroundedAt > GRACE_MS
    }

    private companion object {
        const val KEY_ENABLED = "enabled"
        const val GRACE_MS = 60_000L
    }
}
