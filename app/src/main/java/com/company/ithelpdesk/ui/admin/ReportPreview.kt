// © 2026 Khondaker Arifur Rahman. All Rights Reserved.
// BGL-IT Help — see COPYRIGHT.md. Unauthorized copying, modification or redistribution is prohibited.

package com.company.ithelpdesk.ui.admin

import android.graphics.Bitmap
import android.widget.Toast
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.outlined.Download
import androidx.compose.material.icons.outlined.Share
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.company.ithelpdesk.ui.components.GradientButton
import com.company.ithelpdesk.util.ReportImage

/** Full-screen preview of the chart report, with Share and Save to gallery. */
@Composable
fun ReportPreviewDialog(bitmap: Bitmap, rangeLabel: String, onDismiss: () -> Unit) {
    val context = LocalContext.current
    val image = remember(bitmap) { bitmap.asImageBitmap() }
    Dialog(onDismissRequest = onDismiss, properties = DialogProperties(usePlatformDefaultWidth = false)) {
        Surface(Modifier.fillMaxSize(), color = Color(0xFF0B1020)) {
            Column(Modifier.fillMaxSize().statusBarsPadding().navigationBarsPadding()) {
                Row(Modifier.fillMaxWidth().padding(start = 16.dp, end = 4.dp, top = 8.dp), verticalAlignment = Alignment.CenterVertically) {
                    Column(Modifier.weight(1f)) {
                        Text("Report preview", style = MaterialTheme.typography.titleLarge, color = Color.White)
                        Text(rangeLabel, style = MaterialTheme.typography.bodySmall, color = Color.White.copy(alpha = 0.7f))
                    }
                    IconButton(onClick = onDismiss) { Icon(Icons.Default.Close, "Close", tint = Color.White) }
                }
                Box(Modifier.weight(1f).fillMaxWidth().verticalScroll(rememberScrollState()).padding(12.dp)) {
                    Image(
                        bitmap = image,
                        contentDescription = "Chart report for $rangeLabel",
                        contentScale = ContentScale.FillWidth,
                        modifier = Modifier.fillMaxWidth(),
                    )
                }
                Row(
                    Modifier.fillMaxWidth().background(Color(0xFF121A2E)).padding(12.dp),
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    OutlinedButton(
                        onClick = {
                            val ok = runCatching { ReportImage.saveToGallery(context, bitmap) }.getOrDefault(false)
                            Toast.makeText(
                                context,
                                if (ok) "Saved to Gallery › Pictures › BGL-IT Help" else "Couldn't save on this phone — use Share instead",
                                Toast.LENGTH_LONG,
                            ).show()
                        },
                        shape = CircleShape,
                        modifier = Modifier.weight(1f),
                    ) {
                        Icon(Icons.Outlined.Download, null, tint = Color.White)
                        Spacer(Modifier.width(6.dp))
                        Text("Save", color = Color.White)
                    }
                    GradientButton(
                        "Share",
                        icon = Icons.Outlined.Share,
                        onClick = {
                            runCatching {
                                val file = ReportImage.saveToCache(context, bitmap)
                                context.startActivity(ReportImage.shareIntent(context, file, rangeLabel))
                            }.onFailure { Toast.makeText(context, "Couldn't share: ${it.message}", Toast.LENGTH_LONG).show() }
                        },
                        modifier = Modifier.weight(1.4f),
                    )
                }
            }
        }
    }
}
