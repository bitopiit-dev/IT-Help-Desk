// © 2026 Khondaker Arifur Rahman. All Rights Reserved.
// BGL-IT Help — see COPYRIGHT.md. Unauthorized copying, modification or redistribution is prohibited.

package com.company.ithelpdesk.domain.logic

/** A ready-made admin reply. [label] is the chip text; {name} in [text] becomes the requester's first name. */
data class QuickReply(val label: String, val text: String)

object QuickReplies {

    val DEFAULTS: List<QuickReply> = listOf(
        QuickReply("On it", "Hi {name}, thanks for reporting this. I'm looking into it now."),
        QuickReply("Need details", "Hi {name}, could you send a photo of the error and tell me when it started?"),
        QuickReply("Restart first", "Hi {name}, please restart the device once and let me know if the problem is still there."),
        QuickReply("Coming to desk", "Hi {name}, I'll come to your desk shortly. Please keep the device on."),
        QuickReply("Waiting for parts", "Hi {name}, we've ordered the part needed for this. I'll update you as soon as it arrives."),
        QuickReply("Please confirm", "Hi {name}, this should be fixed now. Please check and tap Confirm, or reply if it's still not working."),
    )

    /** Fills the template for a requester; falls back to a greeting without a name. */
    fun fill(reply: QuickReply, requesterName: String): String {
        val first = requesterName.trim().split(Regex("\\s+")).firstOrNull().orEmpty()
        return if (first.isBlank()) reply.text.replace("Hi {name}, ", "Hi, ").replace("{name}", "")
        else reply.text.replace("{name}", first)
    }
}
