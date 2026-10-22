// © 2026 Khondaker Arifur Rahman. All Rights Reserved.
// BGL-IT Help — see COPYRIGHT.md. Unauthorized copying, modification or redistribution is prohibited.

package com.company.ithelpdesk.domain

import com.company.ithelpdesk.domain.logic.QuickReplies
import com.company.ithelpdesk.domain.logic.QuickReply
import com.company.ithelpdesk.domain.logic.SelfHelp
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class SelfHelpTest {
    @Test fun `nothing is suggested for an empty form`() {
        assertTrue(SelfHelp.suggest(null, null, "").isEmpty())
    }

    @Test fun `category alone gives tips from that category`() {
        val ids = SelfHelp.suggest("Printer", null, "").map { it.id }
        assertTrue(ids.isNotEmpty())
        assertTrue(ids.all { it == "printer" || it == "paper-jam" })
    }

    @Test fun `typed words rank the closest tip first`() {
        assertEquals("paper-jam", SelfHelp.suggest("Printer", null, "paper jam in tray 2").first().id)
        assertEquals("vpn", SelfHelp.suggest("Network", "VPN", "can't connect from home").first().id)
    }

    @Test fun `words match whole words only`() {
        // "email" must not trigger the "mail" category keyword, "slowly" must not trigger "slow".
        assertTrue(SelfHelp.suggest(null, null, "walked slowly").isEmpty())
    }

    @Test fun `a tip from another category needs a strong text match`() {
        val ids = SelfHelp.suggest("Printer", null, "screen").map { it.id }
        assertTrue("monitor" !in ids)
    }

    @Test fun `no category still matches clear text`() {
        assertEquals("password", SelfHelp.suggest(null, null, "my password is incorrect").first().id)
    }

    @Test fun `item requests get no quick fixes`() {
        assertTrue(SelfHelp.suggest("Item Request", "Printer Head", "need a new printer head").isEmpty())
    }

    @Test fun `item request is a default category with the requested items`() {
        val c = com.company.ithelpdesk.domain.model.Category.DEFAULTS.first { it.id == "item-request" }
        assertEquals("Item Request", c.name)
        listOf("Mouse", "Keyboard", "Monitor", "PC", "Laptop", "RAM", "SSD", "HDD", "Data Cable", "USB Hub",
            "Type-C Cable", "LAN Cable", "Android Charger", "Tab", "LAN Phone", "Scanner", "Printer Head", "Speaker",
            "PA System", "Camera", "Conferencing Camera", "Laptop Bag", "Wi-Fi Router",
        ).forEach { assertTrue("missing $it", it in c.subCategories) }
        assertEquals("Other", c.subCategories.last())
    }

    @Test fun `respects the limit`() {
        assertTrue(SelfHelp.suggest("Network", null, "wifi slow vpn", limit = 1).size == 1)
    }
}

class QuickRepliesTest {
    @Test fun `fills the first name`() {
        val r = QuickReply("x", "Hi {name}, done.")
        assertEquals("Hi Rahim, done.", QuickReplies.fill(r, "  Rahim Uddin "))
    }

    @Test fun `blank name drops the placeholder cleanly`() {
        val r = QuickReply("x", "Hi {name}, done.")
        assertEquals("Hi, done.", QuickReplies.fill(r, ""))
    }

    @Test fun `every default uses the placeholder once`() {
        QuickReplies.DEFAULTS.forEach { assertEquals(1, Regex("\\{name\\}").findAll(it.text).count()) }
    }
}
