// © 2026 Khondaker Arifur Rahman. All Rights Reserved.
// BGL-IT Help — see COPYRIGHT.md. Unauthorized copying, modification or redistribution is prohibited.

package com.company.ithelpdesk.data

import android.content.Context
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class LoginPreferences @Inject constructor(
    @ApplicationContext private val context: Context,
) {
    private val prefs = context.getSharedPreferences("bgl_login_prefs", Context.MODE_PRIVATE)

    var rememberMe: Boolean
        get() = prefs.getBoolean("remember_me", false)
        set(value) = prefs.edit().putBoolean("remember_me", value).apply()

    var savedEmail: String
        get() = prefs.getString("saved_email", "") ?: ""
        set(value) = prefs.edit().putString("saved_email", value).apply()

    var loginHistory: List<String>
        get() {
            val raw = prefs.getString("login_history_list", "") ?: ""
            if (raw.isBlank()) return emptyList()
            return raw.split(",").map { it.trim() }.filter { it.isNotBlank() }.distinct()
        }
        set(value) {
            val raw = value.distinct().joinToString(",")
            prefs.edit().putString("login_history_list", raw).apply()
        }

    fun saveLogin(email: String, remember: Boolean) {
        val cleanEmail = email.trim()
        rememberMe = remember
        if (remember && cleanEmail.isNotBlank()) {
            savedEmail = cleanEmail
            val currentHistory = loginHistory.toMutableList()
            currentHistory.remove(cleanEmail)
            currentHistory.add(0, cleanEmail)
            loginHistory = currentHistory.take(5)
        } else if (!remember) {
            savedEmail = ""
        }
    }

    fun clearHistory() {
        prefs.edit().remove("login_history_list").remove("saved_email").remove("remember_me").apply()
    }
}
