// © 2026 Khondaker Arifur Rahman. All Rights Reserved.
// BGL-IT Help — see COPYRIGHT.md. Unauthorized copying, modification or redistribution is prohibited.

package com.company.ithelpdesk.ui.components

import android.Manifest
import android.content.ActivityNotFoundException
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Call
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat

/** Keeps digits and a leading +, so "017-1100 1470" becomes "01711001470". */
fun cleanPhone(raw: String): String {
    val t = raw.trim()
    val digits = t.filter { it.isDigit() }
    return if (t.startsWith("+")) "+$digits" else digits
}

private fun startCall(context: Context, phone: String, direct: Boolean) {
    val uri = Uri.parse("tel:${cleanPhone(phone)}")
    try {
        context.startActivity(Intent(if (direct) Intent.ACTION_CALL else Intent.ACTION_DIAL, uri))
    } catch (e: ActivityNotFoundException) {
        Toast.makeText(context, "This device can't make phone calls.", Toast.LENGTH_LONG).show()
    } catch (e: SecurityException) {
        context.startActivity(Intent(Intent.ACTION_DIAL, uri))
    }
}

/**
 * Returns a function that calls a number straight from the app.
 * Asks "Call Rahim?" first, then places the call directly (the phone asks for call permission once).
 * If permission is refused, the dialer opens with the number filled in.
 */
@Composable
fun rememberPhoneCaller(): (name: String, phone: String) -> Unit {
    val context = LocalContext.current
    var pending by remember { mutableStateOf<Pair<String, String>?>(null) }
    var toCall by remember { mutableStateOf<String?>(null) }
    val permission = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { granted ->
        toCall?.let { startCall(context, it, direct = granted) }
        toCall = null
    }

    pending?.let { (name, phone) ->
        AlertDialog(
            onDismissRequest = { pending = null },
            icon = { Icon(Icons.Filled.Call, null) },
            title = { Text("Call $name?") },
            text = { Text(phone, style = MaterialTheme.typography.titleMedium) },
            confirmButton = {
                TextButton(onClick = {
                    pending = null
                    val granted = ContextCompat.checkSelfPermission(context, Manifest.permission.CALL_PHONE) ==
                        PackageManager.PERMISSION_GRANTED
                    if (granted) {
                        startCall(context, phone, direct = true)
                    } else {
                        toCall = phone
                        permission.launch(Manifest.permission.CALL_PHONE)
                    }
                }) { Text("📞 Call now") }
            },
            dismissButton = { TextButton(onClick = { pending = null }) { Text("Cancel") } },
        )
    }
    return { name, phone -> if (phone.isNotBlank()) pending = name to phone }
}

/** Round green call button. */
@Composable
fun CallButton(onClick: () -> Unit, modifier: Modifier = Modifier, contentDescription: String = "Call") {
    IconButton(onClick = onClick, modifier = modifier.size(44.dp).background(Color(0xFF22C55E), CircleShape)) {
        Icon(Icons.Filled.Call, contentDescription = contentDescription, tint = Color.White, modifier = Modifier.size(22.dp))
    }
}
