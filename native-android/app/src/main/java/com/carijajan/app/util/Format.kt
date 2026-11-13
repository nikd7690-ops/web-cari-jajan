package com.carijajan.app.util

import java.text.NumberFormat
import java.util.Locale
import kotlin.math.roundToLong

object Format {
    private val rupiahFormat = NumberFormat.getNumberInstance(Locale("id", "ID"))

    fun rupiah(angka: Double): String = "Rp " + rupiahFormat.format(angka.roundToLong())

    fun jarak(meter: Double?): String {
        if (meter == null || meter <= 0) return ""
        return if (meter < 1000) {
            "${meter.roundToLong()} m"
        } else {
            String.format(Locale.US, "%.1f km", meter / 1000.0)
        }
    }

    fun labelKategori(value: String): String = when (value) {
        "pkl" -> "Pedagang kaki lima"
        "warung" -> "Warung"
        "rumah_makan", "cafe" -> "Rumah makan"
        "keliling" -> "Penjual keliling menetap"
        else -> value
    }

    fun statusLabel(status: String): String = when (status) {
        "approved" -> "Tayang"
        "rejected" -> "Ditolak"
        else -> "Menunggu kurasi"
    }
}
