package com.carijajan.app.util

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import java.io.ByteArrayOutputStream
import java.io.File
import kotlin.math.max
import kotlin.math.min
import kotlin.math.roundToInt

object Gambar {
    private const val MAX_SISI = 1280
    private const val KUALITAS = 80
    private const val MAX_BYTES = 2 * 1024 * 1024

    fun kompresFile(file: File): ByteArray? {
        val bounds = BitmapFactory.Options().apply { inJustDecodeBounds = true }
        BitmapFactory.decodeFile(file.absolutePath, bounds)
        if (bounds.outWidth <= 0 || bounds.outHeight <= 0) return null

        val options = BitmapFactory.Options().apply {
            inSampleSize = hitungSample(bounds.outWidth, bounds.outHeight)
        }
        val bitmap = BitmapFactory.decodeFile(file.absolutePath, options) ?: return null
        return proses(bitmap)
    }

    fun kompresUri(context: Context, uri: Uri): ByteArray? {
        val bounds = BitmapFactory.Options().apply { inJustDecodeBounds = true }
        context.contentResolver.openInputStream(uri)?.use {
            BitmapFactory.decodeStream(it, null, bounds)
        }
        if (bounds.outWidth <= 0 || bounds.outHeight <= 0) return null

        val options = BitmapFactory.Options().apply {
            inSampleSize = hitungSample(bounds.outWidth, bounds.outHeight)
        }
        val bitmap = context.contentResolver.openInputStream(uri)?.use {
            BitmapFactory.decodeStream(it, null, options)
        } ?: return null
        return proses(bitmap)
    }

    private fun hitungSample(width: Int, height: Int): Int {
        var sample = 1
        var w = width
        var h = height
        while (w / 2 >= MAX_SISI && h / 2 >= MAX_SISI) {
            w /= 2
            h /= 2
            sample *= 2
        }
        return sample
    }

    private fun proses(bitmap: Bitmap): ByteArray {
        val skala = min(1f, MAX_SISI.toFloat() / max(bitmap.width, bitmap.height))
        val lebar = max(1, (bitmap.width * skala).roundToInt())
        val tinggi = max(1, (bitmap.height * skala).roundToInt())

        val scaled = if (skala < 1f) {
            Bitmap.createScaledBitmap(bitmap, lebar, tinggi, true)
        } else {
            bitmap
        }

        var kualitas = KUALITAS
        var bytes: ByteArray
        do {
            val out = ByteArrayOutputStream()
            scaled.compress(Bitmap.CompressFormat.JPEG, kualitas, out)
            bytes = out.toByteArray()
            kualitas -= 10
        } while (bytes.size > MAX_BYTES && kualitas >= 40)

        if (scaled !== bitmap) scaled.recycle()
        bitmap.recycle()
        return bytes
    }
}
