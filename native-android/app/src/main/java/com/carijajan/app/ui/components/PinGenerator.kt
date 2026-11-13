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
import kotlin.math.roundToInt

object PinGenerator {
    // Cache bitmap warung agar tidak menggambar ulang pin yang sama
    private val pinCache = LruCache<String, Bitmap>(128)
    // Cache puck lokasi berdasarkan kuantisasi sudut rotasi (per 3 derajat)
    private val puckCache = LruCache<Int, Bitmap>(120)

    fun dapatkanPin(
        context: Context,
        namaToko: String,
        kategori: String,
        buka: Boolean,
        diskonPersen: Int = 0
    ): Bitmap {
        val key = "${namaToko}_${kategori}_${buka}_$diskonPersen"
        pinCache.get(key)?.let { return it }

        val density = context.resources.displayMetrics.density
        val bitmap = gambarPin(namaToko, kategori, buka, diskonPersen, density)
        pinCache.put(key, bitmap)
        return bitmap
    }

    /**
     * Menggambar puck lokasi real-time dengan tanda panah arah hadap (heading/bearing).
     */
    fun dapatkanPuckLokasi(context: Context, heading: Float): Bitmap {
        // Kuantisasi derajat ke kelipatan 3 derajat agar menggunakan cache efisien
        val quantizedHeading = ((heading % 360f + 360f) % 360f / 3f).roundToInt() * 3
        puckCache.get(quantizedHeading)?.let { return it }

        val density = context.resources.displayMetrics.density
        val bitmap = gambarPuckLokasi(quantizedHeading.toFloat(), density)
        puckCache.put(quantizedHeading, bitmap)
        return bitmap
    }

    private fun gambarPuckLokasi(heading: Float, density: Float): Bitmap {
        val dp = { value: Float -> value * density }
        val size = dp(46f)
        val bitmap = Bitmap.createBitmap(size.toInt(), size.toInt(), Bitmap.Config.ARGB_8888)
        val canvas = Canvas(bitmap)

        val cx = size / 2f
        val cy = size / 2f

        // 1. Lingkaran luar berbayang halus (halo akurasi GPS)
        val haloPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.parseColor("#33EA681E") // Orange Brand transparan
            style = Paint.Style.FILL
        }
        canvas.drawCircle(cx, cy, dp(21f), haloPaint)

        // 2. Tanda Panah Arah Hadap (menunjuk sesuai sudut heading)
        canvas.save()
        canvas.rotate(heading, cx, cy)

        val arrowPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.parseColor("#EA681E")
            style = Paint.Style.FILL
        }
        val arrowBorderPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.WHITE
            style = Paint.Style.STROKE
            strokeWidth = dp(1.5f)
            strokeJoin = Paint.Join.ROUND
        }

        val arrowPath = Path().apply {
            moveTo(cx, cy - dp(21f)) // Ujung tajam panah di depan
            lineTo(cx - dp(7.5f), cy - dp(9f))
            lineTo(cx, cy - dp(12f)) // Cekungan belakang panah
            lineTo(cx + dp(7.5f), cy - dp(9f))
            close()
        }
        canvas.drawPath(arrowPath, arrowPaint)
        canvas.drawPath(arrowPath, arrowBorderPaint)

        canvas.restore()

        // 3. Lingkaran Inti Posisi (Titik Biru/Hijau dengan Lingkar Putih Bersih)
        val outerDotPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.WHITE
            style = Paint.Style.FILL
            setShadowLayer(dp(2.5f), 0f, dp(1f), Color.parseColor("#44000000"))
        }
        canvas.drawCircle(cx, cy, dp(9.5f), outerDotPaint)

        val innerDotPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.parseColor("#EA681E")
            style = Paint.Style.FILL
        }
        canvas.drawCircle(cx, cy, dp(7f), innerDotPaint)

        // Titik kilau putih kecil di tengah
        val centerDotPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.WHITE
            style = Paint.Style.FILL
        }
        canvas.drawCircle(cx, cy, dp(2.2f), centerDotPaint)

        return bitmap
    }

    private fun gambarPin(
        namaToko: String,
        kategori: String,
        buka: Boolean,
        diskonPersen: Int,
        density: Float
    ): Bitmap {
        val dp = { value: Float -> value * density }

        // Singkat nama toko jika terlalu panjang agar pin tetap proporsional di peta
        val namaTampil = if (namaToko.length > 18) namaToko.take(16) + "…" else namaToko

        // Cat teks nama warung
        val textPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.parseColor("#18181B")
            textSize = dp(11.5f)
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
        }
        val textWidth = textPaint.measureText(namaTampil)
        val fontMetrics = textPaint.fontMetrics

        // Dimensi pin utama
        val iconSize = dp(24f)
        val padX = dp(8f)
        val padY = dp(5f)
        val gap = dp(7f)
        val arrowH = dp(6f)
        val arrowW = dp(10f)
        val radius = dp(16f)

        val bubbleW = padX + iconSize + gap + textWidth + padX + dp(4f)
        val bubbleH = padY + iconSize + padY

        // ── Komputasi Pop-Up Callout Diskon di Atas Pin ─────────────────────
        val adaDiskon = diskonPersen > 0
        val diskonTeks = "Diskon $diskonPersen%"
        val diskonTextPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.WHITE
            textSize = dp(10f)
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
        }
        val diskonTextWidth = if (adaDiskon) diskonTextPaint.measureText(diskonTeks) else 0f
        val diskonMetrics = diskonTextPaint.fontMetrics

        val badgePadX = dp(7f)
        val badgeH = dp(18f)
        val badgeIconSize = dp(11f)
        val badgeGap = dp(4f)
        val badgeW = if (adaDiskon) badgePadX + badgeIconSize + badgeGap + diskonTextWidth + badgePadX else 0f
        val calloutArrowH = dp(4f)
        val calloutArrowW = dp(6f)
        val calloutGap = dp(3f)

        // Tinggi ekstra di bagian atas kanvas untuk pop-up diskon
        val extraTopH = if (adaDiskon) badgeH + calloutArrowH + calloutGap else 0f

        val contentW = maxOf(bubbleW, badgeW)
        val totalW = contentW + dp(8f)
        val totalH = extraTopH + bubbleH + arrowH + dp(6f)

        val bitmap = Bitmap.createBitmap(
            totalW.toInt().coerceAtLeast(1),
            totalH.toInt().coerceAtLeast(1),
            Bitmap.Config.ARGB_8888
        )
        val canvas = Canvas(bitmap)

        val centerX = totalW / 2f
        val warnaUtama = if (buka) Color.parseColor("#EA681E") else Color.parseColor("#78716C")
        val warnaBgIcon = if (buka) Color.parseColor("#FFEDD5") else Color.parseColor("#F5F5F4")

        // ── 1. Gambar Pop-Up Badge Diskon Mengambang di Atas Pin ────────────
        if (adaDiskon) {
            val badgeLeft = centerX - badgeW / 2f
            val badgeTop = dp(3f)
            val badgeBottom = badgeTop + badgeH
            val badgeRect = RectF(badgeLeft, badgeTop, badgeLeft + badgeW, badgeBottom)

            val popupPath = Path().apply {
                addRoundRect(badgeRect, dp(9f), dp(9f), Path.Direction.CW)
                // Panah kecil callout menunjuk ke gelembung pin di bawahnya
                moveTo(centerX - calloutArrowW / 2f, badgeBottom - dp(0.5f))
                lineTo(centerX, badgeBottom + calloutArrowH)
                lineTo(centerX + calloutArrowW / 2f, badgeBottom - dp(0.5f))
                close()
            }

            // Bayangan halus pada pop-up diskon
            val popupFillPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                color = Color.parseColor("#DC2626") // Merah Diskon Terang
                style = Paint.Style.FILL
                setShadowLayer(dp(3f), 0f, dp(1.5f), Color.parseColor("#44000000"))
            }
            canvas.drawPath(popupPath, popupFillPaint)

            // Garis tepi putih tipis pada pop-up badge
            val popupStrokePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                color = Color.WHITE
                style = Paint.Style.STROKE
                strokeWidth = dp(1.2f)
                strokeJoin = Paint.Join.ROUND
            }
            canvas.drawPath(popupPath, popupStrokePaint)

            // Ikon persen '%' dalam lingkaran putih kecil
            val pIconCx = badgeLeft + badgePadX + badgeIconSize / 2f
            val pIconCy = badgeTop + badgeH / 2f
            val pIconCirclePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                color = Color.WHITE
                style = Paint.Style.FILL
            }
            canvas.drawCircle(pIconCx, pIconCy, badgeIconSize / 2f, pIconCirclePaint)

            val pTextPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                color = Color.parseColor("#DC2626")
                textSize = dp(7.5f)
                typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
                textAlign = Paint.Align.CENTER
            }
            val pTextY = pIconCy - ((pTextPaint.fontMetrics.ascent + pTextPaint.fontMetrics.descent) / 2f)
            canvas.drawText("%", pIconCx, pTextY, pTextPaint)

            // Teks Diskon
            val diskonTextX = badgeLeft + badgePadX + badgeIconSize + badgeGap
            val diskonTextY = pIconCy - ((diskonMetrics.ascent + diskonMetrics.descent) / 2f)
            canvas.drawText(diskonTeks, diskonTextX, diskonTextY, diskonTextPaint)
        }

        // ── 2. Gambar Gelembung Pin Utama + Panah Lokasi Bawah ──────────────
        val bubbleLeft = centerX - bubbleW / 2f
        val bubbleTop = dp(3f) + extraTopH
        val bubbleBottom = bubbleTop + bubbleH

        val bubbleRect = RectF(bubbleLeft, bubbleTop, bubbleLeft + bubbleW, bubbleBottom)
        val bubblePath = Path().apply {
            addRoundRect(bubbleRect, radius, radius, Path.Direction.CW)
            // Gambar ekor panah di bagian bawah tengah menunjuk ke titik GPS
            moveTo(centerX - arrowW / 2f, bubbleBottom - dp(1f))
            lineTo(centerX, bubbleBottom + arrowH)
            lineTo(centerX + arrowW / 2f, bubbleBottom - dp(1f))
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

        // Lingkaran latar ikon kategori
        val iconCenterLeft = bubbleLeft + padX + iconSize / 2f
        val iconCenterTop = bubbleTop + padY + iconSize / 2f
        val iconBgPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = warnaBgIcon
            style = Paint.Style.FILL
        }
        canvas.drawCircle(iconCenterLeft, iconCenterTop, iconSize / 2f, iconBgPaint)

        // Simbol ikon sesuai kategori
        gambarIkonKategori(
            canvas = canvas,
            kategori = kategori,
            cx = iconCenterLeft,
            cy = iconCenterTop,
            size = iconSize * 0.65f,
            warna = warnaUtama,
            strokeWidth = dp(1.6f)
        )

        // Teks nama warung
        val textX = bubbleLeft + padX + iconSize + gap
        val textY = iconCenterTop - ((fontMetrics.ascent + fontMetrics.descent) / 2f)
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
                // Ikon Gerobak PKL
                val body = Path().apply {
                    moveTo(cx - half * 0.7f, cy - half * 0.4f)
                    lineTo(cx + half * 0.8f, cy - half * 0.4f)
                    lineTo(cx + half * 0.6f, cy + half * 0.4f)
                    lineTo(cx - half * 0.6f, cy + half * 0.4f)
                    close()
                }
                canvas.drawPath(body, paint)
                canvas.drawLine(cx - half * 0.7f, cy - half * 0.4f, cx - half * 0.95f, cy - half * 0.7f, paint)
                canvas.drawCircle(cx - half * 0.35f, cy + half * 0.65f, half * 0.22f, fillPaint)
                canvas.drawCircle(cx + half * 0.35f, cy + half * 0.65f, half * 0.22f, fillPaint)
            }
            "keliling" -> {
                // Ikon Motor / Kendaraan Keliling
                val rRoda = half * 0.32f
                val rodaLeftX = cx - half * 0.55f
                val rodaRightX = cx + half * 0.55f
                val rodaY = cy + half * 0.45f

                canvas.drawCircle(rodaLeftX, rodaY, rRoda, paint)
                canvas.drawCircle(rodaRightX, rodaY, rRoda, paint)

                val motorPath = Path().apply {
                    moveTo(rodaLeftX, rodaY)
                    lineTo(cx - half * 0.1f, cy - half * 0.15f)
                    lineTo(cx + half * 0.35f, cy - half * 0.15f)
                    lineTo(rodaRightX, rodaY)
                    moveTo(cx + half * 0.35f, cy - half * 0.15f)
                    lineTo(cx + half * 0.45f, cy - half * 0.75f)
                }
                canvas.drawPath(motorPath, paint)

                val box = RectF(cx - half * 0.8f, cy - half * 0.55f, cx - half * 0.15f, cy - half * 0.05f)
                canvas.drawRoundRect(box, 2f, 2f, paint)
            }
            "rumah_makan", "cafe" -> {
                // Ikon Mangkuk Makanan Hangat
                val bowlRect = RectF(cx - half * 0.8f, cy - half * 0.2f, cx + half * 0.8f, cy + half * 0.75f)
                canvas.drawArc(bowlRect, 0f, 180f, true, paint)
                canvas.drawLine(cx - half * 0.35f, cy - half * 0.45f, cx - half * 0.35f, cy - half * 0.75f, paint)
                canvas.drawLine(cx, cy - half * 0.55f, cx, cy - half * 0.85f, paint)
                canvas.drawLine(cx + half * 0.35f, cy - half * 0.45f, cx + half * 0.35f, cy - half * 0.75f, paint)
            }
            else -> {
                // Ikon Rumah / Toko Tetap
                val atap = Path().apply {
                    moveTo(cx, cy - half * 0.85f)
                    lineTo(cx + half * 0.85f, cy - half * 0.15f)
                    lineTo(cx - half * 0.85f, cy - half * 0.15f)
                    close()
                }
                canvas.drawPath(atap, paint)

                val dinding = RectF(cx - half * 0.65f, cy - half * 0.15f, cx + half * 0.65f, cy + half * 0.75f)
                canvas.drawRect(dinding, paint)

                val pintu = RectF(cx - half * 0.22f, cy + half * 0.2f, cx + half * 0.22f, cy + half * 0.75f)
                canvas.drawRect(pintu, fillPaint)
            }
        }
    }
}
