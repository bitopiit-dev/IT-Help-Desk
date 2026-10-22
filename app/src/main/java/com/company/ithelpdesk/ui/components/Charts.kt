// © 2026 Khondaker Arifur Rahman. All Rights Reserved.
// BGL-IT Help — see COPYRIGHT.md. Unauthorized copying, modification or redistribution is prohibited.

package com.company.ithelpdesk.ui.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
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
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.company.ithelpdesk.domain.logic.TrendPoint

// Lightweight Compose charts. Every chart also exposes its numbers as text for TalkBack.

data class ChartSlice(val label: String, val value: Int, val color: Color)

@Composable
fun DonutChart(slices: List<ChartSlice>, modifier: Modifier = Modifier) {
    val total = slices.sumOf { it.value }.coerceAtLeast(1)
    val track = MaterialTheme.colorScheme.surfaceVariant
    val description = slices.joinToString { "${it.label} ${it.value}" }
    Row(modifier.fillMaxWidth().semantics { contentDescription = "Tickets by status: $description" },
        verticalAlignment = Alignment.CenterVertically) {
        Box(Modifier.size(140.dp), contentAlignment = Alignment.Center) {
            Canvas(Modifier.size(130.dp)) {
                val stroke = 22.dp.toPx()
                val arcSize = Size(size.width - stroke, size.height - stroke)
                val topLeft = Offset(stroke / 2, stroke / 2)
                drawArc(track, 0f, 360f, false, topLeft, arcSize, style = Stroke(stroke))
                var start = -90f
                slices.filter { it.value > 0 }.forEach { s ->
                    val sweep = 360f * s.value / total
                    drawArc(s.color, start, (sweep - 1.5f).coerceAtLeast(0.5f), false, topLeft, arcSize, style = Stroke(stroke))
                    start += sweep
                }
            }
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text(slices.sumOf { it.value }.toString(), style = MaterialTheme.typography.headlineSmall)
                Text("tickets", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }
        Spacer(Modifier.width(16.dp))
        Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
            slices.filter { it.value > 0 }.forEach { s ->
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(Modifier.size(10.dp).background(s.color, CircleShape))
                    Spacer(Modifier.width(6.dp))
                    Text("${s.label}  ${s.value}", style = MaterialTheme.typography.bodySmall)
                }
            }
        }
    }
}

/** Horizontal bars with the value written at the end of each bar. */
@Composable
fun BarList(
    items: List<Pair<String, Float>>,
    valueLabel: (Float) -> String,
    modifier: Modifier = Modifier,
    color: Color = MaterialTheme.colorScheme.primary,
    maxItems: Int = 8,
) {
    val shown = items.take(maxItems)
    val max = shown.maxOfOrNull { it.second }?.takeIf { it > 0f } ?: 1f
    Column(modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(8.dp)) {
        shown.forEach { (label, value) ->
            Column(Modifier.semantics(mergeDescendants = true) {}) {
                Row {
                    Text(label, style = MaterialTheme.typography.bodySmall, modifier = Modifier.weight(1f),
                        maxLines = 1, overflow = TextOverflow.Ellipsis)
                    Text(valueLabel(value), style = MaterialTheme.typography.labelMedium)
                }
                Box(Modifier.fillMaxWidth().height(10.dp).background(MaterialTheme.colorScheme.surfaceVariant, RoundedCornerShape(5.dp))) {
                    Box(Modifier.fillMaxWidth(value / max).fillMaxHeight().background(color, RoundedCornerShape(5.dp)))
                }
            }
        }
    }
}

@Composable
fun TrendChart(points: List<TrendPoint>, modifier: Modifier = Modifier) {
    val createdColor = MaterialTheme.colorScheme.primary
    val resolvedColor = MaterialTheme.colorScheme.tertiary
    val grid = MaterialTheme.colorScheme.outlineVariant
    val maxY = (points.maxOfOrNull { maxOf(it.created, it.resolved) } ?: 0).coerceAtLeast(1)
    val summary = "Created ${points.sumOf { it.created }}, resolved ${points.sumOf { it.resolved }} over ${points.size} days"
    Column(modifier.fillMaxWidth().semantics { contentDescription = summary }) {
        Row(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
            LegendDot("Created", createdColor)
            LegendDot("Resolved", resolvedColor)
            Spacer(Modifier.weight(1f))
            Text("max $maxY/day", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
        Spacer(Modifier.height(8.dp))
        Canvas(Modifier.fillMaxWidth().height(160.dp)) {
            val h = size.height
            val w = size.width
            for (i in 0..4) {
                val y = h * i / 4f
                drawLine(grid, Offset(0f, y), Offset(w, y), strokeWidth = 1f)
            }
            if (points.isEmpty()) return@Canvas
            val stepX = if (points.size == 1) 0f else w / (points.size - 1)
            fun series(values: List<Int>, color: Color) {
                val path = Path()
                values.forEachIndexed { i, v ->
                    val x = if (points.size == 1) w / 2 else i * stepX
                    val y = h - (v.toFloat() / maxY) * h
                    if (i == 0) path.moveTo(x, y) else path.lineTo(x, y)
                    if (points.size <= 31) drawCircle(color, 3.dp.toPx(), Offset(x, y))
                }
                drawPath(path, color, style = Stroke(width = 2.5.dp.toPx()))
            }
            series(points.map { it.created }, createdColor)
            series(points.map { it.resolved }, resolvedColor)
        }
        Row(Modifier.fillMaxWidth().padding(top = 4.dp)) {
            Text(points.firstOrNull()?.label ?: "", style = MaterialTheme.typography.labelSmall, modifier = Modifier.weight(1f))
            Text(points.lastOrNull()?.label ?: "", style = MaterialTheme.typography.labelSmall)
        }
    }
}

@Composable
private fun LegendDot(label: String, color: Color) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Box(Modifier.size(10.dp).background(color, CircleShape))
        Spacer(Modifier.width(6.dp))
        Text(label, style = MaterialTheme.typography.labelMedium)
    }
}
