// © 2026 Khondaker Arifur Rahman. All Rights Reserved.
// BGL-IT Help — see COPYRIGHT.md. Unauthorized copying, modification or redistribution is prohibited.

package com.company.ithelpdesk.domain.logic

/**
 * A short fix people can try on their own before raising a ticket.
 * [categories] are matched against the category name; [keywords] against what the person types.
 */
data class QuickFix(
    val id: String,
    val title: String,
    val steps: List<String>,
    val categories: Set<String>,
    val keywords: Set<String>,
)

/**
 * Suggests quick fixes while someone fills in a new ticket. Works offline and costs nothing:
 * the list lives in the app, so the free Firebase plan is not affected.
 */
object SelfHelp {

    val ALL: List<QuickFix> = listOf(
        QuickFix(
            "wifi", "Wi-Fi not connecting or keeps dropping",
            listOf(
                "Turn Wi-Fi off and on again, and check that airplane mode is off.",
                "Forget the office network, then join it again and re-enter the password.",
                "Ask a colleague nearby: if their Wi-Fi is also down, raise a ticket so IT can check the access point.",
            ),
            setOf("network"), setOf("wifi", "wi-fi", "wireless", "disconnect", "dropping", "no internet", "network"),
        ),
        QuickFix(
            "slow-internet", "Internet is slow",
            listOf(
                "Close video calls, downloads and cloud sync you are not using.",
                "Restart your browser; if one site is slow and others are fine, the problem is that site.",
                "Plug in a LAN cable if your desk has one and compare.",
            ),
            setOf("network"), setOf("slow internet", "internet slow", "slow", "buffering", "speed", "lag"),
        ),
        QuickFix(
            "vpn", "VPN will not connect",
            listOf(
                "Check that normal internet works first by opening any website.",
                "Quit the VPN app completely and open it again.",
                "Make sure your computer's date and time are correct; a wrong clock blocks the VPN.",
            ),
            setOf("network"), setOf("vpn", "remote", "forticlient", "anyconnect"),
        ),
        QuickFix(
            "printer", "Printer not printing",
            listOf(
                "Check that the printer is on, has paper and shows no error light.",
                "Open the print queue on your computer, cancel stuck jobs and print again.",
                "Make sure you picked the right printer, not \"Microsoft Print to PDF\".",
            ),
            setOf("printer", "print"), setOf("print", "printer", "printing", "queue", "offline"),
        ),
        QuickFix(
            "paper-jam", "Paper jam",
            listOf(
                "Turn the printer off before opening any cover.",
                "Pull jammed paper out slowly in the direction it travels; torn bits cause the next jam.",
                "Fan the paper stack before loading it back into the tray.",
            ),
            setOf("printer", "print"), setOf("jam", "jammed", "stuck paper", "paper"),
        ),
        QuickFix(
            "outlook", "Email not sending or receiving",
            listOf(
                "Check that you are online and that Outlook does not say \"Working offline\" at the bottom.",
                "Look in the Outbox: large attachments can block every message behind them.",
                "Close Outlook fully and open it again.",
            ),
            setOf("email", "mail"), setOf("outlook", "email", "mail", "send", "receive", "outbox", "inbox"),
        ),
        QuickFix(
            "mailbox-full", "Mailbox full",
            listOf(
                "Empty Deleted Items and Junk Email.",
                "Sort by size and delete or save old messages with big attachments.",
            ),
            setOf("email", "mail"), setOf("mailbox full", "quota", "storage full", "mailbox"),
        ),
        QuickFix(
            "password", "Password not accepted",
            listOf(
                "Check Caps Lock, and that the keyboard is set to English, not Bangla.",
                "Type the password in a notes app first to see what is being typed, then copy it in.",
                "If the account says it is locked, raise a ticket: IT can unlock it.",
            ),
            setOf("access", "password"), setOf("password", "login", "log in", "sign in", "locked", "incorrect"),
        ),
        QuickFix(
            "app-crash", "App crashes or shows an error",
            listOf(
                "Save your work, close the app and open it again.",
                "Restart the computer; pending updates often finish on restart.",
                "Take a photo of the error message: it helps IT a lot if you still need to raise a ticket.",
            ),
            setOf("software", "erp"), setOf("crash", "crashes", "error", "not responding", "freeze", "frozen", "hang"),
        ),
        QuickFix(
            "monitor", "Monitor shows nothing",
            listOf(
                "Check the power light on the monitor and both ends of the display cable.",
                "Use the monitor's Input or Source button to pick HDMI or DisplayPort.",
                "On a laptop, press Windows + P and choose Duplicate or Extend.",
            ),
            setOf("hardware"), setOf("monitor", "screen", "display", "no signal", "black screen"),
        ),
        QuickFix(
            "keyboard-mouse", "Keyboard or mouse not working",
            listOf(
                "Unplug the USB cable or receiver and plug it into a different port.",
                "For wireless ones, replace the batteries and switch the device off and on.",
            ),
            setOf("hardware"), setOf("keyboard", "mouse", "typing", "cursor", "click"),
        ),
        QuickFix(
            "slow-pc", "Computer is slow",
            listOf(
                "Restart it, especially if it has not been restarted this week.",
                "Close browser tabs you are not using; each one uses memory.",
                "Check free disk space; below about 10% free, everything slows down.",
            ),
            setOf("hardware", "software"), setOf("slow", "hanging", "takes long", "performance", "laptop slow", "pc slow"),
        ),
    )

    /**
     * Best matches for the form as typed. A typed keyword counts more than the category,
     * so "printer jam" beats a generic printer tip. Returns nothing until there is a signal.
     */
    fun suggest(category: String?, subCategory: String?, text: String, limit: Int = 2): List<QuickFix> {
        val cat = category.orEmpty().lowercase()
        // Asking for new equipment is not a fault, so there is nothing to try first.
        if (cat.contains("request")) return emptyList()
        val typed = (subCategory.orEmpty() + " " + text).lowercase()
        if (cat.isBlank() && typed.isBlank()) return emptyList()
        return ALL.asSequence()
            .map { fix -> fix to score(fix, cat, typed) }
            .filter { it.second > 0 }
            .sortedByDescending { it.second }
            .take(limit)
            .map { it.first }
            .toList()
    }

    private fun score(fix: QuickFix, category: String, typed: String): Int {
        val catHit = category.isNotBlank() && fix.categories.any { category.contains(it) }
        val wordHits = fix.keywords.count { containsWord(typed, it) }
        if (!catHit && wordHits == 0) return 0
        // A tip from another category only shows when the text clearly points at it.
        if (category.isNotBlank() && !catHit && wordHits < 2) return 0
        return (if (catHit) 2 else 0) + wordHits * 3
    }

    /** Whole-word (or whole-phrase) match, so "mail" does not match "email" and "slow" does not match "slowly". */
    private fun containsWord(text: String, word: String): Boolean {
        var from = 0
        while (true) {
            val i = text.indexOf(word, from)
            if (i < 0) return false
            val before = if (i == 0) ' ' else text[i - 1]
            val afterIdx = i + word.length
            val after = if (afterIdx >= text.length) ' ' else text[afterIdx]
            if (!before.isLetterOrDigit() && !after.isLetterOrDigit()) return true
            from = i + 1
        }
    }
}
