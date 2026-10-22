// © 2026 Khondaker Arifur Rahman. All Rights Reserved.
// BGL-IT Help — see COPYRIGHT.md. Unauthorized copying, modification or redistribution is prohibited.

package com.company.ithelpdesk

import android.Manifest
import android.content.Intent
import android.os.Build
import android.os.Bundle
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.runtime.getValue
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import androidx.fragment.app.FragmentActivity
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.company.ithelpdesk.data.SessionManager
import com.company.ithelpdesk.data.SessionState
import com.company.ithelpdesk.ui.AppRoot
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.flow.MutableStateFlow
import javax.inject.Inject

/**
 * FragmentActivity (a ComponentActivity) so the admin app lock can show the system BiometricPrompt.
 */
@AndroidEntryPoint
class MainActivity : FragmentActivity() {

    @Inject lateinit var session: SessionManager

    /** Ticket to open when the app is launched from a notification. */
    private val pendingTicketId = MutableStateFlow<String?>(null)

    private val notificationPermission = registerForActivityResult(ActivityResultContracts.RequestPermission()) { }

    override fun onCreate(savedInstanceState: Bundle?) {
        // Keeps the system splash up until we know who is signed in, so there is no logo flash in between.
        val splash = installSplashScreen()
        super.onCreate(savedInstanceState)
        // Never hold it longer than 2.5 s (slow network): the in-app loading logo takes over after that.
        val splashStart = android.os.SystemClock.uptimeMillis()
        splash.setKeepOnScreenCondition {
            val waiting = !::session.isInitialized || session.state.value is SessionState.Loading
            waiting && android.os.SystemClock.uptimeMillis() - splashStart < 2_500
        }
        enableEdgeToEdge()
        handleIntent(intent)
        if (Build.VERSION.SDK_INT >= 33) notificationPermission.launch(Manifest.permission.POST_NOTIFICATIONS)
        setContent {
            val pending by pendingTicketId.collectAsStateWithLifecycle()
            AppRoot(
                pendingTicketId = pending,
                onPendingTicketHandled = { pendingTicketId.value = null },
            )
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        handleIntent(intent)
    }

    private fun handleIntent(intent: Intent?) {
        intent?.getStringExtra(EXTRA_TICKET_ID)?.let { pendingTicketId.value = it }
    }

    companion object {
        const val EXTRA_TICKET_ID = "ticketId"
    }
}
