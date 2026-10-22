// © 2026 Khondaker Arifur Rahman. All Rights Reserved.
// BGL-IT Help — see COPYRIGHT.md. Unauthorized copying, modification or redistribution is prohibited.

package com.company.ithelpdesk.domain.model

data class SlaTarget(val respondMinutes: Int, val resolveMinutes: Int)

data class SlaSettings(val targets: Map<Priority, SlaTarget>) {
    fun target(priority: Priority): SlaTarget = targets[priority] ?: DEFAULT.targets.getValue(priority)

    companion object {
        val DEFAULT = SlaSettings(
            mapOf(
                Priority.CRITICAL to SlaTarget(respondMinutes = 15, resolveMinutes = 4 * 60),
                Priority.HIGH to SlaTarget(respondMinutes = 60, resolveMinutes = 8 * 60),
                Priority.MEDIUM to SlaTarget(respondMinutes = 4 * 60, resolveMinutes = 24 * 60),
                Priority.LOW to SlaTarget(respondMinutes = 8 * 60, resolveMinutes = 72 * 60),
            ),
        )
    }
}

data class Category(
    val id: String,
    val name: String,
    val subCategories: List<String> = emptyList(),
    val emoji: String = "",
) {
    companion object {
        const val ITEM_REQUEST_ID = "item-request"

        /** What people can ask IT for under "Item Request" (shown in "Narrow it down"). */
        val ITEM_REQUEST_ITEMS: List<String> = listOf(
            "Mouse", "Keyboard", "Monitor", "PC", "Laptop", "RAM", "SSD", "HDD",
            "Data Cable", "USB Hub", "Type-C Cable", "LAN Cable", "Android Charger", "Tab", "LAN Phone",
            "Scanner", "Printer Head", "Speaker", "PA System", "Camera", "Conferencing Camera",
            "Laptop Bag", "Wi-Fi Router", "Headphone / Headset", "Pen Drive", "Toner / Cartridge",
            "UPS", "Multi-plug", "Other",
        )

        /** Used when the admin has not configured categories yet. */
        val DEFAULTS: List<Category> = listOf(
            Category("hardware", "Hardware", listOf("Laptop / Desktop", "Monitor", "Keyboard / Mouse", "Phone", "Other"), "💻"),
            Category("software", "Software", listOf("Installation", "Crash / Error", "License", "Update", "Other"), "🧩"),
            Category("network", "Network", listOf("Wi-Fi", "LAN / Cable", "VPN", "Internet slow", "Other"), "📶"),
            Category("email", "Email", listOf("Cannot send / receive", "Outlook", "Spam / Phishing", "Mailbox full", "Other"), "✉️"),
            Category("printer", "Printer", listOf("Not printing", "Paper jam", "Toner / Ink", "Scanner", "Other"), "🖨️"),
            Category("access", "Access / Password", listOf("Password reset", "Account locked", "New access", "Shared drive", "Other"), "🔐"),
            Category("erp", "ERP", listOf("Login", "Report", "Data correction", "Other"), "📊"),
            Category(ITEM_REQUEST_ID, "Item Request", ITEM_REQUEST_ITEMS, "📦"),
            Category("other", "Other", emptyList(), "✨"),
        )
    }
}

data class Department(val id: String, val name: String)
