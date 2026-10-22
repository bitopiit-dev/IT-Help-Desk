// © 2026 Khondaker Arifur Rahman. All Rights Reserved.
// BGL-IT Help — see COPYRIGHT.md. Unauthorized copying, modification or redistribution is prohibited.

package com.company.ithelpdesk.ui.user

import android.content.Intent
import android.net.Uri
import androidx.compose.ui.platform.LocalContext
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Notifications
import androidx.compose.material3.FilterChip
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
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.company.ithelpdesk.ui.components.UserAvatar
import com.company.ithelpdesk.ui.components.Banner
import com.company.ithelpdesk.ui.components.CountBubble
import com.company.ithelpdesk.ui.components.EmptyState
import com.company.ithelpdesk.ui.components.ErrorBox
import com.company.ithelpdesk.ui.components.GradientButton
import com.company.ithelpdesk.ui.components.GradientHero
import com.company.ithelpdesk.ui.components.LoadingBox
import com.company.ithelpdesk.ui.components.SkeletonList
import com.company.ithelpdesk.ui.components.SectionTitle
import com.company.ithelpdesk.ui.components.StatTile
import com.company.ithelpdesk.ui.components.UserTicketCard
import com.company.ithelpdesk.ui.theme.LocalBrand
import com.company.ithelpdesk.util.rememberNow
import java.time.LocalTime

private fun greeting(): String = when (LocalTime.now().hour) {
    in 5..11 -> "Good morning"
    in 12..16 -> "Good afternoon"
    in 17..21 -> "Good evening"
    else -> "Up late"
}

@Composable
fun UserHomeScreen(
    onNewTicket: (String?) -> Unit,
    onOpenTicket: (String) -> Unit,
    onSeeAll: () -> Unit,
    onAlerts: () -> Unit,
    unread: Int,
    viewModel: MyTicketsViewModel = hiltViewModel(),
) {
    val ui by viewModel.ui.collectAsStateWithLifecycle()
    val categories by viewModel.categories.collectAsStateWithLifecycle()
    val now = rememberNow()
    val brand = LocalBrand.current
    val me = viewModel.me

    LazyColumn(Modifier.fillMaxSize(), contentPadding = PaddingValues(bottom = 24.dp)) {
        item {
            GradientHero {
                Column(Modifier.statusBarsPadding().padding(20.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        UserAvatar(me.uid, me.initials, size = 46.dp, photoVersion = me.photoVersion, ring = Color.White)
                        Spacer(Modifier.width(12.dp))
                        Column(Modifier.weight(1f)) {
                            Text("${greeting()},", color = Color.White.copy(alpha = 0.85f), style = MaterialTheme.typography.bodyMedium)
                            Text("${me.firstName} 👋", color = Color.White, style = MaterialTheme.typography.headlineSmall)
                        }
                        Box {
                            IconButton(onClick = onAlerts) {
                                Icon(Icons.Outlined.Notifications, contentDescription = "Alerts", tint = Color.White)
                            }
                            CountBubble(unread, Modifier.align(Alignment.TopEnd))
                        }
                    }
                    Spacer(Modifier.height(16.dp))
                    com.company.ithelpdesk.ui.components.LiveClock(onGradient = true)
                    Spacer(Modifier.height(18.dp))
                    Text("What's acting up today?", color = Color.White, style = MaterialTheme.typography.titleLarge)
                    Text("Tell IT in under a minute. We'll keep you posted.", color = Color.White.copy(alpha = 0.85f),
                        style = MaterialTheme.typography.bodyMedium)
                    Spacer(Modifier.height(16.dp))
                    val context = LocalContext.current
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Box(
                            Modifier
                                .weight(1f)
                                .clip(CircleShape)
                                .background(Color.White)
                                .clickable { onNewTicket(null) }
                                .padding(horizontal = 12.dp, vertical = 12.dp),
                            contentAlignment = Alignment.Center,
                        ) {
                            Text("＋ Raise a ticket", color = Color(0xFF7C3AED), style = MaterialTheme.typography.titleSmall, maxLines = 1)
                        }

                        Box(
                            Modifier
                                .weight(1f)
                                .clip(CircleShape)
                                .background(Color.White)
                                .clickable {
                                    val intent = Intent(Intent.ACTION_VIEW, Uri.parse("https://chat.whatsapp.com/FsHMyUp3jnJ31IUQ7Npcwm"))
                                    context.startActivity(intent)
                                }
                                .padding(horizontal = 12.dp, vertical = 12.dp),
                            contentAlignment = Alignment.Center,
                        ) {
                            Text("💬 WhatsApp Group", color = Color(0xFF16A34A), style = MaterialTheme.typography.titleSmall, maxLines = 1)
                        }
                    }
                    Spacer(Modifier.height(8.dp))
                }
            }
        }
        item {
            Row(Modifier.padding(horizontal = 16.dp, vertical = 16.dp), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                StatTile("Open", ui.open.size.toString(), "🔥", brand.cardTints[0], Modifier.weight(1f), onClick = onSeeAll)
                StatTile("Need you", ui.needsMe.size.toString(), "👀", brand.cardTints[2], Modifier.weight(1f), onClick = onSeeAll)
                StatTile("Fixed", ui.done.count { it.resolvedAt != null }.toString(), "✅", brand.cardTints[4], Modifier.weight(1f), onClick = onSeeAll)
            }
        }
        if (ui.needsMe.isNotEmpty()) {
            item {
                Banner(
                    text = "${ui.needsMe.size} ticket${if (ui.needsMe.size > 1) "s" else ""} waiting on you — confirm a fix or reply to IT.",
                    emoji = "⚡",
                    color = MaterialTheme.colorScheme.tertiary,
                    modifier = Modifier.padding(horizontal = 16.dp),
                    onClick = { onOpenTicket(ui.needsMe.first().id) },
                )
            }
        }
        item {
            SectionTitle("Quick start", Modifier.padding(horizontal = 16.dp))
            LazyRow(contentPadding = PaddingValues(horizontal = 16.dp), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                items(categories, key = { it.id.ifBlank { it.name } }) { c ->
                    val tint = brand.cardTints[Math.floorMod(c.name.hashCode(), brand.cardTints.size)]
                    Column(
                        Modifier
                            .width(96.dp)
                            .clip(MaterialTheme.shapes.large)
                            .background(Brush.verticalGradient(listOf(tint.copy(alpha = 0.25f), tint.copy(alpha = 0.08f))))
                            .border(1.dp, tint.copy(alpha = 0.3f), MaterialTheme.shapes.large)
                            .clickable { onNewTicket(c.id.ifBlank { c.name }) }
                            .padding(vertical = 14.dp, horizontal = 8.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                    ) {
                        Text(c.emoji.ifBlank { com.company.ithelpdesk.ui.components.categoryEmoji(c.name) }, fontSize = 26.sp)
                        Spacer(Modifier.height(6.dp))
                        Text(c.name, style = MaterialTheme.typography.labelMedium, textAlign = TextAlign.Center, maxLines = 2,
                            overflow = TextOverflow.Ellipsis)
                    }
                }
            }
        }
        item {
            SectionTitle("Recent tickets", Modifier.padding(horizontal = 16.dp, vertical = 8.dp)) {
                TextButton(onClick = onSeeAll) { Text("See all") }
            }
        }
        when {
            ui.loading -> item { LoadingBox(Modifier.height(160.dp)) }
            ui.error != null -> item { ErrorBox(ui.error!!, onRetry = null) }
            ui.tickets.isEmpty() -> item {
                EmptyState("🎉", "All clear!", "No tickets yet. When something breaks, tap the big + button.") {
                    GradientButton("Raise my first ticket", onClick = { onNewTicket(null) })
                }
            }
            else -> items(ui.tickets.take(5), key = { it.id }) { t ->
                UserTicketCard(t, now, onClick = { onOpenTicket(t.id) }, modifier = Modifier.padding(horizontal = 16.dp, vertical = 5.dp))
            }
        }
    }
}

private enum class TicketFilter(val label: String) { ACTIVE("Active"), NEEDS_ME("Needs me"), DONE("Done"), ALL("All") }

@Composable
fun UserTicketsScreen(onOpenTicket: (String) -> Unit, onNewTicket: () -> Unit, viewModel: MyTicketsViewModel = hiltViewModel()) {
    val ui by viewModel.ui.collectAsStateWithLifecycle()
    val now = rememberNow()
    var filter by rememberSaveable { mutableStateOf(TicketFilter.ACTIVE) }
    val list = when (filter) {
        TicketFilter.ACTIVE -> ui.open
        TicketFilter.NEEDS_ME -> ui.needsMe
        TicketFilter.DONE -> ui.done
        TicketFilter.ALL -> ui.tickets
    }
    Column(Modifier.fillMaxSize().statusBarsPadding()) {
        Text("My tickets", style = MaterialTheme.typography.headlineMedium, modifier = Modifier.padding(start = 20.dp, top = 16.dp))
        LazyRow(contentPadding = PaddingValues(horizontal = 16.dp, vertical = 10.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            items(TicketFilter.entries) { f ->
                val count = when (f) {
                    TicketFilter.ACTIVE -> ui.open.size
                    TicketFilter.NEEDS_ME -> ui.needsMe.size
                    TicketFilter.DONE -> ui.done.size
                    TicketFilter.ALL -> ui.tickets.size
                }
                FilterChip(selected = filter == f, onClick = { filter = f }, label = { Text("${f.label} · $count") },
                    shape = CircleShape)
            }
        }
        when {
            ui.loading -> SkeletonList()
            ui.error != null -> ErrorBox(ui.error!!, onRetry = null)
            list.isEmpty() -> EmptyState(
                if (filter == TicketFilter.DONE) "🗂️" else "🌈",
                if (filter == TicketFilter.DONE) "Nothing closed yet" else "Nothing here",
                if (filter == TicketFilter.ACTIVE) "No open tickets. Enjoy the smooth sailing!" else "Try another filter.",
            ) {
                if (filter == TicketFilter.ACTIVE) GradientButton("Raise a ticket", onClick = onNewTicket)
            }
            else -> LazyColumn(contentPadding = PaddingValues(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                items(list, key = { it.id }) { t -> UserTicketCard(t, now, onClick = { onOpenTicket(t.id) }, modifier = Modifier.animateItem()) }
            }
        }
    }
}
