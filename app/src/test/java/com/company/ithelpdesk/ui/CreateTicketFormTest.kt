// © 2026 Khondaker Arifur Rahman. All Rights Reserved.
// BGL-IT Help — see COPYRIGHT.md. Unauthorized copying, modification or redistribution is prohibited.

package com.company.ithelpdesk.ui

import com.company.ithelpdesk.domain.model.Category
import com.company.ithelpdesk.ui.user.CreateTicketForm
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test

class CreateTicketFormTest {
    @Test fun `empty form is invalid with messages for each field`() {
        val f = CreateTicketForm()
        assertFalse(f.isValid)
        assertNotNull(f.titleError)
        assertNotNull(f.descriptionError)
        assertNotNull(f.categoryError)
    }

    @Test fun `filled form is valid`() {
        val f = CreateTicketForm(
            title = "Outlook keeps crashing",
            description = "Crashes every time I open the calendar",
            category = Category.DEFAULTS.first { it.name == "Email" },
        )
        assertTrue(f.isValid)
    }

    @Test fun `too short description is rejected`() {
        val f = CreateTicketForm(title = "x", description = "broken", category = Category.DEFAULTS.first())
        assertFalse(f.isValid)
    }
}
