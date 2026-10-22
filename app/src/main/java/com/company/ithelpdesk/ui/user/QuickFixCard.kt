// © 2026 Khondaker Arifur Rahman. All Rights Reserved.
// BGL-IT Help — see COPYRIGHT.md. Unauthorized copying, modification or redistribution is prohibited.

package com.company.ithelpdesk.ui.user

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateContentSize
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.ExpandLess
import androidx.compose.material.icons.filled.ExpandMore
import androidx.compose.material.icons.outlined.Lightbulb
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.stateDescription
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.company.ithelpdesk.domain.logic.QuickFix

/**
 * "Try this first" box on the new-ticket form. Appears once the category or text points at a
 * common problem; the person can expand a fix, close the form if it worked, or hide the box.
 */
@Composable
fun QuickFixCard(
    fixes: List<QuickFix>,
    onFixed: () -> Unit,
    modifier: Modifier = Modifier,
) {
    var hidden by rememberSaveable { mutableStateOf(false) }
    AnimatedVisibility(
        visible = fixes.isNotEmpty() && !hidden,
        enter = fadeIn() + expandVertically(),
        exit = fadeOut() + shrinkVertically(),
        modifier = modifier,
    ) {
        val scheme = MaterialTheme.colorScheme
        Column(
            Modifier
                .fillMaxWidth()
                .clip(MaterialTheme.shapes.large)
                .background(scheme.tertiaryContainer.copy(alpha = 0.55f))
                .border(1.dp, scheme.tertiary.copy(alpha = 0.35f), MaterialTheme.shapes.large)
                .animateContentSize()
                .padding(start = 14.dp, end = 4.dp, top = 6.dp, bottom = 10.dp),
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Outlined.Lightbulb, contentDescription = null, tint = scheme.tertiary, modifier = Modifier.size(20.dp))
                Spacer(Modifier.width(8.dp))
                Column(Modifier.weight(1f)) {
                    Text("Try this first", style = MaterialTheme.typography.titleSmall, color = scheme.onTertiaryContainer)
                    Text(
                        "Most people fix this in a minute. If not, carry on below.",
                        style = MaterialTheme.typography.bodySmall,
                        color = scheme.onTertiaryContainer.copy(alpha = 0.8f),
                    )
                }
                IconButton(onClick = { hidden = true }) { Icon(Icons.Default.Close, contentDescription = "Hide tips") }
            }
            fixes.forEach { fix -> FixItem(fix, onFixed) }
        }
    }
}

@Composable
private fun FixItem(fix: QuickFix, onFixed: () -> Unit) {
    var open by rememberSaveable(fix.id) { mutableStateOf(false) }
    val haptics = LocalHapticFeedback.current
    val scheme = MaterialTheme.colorScheme
    Column(Modifier.padding(end = 10.dp, top = 6.dp)) {
        Row(
            Modifier
                .fillMaxWidth()
                .clip(MaterialTheme.shapes.medium)
                .background(scheme.surface.copy(alpha = 0.7f))
                .clickable(role = Role.Button, onClickLabel = if (open) "Hide steps" else "Show steps") { open = !open }
                .semantics { stateDescription = if (open) "Expanded" else "Collapsed" }
                .padding(horizontal = 12.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(fix.title, style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.SemiBold, modifier = Modifier.weight(1f))
            Icon(if (open) Icons.Default.ExpandLess else Icons.Default.ExpandMore, contentDescription = null)
        }
        AnimatedVisibility(open, enter = fadeIn() + expandVertically(), exit = fadeOut() + shrinkVertically()) {
            Column(Modifier.padding(start = 12.dp, top = 8.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                // Steps are an ordered sequence, so they are numbered.
                fix.steps.forEachIndexed { i, step ->
                    Row {
                        Box(
                            Modifier.size(20.dp).clip(CircleShape).background(scheme.tertiary),
                            contentAlignment = Alignment.Center,
                        ) { Text("${i + 1}", style = MaterialTheme.typography.labelSmall, color = scheme.onTertiary) }
                        Spacer(Modifier.width(8.dp))
                        Text(step, style = MaterialTheme.typography.bodySmall, modifier = Modifier.weight(1f))
                    }
                }
                TextButton(onClick = {
                    haptics.performHapticFeedback(HapticFeedbackType.LongPress)
                    onFixed()
                }) { Text("That fixed it, close the form") }
            }
        }
    }
}
