package com.company.ithelpdesk.data

import com.company.ithelpdesk.domain.model.Attachment
import com.company.ithelpdesk.domain.model.AppNotification
import com.company.ithelpdesk.domain.model.Category
import com.company.ithelpdesk.domain.model.Comment
import com.company.ithelpdesk.domain.model.ContactMethod
import com.company.ithelpdesk.domain.model.Department
import com.company.ithelpdesk.domain.model.DeviceInfo
import com.company.ithelpdesk.domain.model.Priority
import com.company.ithelpdesk.domain.model.Resolution
import com.company.ithelpdesk.domain.model.Role
import com.company.ithelpdesk.domain.model.SlaSettings
import com.company.ithelpdesk.domain.model.SlaState
import com.company.ithelpdesk.domain.model.SlaTarget
import com.company.ithelpdesk.domain.model.StatusChange
import com.company.ithelpdesk.domain.model.Ticket
import com.company.ithelpdesk.domain.model.TicketStatus
import com.company.ithelpdesk.domain.model.UserProfile
import com.google.firebase.firestore.DocumentSnapshot
import com.google.firebase.firestore.FieldValue

// Manual mapping keeps the Firestore schema explicit and avoids reflection (safe with R8).

private fun Any?.asMap(): Map<*, *>? = this as? Map<*, *>
private fun Map<*, *>.str(key: String): String = this[key] as? String ?: ""

fun Attachment.toMap(): Map<String, Any> = mapOf("id" to id, "name" to name, "url" to url)

/** Reads both new inline photos ({id, name}) and Cloud Storage photos from older builds ({url, path, name}). */
private fun Any?.toAttachments(): List<Attachment> =
    (this as? List<*>)?.mapNotNull { item ->
        item.asMap()?.let {
            val id = it.str("id").ifBlank { it.str("path") }
            if (id.isBlank() && it.str("url").isBlank()) null else Attachment(id = id, name = it.str("name"), url = it.str("url"))
        }
    } ?: emptyList()

fun DocumentSnapshot.toUserProfile(): UserProfile? {
    if (!exists()) return null
    return UserProfile(
        uid = id,
        name = getString("name") ?: "",
        email = getString("email") ?: "",
        employeeId = getString("employeeId") ?: "",
        department = getString("department") ?: "",
        phone = getString("phone") ?: "",
        location = getString("location") ?: "",
        role = Role.from(getString("role")),
        isApproved = getBoolean("isApproved") ?: false,
        isActive = getBoolean("isActive") ?: true,
        createdAt = millis("createdAt") ?: 0L,
        approvedAt = millis("approvedAt"),
        approvedByName = getString("approvedByName") ?: "",
    )
}

fun UserProfile.toCreateMap(): Map<String, Any?> = mapOf(
    "name" to name,
    "email" to email,
    "employeeId" to employeeId,
    "department" to department,
    "phone" to phone,
    "location" to location,
    "role" to Role.USER.name,
    "isApproved" to false,
    "isActive" to true,
    "fcmTokens" to emptyList<String>(),
    "createdAt" to FieldValue.serverTimestamp(),
)

fun DocumentSnapshot.toTicket(): Ticket? {
    if (!exists()) return null
    val device = get("deviceInfo").asMap()?.let {
        DeviceInfo(it.str("manufacturer"), it.str("model"), it.str("androidVersion"), it.str("appVersion"))
    }
    val resolution = get("resolution").asMap()?.let {
        Resolution(it.str("rootCause"), it.str("actionTaken"), it.str("category"), it.str("partsUsed"))
    }
    return Ticket(
        id = id,
        ticketNumber = getString("ticketNumber") ?: "",
        title = getString("title") ?: "",
        description = getString("description") ?: "",
        category = getString("category") ?: "",
        subCategory = getString("subCategory") ?: "",
        priority = Priority.from(getString("priority")),
        status = TicketStatus.from(getString("status")),
        requesterId = getString("requesterId") ?: "",
        requesterName = getString("requesterName") ?: "",
        requesterPhone = getString("requesterPhone") ?: "",
        requesterEmail = getString("requesterEmail") ?: "",
        department = getString("department") ?: "",
        location = getString("location") ?: "",
        assigneeId = getString("assigneeId"),
        assigneeName = getString("assigneeName"),
        attachments = get("attachments").toAttachments(),
        deviceInfo = device,
        assetTag = getString("assetTag") ?: "",
        contactMethod = ContactMethod.from(getString("contactMethod")),
        createdAt = millis("createdAt") ?: 0L,
        updatedAt = millis("updatedAt"),
        firstResponseAt = millis("firstResponseAt"),
        resolvedAt = millis("resolvedAt"),
        closedAt = millis("closedAt"),
        onHoldSince = millis("onHoldSince"),
        onHoldTotalMs = getLong("onHoldTotalMs") ?: 0L,
        slaResponseDueAt = millis("slaResponseDueAt"),
        slaResolveDueAt = millis("slaResolveDueAt"),
        slaState = SlaState.from(getString("slaState")),
        rating = getLong("rating")?.toInt(),
        ratingComment = getString("ratingComment") ?: "",
        resolution = resolution,
        reopenCount = getLong("reopenCount")?.toInt() ?: 0,
    )
}

fun Ticket.toCreateMap(): Map<String, Any?> = mapOf(
    "ticketNumber" to ticketNumber,
    "title" to title,
    "description" to description,
    "category" to category,
    "subCategory" to subCategory,
    "priority" to priority.name,
    "status" to TicketStatus.NEW.name,
    "requesterId" to requesterId,
    "requesterName" to requesterName,
    "requesterPhone" to requesterPhone,
    "requesterEmail" to requesterEmail,
    "department" to department,
    "location" to location,
    "assigneeId" to null,
    "assigneeName" to null,
    "attachments" to attachments.map { it.toMap() },
    "deviceInfo" to deviceInfo?.let {
        mapOf(
            "manufacturer" to it.manufacturer,
            "model" to it.model,
            "androidVersion" to it.androidVersion,
            "appVersion" to it.appVersion,
        )
    },
    "assetTag" to assetTag,
    "contactMethod" to contactMethod.name,
    "createdAt" to FieldValue.serverTimestamp(),
    "updatedAt" to FieldValue.serverTimestamp(),
    "slaResponseDueAt" to slaResponseDueAt?.toTimestamp(),
    "slaResolveDueAt" to slaResolveDueAt?.toTimestamp(),
    "onHoldTotalMs" to 0L,
    "reopenCount" to 0,
    "slaState" to SlaState.ON_TRACK.name,
)

fun Resolution.toMap(): Map<String, Any> = mapOf(
    "rootCause" to rootCause,
    "actionTaken" to actionTaken,
    "category" to category,
    "partsUsed" to partsUsed,
)

fun DocumentSnapshot.toComment(): Comment = Comment(
    id = id,
    authorId = getString("authorId") ?: "",
    authorName = getString("authorName") ?: "",
    authorRole = Role.from(getString("authorRole")),
    text = getString("text") ?: "",
    images = get("images").toAttachments(),
    isInternal = getBoolean("isInternal") ?: false,
    createdAt = millis("createdAt") ?: 0L,
)

fun DocumentSnapshot.toStatusChange(): StatusChange = StatusChange(
    id = id,
    from = getString("from")?.let { TicketStatus.from(it) },
    to = TicketStatus.from(getString("to")),
    changedBy = getString("changedBy") ?: "",
    changedByName = getString("changedByName") ?: "",
    changedAt = millis("changedAt") ?: 0L,
    note = getString("note") ?: "",
)

fun DocumentSnapshot.toSlaSettings(): SlaSettings {
    if (!exists()) return SlaSettings.DEFAULT
    val targets = Priority.entries.associateWith { p ->
        val m = get(p.name).asMap()
        val d = SlaSettings.DEFAULT.target(p)
        SlaTarget(
            respondMinutes = (m?.get("respondMinutes") as? Number)?.toInt() ?: d.respondMinutes,
            resolveMinutes = (m?.get("resolveMinutes") as? Number)?.toInt() ?: d.resolveMinutes,
        )
    }
    return SlaSettings(targets)
}

fun SlaSettings.toMap(): Map<String, Any> = Priority.entries.associate { p ->
    val t = target(p)
    p.name to mapOf("respondMinutes" to t.respondMinutes, "resolveMinutes" to t.resolveMinutes)
}

fun DocumentSnapshot.toCategory(): Category = Category(
    id = id,
    name = getString("name") ?: "",
    subCategories = (get("subCategories") as? List<*>)?.filterIsInstance<String>() ?: emptyList(),
    emoji = getString("emoji") ?: "",
)

fun Category.toMap(): Map<String, Any?> = mapOf(
    "name" to name,
    "subCategories" to subCategories,
    "emoji" to emoji,
)

fun DocumentSnapshot.toDepartment(): Department = Department(id = id, name = getString("name") ?: "")

fun DocumentSnapshot.toNotification(): AppNotification = AppNotification(
    id = id,
    audience = getString("audience") ?: "",
    title = getString("title") ?: "",
    body = getString("body") ?: "",
    ticketId = getString("ticketId"),
    kind = getString("kind") ?: "",
    createdBy = getString("createdBy") ?: "",
    createdAt = millis("createdAt") ?: 0L,
    readBy = (get("readBy") as? List<*>)?.filterIsInstance<String>() ?: emptyList(),
)
