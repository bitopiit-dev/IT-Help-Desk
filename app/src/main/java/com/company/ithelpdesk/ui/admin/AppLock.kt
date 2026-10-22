// © 2026 Khondaker Arifur Rahman. All Rights Reserved.
// BGL-IT Help — see COPYRIGHT.md. Unauthorized copying, modification or redistribution is prohibited.

package com.company.ithelpdesk.ui.admin

import android.content.Context
import android.content.ContextWrapper
import androidx.biometric.BiometricManager
import androidx.biometric.BiometricManager.Authenticators.BIOMETRIC_WEAK
import androidx.biometric.BiometricManager.Authenticators.DEVICE_CREDENTIAL
import androidx.biometric.BiometricPrompt
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Fingerprint
import androidx.compose.material.icons.outlined.Lock
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import androidx.fragment.app.FragmentActivity
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.ViewModel
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.company.ithelpdesk.data.AppLockStore
import com.company.ithelpdesk.ui.components.AppCard
import com.company.ithelpdesk.ui.components.GradientButton
import com.company.ithelpdesk.ui.theme.LocalBrand
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject

/** Biometrics, or the phone PIN/pattern as a fallback. This pair works on every Android version we support. */
private const val AUTHENTICATORS = BIOMETRIC_WEAK or DEVICE_CREDENTIAL

@HiltViewModel
class AppLockViewModel @Inject constructor(val store: AppLockStore) : ViewModel()

private fun Context.findFragmentActivity(): FragmentActivity? {
    var c: Context? = this
    while (c is ContextWrapper) {
        if (c is FragmentActivity) return c
        c = c.baseContext
    }
    return null
}

private fun canUseLock(context: Context): Boolean =
    BiometricManager.from(context).canAuthenticate(AUTHENTICATORS) == BiometricManager.BIOMETRIC_SUCCESS

/**
 * Covers the admin app until the person proves it is them. The app underneath stays composed,
 * so the open screen and navigation are exactly where they left them after unlocking.
 */
@Composable
fun AppLockGate(viewModel: AppLockViewModel = hiltViewModel(), content: @Composable () -> Unit) {
    val store = viewModel.store
    val context = LocalContext.current
    val enabled by store.enabled.collectAsStateWithLifecycle()
    var locked by remember { mutableStateOf(store.needsUnlock()) }
    var message by remember { mutableStateOf<String?>(null) }

    val lifecycleOwner = LocalLifecycleOwner.current
    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            when (event) {
                Lifecycle.Event.ON_STOP -> store.markBackgrounded()
                Lifecycle.Event.ON_START -> if (store.needsUnlock()) locked = true
                else -> Unit
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose { lifecycleOwner.lifecycle.removeObserver(observer) }
    }
    LaunchedEffect(enabled) { if (!enabled) locked = false }

    fun prompt() {
        val activity = context.findFragmentActivity()
        // If the phone no longer has a screen lock, there is nothing to check against: never lock the admin out.
        if (activity == null || !canUseLock(context)) {
            store.markUnlocked()
            locked = false
            return
        }
        message = null
        val callback = object : BiometricPrompt.AuthenticationCallback() {
            override fun onAuthenticationSucceeded(result: BiometricPrompt.AuthenticationResult) {
                store.markUnlocked()
                locked = false
            }

            override fun onAuthenticationError(errorCode: Int, errString: CharSequence) {
                message = when (errorCode) {
                    BiometricPrompt.ERROR_USER_CANCELED, BiometricPrompt.ERROR_CANCELED,
                    BiometricPrompt.ERROR_NEGATIVE_BUTTON -> null
                    else -> errString.toString()
                }
            }
        }
        val info = BiometricPrompt.PromptInfo.Builder()
            .setTitle("Unlock BGL-IT admin")
            .setSubtitle("Use your fingerprint, face or screen lock")
            .setAllowedAuthenticators(AUTHENTICATORS)
            .build()
        BiometricPrompt(activity, ContextCompat.getMainExecutor(activity), callback).authenticate(info)
    }

    LaunchedEffect(locked) { if (locked) prompt() }

    Box(Modifier.fillMaxSize()) {
        // While locked, TalkBack must not read the screen behind the cover.
        Box(if (locked) Modifier.clearAndSetSemantics { } else Modifier) { content() }
        AnimatedVisibility(locked, enter = fadeIn(), exit = fadeOut()) {
            LockCover(message = message, onUnlock = { prompt() })
        }
    }
}

@Composable
private fun LockCover(message: String?, onUnlock: () -> Unit) {
    val brand = LocalBrand.current
    Surface(
        color = MaterialTheme.colorScheme.background,
        modifier = Modifier
            .fillMaxSize()
            .pointerInput(Unit) { awaitPointerEventScope { while (true) awaitPointerEvent() } },
    ) {
        Column(
            Modifier.fillMaxSize().padding(32.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center,
        ) {
            Box(
                Modifier.size(88.dp).clip(CircleShape).background(brand.heroBrush),
                contentAlignment = Alignment.Center,
            ) { Icon(Icons.Outlined.Lock, contentDescription = null, tint = androidx.compose.ui.graphics.Color.White, modifier = Modifier.size(40.dp)) }
            Spacer(Modifier.height(20.dp))
            Text("Admin app locked", style = MaterialTheme.typography.headlineSmall)
            Spacer(Modifier.height(6.dp))
            Text(
                "Unlock to see tickets and people.",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center,
            )
            if (message != null) {
                Spacer(Modifier.height(10.dp))
                Text(message, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.error, textAlign = TextAlign.Center)
            }
            Spacer(Modifier.height(24.dp))
            GradientButton("Unlock", onClick = onUnlock, icon = Icons.Outlined.Fingerprint, modifier = Modifier.fillMaxWidth())
        }
    }
}

/** Switch on the admin profile screen. */
@Composable
fun AppLockSetting(viewModel: AppLockViewModel = hiltViewModel()) {
    val context = LocalContext.current
    val enabled by viewModel.store.enabled.collectAsStateWithLifecycle()
    val available = remember(context) { canUseLock(context) }
    AppCard {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(Icons.Outlined.Fingerprint, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
            Spacer(Modifier.width(12.dp))
            Column(Modifier.weight(1f)) {
                Text("App lock", style = MaterialTheme.typography.titleMedium)
                Text(
                    if (available) "Ask for fingerprint, face or screen lock when the admin app opens, " +
                        "or after a minute in the background."
                    else "Set up a screen lock or fingerprint in your phone's settings to use this.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            Spacer(Modifier.width(8.dp))
            Switch(
                checked = enabled && available,
                onCheckedChange = { viewModel.store.setEnabled(it) },
                enabled = available,
            )
        }
    }
}
