package com.company.ithelpdesk.data

import com.company.ithelpdesk.domain.logic.NotificationRules
import com.company.ithelpdesk.domain.model.Role
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
) : UserRepository {

    private val users get() = db.collection("users")

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

    override suspend fun addFcmToken(uid: String, token: String) {
        users.document(uid).update("fcmTokens", FieldValue.arrayUnion(token)).await()
    }

    override suspend fun removeFcmToken(uid: String, token: String) {
        users.document(uid).update("fcmTokens", FieldValue.arrayRemove(token)).await()
    }
}
