package com.carijajan.app.ui.components

import android.content.Context
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.Path
import android.graphics.RectF
import android.graphics.Typeface
import androidx.collection.LruCache

object PinGenerator {
    // Cache bitmap agar tidak menggambar ulang pin yang sama berulang kali
    private val pinCache = LruCache<String, Bitmap>(128)

    fun dapatkanPin(
        context: Context,
        namaToko: String,
        kategori: String,
        buka: Boolean
    ): Bitmap {
        val key = "${namaToko}_${kategori}_$buka"
        pinCache.get(key)?.let { return it }

        val density = context.resources.displayMetrics.density
        val bitmap = gambarPin(namaToko, kategori, buka, density)
        pinCache.put(key, bitmap)
        return bitmap
    }

    private fun gambarPin(
        namaToko: String,
        kategori: String,
        buka: Boolean,
        density: Float
    ): Bitmap {
        val dp = { value: Float -> value * density }

        // Singkat nama toko jika terlalu panjang agar pin tetap proporsional di peta
        val namaTampil = if (namaToko.length > 18) namaToko.take(16) + "…" else namaToko

        // Cat teks
        val textPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.parseColor("#18181B")
            textSize = dp(11.5f)
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
        }
        val textWidth = textPaint.measureText(namaTampil)
        val fontMetrics = textPaint.fontMetrics
        val textHeight = fontMetrics.descent - fontMetrics.ascent

        // Dimensi pin
        val iconSize = dp(24f)
        val padX = dp(8f)
        val padY = dp(5f)
        val gap = dp(7f)
        val arrowH = dp(6f)
        val arrowW = dp(10f)
        val radius = dp(16f)

        val bubbleW = padX + iconSize + gap + textWidth + padX + dp(4f)
        val bubbleH = padY + iconSize + padY
        val totalW = bubbleW + dp(4f) // padding ekstra untuk stroke
        val totalH = bubbleH + arrowH + dp(4f)

        val bitmap = Bitmap.createBitmap(
            totalW.toInt().coerceAtLeast(1),
            totalH.toInt().coerceAtLeast(1),
            Bitmap.Config.ARGB_8888
        )
        val canvas = Canvas(bitmap)

        val warnaUtama = if (buka) Color.parseColor("#059669") else Color.parseColor("#6B7280")
        val warnaBgIcon = if (buka) Color.parseColor("#D1FAE5") else Color.parseColor("#F3F4F6")

        // 1. Gambar latar belakang gelembung pin + ekor panah
        val bubbleRect = RectF(dp(2f), dp(2f), dp(2f) + bubbleW, dp(2f) + bubbleH)
        val bubblePath = Path().apply {
            addRoundRect(bubbleRect, radius, radius, Path.Direction.CW)
            // Gambar ekor panah di bagian bawah tengah
            val centerX = dp(2f) + bubbleW / 2f
            val bottomY = dp(2f) + bubbleH
            moveTo(centerX - arrowW / 2f, bottomY - dp(1f))
            lineTo(centerX, bottomY + arrowH)
            lineTo(centerX + arrowW / 2f, bottomY - dp(1f))
            close()
        }

        // Isi gelembung (putih solid)
        val fillPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.WHITE
            style = Paint.Style.FILL
            setShadowLayer(dp(3f), 0f, dp(1.5f), Color.parseColor("#33000000"))
        }
        canvas.drawPath(bubblePath, fillPaint)

        // Garis tepi / border pin
        val strokePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = warnaUtama
            style = Paint.Style.STROKE
            strokeWidth = dp(1.8f)
            strokeJoin = Paint.Join.ROUND
        }
        canvas.drawPath(bubblePath, strokePaint)

        // 2. Gambar lingkaran latar ikon kategori
        val iconCenterLeft = dp(2f) + padX + iconSize / 2f
        val iconCenterTop = dp(2f) + padY + iconSize / 2f
        val iconBgPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = warnaBgIcon
            style = Paint.Style.FILL
        }
        canvas.drawCircle(iconCenterLeft, iconCenterTop, iconSize / 2f, iconBgPaint)

        // 3. Gambar simbol ikon sesuai kategori
        gambarIkonKategori(
            canvas = canvas,
            kategori = kategori,
            cx = iconCenterLeft,
            cy = iconCenterTop,
            size = iconSize * 0.65f,
            warna = warnaUtama,
            strokeWidth = dp(1.6f)
        )

        // 4. Gambar teks nama warung
        val textX = dp(2f) + padX + iconSize + gap
        val textY = dp(2f) + padY + (iconSize / 2f) - ((fontMetrics.ascent + fontMetrics.descent) / 2f)
        canvas.drawText(namaTampil, textX, textY, textPaint)

        return bitmap
    }

    private fun gambarIkonKategori(
        canvas: Canvas,
        kategori: String,
        cx: Float,
        cy: Float,
        size: Float,
        warna: Int,
        strokeWidth: Float
    ) {
        val paint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = warna
            style = Paint.Style.STROKE
            this.strokeWidth = strokeWidth
            strokeCap = Paint.Cap.ROUND
            strokeJoin = Paint.Join.ROUND
        }
        val fillPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = warna
            style = Paint.Style.FILL
        }

        val half = size / 2f

        when (kategori) {
            "pkl" -> {
                // Ikon Gerobak PKL (badan gerobak + 2 roda + gagang)
                val body = Path().apply {
                    moveTo(cx - half * 0.7f, cy - half * 0.4f)
                    lineTo(cx + half * 0.8f, cy - half * 0.4f)
                    lineTo(cx + half * 0.6f, cy + half * 0.4f)
                    lineTo(cx - half * 0.6f, cy + half * 0.4f)
                    close()
                }
                canvas.drawPath(body, paint)
                // Gagang gerobak
                canvas.drawLine(cx - half * 0.7f, cy - half * 0.4f, cx - half * 0.95f, cy - half * 0.7f, paint)
                // Roda 1 & 2
                canvas.drawCircle(cx - half * 0.35f, cy + half * 0.65f, half * 0.22f, fillPaint)
                canvas.drawCircle(cx + half * 0.35f, cy + half * 0.65f, half * 0.22f, fillPaint)
            }
            "keliling" -> {
                // Ikon Motor / Kendaraan Keliling (2 roda + bodi motor)
                val rRoda = half * 0.32f
                val rodaLeftX = cx - half * 0.55f
                val rodaRightX = cx + half * 0.55f
                val rodaY = cy + half * 0.45f

                // Roda depan & belakang
                canvas.drawCircle(rodaLeftX, rodaY, rRoda, paint)
                canvas.drawCircle(rodaRightX, rodaY, rRoda, paint)

                // Rangka motor
                val motorPath = Path().apply {
                    moveTo(rodaLeftX, rodaY)
                    lineTo(cx - half * 0.1f, cy - half * 0.15f)
                    lineTo(cx + half * 0.35f, cy - half * 0.15f)
                    lineTo(rodaRightX, rodaY)
                    // Setang motor
                    moveTo(cx + half * 0.35f, cy - half * 0.15f)
                    lineTo(cx + half * 0.45f, cy - half * 0.75f)
                }
                canvas.drawPath(motorPath, paint)

                // Keranjang/boks jualan di belakang
                val box = RectF(cx - half * 0.8f, cy - half * 0.55f, cx - half * 0.15f, cy - half * 0.05f)
                canvas.drawRoundRect(box, 2f, 2f, paint)
            }
            "rumah_makan", "cafe" -> {
                // Ikon Mangkuk Makanan Hangat / Rumah Makan
                val bowlRect = RectF(cx - half * 0.8f, cy - half * 0.2f, cx + half * 0.8f, cy + half * 0.75f)
                canvas.drawArc(bowlRect, 0f, 180f, true, paint)
                // Garis uap hangat di atas
                canvas.drawLine(cx - half * 0.35f, cy - half * 0.45f, cx - half * 0.35f, cy - half * 0.75f, paint)
                canvas.drawLine(cx, cy - half * 0.55f, cx, cy - half * 0.85f, paint)
                canvas.drawLine(cx + half * 0.35f, cy - half * 0.45f, cx + half * 0.35f, cy - half * 0.75f, paint)
            }
            else -> {
                // Ikon Rumah / Toko Tetap (Atap segitiga + dinding + pintu)
                val atap = Path().apply {
                    moveTo(cx, cy - half * 0.85f)
                    lineTo(cx + half * 0.85f, cy - half * 0.15f)
                    lineTo(cx - half * 0.85f, cy - half * 0.15f)
                    close()
                }
                canvas.drawPath(atap, paint)

                // Dinding rumah
                val dinding = RectF(cx - half * 0.65f, cy - half * 0.15f, cx + half * 0.65f, cy + half * 0.75f)
                canvas.drawRect(dinding, paint)

                // Pintu
                val pintu = RectF(cx - half * 0.22f, cy + half * 0.2f, cx + half * 0.22f, cy + half * 0.75f)
                canvas.drawRect(pintu, fillPaint)
            }
        }
    }
}
