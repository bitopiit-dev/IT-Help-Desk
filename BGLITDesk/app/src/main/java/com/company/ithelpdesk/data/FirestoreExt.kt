package com.company.ithelpdesk.data

import com.company.ithelpdesk.domain.repository.WriteResult
import com.google.android.gms.tasks.Task
import com.google.firebase.Timestamp
import com.google.firebase.firestore.DocumentReference
import com.google.firebase.firestore.DocumentSnapshot
import com.google.firebase.firestore.Query
import com.google.firebase.firestore.QuerySnapshot
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withTimeoutOrNull
import java.util.Date

fun Query.asFlow(): Flow<QuerySnapshot> = callbackFlow {
    val registration = addSnapshotListener { snapshot, error ->
        if (error != null) {
            close(error)
            return@addSnapshotListener
        }
        if (snapshot != null) trySend(snapshot)
    }
    awaitClose { registration.remove() }
}

fun DocumentReference.asFlow(): Flow<DocumentSnapshot> = callbackFlow {
    val registration = addSnapshotListener { snapshot, error ->
        if (error != null) {
            close(error)
            return@addSnapshotListener
        }
        if (snapshot != null) trySend(snapshot)
    }
    awaitClose { registration.remove() }
}

/**
 * Firestore applies writes locally at once and syncs when online. A commit Task only completes
 * after the server acknowledges it, so when offline we stop waiting and report the write as queued.
 */
suspend fun Task<Void>.awaitOrQueued(timeoutMs: Long = 6_000): WriteResult {
    val done = withTimeoutOrNull(timeoutMs) { await() ; true }
    return if (done == true) WriteResult.SYNCED else WriteResult.QUEUED
}

fun DocumentSnapshot.millis(field: String): Long? =
    getTimestamp(field, DocumentSnapshot.ServerTimestampBehavior.ESTIMATE)?.toDate()?.time

fun Long.toTimestamp(): Timestamp = Timestamp(Date(this))
