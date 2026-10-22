package com.company.ithelpdesk.data

import com.company.ithelpdesk.domain.logic.NotificationDraft
import com.company.ithelpdesk.domain.model.AppNotification
import com.company.ithelpdesk.domain.model.UserProfile
import com.company.ithelpdesk.domain.repository.NotificationRepository
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class NotificationRepositoryImpl @Inject constructor(
    private val db: FirebaseFirestore,
) : NotificationRepository {

    private val col get() = db.collection("notifications")

    /** The query must match the security rules: users see their own, admins also see the shared admin feed. */
    fun feedQuery(profile: UserProfile): Query {
        val audiences = if (profile.role.isAdmin) listOf(profile.uid, AppNotification.AUDIENCE_ADMINS) else listOf(profile.uid)
        return col.whereIn("audience", audiences)
            .orderBy("createdAt", Query.Direction.DESCENDING)
            .limit(60)
    }

    override fun observeFor(profile: UserProfile): Flow<List<AppNotification>> =
        feedQuery(profile).asFlow().map { snap -> snap.documents.map { it.toNotification() } }

    override suspend fun send(drafts: List<NotificationDraft>, actorUid: String) {
        if (drafts.isEmpty()) return
        val batch = db.batch()
        drafts.forEach { d ->
            batch.set(
                col.document(),
                mapOf(
                    "audience" to d.audience,
                    "title" to d.title.take(140),
                    "body" to d.body.take(300),
                    "ticketId" to d.ticketId,
                    "kind" to d.kind,
                    "createdBy" to actorUid,
                    "createdAt" to FieldValue.serverTimestamp(),
                    "readBy" to emptyList<String>(),
                ),
            )
        }
        batch.commit().awaitOrQueued()
    }

    override suspend fun markRead(ids: List<String>, uid: String) {
        if (ids.isEmpty()) return
        ids.chunked(400).forEach { chunk ->
            val batch = db.batch()
            chunk.forEach { batch.update(col.document(it), "readBy", FieldValue.arrayUnion(uid)) }
            batch.commit().awaitOrQueued()
        }
    }
}
