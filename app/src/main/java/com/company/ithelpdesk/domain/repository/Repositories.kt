// © 2026 Khondaker Arifur Rahman. All Rights Reserved.
// BGL-IT Help — see COPYRIGHT.md. Unauthorized copying, modification or redistribution is prohibited.

package com.company.ithelpdesk.domain.repository

import android.net.Uri
import com.company.ithelpdesk.domain.logic.DashboardSource
import com.company.ithelpdesk.domain.logic.NotificationDraft
import com.company.ithelpdesk.domain.model.AppNotification
import com.company.ithelpdesk.domain.model.Attachment
import com.company.ithelpdesk.domain.model.AuthUser
import com.company.ithelpdesk.domain.model.Category
import com.company.ithelpdesk.domain.model.Comment
import com.company.ithelpdesk.domain.model.Department
import com.company.ithelpdesk.domain.model.Resolution
import com.company.ithelpdesk.domain.model.Role
import com.company.ithelpdesk.domain.model.SlaSettings
import com.company.ithelpdesk.domain.model.StatusChange
import com.company.ithelpdesk.domain.model.Ticket
import com.company.ithelpdesk.domain.model.TicketStatus
import com.company.ithelpdesk.domain.model.UserProfile
import kotlinx.coroutines.flow.Flow

/** Writes complete immediately when online; offline they are queued and synced later. */
enum class WriteResult { SYNCED, QUEUED }

interface AuthRepository {
    fun observeAuthUser(): Flow<AuthUser?>
    val currentUser: AuthUser?
    suspend fun signIn(email: String, password: String)
    suspend fun register(email: String, password: String): AuthUser
    suspend fun sendPasswordReset(email: String)
    suspend fun updatePassword(newPassword: String)
    fun signOut()
}

interface UserRepository {
    fun observeProfile(uid: String): Flow<UserProfile?>
    fun observeAllUsers(): Flow<List<UserProfile>>
    fun observeAdmins(): Flow<List<UserProfile>>
    suspend fun createProfile(profile: UserProfile)
    suspend fun updateOwnProfile(uid: String, name: String, department: String, phone: String, location: String, employeeId: String)
    suspend fun approve(user: UserProfile, admin: UserProfile)
    suspend fun decline(user: UserProfile, admin: UserProfile)
    suspend fun setRole(uid: String, role: Role)
    suspend fun setActive(uid: String, active: Boolean)
    /** Compresses the picture to a small square and saves it as this user's profile photo. */
    suspend fun setPhoto(uid: String, uri: Uri)
    suspend fun removePhoto(uid: String)
    /** JPEG bytes of a profile photo. [version] 0 = known to have no photo, -1 = unknown (try anyway). */
    suspend fun loadPhoto(uid: String, version: Long): ByteArray?
    suspend fun addFcmToken(uid: String, token: String)
    suspend fun removeFcmToken(uid: String, token: String)
}

interface TicketRepository {
    fun newTicketId(): String
    fun observeMyTickets(uid: String): Flow<List<Ticket>>
    fun observeOpenTickets(): Flow<List<Ticket>>
    fun observeRecentClosed(limit: Long = 100): Flow<List<Ticket>>
    fun observeTicketsByAssignee(uid: String): Flow<List<Ticket>>
    fun observeTicket(id: String): Flow<Ticket?>
    fun observeComments(ticketId: String, includeInternal: Boolean): Flow<List<Comment>>
    fun observeHistory(ticketId: String): Flow<List<StatusChange>>
    fun observeDashboardSource(createdFrom: Long, resolvedFrom: Long): Flow<DashboardSource>

    /** Compresses the photo and stores it inside Firestore (no Cloud Storage needed). */
    suspend fun saveImage(ticketId: String, uri: Uri, owner: UserProfile): Attachment
    /** JPEG bytes of an inline photo, cached in memory. */
    suspend fun loadImage(ticketId: String, imageId: String): ByteArray?

    suspend fun createTicket(ticket: Ticket, sla: SlaSettings): WriteResult
    suspend fun changeStatus(
        ticket: Ticket,
        to: TicketStatus,
        actor: UserProfile,
        note: String = "",
        resolution: Resolution? = null,
    ): WriteResult
    suspend fun assign(ticket: Ticket, assignee: UserProfile, actor: UserProfile): WriteResult
    suspend fun addComment(ticket: Ticket, actor: UserProfile, text: String, images: List<Attachment>, internal: Boolean): WriteResult
    suspend fun rate(ticket: Ticket, actor: UserProfile, rating: Int, comment: String): WriteResult
}

interface NotificationRepository {
    /** Notifications for this person: their own, plus the shared admin feed for admins. */
    fun observeFor(profile: UserProfile): Flow<List<AppNotification>>
    suspend fun send(drafts: List<NotificationDraft>, actorUid: String)
    suspend fun markRead(ids: List<String>, uid: String)
}

interface SettingsRepository {
    fun observeSla(): Flow<SlaSettings>
    suspend fun saveSla(settings: SlaSettings)
    fun observeCategories(): Flow<List<Category>>
    suspend fun saveCategory(category: Category)
    suspend fun deleteCategory(id: String)
    suspend fun addDefaultCategories()
    fun observeDepartments(): Flow<List<Department>>
    suspend fun saveDepartment(name: String)
    suspend fun deleteDepartment(id: String)
}
