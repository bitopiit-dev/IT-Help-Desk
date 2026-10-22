// © 2026 Khondaker Arifur Rahman. All Rights Reserved.
// BGL-IT Help — see COPYRIGHT.md. Unauthorized copying, modification or redistribution is prohibited.

package com.company.ithelpdesk.util

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Matrix
import android.net.Uri
import androidx.exifinterface.media.ExifInterface
import java.io.ByteArrayOutputStream

/**
 * Shrinks photos so they fit inside a single Firestore document (1 MiB limit, base64 adds 33 %).
 * Tries 1280 px first, lowering JPEG quality and then size until the result is under [MAX_BYTES].
 */
object ImageCompressor {
    const val MAX_BYTES = 600_000
    private val SIDES = intArrayOf(1280, 1024, 800, 640)

    /** Square, centre-cropped 320 px JPEG (~20–40 KB) for profile photos. */
    fun avatar(context: Context, uri: Uri, side: Int = 320): ByteArray {
        val bytes = context.contentResolver.openInputStream(uri)?.use { it.readBytes() }
            ?: error("Could not read image file")
        val bounds = BitmapFactory.Options().apply { inJustDecodeBounds = true }
        BitmapFactory.decodeByteArray(bytes, 0, bytes.size, bounds)
        if (bounds.outWidth <= 0 || bounds.outHeight <= 0) error("Could not read image bounds")

        var sample = 1
        while (minOf(bounds.outWidth, bounds.outHeight) / (sample * 2) >= side) sample *= 2

        val decoded = BitmapFactory.decodeByteArray(
            bytes, 0, bytes.size,
            BitmapFactory.Options().apply { inSampleSize = sample },
        ) ?: error("Could not decode image")

        val rotation = runCatching {
            ExifInterface(bytes.inputStream()).getAttributeInt(
                ExifInterface.TAG_ORIENTATION, ExifInterface.ORIENTATION_NORMAL,
            ).let {
                when (it) {
                    ExifInterface.ORIENTATION_ROTATE_90 -> 90f
                    ExifInterface.ORIENTATION_ROTATE_180 -> 180f
                    ExifInterface.ORIENTATION_ROTATE_270 -> 270f
                    else -> 0f
                }
            }
        }.getOrDefault(0f)

        val crop = minOf(decoded.width, decoded.height)
        val x = (decoded.width - crop) / 2
        val y = (decoded.height - crop) / 2
        val scale = side.toFloat() / crop
        val matrix = Matrix().apply {
            postScale(scale, scale)
            if (rotation != 0f) postRotate(rotation)
        }
        val out = Bitmap.createBitmap(decoded, x, y, crop, crop, matrix, true)
        return ByteArrayOutputStream().use { stream ->
            out.compress(Bitmap.CompressFormat.JPEG, 82, stream)
            stream.toByteArray()
        }
    }

    fun compress(context: Context, uri: Uri): ByteArray {
        val bytes = context.contentResolver.openInputStream(uri)?.use { it.readBytes() }
            ?: error("Could not read image file")
        val bounds = BitmapFactory.Options().apply { inJustDecodeBounds = true }
        BitmapFactory.decodeByteArray(bytes, 0, bytes.size, bounds)
        if (bounds.outWidth <= 0 || bounds.outHeight <= 0) error("Could not read image bounds")

        var sample = 1
        while (bounds.outWidth / (sample * 2) >= SIDES[0] || bounds.outHeight / (sample * 2) >= SIDES[0]) sample *= 2

        val decoded = BitmapFactory.decodeByteArray(
            bytes, 0, bytes.size,
            BitmapFactory.Options().apply { inSampleSize = sample },
        ) ?: error("Could not decode image")

        val rotation = runCatching {
            ExifInterface(bytes.inputStream()).getAttributeInt(
                ExifInterface.TAG_ORIENTATION, ExifInterface.ORIENTATION_NORMAL,
            ).let {
                when (it) {
                    ExifInterface.ORIENTATION_ROTATE_90 -> 90f
                    ExifInterface.ORIENTATION_ROTATE_180 -> 180f
                    ExifInterface.ORIENTATION_ROTATE_270 -> 270f
                    else -> 0f
                }
            }
        }.getOrDefault(0f)

        var last = ByteArray(0)
        for (side in SIDES) {
            val scale = minOf(1f, side.toFloat() / maxOf(decoded.width, decoded.height))
            val matrix = Matrix().apply {
                postScale(scale, scale)
                if (rotation != 0f) postRotate(rotation)
            }
            val scaled = Bitmap.createBitmap(decoded, 0, 0, decoded.width, decoded.height, matrix, true)
            for (quality in intArrayOf(80, 70, 60, 50)) {
                last = ByteArrayOutputStream().use { out ->
                    scaled.compress(Bitmap.CompressFormat.JPEG, quality, out)
                    out.toByteArray()
                }
                if (last.size <= MAX_BYTES) return last
            }
        }
        if (last.size > MAX_BYTES) error("This photo is too large. Please try a different one.")
        return last
    }
}
