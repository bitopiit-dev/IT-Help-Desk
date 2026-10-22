// © 2026 Khondaker Arifur Rahman. All Rights Reserved.
// BGL-IT Help — see COPYRIGHT.md. Unauthorized copying, modification or redistribution is prohibited.

package com.company.ithelpdesk.util

import android.content.ContentValues
import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.LinearGradient
import android.graphics.Paint
import android.graphics.RectF
import android.graphics.Shader
import android.graphics.Typeface
import android.os.Build
import android.provider.MediaStore
import androidx.core.content.FileProvider
import androidx.core.content.res.ResourcesCompat
import com.company.ithelpdesk.R
import com.company.ithelpdesk.domain.logic.DashboardData
import com.company.ithelpdesk.domain.logic.TrendPoint
import com.company.ithelpdesk.domain.model.Priority
import com.company.ithelpdesk.domain.model.TicketStatus
import java.io.File
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter
import java.util.Locale

/**
 * The report as a set of charts in one JPG: status donut, created-vs-resolved bars, category donut,
 * priority bars, department bars, repair time by priority and team bars.
 * Drawn with plain Android Canvas (no extra libraries).
 */
object ReportImage {

    private const val W = 1080
    private const val PAD = 48f
    private const val GAP = 28f
    private const val CARD_R = 36f
    private const val HEADER_H = 300f
    private const val TITLE_H = 104f
    private const val CHART_H = 470f

    private val BG = Color.parseColor("#F3F5FB")
    private val CARD = Color.WHITE
    private val INK = Color.parseColor("#141A2E")
    private val MUTED = Color.parseColor("#6B7390")
    private val LINE = Color.parseColor("#E6E9F2")
    private val SHADOW = Color.parseColor("#14000000")
    private val HERO = intArrayOf(Color.parseColor("#0EA5E9"), Color.parseColor("#6366F1"), Color.parseColor("#A855F7"))

    private val PALETTE = intArrayOf(
        Color.parseColor("#8B5CF6"), Color.parseColor("#EC4899"), Color.parseColor("#F97316"),
        Color.parseColor("#06B6D4"), Color.parseColor("#22C55E"), Color.parseColor("#F59E0B"),
        Color.parseColor("#6366F1"), Color.parseColor("#EF4444"),
    )
    private val CREATED = Color.parseColor("#8B5CF6")
    private val RESOLVED = Color.parseColor("#22C55E")

    private fun statusColor(s: TicketStatus, ratePct: Float = 100f): Int = when (s) {
        TicketStatus.RESOLVED, TicketStatus.CLOSED -> when {
            ratePct >= 80f -> Color.parseColor("#22C55E") // Green (80%-100%)
            ratePct >= 60f -> Color.parseColor("#84CC16") // Lime (60%-79%)
            else -> Color.parseColor("#EF4444")           // Red (<50%)
        }
        TicketStatus.NEW -> Color.parseColor("#06B6D4")
        TicketStatus.ASSIGNED -> Color.parseColor("#8B5CF6")
        TicketStatus.IN_PROGRESS -> Color.parseColor("#6366F1")
        TicketStatus.ON_HOLD_USER -> Color.parseColor("#F59E0B")
        TicketStatus.ON_HOLD_PARTS -> Color.parseColor("#F97316")
        TicketStatus.REOPENED -> Color.parseColor("#EC4899")
        TicketStatus.CANCELLED -> Color.parseColor("#94A3B8")
    }

    private fun priorityColor(p: Priority): Int = when (p) {
        Priority.LOW -> Color.parseColor("#94A3B8")
        Priority.MEDIUM -> Color.parseColor("#06B6D4")
        Priority.HIGH -> Color.parseColor("#F97316")
        Priority.CRITICAL -> Color.parseColor("#EF4444")
    }

    private class Fonts(context: Context) {
        val regular: Typeface = ResourcesCompat.getFont(context, R.font.sora_regular) ?: Typeface.DEFAULT
        val semi: Typeface = ResourcesCompat.getFont(context, R.font.sora_semibold) ?: Typeface.DEFAULT_BOLD
        val bold: Typeface = ResourcesCompat.getFont(context, R.font.sora_bold) ?: Typeface.DEFAULT_BOLD
        val extra: Typeface = ResourcesCompat.getFont(context, R.font.sora_extrabold) ?: Typeface.DEFAULT_BOLD
    }

    private class Block(val height: Float, val draw: (Canvas, Float) -> Float)

    private class Item(val label: String, val value: Float, val color: Int, val valueText: String)

    private fun text(tf: Typeface, size: Float, color: Int) = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        typeface = tf
        textSize = size
        this.color = color
    }

    private fun fill(color: Int) = Paint(Paint.ANTI_ALIAS_FLAG).apply { this.color = color; style = Paint.Style.FILL }

    private fun fit(s: String, p: Paint, max: Float): String {
        if (p.measureText(s) <= max) return s
        var end = s.length
        while (end > 0 && p.measureText(s.substring(0, end) + "…") > max) end--
        return s.substring(0, end) + "…"
    }

    private fun hoursLabel(ms: Long): String = String.format(Locale.US, "%.1fh", ms / 3_600_000.0)

    // ---------- Public API ----------

    fun render(context: Context, data: DashboardData, rangeLabel: String): Bitmap {
        val f = Fonts(context)
        val trend = data.trend.takeLast(14)
        val totalTickets = data.byStatus.values.sum()
        val resolvedAndClosed = (data.byStatus[TicketStatus.RESOLVED] ?: 0) + (data.byStatus[TicketStatus.CLOSED] ?: 0)
        val ratePct = if (totalTickets > 0) (resolvedAndClosed * 100f / totalTickets) else 100f

        val statusItems = TicketStatus.entries.mapNotNull { s ->
            val n = data.byStatus[s] ?: 0
            if (n > 0) Item(s.label, n.toFloat(), statusColor(s, ratePct), n.toString()) else null
        }
        val categoryItems = data.byCategory.take(7).mapIndexed { i, (k, v) -> Item(k, v.toFloat(), PALETTE[i % PALETTE.size], v.toString()) }
        val priorityCounts = data.ticketsInPeriod.groupingBy { it.priority }.eachCount()
        val priorityItems = Priority.entries.reversed().map { p ->
            val n = priorityCounts[p] ?: 0
            Item(p.label, n.toFloat(), priorityColor(p), n.toString())
        }
        val deptItems = data.byDepartment.take(8).mapIndexed { i, (k, v) -> Item(k, v.toFloat(), PALETTE[(i + 3) % PALETTE.size], v.toString()) }
        val repairItems = data.avgRepairByPriority.map { (p, ms) -> Item(p.label, ms / 3_600_000f, priorityColor(p), hoursLabel(ms)) }
        val teamItems = data.workload.take(8).map { w ->
            Item(w.name, w.resolvedInPeriod.toFloat(), PALETTE[0],
                "${w.resolvedInPeriod}" + (w.slaCompliancePct?.let { "  ·  SLA $it%" } ?: ""))
        }

        fun hBarH(n: Int) = TITLE_H + n * 78f + 30f
        val blocks = mutableListOf<Block>()
        blocks += Block(CHART_H) { c, y -> donut(c, f, y, "Tickets by status", statusItems, "tickets") }
        blocks += Block(CHART_H) { c, y -> trendBars(c, f, y, trend) }
        if (categoryItems.isNotEmpty()) blocks += Block(CHART_H) { c, y -> donut(c, f, y, "Tickets by category", categoryItems, "tickets") }
        blocks += Block(CHART_H) { c, y -> columns(c, f, y, "Tickets by priority", priorityItems, null) }
        if (deptItems.isNotEmpty()) blocks += Block(hBarH(deptItems.size)) { c, y -> hBars(c, f, y, "Tickets by department", deptItems, null) }
        if (repairItems.isNotEmpty()) blocks += Block(CHART_H) { c, y -> columns(c, f, y, "Average repair time by priority", repairItems, "hours") }
        if (teamItems.isNotEmpty()) blocks += Block(hBarH(teamItems.size)) { c, y -> hBars(c, f, y, "Resolved by admin", teamItems, "in period") }

        val height = HEADER_H + GAP + blocks.sumOf { (it.height + GAP).toDouble() }.toFloat() + 120f
        val bmp = Bitmap.createBitmap(W, height.toInt(), Bitmap.Config.ARGB_8888)
        val c = Canvas(bmp)
        c.drawColor(BG)
        var y = header(c, f, data, rangeLabel) + GAP
        blocks.forEach { b -> y = b.draw(c, y) + GAP }
        val p = text(f.semi, 22f, MUTED)
        val s = "Generated by BGL-IT Help"
        c.drawText(s, W / 2f - p.measureText(s) / 2, y + 40, p)
        val cp = text(f.regular, 20f, MUTED)
        val cs = com.company.ithelpdesk.Copyright.SHORT
        c.drawText(cs, W / 2f - cp.measureText(cs) / 2, y + 72, cp)
        return bmp
    }

    fun saveToCache(context: Context, bmp: Bitmap): File {
        val dir = File(context.cacheDir, "exports").apply { mkdirs() }
        val stamp = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMdd_HHmm"))
        val file = File(dir, "BGL-IT Help_report_$stamp.jpg")
        file.outputStream().use { bmp.compress(Bitmap.CompressFormat.JPEG, 94, it) }
        return file
    }

    fun shareIntent(context: Context, file: File, rangeLabel: String): Intent {
        val uri = FileProvider.getUriForFile(context, "${context.packageName}.fileprovider", file)
        val send = Intent(Intent.ACTION_SEND).apply {
            type = "image/jpeg"
            putExtra(Intent.EXTRA_STREAM, uri)
            putExtra(Intent.EXTRA_SUBJECT, "BGL-IT Help report – $rangeLabel")
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }
        return Intent.createChooser(send, "Share report")
    }

    /** Saves into Pictures/BGL-IT Help (Android 10+, no permission needed). Returns false on older phones. */
    fun saveToGallery(context: Context, bmp: Bitmap): Boolean {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.Q) return false
        val stamp = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMdd_HHmmss"))
        val values = ContentValues().apply {
            put(MediaStore.Images.Media.DISPLAY_NAME, "BGL-IT Help_report_$stamp.jpg")
            put(MediaStore.Images.Media.MIME_TYPE, "image/jpeg")
            put(MediaStore.Images.Media.RELATIVE_PATH, "Pictures/BGL-IT Help")
        }
        val uri = context.contentResolver.insert(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, values) ?: return false
        context.contentResolver.openOutputStream(uri)?.use { bmp.compress(Bitmap.CompressFormat.JPEG, 94, it) } ?: return false
        return true
    }

    // ---------- Pieces ----------

    private fun card(c: Canvas, f: Fonts, top: Float, h: Float, title: String, subtitle: String?): RectF {
        val r = RectF(PAD, top, W - PAD, top + h)
        c.drawRoundRect(RectF(r.left, r.top + 4, r.right, r.bottom + 4), CARD_R, CARD_R, fill(SHADOW))
        c.drawRoundRect(r, CARD_R, CARD_R, fill(CARD))
        c.drawText(title, r.left + 36, r.top + 64, text(f.bold, 34f, INK))
        if (subtitle != null) {
            val p = text(f.regular, 24f, MUTED)
            c.drawText(subtitle, r.right - 36 - p.measureText(subtitle), r.top + 62, p)
        }
        return r
    }

    private fun header(c: Canvas, f: Fonts, d: DashboardData, rangeLabel: String): Float {
        val p = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            shader = LinearGradient(0f, 0f, W.toFloat(), HEADER_H, HERO, null, Shader.TileMode.CLAMP)
        }
        c.drawRoundRect(RectF(0f, -60f, W.toFloat(), HEADER_H), 60f, 60f, p)
        c.drawCircle(W - 120f, 60f, 160f, fill(Color.argb(28, 255, 255, 255)))
        c.drawText("BGL-IT Help  •  IT Service Report", PAD, 92f, text(f.semi, 28f, Color.argb(230, 255, 255, 255)))
        c.drawText("Report in charts", PAD, 172f, text(f.extra, 58f, Color.WHITE))
        val stamp = LocalDateTime.now().format(DateTimeFormatter.ofPattern("dd MMM yyyy, h:mm a", Locale.US))
        c.drawText("Period: $rangeLabel   •   $stamp", PAD, 222f, text(f.semi, 25f, Color.argb(235, 255, 255, 255)))
        val summary = "${d.totalTickets} created  ·  ${d.resolvedInPeriod} resolved  ·  ${d.open} open  ·  avg repair ${formatDuration(d.avgRepairMs)}"
        val sp = text(f.bold, 25f, Color.WHITE)
        c.drawText(fit(summary, sp, W - 2 * PAD), PAD, 266f, sp)
        return HEADER_H
    }

    /** Donut with legend (count and %). */
    private fun donut(c: Canvas, f: Fonts, top: Float, title: String, items: List<Item>, unit: String): Float {
        val total = items.sumOf { it.value.toDouble() }.toFloat()
        val r = card(c, f, top, CHART_H, title, "${total.toInt()} total")
        val cx = r.left + 200
        val cy = r.top + 270
        val rad = 125f
        val oval = RectF(cx - rad, cy - rad, cx + rad, cy + rad)
        val stroke = Paint(Paint.ANTI_ALIAS_FLAG).apply { style = Paint.Style.STROKE; strokeWidth = 50f }
        stroke.color = LINE
        c.drawArc(oval, 0f, 360f, false, stroke)
        var start = -90f
        items.forEach { it ->
            val sweep = if (total > 0) 360f * it.value / total else 0f
            stroke.color = it.color
            c.drawArc(oval, start, (sweep - if (items.size > 1) 2f else 0f).coerceAtLeast(0.5f), false, stroke)
            start += sweep
        }
        val big = text(f.extra, 58f, INK)
        val num = total.toInt().toString()
        c.drawText(num, cx - big.measureText(num) / 2, cy + 16, big)
        val small = text(f.semi, 22f, MUTED)
        c.drawText(unit, cx - small.measureText(unit) / 2, cy + 50, small)

        val lx = r.left + 420
        var ly = r.top + 150
        val name = text(f.semi, 26f, INK)
        val cnt = text(f.bold, 26f, INK)
        if (items.isEmpty()) c.drawText("No tickets in this period", lx, ly, text(f.regular, 26f, MUTED))
        items.take(7).forEach { it ->
            val pct = if (total > 0) (it.value * 100 / total).toInt() else 0
            val right = "${it.valueText}  ·  $pct%"
            c.drawRoundRect(RectF(lx, ly - 22, lx + 22, ly), 6f, 6f, fill(it.color))
            c.drawText(fit(it.label, name, r.right - 40 - cnt.measureText(right) - lx - 60), lx + 38, ly, name)
            c.drawText(right, r.right - 40 - cnt.measureText(right), ly, cnt)
            ly += 44
        }
        return r.bottom
    }

    /** Grouped vertical bars per day. */
    private fun trendBars(c: Canvas, f: Fonts, top: Float, trend: List<TrendPoint>): Float {
        val r = card(c, f, top, CHART_H, "Created vs resolved", if (trend.isEmpty()) null else "per day")
        val lg = text(f.semi, 22f, MUTED)
        c.drawRoundRect(RectF(r.left + 36, r.top + 92, r.left + 56, r.top + 112), 5f, 5f, fill(CREATED))
        c.drawText("Created", r.left + 66, r.top + 110, lg)
        c.drawRoundRect(RectF(r.left + 190, r.top + 92, r.left + 210, r.top + 112), 5f, 5f, fill(RESOLVED))
        c.drawText("Resolved", r.left + 220, r.top + 110, lg)
        val chart = RectF(r.left + 36, r.top + 160, r.right - 36, r.bottom - 70)
        grid(c, chart)
        if (trend.isEmpty()) return r.bottom
        val maxY = trend.maxOf { maxOf(it.created, it.resolved) }.coerceAtLeast(1)
        val slot = chart.width() / trend.size
        val bw = (slot * 0.32f).coerceAtMost(30f)
        val label = text(f.regular, 19f, MUTED)
        val valP = text(f.bold, 19f, INK)
        trend.forEachIndexed { i, p ->
            val sx = chart.left + slot * i + slot / 2
            listOf(Triple(p.created, sx - bw - 2, CREATED), Triple(p.resolved, sx + 2, RESOLVED)).forEach { (v, x, col) ->
                if (v > 0) {
                    val h = chart.height() * v / maxY
                    c.drawRoundRect(RectF(x, chart.bottom - h, x + bw, chart.bottom), 8f, 8f, fill(col))
                    if (trend.size <= 10) c.drawText(v.toString(), x + bw / 2 - valP.measureText(v.toString()) / 2, chart.bottom - h - 8, valP)
                }
            }
            val every = if (trend.size > 8) 2 else 1
            if (i % every == 0 || i == trend.lastIndex) c.drawText(p.label, sx - label.measureText(p.label) / 2, chart.bottom + 36, label)
        }
        return r.bottom
    }

    /** Single-series vertical columns with the value on top. */
    private fun columns(c: Canvas, f: Fonts, top: Float, title: String, items: List<Item>, subtitle: String?): Float {
        val r = card(c, f, top, CHART_H, title, subtitle)
        val chart = RectF(r.left + 60, r.top + 140, r.right - 60, r.bottom - 74)
        grid(c, chart)
        val max = items.maxOfOrNull { it.value }?.takeIf { it > 0f } ?: 1f
        val slot = chart.width() / items.size.coerceAtLeast(1)
        val bw = (slot * 0.5f).coerceAtMost(120f)
        val label = text(f.semi, 23f, MUTED)
        val valP = text(f.bold, 24f, INK)
        items.forEachIndexed { i, it ->
            val cx = chart.left + slot * i + slot / 2
            val h = chart.height() * it.value / max
            if (it.value > 0f) {
                val bar = RectF(cx - bw / 2, chart.bottom - h, cx + bw / 2, chart.bottom)
                val shader = LinearGradient(0f, bar.top, 0f, bar.bottom, it.color, Color.argb(150, Color.red(it.color), Color.green(it.color), Color.blue(it.color)), Shader.TileMode.CLAMP)
                c.drawRoundRect(bar, 14f, 14f, Paint(Paint.ANTI_ALIAS_FLAG).apply { this.shader = shader })
            }
            c.drawText(it.valueText, cx - valP.measureText(it.valueText) / 2, chart.bottom - h - 12, valP)
            val l = fit(it.label, label, slot - 8)
            c.drawText(l, cx - label.measureText(l) / 2, chart.bottom + 38, label)
        }
        return r.bottom
    }

    /** Horizontal bars, one row per item. */
    private fun hBars(c: Canvas, f: Fonts, top: Float, title: String, items: List<Item>, subtitle: String?): Float {
        val h = TITLE_H + items.size * 78f + 30f
        val r = card(c, f, top, h, title, subtitle)
        val max = items.maxOfOrNull { it.value }?.takeIf { it > 0f } ?: 1f
        val name = text(f.semi, 26f, INK)
        val cnt = text(f.bold, 26f, INK)
        var y = r.top + TITLE_H + 26
        items.forEach { it ->
            c.drawText(fit(it.label, name, r.width() - 72 - cnt.measureText(it.valueText) - 24), r.left + 36, y, name)
            c.drawText(it.valueText, r.right - 36 - cnt.measureText(it.valueText), y, cnt)
            val track = RectF(r.left + 36, y + 14, r.right - 36, y + 34)
            c.drawRoundRect(track, 10f, 10f, fill(LINE))
            if (it.value > 0f) c.drawRoundRect(RectF(track.left, track.top, track.left + track.width() * it.value / max, track.bottom), 10f, 10f, fill(it.color))
            y += 78
        }
        return r.bottom
    }

    private fun grid(c: Canvas, chart: RectF) {
        val p = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = LINE; strokeWidth = 2f }
        for (i in 0..3) {
            val gy = chart.top + chart.height() * i / 3
            c.drawLine(chart.left, gy, chart.right, gy, p)
        }
    }
}
