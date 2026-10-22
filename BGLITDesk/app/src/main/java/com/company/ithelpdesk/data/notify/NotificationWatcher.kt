package com.company.ithelpdesk.data.notify

import android.content.Context
import com.company.ithelpdesk.data.SessionManager
import com.company.ithelpdesk.data.SessionState
import com.company.ithelpdesk.di.ApplicationScope
import com.company.ithelpdesk.domain.repository.NotificationRepository
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.emptyFlow
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.onEach
import javax.inject.Inject
import javax.inject.Singleton

/** While the app process is alive, shows phone notifications the moment they arrive in Firestore. */
@OptIn(ExperimentalCoroutinesApi::class)
@Singleton
class NotificationWatcher @Inject constructor(
    @ApplicationContext private val context: Context,
    private val session: SessionManager,
    private val repo: NotificationRepository,
    @ApplicationScope private val scope: CoroutineScope,
) {
    private var started = false

    fun start() {
        if (started) return
        started = true
        NotificationCheckWorker.schedule(context)
        session.state
            .map { (it as? SessionState.Ready)?.profile }
            .distinctUntilChanged { a, b -> a?.uid == b?.uid && a?.role == b?.role }
            .flatMapLatest { profile ->
                if (profile == null) {
                    emptyFlow()
                } else {
                    repo.observeFor(profile)
                        .onEach { SystemNotifier.showNew(context, it, profile.uid) }
                        .catch { }
                }
            }
            .launchIn(scope)
    }
}
