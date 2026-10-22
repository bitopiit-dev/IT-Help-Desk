// © 2026 Khondaker Arifur Rahman. All Rights Reserved.
// BGL-IT Help — see COPYRIGHT.md. Unauthorized copying, modification or redistribution is prohibited.

package com.company.ithelpdesk.domain.model

enum class TicketStatus(val label: String) {
    NEW("New"),
    ASSIGNED("Assigned"),
    IN_PROGRESS("In Progress"),
    ON_HOLD_USER("Waiting for User"),
    ON_HOLD_PARTS("Waiting for Parts"),
    RESOLVED("Resolved"),
    CLOSED("Closed"),
    REOPENED("Reopened"),
    CANCELLED("Cancelled");

    val isOnHold: Boolean get() = this == ON_HOLD_USER || this == ON_HOLD_PARTS
    val isOpen: Boolean get() = this != RESOLVED && this != CLOSED && this != CANCELLED

    companion object {
        val OPEN: List<TicketStatus> = entries.filter { it.isOpen }
        fun from(value: String?): TicketStatus = entries.firstOrNull { it.name == value } ?: NEW
    }
}

enum class Priority(val label: String, val rank: Int) {
    LOW("Low", 0),
    MEDIUM("Medium", 1),
    HIGH("High", 2),
    CRITICAL("Critical", 3);

    companion object {
        fun from(value: String?): Priority = entries.firstOrNull { it.name == value } ?: MEDIUM
    }
}

enum class SlaState(val label: String) {
    ON_TRACK("On track"),
    AT_RISK("At risk"),
    BREACHED("Breached"),
    MET("Met");

    companion object {
        fun from(value: String?): SlaState = entries.firstOrNull { it.name == value } ?: ON_TRACK
    }
}

enum class ContactMethod(val label: String) {
    IN_APP("AnyDesk"),
    PHONE("Phone call"),
    EMAIL("Email"),
    IN_PERSON("Visit my desk");

    companion object {
        fun from(value: String?): ContactMethod = entries.firstOrNull { it.name == value } ?: IN_APP
    }
}

/**
 * A photo on a ticket or message.
 * New photos are stored inside Firestore (`tickets/{id}/images/{imageId}`, compressed JPEG as base64),
 * so no Cloud Storage bucket or paid plan is needed. [url] is only set for photos from older builds
 * that used Cloud Storage.
 */
data class Attachment(val id: String, val name: String = "", val url: String = "") {
    val isInline: Boolean get() = url.isBlank()
}

data class DeviceInfo(
    val manufacturer: String,
    val model: String,
    val androidVersion: String,
    val appVersion: String,
) {
    val summary: String get() = "$manufacturer $model · Android $androidVersion · app $appVersion"
}

data class Resolution(
    val rootCause: String,
    val actionTaken: String,
    val category: String,
    val partsUsed: String = "",
)

data class Ticket(
    val id: String,
    val ticketNumber: String = "",
    val title: String,
    val description: String,
    val category: String,
    val subCategory: String = "",
    val priority: Priority = Priority.MEDIUM,
    val status: TicketStatus = TicketStatus.NEW,
    val requesterId: String,
    val requesterName: String,
    val requesterPhone: String = "",
    val requesterEmail: String = "",
    val department: String = "",
    val location: String = "",
    val assigneeId: String? = null,
    val assigneeName: String? = null,
    val assigneePhone: String = "",
    val attachments: List<Attachment> = emptyList(),
    val deviceInfo: DeviceInfo? = null,
    val assetTag: String = "",
    val contactMethod: ContactMethod = ContactMethod.IN_APP,
    val createdAt: Long = 0L,
    val updatedAt: Long? = null,
    val firstResponseAt: Long? = null,
    val resolvedAt: Long? = null,
    val closedAt: Long? = null,
    val onHoldSince: Long? = null,
    val onHoldTotalMs: Long = 0L,
    val slaResponseDueAt: Long? = null,
    val slaResolveDueAt: Long? = null,
    val slaState: SlaState = SlaState.ON_TRACK,
    val rating: Int? = null,
    val ratingComment: String = "",
    val resolution: Resolution? = null,
    val reopenCount: Int = 0,
) {
    /** Ticket numbers come from a Firestore counter; offline-created tickets show a short id instead. */
    val displayNumber: String get() = ticketNumber.ifBlank { "#" + id.take(6).uppercase() }
}

data class Comment(
    val id: String,
    val authorId: String,
    val authorName: String,
    val authorRole: Role,
    val text: String,
    val images: List<Attachment> = emptyList(),
    val isInternal: Boolean = false,
    val createdAt: Long = 0L,
)

data class StatusChange(
    val id: String,
    val from: TicketStatus?,
    val to: TicketStatus,
    val changedBy: String,
    val changedByName: String,
    val changedAt: Long,
    val note: String = "",
)
