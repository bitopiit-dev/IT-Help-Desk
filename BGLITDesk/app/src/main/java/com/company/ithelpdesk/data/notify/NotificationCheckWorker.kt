package com.company.ithelpdesk.data.notify

import android.content.Context
import androidx.work.Constraints
import androidx.work.CoroutineWorker
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.NetworkType
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.WorkerParameters
import com.company.ithelpdesk.data.toNotification
import com.company.ithelpdesk.domain.model.AppNotification
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query
import kotlinx.coroutines.tasks.await
import java.util.concurrent.TimeUnit

/**
 * Background check for new notifications roughly every 15 minutes (Android's minimum),
 * so people hear about updates even when the app is closed, without Cloud Functions or a paid plan.
 */
class NotificationCheckWorker(context: Context, params: WorkerParameters) : CoroutineWorker(context, params) {

    override suspend fun doWork(): Result {
        val user = FirebaseAuth.getInstance().currentUser ?: return Result.success()
        return try {
            val db = FirebaseFirestore.getInstance()
            val me = db.collection("users").document(user.uid).get().await()
            if (me.getBoolean("isApproved") != true || me.getBoolean("isActive") == false) return Result.success()
            val isAdmin = me.getString("role") == "ADMIN"
            val audiences = if (isAdmin) listOf(user.uid, AppNotification.AUDIENCE_ADMINS) else listOf(user.uid)
            val snap = db.collection("notifications")
                .whereIn("audience", audiences)
                .orderBy("createdAt", Query.Direction.DESCENDING)
                .limit(30)
                .get()
                .await()
            SystemNotifier.showNew(applicationContext, snap.documents.map { it.toNotification() }, user.uid)
            Result.success()
        } catch (e: Exception) {
            Result.retry()
        }
    }

    companion object {
        private const val NAME = "bglitdesk-notification-check"

        fun schedule(context: Context) {
            val request = PeriodicWorkRequestBuilder<NotificationCheckWorker>(15, TimeUnit.MINUTES)
                .setConstraints(Constraints.Builder().setRequiredNetworkType(NetworkType.CONNECTED).build())
                .build()
            WorkManager.getInstance(context)
                .enqueueUniquePeriodicWork(NAME, ExistingPeriodicWorkPolicy.KEEP, request)
        }
    }
}
