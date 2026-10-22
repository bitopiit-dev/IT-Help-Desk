// © 2026 Khondaker Arifur Rahman. All Rights Reserved.
// BGL-IT Help — see COPYRIGHT.md. Unauthorized copying, modification or redistribution is prohibited.

package com.company.ithelpdesk.data.notify

import android.Manifest
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.media.AudioAttributes
import android.net.Uri
import android.provider.Settings
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat
import com.company.ithelpdesk.MainActivity
import com.company.ithelpdesk.R
import com.company.ithelpdesk.domain.model.AppNotification

/**
 * Turns [AppNotification]s into phone notifications, at most once each.
 * Three sources feed it and it de-duplicates between them:
 *  1. a live Firestore listener while the app is running ([NotificationWatcher]),
 *  2. a background check every ~15 minutes ([NotificationCheckWorker]), which works on the free plan,
 *  3. optional instant push from Cloud Functions if the project is on the Blaze plan ([PushService]).
 */
object SystemNotifier {
    /**
     * New channel id: Android never lets an app change sound settings of an existing channel,
     * so a fresh id guarantees the BGL-IT Help chime is ON by default on every phone.
     */
    const val CHANNEL_ID = "bglitdesk_alerts_v2"
    private val OLD_CHANNELS = listOf("tickets", "bglitdesk_updates")
    private const val PREFS = "bglitdesk_notifier"
    private const val KEY_SHOWN = "shown_ids"
    private const val KEY_SINCE = "since_ms"
    private const val MAX_REMEMBERED = 300

    fun soundUri(context: Context): Uri = Uri.parse("android.resource://${context.packageName}/${R.raw.bglit_chime}")

    fun createChannel(context: Context) {
        val manager = context.getSystemService(NotificationManager::class.java)
        OLD_CHANNELS.forEach { runCatching { manager.deleteNotificationChannel(it) } }
        val audio = AudioAttributes.Builder()
            .setUsage(AudioAttributes.USAGE_NOTIFICATION)
            .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
            .build()
        val channel = NotificationChannel(
            CHANNEL_ID,
            context.getString(R.string.notification_channel_tickets),
            NotificationManager.IMPORTANCE_HIGH,
        ).apply {
            description = context.getString(R.string.notification_channel_tickets_desc)
            setSound(soundUri(context), audio)
            enableVibration(true)
            vibrationPattern = longArrayOf(0, 180, 120, 180)
            enableLights(true)
            lightColor = 0xFF7C3AED.toInt()
            lockscreenVisibility = android.app.Notification.VISIBILITY_PUBLIC
            setShowBadge(true)
        }
        manager.createNotificationChannel(channel)
    }

    /** False when the person has turned notifications (or this channel) off in phone settings. */
    fun isEnabled(context: Context): Boolean {
        if (!NotificationManagerCompat.from(context).areNotificationsEnabled()) return false
        val ch = context.getSystemService(NotificationManager::class.java).getNotificationChannel(CHANNEL_ID)
        return ch == null || ch.importance != NotificationManager.IMPORTANCE_NONE
    }

    fun openSettings(context: Context) {
        val intent = Intent(Settings.ACTION_APP_NOTIFICATION_SETTINGS)
            .putExtra(Settings.EXTRA_APP_PACKAGE, context.packageName)
            .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        runCatching { context.startActivity(intent) }
    }

    private fun prefs(context: Context) = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)

    /** Only notifications newer than this are shown, so a fresh install does not replay old history. */
    @Synchronized
    fun since(context: Context): Long {
        val p = prefs(context)
        val saved = p.getLong(KEY_SINCE, 0L)
        if (saved > 0) return saved
        val now = System.currentTimeMillis()
        p.edit().putLong(KEY_SINCE, now).apply()
        return now
    }

    /** Shows every notification in [items] that is new, unread and not caused by [myUid]. */
    @Synchronized
    fun showNew(context: Context, items: List<AppNotification>, myUid: String) {
        val p = prefs(context)
        val since = since(context)
        val shown = p.getString(KEY_SHOWN, "").orEmpty().split(',').filter { it.isNotBlank() }.toMutableList()
        var changed = false
        items.sortedBy { it.createdAt }
            .filter { it.createdAt >= since && !it.isReadBy(myUid) && it.id !in shown }
            .forEach { n ->
                post(context, n.id, n.title, n.body, n.ticketId)
                shown += n.id
                changed = true
            }
        if (changed) {
            p.edit().putString(KEY_SHOWN, shown.takeLast(MAX_REMEMBERED).joinToString(",")).apply()
        }
    }

    /** Push messages carry the notification id, so they share the same de-duplication. */
    @Synchronized
    fun showPush(context: Context, id: String?, title: String, body: String, ticketId: String?) {
        val p = prefs(context)
        val shown = p.getString(KEY_SHOWN, "").orEmpty().split(',').filter { it.isNotBlank() }.toMutableList()
        if (id != null && id in shown) return
        post(context, id ?: title, title, body, ticketId)
        if (id != null) {
            shown += id
            p.edit().putString(KEY_SHOWN, shown.takeLast(MAX_REMEMBERED).joinToString(",")).apply()
        }
    }

    fun reset(context: Context) {
        prefs(context).edit().clear().apply()
    }

    private fun post(context: Context, key: String, title: String, body: String, ticketId: String?) {
        if (Build.VERSION.SDK_INT >= 33 &&
            ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS) !=
            PackageManager.PERMISSION_GRANTED
        ) {
            return
        }
        val intent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_SINGLE_TOP
            ticketId?.let { putExtra(MainActivity.EXTRA_TICKET_ID, it) }
        }
        val pending = PendingIntent.getActivity(
            context,
            key.hashCode(),
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
        )
        val notification = NotificationCompat.Builder(context, CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_notification)
            .setColor(0xFF7C3AED.toInt())
            .setContentTitle(title)
            .setContentText(body)
            .setStyle(NotificationCompat.BigTextStyle().bigText(body))
            .setAutoCancel(true)
            .setContentIntent(pending)
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setCategory(NotificationCompat.CATEGORY_MESSAGE)
            .setSound(soundUri(context))
            .setVibrate(longArrayOf(0, 180, 120, 180))
            .build()
        runCatching { NotificationManagerCompat.from(context).notify(key.hashCode(), notification) }
    }
}
