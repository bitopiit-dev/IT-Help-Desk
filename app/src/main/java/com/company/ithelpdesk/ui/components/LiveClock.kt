// © 2026 Khondaker Arifur Rahman. All Rights Reserved.
// BGL-IT Help — see COPYRIGHT.md. Unauthorized copying, modification or redistribution is prohibited.

package com.company.ithelpdesk.ui.components

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.company.ithelpdesk.ui.theme.LocalBrand
import com.company.ithelpdesk.ui.theme.Sora
import kotlinx.coroutines.delay
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter
import java.time.format.TextStyle as JTextStyle
import java.util.Locale

/** Current local time, ticking exactly on each second. */
@Composable
private fun rememberTicker(): LocalDateTime {
    var now by remember { mutableStateOf(LocalDateTime.now()) }
    LaunchedEffect(Unit) {
        while (true) {
            now = LocalDateTime.now()
            delay(1000L - (System.currentTimeMillis() % 1000L))
        }
    }
    return now
}

/** One digit that rolls up when it changes. Fixed width so the clock never jiggles. */
@Composable
private fun RollingDigit(ch: Char, style: TextStyle, width: Dp) {
    AnimatedContent(
        targetState = ch,
        transitionSpec = {
            (slideInVertically { it } + fadeIn()) togetherWith (slideOutVertically { -it } + fadeOut())
        },
        label = "digit",
        modifier = Modifier.width(width),
        contentAlignment = Alignment.Center,
    ) { c -> Text(c.toString(), style = style) }
}

/**
 * Modern digital clock: rolling HH:MM, small seconds, AM/PM, day name and full date.
 * [onGradient] = true for use on a coloured hero (glass look); false for a card on the page.
 */
@Composable
fun LiveClock(modifier: Modifier = Modifier, onGradient: Boolean = true) {
    val now = rememberTicker()
    val brand = LocalBrand.current
    val fg = if (onGradient) Color.White else MaterialTheme.colorScheme.onSurface
    val sub = if (onGradient) Color.White.copy(alpha = 0.8f) else MaterialTheme.colorScheme.onSurfaceVariant
    val time = now.format(DateTimeFormatter.ofPattern("hh:mm", Locale.US))
    val seconds = now.format(DateTimeFormatter.ofPattern("ss", Locale.US))
    val ampm = now.format(DateTimeFormatter.ofPattern("a", Locale.US))
    val day = now.dayOfWeek.getDisplayName(JTextStyle.FULL, Locale.US)
    val date = now.format(DateTimeFormatter.ofPattern("dd MMMM yyyy", Locale.US))
    val shape = RoundedCornerShape(26.dp)

    val blink = rememberInfiniteTransition(label = "blink")
    val colonAlpha by blink.animateFloat(1f, 0.25f, infiniteRepeatable(tween(500), RepeatMode.Reverse), label = "colon")

    val surface = if (onGradient) {
        Modifier.background(Color.White.copy(alpha = 0.16f), shape).border(1.dp, Color.White.copy(alpha = 0.28f), shape)
    } else {
        Modifier.background(MaterialTheme.colorScheme.surfaceContainerLow, shape)
            .border(1.5.dp, Brush.linearGradient(brand.accent), shape)
    }
    val big = TextStyle(fontFamily = Sora, fontWeight = FontWeight.ExtraBold, fontSize = 44.sp, color = fg)
    val small = TextStyle(fontFamily = Sora, fontWeight = FontWeight.Bold, fontSize = 18.sp, color = fg)

    Row(
        modifier
            .fillMaxWidth()
            .clip(shape)
            .then(surface)
            .padding(horizontal = 18.dp, vertical = 14.dp)
            .clearAndSetSemantics { contentDescription = "$day, $date, ${now.format(DateTimeFormatter.ofPattern("h:mm a", Locale.US))}" },
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Row(verticalAlignment = Alignment.Bottom) {
            time.forEach { c ->
                if (c == ':') {
                    Text(":", style = big, modifier = Modifier.alpha(colonAlpha).padding(horizontal = 1.dp))
                } else {
                    RollingDigit(c, big, 28.dp)
                }
            }
            Column(Modifier.padding(start = 6.dp, bottom = 8.dp)) {
                Text(ampm, style = small.copy(fontSize = 13.sp, color = sub))
                Row {
                    seconds.forEach { RollingDigit(it, small, 11.dp) }
                }
            }
        }
        Spacer(Modifier.weight(1f))
        Column(horizontalAlignment = Alignment.End) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    Modifier.size(8.dp).clip(CircleShape)
                        .background(if (onGradient) Color(0xFFA3E635) else MaterialTheme.colorScheme.secondary),
                )
                Spacer(Modifier.width(6.dp))
                Text(day, style = TextStyle(fontFamily = Sora, fontWeight = FontWeight.Bold, fontSize = 18.sp, color = fg))
            }
            Spacer(Modifier.height(2.dp))
            Text(date, style = MaterialTheme.typography.bodyMedium.copy(color = sub))
        }
    }
}
