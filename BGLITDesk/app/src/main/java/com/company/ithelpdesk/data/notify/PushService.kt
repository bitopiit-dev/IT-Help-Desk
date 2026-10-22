package com.company.ithelpdesk.data.notify

import com.company.ithelpdesk.R
import com.company.ithelpdesk.domain.repository.UserRepository
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.messaging.FirebaseMessagingService
import com.google.firebase.messaging.RemoteMessage
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.launch
import javax.inject.Inject

/** Instant push. Only used if the optional Cloud Function in `functions/` is deployed (Blaze plan). */
@AndroidEntryPoint
class PushService : FirebaseMessagingService() {

    @Inject lateinit var users: UserRepository

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    override fun onNewToken(token: String) {
        val uid = FirebaseAuth.getInstance().currentUser?.uid ?: return
        scope.launch { runCatching { users.addFcmToken(uid, token) } }
    }

    override fun onMessageReceived(message: RemoteMessage) {
        val title = message.data["title"] ?: message.notification?.title ?: getString(R.string.app_name)
        val body = message.data["body"] ?: message.notification?.body ?: return
        SystemNotifier.showPush(this, message.data["notificationId"], title, body, message.data["ticketId"])
    }

    override fun onDestroy() {
        scope.cancel()
        super.onDestroy()
    }
}
