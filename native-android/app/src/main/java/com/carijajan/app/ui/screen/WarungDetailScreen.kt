package com.carijajan.app.ui.screen

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FilterChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import coil3.compose.AsyncImage
import com.carijajan.app.AppContainer
import com.carijajan.app.data.MenuDto
import com.carijajan.app.data.VendorDto
import com.carijajan.app.ui.components.PesanError
import com.carijajan.app.util.CartItem
import com.carijajan.app.util.Format
import com.carijajan.app.util.Keranjang
import com.carijajan.app.util.WhatsApp
import kotlinx.coroutines.launch

@Composable
fun WarungDetailScreen(
    container: AppContainer,
    vendorId: String,
    onBack: () -> Unit
) {
    var vendor by remember { mutableStateOf<VendorDto?>(null) }
    var loading by remember { mutableStateOf(true) }
    var error by remember { mutableStateOf("") }
    val cart = remember { mutableStateListOf<CartItem>() }
    var mode by remember { mutableStateOf("Ambil sendiri") }
    val context = LocalContext.current
    val scope = rememberCoroutineScope()

    LaunchedEffect(vendorId) {
        loading = true
        val res = container.vendorRepository.detail(vendorId)
        res.onSuccess {
            vendor = it
            mode = if (it.layananAntar) "Diantar" else "Ambil sendiri"
        }.onFailure { error = it.message ?: "Gagal mengambil data warung" }
        loading = false
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .statusBarsPadding()
            .padding(horizontal = 16.dp)
    ) {
        TextButton(onClick = onBack) { Text("← Kembali") }

        if (loading) {
            Column(
                modifier = Modifier.fillMaxSize(),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center
            ) {
                CircularProgressIndicator()
                Text("Memuat warung...", modifier = Modifier.padding(top = 10.dp))
            }
            return
        }

        val toko = vendor
        if (toko == null) {
            PesanError(error.ifBlank { "Warung tidak ditemukan" })
            return
        }

        LazyColumn(
            modifier = Modifier.weight(1f),
            verticalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            if (toko.foto.isNotBlank()) {
                item {
                    AsyncImage(
                        model = toko.foto,
                        contentDescription = "Foto ${toko.namaToko}",
                        modifier = Modifier.fillMaxWidth().padding(bottom = 8.dp)
                    )
                }
            }
            item {
                Column(modifier = Modifier.padding(bottom = 10.dp)) {
                    Text(toko.namaToko, style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold)
                    Text(
                        "${Format.labelKategori(toko.kategori)} · ${toko.alamatPatokan}",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            text = if (toko.buka) "Buka" else "Tutup",
                            color = if (toko.buka) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.error,
                            fontWeight = FontWeight.SemiBold
                        )
                        Spacer(Modifier.width(10.dp))
                        Text("Jam ${toko.jamBuka}-${toko.jamTutup}", style = MaterialTheme.typography.labelSmall)
                    }
                    if (!toko.buka) {
                        Text(
                            "Warung sedang tutup, pesanan mungkin baru diproses saat buka.",
                            color = MaterialTheme.colorScheme.error,
                            style = MaterialTheme.typography.bodySmall
                        )
                    }
                    Text("Menu", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.SemiBold, modifier = Modifier.padding(top = 12.dp))
                }
            }
            if (toko.menus.isEmpty()) {
                item { Text("Belum ada menu.", color = MaterialTheme.colorScheme.onSurfaceVariant) }
            }
            items(toko.menus) { menu ->
                MenuRow(
                    menu = menu,
                    cartItem = cart.firstOrNull { it.id == menu.id.orEmpty() },
                    onAdd = {
                        if (menu.id != null) {
                            val updated = Keranjang.tambahItem(cart, menu.id, menu.namaMenu, menu.harga)
                            cart.clear()
                            cart.addAll(updated)
                        }
                    },
                    onRemove = {
                        if (menu.id != null) {
                            val updated = Keranjang.kurangItem(cart, menu.id)
                            cart.clear()
                            cart.addAll(updated)
                        }
                    }
                )
            }
            item {
                Column(modifier = Modifier.padding(vertical = 10.dp)) {
                    Text("Opsi pengambilan", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.SemiBold)
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.padding(top = 6.dp)) {
                        if (toko.layananAntar) {
                            FilterChip(selected = mode == "Diantar", onClick = { mode = "Diantar" }, label = { Text("Diantar") })
                        }
                        FilterChip(selected = mode == "Ambil sendiri", onClick = { mode = "Ambil sendiri" }, label = { Text("Ambil sendiri") })
                    }
                }
            }
        }

        Surface(tonalElevation = 4.dp, modifier = Modifier.fillMaxWidth()) {
            Column(modifier = Modifier.padding(vertical = 10.dp)) {
                Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                    Text("${Keranjang.hitungJumlahItem(cart)} item", modifier = Modifier.weight(1f))
                    Text("Total: ${Format.rupiah(Keranjang.hitungTotal(cart))}", fontWeight = FontWeight.SemiBold)
                }
                Button(
                    onClick = {
                        val teks = Keranjang.buatTeksPesanan(toko.namaToko, cart, mode)
                        WhatsApp.open(context, toko.nomorWA, teks)
                    },
                    enabled = cart.isNotEmpty(),
                    modifier = Modifier.fillMaxWidth().padding(top = 8.dp)
                ) {
                    Text(if (cart.isEmpty()) "Pilih menu dulu" else "Pesan via WhatsApp")
                }
            }
        }
    }
}

@Composable
private fun MenuRow(
    menu: MenuDto,
    cartItem: CartItem?,
    onAdd: () -> Unit,
    onRemove: () -> Unit
) {
    Surface(
        color = if (menu.tersedia) MaterialTheme.colorScheme.surface else MaterialTheme.colorScheme.surfaceVariant,
        shape = MaterialTheme.shapes.medium,
        tonalElevation = 1.dp,
        modifier = Modifier.fillMaxWidth().padding(vertical = 3.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    menu.namaMenu,
                    fontWeight = FontWeight.Medium,
                    color = if (menu.tersedia) MaterialTheme.colorScheme.onSurface else MaterialTheme.colorScheme.onSurfaceVariant
                )
                Text(
                    Format.rupiah(menu.harga) + if (menu.tersedia) "" else " · habis",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            OutlinedButton(onClick = onRemove, enabled = cartItem != null) { Text("−") }
            Text(
                text = "${cartItem?.qty ?: 0}",
                modifier = Modifier.padding(horizontal = 8.dp),
                fontWeight = FontWeight.SemiBold
            )
            OutlinedButton(onClick = onAdd, enabled = menu.tersedia) { Text("+") }
        }
    }
}
