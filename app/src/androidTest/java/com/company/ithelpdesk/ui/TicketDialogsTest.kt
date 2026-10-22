// © 2026 Khondaker Arifur Rahman. All Rights Reserved.
// BGL-IT Help — see COPYRIGHT.md. Unauthorized copying, modification or redistribution is prohibited.

package com.company.ithelpdesk.ui

import androidx.compose.ui.test.assertIsEnabled
import androidx.compose.ui.test.assertIsNotEnabled
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performTextInput
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.company.ithelpdesk.domain.model.Resolution
import com.company.ithelpdesk.ui.theme.HelpdeskTheme
import com.company.ithelpdesk.ui.ticket.NoteDialog
import com.company.ithelpdesk.ui.ticket.RESOLUTION_TYPES
import com.company.ithelpdesk.ui.ticket.RateDialog
import com.company.ithelpdesk.ui.ticket.ResolveDialog
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

/** UI tests for the status-change dialogs (reopen, resolve, rate). Run with ./gradlew connectedAndroidTest */
@RunWith(AndroidJUnit4::class)
class TicketDialogsTest {

    @get:Rule val compose = createComposeRule()

    @Test fun reopenRequiresAReason() {
        var confirmed: String? = null
        compose.setContent {
            HelpdeskTheme {
                NoteDialog("Reopen ticket", "What is still wrong? (required)", required = true, onDismiss = {}, onConfirm = { confirmed = it })
            }
        }
        compose.onNodeWithText("Confirm").assertIsNotEnabled()
        compose.onNodeWithText("What is still wrong? (required)").performTextInput("Still no Wi-Fi")
        compose.onNodeWithText("Confirm").assertIsEnabled().performClick()
        assertEquals("Still no Wi-Fi", confirmed)
    }

    @Test fun resolveNeedsRootCauseActionAndType() {
        var result: Resolution? = null
        compose.setContent { HelpdeskTheme { ResolveDialog(onDismiss = {}, onConfirm = { result = it }) } }
        compose.onNodeWithText("Resolve").assertIsNotEnabled()
        compose.onNodeWithText("Root cause").performTextInput("Driver crashed")
        compose.onNodeWithText("Action taken").performTextInput("Reinstalled driver")
        compose.onNodeWithContentDescription("Resolution type: not selected").performClick()
        compose.onNodeWithText(RESOLUTION_TYPES.first()).performClick()
        compose.onNodeWithText("Resolve").assertIsEnabled().performClick()
        assertEquals(Resolution("Driver crashed", "Reinstalled driver", RESOLUTION_TYPES.first(), ""), result)
    }

    @Test fun ratingNeedsAtLeastOneStar() {
        var stars = 0
        compose.setContent { HelpdeskTheme { RateDialog(onDismiss = {}, onConfirm = { s, _ -> stars = s }) } }
        compose.onNodeWithText("Submit").assertIsNotEnabled()
        compose.onNodeWithContentDescription("4 stars").performClick()
        compose.onNodeWithText("Submit").performClick()
        assertEquals(4, stars)
    }
}
