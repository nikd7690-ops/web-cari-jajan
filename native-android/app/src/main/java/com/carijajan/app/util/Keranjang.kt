package com.carijajan.app.util

data class CartItem(
    val id: String,
    val namaMenu: String,
    val harga: Double,
    val qty: Int
)

object Keranjang {
    fun cariItem(keranjang: List<CartItem>, id: String): CartItem? =
        keranjang.firstOrNull { it.id == id }

    fun tambahItem(keranjang: List<CartItem>, id: String, namaMenu: String, harga: Double): List<CartItem> {
        val ada = cariItem(keranjang, id)
        return if (ada != null) {
            keranjang.map { if (it.id == id) it.copy(qty = it.qty + 1) else it }
        } else {
            keranjang + CartItem(id = id, namaMenu = namaMenu, harga = harga, qty = 1)
        }
    }

    fun kurangItem(keranjang: List<CartItem>, id: String): List<CartItem> =
        keranjang.map { if (it.id == id) it.copy(qty = it.qty - 1) else it }
            .filter { it.qty > 0 }

    fun hitungJumlahItem(keranjang: List<CartItem>): Int =
        keranjang.sumOf { it.qty }

    fun hitungTotal(keranjang: List<CartItem>): Double =
        keranjang.sumOf { it.harga * it.qty }

    fun normalisasiNomorWA(nomor: String): String {
        val digit = nomor.filter { it.isDigit() }
        return when {
            digit.startsWith("0") -> "62" + digit.drop(1)
            else -> digit
        }
    }

    fun buatTeksPesanan(namaToko: String, keranjang: List<CartItem>, mode: String): String {
        val baris = keranjang.mapIndexed { index, item ->
            "${index + 1}. ${item.namaMenu} x${item.qty} = ${Format.rupiah(item.harga * item.qty)}"
        }
        return buildList {
            add("Halo, saya mau pesan di $namaToko:")
            add("")
            addAll(baris)
            add("")
            add("Total: ${Format.rupiah(hitungTotal(keranjang))}")
            add("Opsi: $mode")
            add("")
            add("Terima kasih.")
        }.joinToString("\n")
    }
}
