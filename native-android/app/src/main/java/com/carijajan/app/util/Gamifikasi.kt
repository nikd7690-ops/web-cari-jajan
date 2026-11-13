package com.carijajan.app.util

/**
 * Utilitas Gamifikasi untuk Kontributor Komunitas Cari Jajan.
 * Sesuai aturan: Level 1 dimulai dari 0 XP, setiap kelipatan 100 XP naik 1 level.
 * Setiap warung yang diajukan dan disetujui kurator admin memberikan +50 XP.
 */
object Gamifikasi {
    const val XP_PER_APPROVAL = 50
    const val XP_PER_LEVEL = 100

    fun levelDariXp(xp: Int): Int {
        if (xp <= 0) return 1
        return 1 + (xp / XP_PER_LEVEL)
    }

    /**
     * Mengembalikan progress XP dalam level saat ini (rentang 0.0f - 1.0f) untuk ProgressBar.
     */
    fun progressLevel(xp: Int): Float {
        if (xp <= 0) return 0f
        val xpDiLevelIni = xp % XP_PER_LEVEL
        return (xpDiLevelIni.toFloat() / XP_PER_LEVEL.toFloat()).coerceIn(0f, 1f)
    }

    /**
     * Gelar kehormatan kontributor berdasarkan level.
     */
    fun gelarLevel(level: Int): String = when (level) {
        1 -> "Penjelajah Jajanan"
        2 -> "Pemberi Rekomendasi"
        3 -> "Pahlawan UMKM Lokal"
        4 -> "Kolektor Kuliner Nusantara"
        else -> "Duta Jajanan Legendaris"
    }

    /**
     * Deskripsi manfaat level untuk memotivasi kontributor.
     */
    fun deskripsiGelar(level: Int): String = when (level) {
        1 -> "Baru memulai petualangan kuliner. Ajukan warung tersembunyi pertamamu!"
        2 -> "Mulai dikenal komunitas pemburu jajan. Kontribusimu sangat berharga."
        3 -> "Pahlawan nyata bagi UMKM mikro agar makin dikenal dan laku."
        4 -> "Kolektor kuliner sejati dengan rekomendasi terpercaya di peta."
        else -> "Legenda kuliner lokal dengan pengaruh besar bagi UMKM nusantara."
    }
}
