package com.carijajan.app.util

import android.content.Context
import android.content.Intent
import android.net.Uri

object WhatsApp {
    fun open(context: Context, nomor: String, teks: String) {
        val normalized = Keranjang.normalisasiNomorWA(nomor)
        val url = "https://wa.me/$normalized?text=${Uri.encode(teks)}"
        val intent = Intent(Intent.ACTION_VIEW, Uri.parse(url))
            .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        try {
            context.startActivity(intent)
        } catch (_: Exception) {
            val fallback = Intent(Intent.ACTION_VIEW, Uri.parse(url))
                .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            context.startActivity(fallback)
        }
    }
}
