// © 2026 Khondaker Arifur Rahman. All Rights Reserved.
// BGL-IT Help — see COPYRIGHT.md. Unauthorized copying, modification or redistribution is prohibited.

package com.company.ithelpdesk.data

import android.content.Context
import android.net.Uri
import android.util.Base64
import android.util.LruCache
import com.company.ithelpdesk.domain.logic.DashboardSource
import com.company.ithelpdesk.domain.logic.NotificationRules
import com.company.ithelpdesk.domain.logic.SlaCalculator
import com.company.ithelpdesk.domain.logic.StatusWorkflow
import com.company.ithelpdesk.domain.logic.TicketNumber
import com.company.ithelpdesk.domain.model.Attachment
import com.company.ithelpdesk.domain.model.Comment
import com.company.ithelpdesk.domain.model.Resolution
import com.company.ithelpdesk.domain.model.SlaSettings
import com.company.ithelpdesk.domain.model.StatusChange
import com.company.ithelpdesk.domain.model.Ticket
import com.company.ithelpdesk.domain.model.TicketStatus
import com.company.ithelpdesk.domain.model.UserProfile
import com.company.ithelpdesk.domain.repository.NotificationRepository
import com.company.ithelpdesk.domain.repository.TicketRepository
import com.company.ithelpdesk.domain.repository.WriteResult
import com.company.ithelpdesk.util.ImageCompressor
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeout
import java.util.UUID
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Everything here runs from the app on the free Spark plan:
 *  - ticket numbers come from a counter document updated in a transaction,
 *  - SLA due dates are set when the ticket is created,
 *  - photos are compressed and stored in Firestore,
 *  - notifications are written to the `notifications` collection.
 */
@Singleton
class TicketRepositoryImpl @Inject constructor(
    private val db: FirebaseFirestore,
    private val notifications: NotificationRepository,
    @ApplicationContext private val context: Context,
) : TicketRepository {

    private val tickets get() = db.collection("tickets")
    private val openStatusNames = TicketStatus.OPEN.map { it.name }
    private val closedStatusNames = TicketStatus.entries.filter { !it.isOpen }.map { it.name }

    /** About 12 MB of decoded photos kept in memory. */
    private val imageCache = object : LruCache<String, ByteArray>(12 * 1024 * 1024) {
        override fun sizeOf(key: String, value: ByteArray) = value.size
    }

    override fun newTicketId(): String = tickets.document().id

    override fun observeMyTickets(uid: String): Flow<List<Ticket>> =
        tickets.whereEqualTo("requesterId", uid)
            .orderBy("createdAt", Query.Direction.DESCENDING)
            .asFlow()
            .map { snap -> snap.documents.mapNotNull { it.toTicket() } }

    override fun observeOpenTickets(): Flow<List<Ticket>> =
        tickets.whereIn("status", openStatusNames)
            .orderBy("createdAt", Query.Direction.DESCENDING)
            .asFlow()
            .map { snap -> snap.documents.mapNotNull { it.toTicket() } }

    override fun observeRecentClosed(limit: Long): Flow<List<Ticket>> =
        tickets.whereIn("status", closedStatusNames)
            .orderBy("createdAt", Query.Direction.DESCENDING)
            .limit(limit)
            .asFlow()
            .map { snap -> snap.documents.mapNotNull { it.toTicket() } }

    override fun observeTicketsByAssignee(uid: String): Flow<List<Ticket>> =
        tickets.whereEqualTo("assigneeId", uid)
            .orderBy("createdAt", Query.Direction.DESCENDING)
            .limit(200)
            .asFlow()
            .map { snap -> snap.documents.mapNotNull { it.toTicket() } }

    override fun observeTicket(id: String): Flow<Ticket?> =
        tickets.document(id).asFlow().map { it.toTicket() }

    override fun observeComments(ticketId: String, includeInternal: Boolean): Flow<List<Comment>> {
        var query: Query = tickets.document(ticketId).collection("comments")
        // Security rules only allow requesters to list non-internal comments, so the query must say so.
        if (!includeInternal) query = query.whereEqualTo("isInternal", false)
        return query.orderBy("createdAt", Query.Direction.ASCENDING)
            .asFlow()
            .map { snap -> snap.documents.map { it.toComment() } }
    }

    override fun observeHistory(ticketId: String): Flow<List<StatusChange>> =
        tickets.document(ticketId).collection("statusHistory")
            .orderBy("changedAt", Query.Direction.ASCENDING)
            .asFlow()
            .map { snap -> snap.documents.map { it.toStatusChange() } }

    override fun observeDashboardSource(createdFrom: Long, resolvedFrom: Long): Flow<DashboardSource> {
        val created = tickets.whereGreaterThanOrEqualTo("createdAt", createdFrom.toTimestamp()).asFlow()
            .map { s -> s.documents.mapNotNull { it.toTicket() } }
        val resolved = tickets.whereGreaterThanOrEqualTo("resolvedAt", resolvedFrom.toTimestamp()).asFlow()
            .map { s -> s.documents.mapNotNull { it.toTicket() } }
        val open = tickets.whereIn("status", openStatusNames).asFlow()
            .map { s -> s.documents.mapNotNull { it.toTicket() } }
        return combine(created, resolved, open) { c, r, o -> DashboardSource(c, r, o) }
    }

    // ---------- Photos (stored inside Firestore) ----------

    override suspend fun saveImage(ticketId: String, uri: Uri, owner: UserProfile): Attachment {
        val bytes = withContext(Dispatchers.IO) { ImageCompressor.compress(context, uri) }
        val encoded = withContext(Dispatchers.Default) { Base64.encodeToString(bytes, Base64.NO_WRAP) }
        val ref = tickets.document(ticketId).collection("images").document()
        ref.set(
            mapOf(
                "data" to encoded,
                "mime" to "image/jpeg",
                "size" to bytes.size,
                "createdBy" to owner.uid,
                "createdAt" to FieldValue.serverTimestamp(),
            ),
        ).awaitOrQueued(timeoutMs = 20_000)
        imageCache.put(ref.id, bytes)
        return Attachment(id = ref.id, name = "${UUID.randomUUID().toString().take(8)}.jpg")
    }

    override suspend fun loadImage(ticketId: String, imageId: String): ByteArray? {
        imageCache.get(imageId)?.let { return it }
        val snap = tickets.document(ticketId).collection("images").document(imageId).get().await()
        val data = snap.getString("data") ?: return null
        val bytes = withContext(Dispatchers.Default) { Base64.decode(data, Base64.DEFAULT) }
        imageCache.put(imageId, bytes)
        return bytes
    }

    // ---------- Writes ----------

    override suspend fun createTicket(ticket: Ticket, sla: SlaSettings): WriteResult {
        val now = System.currentTimeMillis()
        val (respondDue, resolveDue) = SlaCalculator.dueDates(now, ticket.priority, sla)
        val base = ticket.copy(slaResponseDueAt = respondDue, slaResolveDueAt = resolveDue)
        val ref = tickets.document(ticket.id)
        val day = TicketNumber.dateOf(now)
        val counter = db.collection("counters").document(TicketNumber.counterId(day))

        // Preferred path: a transaction gives every ticket a unique number such as BGL-261022-0042.
        val numbered = runCatching {
            withTimeout(10_000) {
                db.runTransaction { tx ->
                    val current = tx.get(counter).getLong("value") ?: 0L
                    val next = current + 1
                    tx.set(counter, mapOf("value" to next))
                    tx.set(ref, base.copy(ticketNumber = TicketNumber.format(day, next)).toCreateMap())
                    next
                }.await()
            }
        }

        val result: WriteResult
        val saved: Ticket
        if (numbered.isSuccess) {
            saved = base.copy(ticketNumber = TicketNumber.format(day, numbered.getOrThrow()))
            result = WriteResult.SYNCED
        } else {
            val error = numbered.exceptionOrNull()
            if (error?.message.orEmpty().contains("PERMISSION_DENIED", true)) throw error!!
            // Offline: transactions need the server, so save without a number. It still syncs later.
            saved = base
            result = ref.set(base.toCreateMap()).awaitOrQueued()
        }
        runCatching { notifications.send(listOf(NotificationRules.ticketCreated(saved)), ticket.requesterId) }
        return result
    }

    override suspend fun changeStatus(
        ticket: Ticket,
        to: TicketStatus,
        actor: UserProfile,
        note: String,
        resolution: Resolution?,
    ): WriteResult {
        val now = System.currentTimeMillis()
        val fx = StatusWorkflow.effectsOf(ticket, to, actor.role.isStaff, now)
        val updates = mutableMapOf<String, Any?>(
            "status" to to.name,
            "updatedAt" to FieldValue.serverTimestamp(),
            "lastChangedBy" to actor.uid,
            "lastChangedByName" to actor.name,
        )
        if (fx.touchesHold) {
            updates["onHoldSince"] = fx.onHoldSince?.toTimestamp()
            if (fx.addOnHoldMs > 0) updates["onHoldTotalMs"] = FieldValue.increment(fx.addOnHoldMs)
        }
        if (fx.markFirstResponse) updates["firstResponseAt"] = FieldValue.serverTimestamp()
        if (fx.markResolved) updates["resolvedAt"] = FieldValue.serverTimestamp()
        if (fx.clearResolved) {
            updates["resolvedAt"] = null
            updates["closedAt"] = null
            updates["reopenedAt"] = FieldValue.serverTimestamp()
        }
        if (fx.markClosed) updates["closedAt"] = FieldValue.serverTimestamp()
        if (fx.incrementReopen) updates["reopenCount"] = FieldValue.increment(1)
        if (resolution != null) updates["resolution"] = resolution.toMap()

        val ref = tickets.document(ticket.id)
        val batch = db.batch()
        batch.update(ref, updates)
        batch.set(ref.collection("statusHistory").document(), historyEntry(ticket.status, to, actor, note))
        val result = batch.commit().awaitOrQueued()
        runCatching { notifications.send(NotificationRules.statusChanged(ticket, to, actor), actor.uid) }
        return result
    }

    override suspend fun assign(ticket: Ticket, assignee: UserProfile, actor: UserProfile): WriteResult {
        val ref = tickets.document(ticket.id)
        val updates = mutableMapOf<String, Any?>(
            "assigneeId" to assignee.uid,
            "assigneeName" to assignee.name,
            "assigneePhone" to assignee.phone,
            "updatedAt" to FieldValue.serverTimestamp(),
            "lastChangedBy" to actor.uid,
            "lastChangedByName" to actor.name,
        )
        if (ticket.firstResponseAt == null) updates["firstResponseAt"] = FieldValue.serverTimestamp()
        val becomesAssigned = ticket.status == TicketStatus.NEW || ticket.status == TicketStatus.REOPENED
        if (becomesAssigned) updates["status"] = TicketStatus.ASSIGNED.name

        val note = if (assignee.uid == actor.uid) "Picked up by ${actor.name}" else "Assigned to ${assignee.name}"
        val batch = db.batch()
        batch.update(ref, updates)
        batch.set(
            ref.collection("statusHistory").document(),
            historyEntry(ticket.status, if (becomesAssigned) TicketStatus.ASSIGNED else ticket.status, actor, note),
        )
        val result = batch.commit().awaitOrQueued()
        runCatching { notifications.send(NotificationRules.assigned(ticket, assignee, actor), actor.uid) }
        return result
    }

    override suspend fun addComment(
        ticket: Ticket,
        actor: UserProfile,
        text: String,
        images: List<Attachment>,
        internal: Boolean,
    ): WriteResult {
        val isInternal = internal && actor.role.isStaff
        val ref = tickets.document(ticket.id)
        val batch = db.batch()
        batch.set(
            ref.collection("comments").document(),
            mapOf(
                "authorId" to actor.uid,
                "authorName" to actor.name,
                "authorRole" to actor.role.name,
                "text" to text.trim(),
                "images" to images.map { it.toMap() },
                "isInternal" to isInternal,
                "createdAt" to FieldValue.serverTimestamp(),
            ),
        )
        if (actor.role.isStaff && ticket.firstResponseAt == null && ticket.requesterId != actor.uid) {
            batch.update(
                ref,
                mapOf("firstResponseAt" to FieldValue.serverTimestamp(), "updatedAt" to FieldValue.serverTimestamp()),
            )
        }
        val result = batch.commit().awaitOrQueued()
        runCatching { notifications.send(NotificationRules.messagePosted(ticket, actor, text, isInternal), actor.uid) }
        return result
    }

    override suspend fun rate(ticket: Ticket, actor: UserProfile, rating: Int, comment: String): WriteResult {
        val stars = rating.coerceIn(1, 5)
        val result = tickets.document(ticket.id).update(
            mapOf(
                "rating" to stars,
                "ratingComment" to comment.trim(),
                "ratedAt" to FieldValue.serverTimestamp(),
                "updatedAt" to FieldValue.serverTimestamp(),
            ),
        ).awaitOrQueued()
        runCatching { notifications.send(NotificationRules.rated(ticket, actor, stars), actor.uid) }
        return result
    }

    private fun historyEntry(from: TicketStatus, to: TicketStatus, actor: UserProfile, note: String) = mapOf(
        "from" to from.name,
        "to" to to.name,
        "changedBy" to actor.uid,
        "changedByName" to actor.name,
        "changedAt" to FieldValue.serverTimestamp(),
        "note" to note.trim(),
    )
}
