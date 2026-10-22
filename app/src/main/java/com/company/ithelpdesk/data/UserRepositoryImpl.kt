// © 2026 Khondaker Arifur Rahman. All Rights Reserved.
// BGL-IT Help — see COPYRIGHT.md. Unauthorized copying, modification or redistribution is prohibited.

package com.company.ithelpdesk.data

import android.content.Context
import android.net.Uri
import android.util.Base64
import android.util.LruCache
import com.company.ithelpdesk.domain.logic.NotificationRules
import com.company.ithelpdesk.util.ImageCompressor
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import com.company.ithelpdesk.domain.model.Role
import com.company.ithelpdesk.domain.model.UserPriority
import com.company.ithelpdesk.domain.model.UserProfile
import com.company.ithelpdesk.domain.repository.NotificationRepository
import com.company.ithelpdesk.domain.repository.UserRepository
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.tasks.await
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class UserRepositoryImpl @Inject constructor(
    private val db: FirebaseFirestore,
    private val notifications: NotificationRepository,
    @ApplicationContext private val context: Context,
) : UserRepository {

    private val users get() = db.collection("users")
    private val avatars get() = db.collection("avatars")

    private val photoCache = object : LruCache<String, ByteArray>(4 * 1024 * 1024) {
        override fun sizeOf(key: String, value: ByteArray) = value.size
    }
    private val noPhoto = java.util.Collections.synchronizedSet(mutableSetOf<String>())

    override suspend fun setPhoto(uid: String, uri: Uri) {
        val bytes = withContext(Dispatchers.IO) { ImageCompressor.avatar(context, uri) }
        val encoded = withContext(Dispatchers.Default) { Base64.encodeToString(bytes, Base64.NO_WRAP) }
        avatars.document(uid).set(mapOf("data" to encoded, "updatedAt" to FieldValue.serverTimestamp())).await()
        users.document(uid).update("photoUpdatedAt", FieldValue.serverTimestamp()).await()
        noPhoto.removeAll { it.startsWith("$uid@") }
        photoCache.evictAll()
    }

    override suspend fun removePhoto(uid: String) {
        avatars.document(uid).delete().await()
        users.document(uid).update("photoUpdatedAt", null).await()
        noPhoto.removeAll { it.startsWith("$uid@") }
        photoCache.evictAll()
    }

    override suspend fun loadPhoto(uid: String, version: Long): ByteArray? {
        if (version == 0L || uid.isBlank()) return null
        val key = "$uid@$version"
        photoCache.get(key)?.let { return it }
        if (key in noPhoto) return null
        val data = runCatching { avatars.document(uid).get().await().getString("data") }.getOrNull()
        if (data == null) {
            noPhoto.add(key)
            return null
        }
        val bytes = withContext(Dispatchers.Default) { Base64.decode(data, Base64.DEFAULT) }
        photoCache.put(key, bytes)
        return bytes
    }

    override fun observeProfile(uid: String): Flow<UserProfile?> =
        users.document(uid).asFlow().map { it.toUserProfile() }

    override fun observeAllUsers(): Flow<List<UserProfile>> =
        users.asFlow().map { snap -> snap.documents.mapNotNull { it.toUserProfile() }.sortedBy { it.name.lowercase() } }

    override fun observeAdmins(): Flow<List<UserProfile>> =
        users.whereEqualTo("role", Role.ADMIN.name).asFlow()
            .map { snap ->
                snap.documents.mapNotNull { it.toUserProfile() }
                    .filter { it.isActive && it.isApproved }
                    .sortedBy { it.name.lowercase() }
            }

    override suspend fun createProfile(profile: UserProfile) {
        users.document(profile.uid).set(profile.toCreateMap()).await()
        // Tell every admin there is someone waiting. Best effort: the request is saved either way.
        runCatching { notifications.send(listOf(NotificationRules.signupRequested(profile)), profile.uid) }
    }

    override suspend fun updateOwnProfile(
        uid: String,
        name: String,
        department: String,
        phone: String,
        location: String,
        employeeId: String,
    ) {
        users.document(uid).update(
            mapOf(
                "name" to name.trim(),
                "department" to department,
                "phone" to phone.trim(),
                "location" to location.trim(),
                "employeeId" to employeeId.trim(),
            ),
        ).awaitOrQueued()
    }

    override suspend fun updateUserProfile(
        uid: String,
        name: String,
        employeeId: String,
        department: String,
        phone: String,
        location: String,
        extraFields: Map<String, String>,
    ) {
        users.document(uid).update(
            mapOf(
                "name" to name.trim(),
                "employeeId" to employeeId.trim(),
                "department" to department.trim(),
                "phone" to phone.trim(),
                "location" to location.trim(),
                "extraFields" to extraFields,
            ),
        ).await()
    }

    override suspend fun approve(user: UserProfile, admin: UserProfile) {
        users.document(user.uid).update(
            mapOf(
                "isApproved" to true,
                "isActive" to true,
                "approvedAt" to FieldValue.serverTimestamp(),
                "approvedBy" to admin.uid,
                "approvedByName" to admin.name,
            ),
        ).await()
        runCatching { notifications.send(listOf(NotificationRules.accountApproved(user.uid)), admin.uid) }
    }

    override suspend fun decline(user: UserProfile, admin: UserProfile) {
        users.document(user.uid).update(
            mapOf(
                "isApproved" to false,
                "isActive" to false,
                "approvedBy" to admin.uid,
                "approvedByName" to admin.name,
            ),
        ).await()
    }

    override suspend fun setRole(uid: String, role: Role) {
        users.document(uid).update("role", role.name).await()
    }

    override suspend fun setActive(uid: String, active: Boolean) {
        users.document(uid).update("isActive", active).await()
    }

    override suspend fun setUserPriority(uid: String, userPriority: UserPriority) {
        users.document(uid).update("userPriority", userPriority.name).await()
    }

    override suspend fun addFcmToken(uid: String, token: String) {
        users.document(uid).update("fcmTokens", FieldValue.arrayUnion(token)).await()
    }

    override suspend fun removeFcmToken(uid: String, token: String) {
        users.document(uid).update("fcmTokens", FieldValue.arrayRemove(token)).await()
    }
}
