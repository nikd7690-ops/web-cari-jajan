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
        diskonPersen: Int = 0,
        terpilih: Boolean = false,
        fotoBitmap: Bitmap? = null
    ): Bitmap {
        val fotoHash = fotoBitmap?.hashCode() ?: 0
        val key = "${namaToko}_${kategori}_${buka}_${diskonPersen}_${terpilih}_$fotoHash"
        pinCache.get(key)?.let { return it }

        val density = context.resources.displayMetrics.density
        val bitmap = gambarPin(
            namaToko = namaToko,
            kategori = kategori,
            buka = buka,
            diskonPersen = diskonPersen,
            terpilih = terpilih,
            fotoBitmap = fotoBitmap,
            density = density
        )
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
        terpilih: Boolean,
        fotoBitmap: Bitmap?,
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

        // ── Komputasi Diskon & Foto Pop-Up di Atas Pin ─────────────────────
        val adaDiskon = diskonPersen > 0
        val diskonTeks = "Diskon $diskonPersen%"
        val diskonTextPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.WHITE
            textSize = dp(9.5f)
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
        }
        val diskonTextWidth = if (adaDiskon) diskonTextPaint.measureText(diskonTeks) else 0f
        val diskonMetrics = diskonTextPaint.fontMetrics

        val adaFoto = terpilih && fotoBitmap != null
        val fotoW = dp(112f)
        val fotoH = dp(72f)
        val fotoRadius = dp(10f)
        val fotoArrowH = dp(5f)
        val fotoArrowW = dp(8f)
        val fotoGap = dp(3.5f)
        val fotoTotalH = if (adaFoto) fotoH + fotoArrowH + fotoGap else 0f

        val badgePadX = dp(7f)
        val badgeH = dp(18f)
        val badgeIconSize = dp(11f)
        val badgeGap = dp(4f)
        val badgeW = if (adaDiskon) badgePadX + badgeIconSize + badgeGap + diskonTextWidth + badgePadX else 0f
        val calloutArrowH = dp(4f)
        val calloutArrowW = dp(6f)
        val calloutGap = dp(3f)
        val diskonTotalH = if (!adaFoto && adaDiskon) badgeH + calloutArrowH + calloutGap else 0f

        // Tinggi ekstra di bagian atas kanvas
        val extraTopH = if (adaFoto) fotoTotalH else diskonTotalH

        val contentW = maxOf(bubbleW, if (adaFoto) fotoW else if (adaDiskon) badgeW else 0f)
        val totalW = contentW + dp(8f)

        // Titik ujung panah penunjuk koordinat GPS
        val marginAtas = dp(3f)
        val arrowTipY = marginAtas + extraTopH + bubbleH + arrowH

        // Kunci stabilitas koordinat:
        // MapLibre meletakkan custom marker pada titik Center (0.5, 0.5) dari bitmap.
        // Dengan menjadikan arrowTipY tepat di tengah vertikal (totalH = arrowTipY * 2),
        // maka ujung panah pin SELALU menancap 100% presisi di koordinat GPS warung,
        // baik saat pin biasa, saat ada promo diskon, maupun saat foto warung mekar di atas pin.
        // Tidak ada lagi pergeseran / membesar ke atas dan ke bawah.
        val totalH = arrowTipY * 2f

        val bitmap = Bitmap.createBitmap(
            totalW.roundToInt().coerceAtLeast(1),
            totalH.roundToInt().coerceAtLeast(1),
            Bitmap.Config.ARGB_8888
        )
        val canvas = Canvas(bitmap)

        val centerX = totalW / 2f
        val warnaUtama = if (terpilih) Color.parseColor("#EA681E") else if (buka) Color.parseColor("#EA681E") else Color.parseColor("#78716C")
        val warnaBgIcon = if (buka) Color.parseColor("#FFEDD5") else Color.parseColor("#F5F5F4")

        // ── 1. Pop-Up Foto Warung Mengambang di Atas Pin (Saat Diklik) ──────
        if (adaFoto && fotoBitmap != null) {
            val fotoLeft = centerX - fotoW / 2f
            val fotoTop = dp(3f)
            val fotoBottom = fotoTop + fotoH
            val fotoRect = RectF(fotoLeft, fotoTop, fotoLeft + fotoW, fotoBottom)

            val calloutPath = Path().apply {
                addRoundRect(fotoRect, fotoRadius, fotoRadius, Path.Direction.CW)
                // Panah segitiga menunjuk ke gelembung pin di bawahnya
                moveTo(centerX - fotoArrowW / 2f, fotoBottom - dp(0.5f))
                lineTo(centerX, fotoBottom + fotoArrowH)
                lineTo(centerX + fotoArrowW / 2f, fotoBottom - dp(0.5f))
                close()
            }

            // Latar putih dengan bayangan tebal
            val shadowPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                color = Color.WHITE
                style = Paint.Style.FILL
                setShadowLayer(dp(4f), 0f, dp(2f), Color.parseColor("#55000000"))
            }
            canvas.drawPath(calloutPath, shadowPaint)

            // Gambar foto warung dengan mode Center-Crop
            canvas.save()
            val clipPath = Path().apply {
                addRoundRect(fotoRect, fotoRadius, fotoRadius, Path.Direction.CW)
            }
            canvas.clipPath(clipPath)

            val srcW = fotoBitmap.width.toFloat()
            val srcH = fotoBitmap.height.toFloat()
            val scale = maxOf(fotoW / srcW, fotoH / srcH)
            val scaledW = srcW * scale
            val scaledH = srcH * scale
            val dx = fotoLeft + (fotoW - scaledW) / 2f
            val dy = fotoTop + (fotoH - scaledH) / 2f

            val matrix = android.graphics.Matrix().apply {
                postScale(scale, scale)
                postTranslate(dx, dy)
            }
            val bmpPaint = Paint(Paint.ANTI_ALIAS_FLAG or Paint.FILTER_BITMAP_FLAG)
            canvas.drawBitmap(fotoBitmap, matrix, bmpPaint)
            canvas.restore()

            // Border putih di sekeliling kartu foto
            val borderPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                color = Color.WHITE
                style = Paint.Style.STROKE
                strokeWidth = dp(2f)
                strokeJoin = Paint.Join.ROUND
            }
            canvas.drawPath(calloutPath, borderPaint)

            // Jika ada diskon, tampilkan badge mini diskon di sudut kiri atas foto
            if (adaDiskon) {
                val bDiskonW = diskonTextWidth + dp(14f)
                val bDiskonH = dp(17f)
                val bRect = RectF(fotoLeft + dp(4f), fotoTop + dp(4f), fotoLeft + dp(4f) + bDiskonW, fotoTop + dp(4f) + bDiskonH)
                val bPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                    color = Color.parseColor("#DC2626")
                    style = Paint.Style.FILL
                }
                canvas.drawRoundRect(bRect, dp(6f), dp(6f), bPaint)
                val bTextPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                    color = Color.WHITE
                    textSize = dp(9f)
                    typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
                    textAlign = Paint.Align.CENTER
                }
                val bTextY = bRect.centerY() - ((bTextPaint.fontMetrics.ascent + bTextPaint.fontMetrics.descent) / 2f)
                canvas.drawText(diskonTeks, bRect.centerX(), bTextY, bTextPaint)
            }
        }
        // ── 2. Pop-Up Badge Diskon Standar (Jika Tidak Ada Foto) ────────────
        else if (adaDiskon) {
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
                color = Color.parseColor("#DC2626")
                style = Paint.Style.FILL
                setShadowLayer(dp(3f), 0f, dp(1.5f), Color.parseColor("#44000000"))
            }
            canvas.drawPath(popupPath, popupFillPaint)

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

        // ── 3. Gambar Gelembung Pin Utama + Panah Lokasi Bawah ──────────────
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

        // Garis tepi / border pin (lebih tebal jika sedang terpilih)
        val strokePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = warnaUtama
            style = Paint.Style.STROKE
            strokeWidth = if (terpilih) dp(2.4f) else dp(1.8f)
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

        // Simbol ikon sesuai kategori (identik dengan ikon di profil warung)
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
                // Ikon Fastfood (Burger & Jajanan - identik Icons.Default.Fastfood)
                val bunTop = RectF(cx - half * 0.85f, cy - half * 0.75f, cx + half * 0.85f, cy + half * 0.05f)
                canvas.drawArc(bunTop, 180f, 180f, true, fillPaint)
                // Patty tengah
                val patty = RectF(cx - half * 0.9f, cy - half * 0.05f, cx + half * 0.9f, cy + half * 0.35f)
                canvas.drawRoundRect(patty, 3f, 3f, paint)
                // Roti bawah
                val bunBottom = RectF(cx - half * 0.8f, cy + half * 0.45f, cx + half * 0.8f, cy + half * 0.8f)
                canvas.drawRoundRect(bunBottom, 3f, 3f, fillPaint)
            }
            "keliling" -> {
                // Ikon Motor / TwoWheeler (identik Icons.Default.TwoWheeler)
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
                // Ikon Restaurant (Garpu & Pisau - identik Icons.Default.Restaurant)
                // Garpu kiri
                val forkX = cx - half * 0.45f
                canvas.drawLine(forkX, cy - half * 0.75f, forkX, cy + half * 0.8f, paint)
                canvas.drawLine(forkX - half * 0.25f, cy - half * 0.75f, forkX - half * 0.25f, cy - half * 0.2f, paint)
                canvas.drawLine(forkX + half * 0.25f, cy - half * 0.75f, forkX + half * 0.25f, cy - half * 0.2f, paint)
                canvas.drawLine(forkX - half * 0.25f, cy - half * 0.2f, forkX + half * 0.25f, cy - half * 0.2f, paint)
                // Pisau kanan
                val knifeX = cx + half * 0.45f
                canvas.drawLine(knifeX, cy - half * 0.75f, knifeX, cy + half * 0.8f, paint)
                val blade = Path().apply {
                    moveTo(knifeX, cy - half * 0.75f)
                    lineTo(knifeX + half * 0.28f, cy - half * 0.45f)
                    lineTo(knifeX, cy - half * 0.15f)
                    close()
                }
                canvas.drawPath(blade, fillPaint)
            }
            else -> {
                // Ikon Storefront (Etalase Toko - identik Icons.Default.Storefront)
                // Kanopi Awning Toko
                val atap = RectF(cx - half * 0.85f, cy - half * 0.75f, cx + half * 0.85f, cy - half * 0.2f)
                canvas.drawRoundRect(atap, 3f, 3f, paint)
                canvas.drawLine(cx - half * 0.3f, cy - half * 0.75f, cx - half * 0.3f, cy - half * 0.2f, paint)
                canvas.drawLine(cx + half * 0.3f, cy - half * 0.75f, cx + half * 0.3f, cy - half * 0.2f, paint)

                // Dinding & Etalase Bawah
                val dinding = RectF(cx - half * 0.75f, cy - half * 0.15f, cx + half * 0.75f, cy + half * 0.8f)
                canvas.drawRect(dinding, paint)

                val pintu = RectF(cx - half * 0.25f, cy + half * 0.2f, cx + half * 0.25f, cy + half * 0.8f)
                canvas.drawRect(pintu, fillPaint)
            }
        }
    }
}
