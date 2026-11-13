package com.carijajan.app.ui.screen

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.VolunteerActivism
import androidx.compose.material3.Icon
import androidx.compose.material3.Surface
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.sp
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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.carijajan.app.AppContainer
import com.carijajan.app.data.MenuInput
import com.carijajan.app.data.VendorInput
import com.carijajan.app.ui.components.JudulBagian
import com.carijajan.app.ui.components.KomponenFoto
import com.carijajan.app.ui.components.PesanError
import kotlinx.coroutines.launch

private val KATEGORI_LIST = listOf(
    "warung" to "Warung",
    "pkl" to "Pedagang kaki lima",
    "rumah_makan" to "Rumah makan",
    "cafe" to "Cafe",
    "keliling" to "Penjual keliling menetap"
)

/** State untuk satu baris menu pada form */
private data class MenuBaris(
    val namaMenu: String = "",
    val harga: String = "",
    val tersedia: Boolean = true
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EditWarungScreen(
    container: AppContainer,
    vendorId: String?,   // null = mode tambah, non-null = mode edit
    onSuccess: () -> Unit,
    onBack: () -> Unit
) {
    val scope = rememberCoroutineScope()
    val isEdit = vendorId != null
    val tipeAkun by container.authRepository.tipeAkun.collectAsStateWithLifecycle()

    // ── Form state ──────────────────────────────────────────────────────────────
    var namaToko by remember { mutableStateOf("") }
    var kategori by remember { mutableStateOf("warung") }
    var latStr by remember { mutableStateOf("") }
    var lngStr by remember { mutableStateOf("") }
    var alamatPatokan by remember { mutableStateOf("") }
    var jamBuka by remember { mutableStateOf("07:00") }
    var jamTutup by remember { mutableStateOf("21:00") }
    var nomorWA by remember { mutableStateOf("") }
    var layananAntar by remember { mutableStateOf(false) }
    var fotoUrl by remember { mutableStateOf("") }
    val menus = remember { mutableStateListOf<MenuBaris>() }

    // ── Foto upload state ────────────────────────────────────────────────────────
    var fotoBytes by remember { mutableStateOf<ByteArray?>(null) }
    var uploadProses by remember { mutableStateOf(false) }
    var uploadPesan by remember { mutableStateOf("") }

    // ── UI state ────────────────────────────────────────────────────────────────
    var loading by remember { mutableStateOf(false) }
    var loadingData by remember { mutableStateOf(isEdit) }
    var error by remember { mutableStateOf("") }
    var kategoriExpanded by remember { mutableStateOf(false) }

    // ── Load data saat mode edit ────────────────────────────────────────────────
    LaunchedEffect(vendorId) {
        if (vendorId == null) return@LaunchedEffect
        loadingData = true
        val res = container.vendorRepository.detail(vendorId)
        res.onSuccess { v ->
            namaToko = v.namaToko
            kategori = v.kategori
            latStr = if (v.lat == 0.0) "" else v.lat.toString()
            lngStr = if (v.lng == 0.0) "" else v.lng.toString()
            alamatPatokan = v.alamatPatokan
            jamBuka = v.jamBuka
            jamTutup = v.jamTutup
            nomorWA = v.nomorWA
            layananAntar = v.layananAntar
            fotoUrl = v.foto
            menus.clear()
            menus.addAll(v.menus.map { MenuBaris(it.namaMenu, it.harga.toLong().toString(), it.tersedia) })
        }.onFailure { error = it.message ?: "Gagal memuat data warung" }
        loadingData = false
    }

    // ── Upload foto lalu simpan ─────────────────────────────────────────────────
    fun simpan() {
        // Validasi dasar
        if (namaToko.isBlank()) { error = "Nama toko wajib diisi."; return }
        val lat = latStr.toDoubleOrNull()
        val lng = lngStr.toDoubleOrNull()
        if (lat == null || lng == null) { error = "Koordinat latitude dan longitude harus berupa angka."; return }
        if (nomorWA.isBlank()) { error = "Nomor WhatsApp wajib diisi."; return }
        if (jamBuka.isBlank() || jamTutup.isBlank()) { error = "Jam buka dan tutup wajib diisi."; return }

        loading = true
        error = ""

        scope.launch {
            // Upload foto jika ada bytes baru
            var finalFotoUrl = fotoUrl
            if (fotoBytes != null) {
                uploadProses = true
                uploadPesan = "Mengunggah foto..."
                val uploadRes = container.vendorRepository.uploadJpeg(fotoBytes!!)
                uploadProses = false
                uploadPesan = ""
                uploadRes.onSuccess { url -> finalFotoUrl = url }
                    .onFailure {
                        error = "Gagal mengunggah foto: ${it.message}. Simpan tanpa foto atau coba lagi."
                        loading = false
                        return@launch
                    }
            }

            val menuInputs = menus
                .filter { it.namaMenu.isNotBlank() }
                .map { baris ->
                    MenuInput(
                        namaMenu = baris.namaMenu.trim(),
                        harga = baris.harga.toDoubleOrNull() ?: 0.0,
                        tersedia = baris.tersedia
                    )
                }

            val input = VendorInput(
                namaToko = namaToko.trim(),
                kategori = kategori,
                lat = lat,
                lng = lng,
                alamatPatokan = alamatPatokan.trim(),
                jamBuka = jamBuka.trim(),
                jamTutup = jamTutup.trim(),
                nomorWA = nomorWA.trim(),
                layananAntar = layananAntar,
                foto = finalFotoUrl,
                menus = menuInputs
            )

            val res = if (isEdit) {
                container.vendorRepository.update(vendorId!!, input)
            } else {
                container.vendorRepository.create(input)
            }

            loading = false
            res.onSuccess { onSuccess() }
                .onFailure { error = it.message ?: "Gagal menyimpan warung." }
        }
    }

    // ── UI ───────────────────────────────────────────────────────────────────────
    if (loadingData) {
        Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                CircularProgressIndicator()
                Text("Memuat data...", modifier = Modifier.padding(top = 10.dp))
            }
        }
        return
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .statusBarsPadding()
            .navigationBarsPadding()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 16.dp, vertical = 8.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        TextButton(onClick = onBack) { Text("← Kembali") }

        Text(
            text = when {
                isEdit -> "Edit Warung"
                tipeAkun == "pembeli" -> "Ajukan Lokasi Warung"
                else -> "Daftarkan Warung Baru"
            },
            style = MaterialTheme.typography.headlineSmall,
            fontWeight = FontWeight.Bold
        )
        Text(
            text = when {
                isEdit -> "Perbarui data menu, jam buka, dan info warung Anda."
                tipeAkun == "pembeli" -> "Temukan warung UMKM tersembunyi dan bantu mereka tayang di peta!"
                else -> "Daftarkan warung atau gerobak Anda agar dapat ditemukan pembeli terdekat."
            },
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )

        // Banner informasi sesuai tipe pengguna
        if (!isEdit && tipeAkun == "pembeli") {
            Surface(
                color = Color(0xFFEFF6FF),
                shape = RoundedCornerShape(10.dp),
                modifier = Modifier.fillMaxWidth().padding(vertical = 2.dp)
            ) {
                Row(modifier = Modifier.padding(10.dp), verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Filled.VolunteerActivism, contentDescription = null, tint = Color(0xFF2563EB), modifier = Modifier.size(20.dp))
                    Spacer(Modifier.width(8.dp))
                    Column {
                        Text("Kontribusi Komunitas (+50 XP)", fontWeight = FontWeight.Bold, fontSize = 12.sp, color = Color(0xFF1E40AF))
                        Text("Warung akan diverifikasi oleh kurator admin sebelum tampil di peta publik.", fontSize = 11.sp, color = Color(0xFF3B82F6))
                    }
                }
            }
        } else if (!isEdit) {
            Surface(
                color = Color(0xFFFFFBEB),
                shape = RoundedCornerShape(10.dp),
                modifier = Modifier.fillMaxWidth().padding(vertical = 2.dp)
            ) {
                Row(modifier = Modifier.padding(10.dp), verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Filled.Info, contentDescription = null, tint = Color(0xFFD97706), modifier = Modifier.size(18.dp))
                    Spacer(Modifier.width(8.dp))
                    Column {
                        Text("Verifikasi Kurasi Awal", fontWeight = FontWeight.Bold, fontSize = 12.sp, color = Color(0xFF92400E))
                        Text("Warung baru akan masuk status 'Menunggu Verifikasi Admin' pada awalan saja.", fontSize = 11.sp, color = Color(0xFFB45309))
                    }
                }
            }
        }

        // ── Info dasar ──────────────────────────────────────────────────────────
        JudulBagian("Info dasar")

        OutlinedTextField(
            value = namaToko,
            onValueChange = { namaToko = it },
            label = { Text("Nama toko *") },
            singleLine = true,
            modifier = Modifier.fillMaxWidth()
        )

        // Dropdown kategori
        ExposedDropdownMenuBox(
            expanded = kategoriExpanded,
            onExpandedChange = { kategoriExpanded = !kategoriExpanded }
        ) {
            OutlinedTextField(
                value = KATEGORI_LIST.firstOrNull { it.first == kategori }?.second ?: kategori,
                onValueChange = {},
                readOnly = true,
                label = { Text("Kategori") },
                trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = kategoriExpanded) },
                modifier = Modifier
                    .fillMaxWidth()
                    .menuAnchor()
            )
            ExposedDropdownMenu(
                expanded = kategoriExpanded,
                onDismissRequest = { kategoriExpanded = false }
            ) {
                KATEGORI_LIST.forEach { (value, label) ->
                    DropdownMenuItem(
                        text = { Text(label) },
                        onClick = {
                            kategori = value
                            kategoriExpanded = false
                        }
                    )
                }
            }
        }

        OutlinedTextField(
            value = alamatPatokan,
            onValueChange = { alamatPatokan = it },
            label = { Text("Patokan alamat") },
            placeholder = { Text("Contoh: Depan Indomaret Jl. Kalimantan") },
            singleLine = true,
            modifier = Modifier.fillMaxWidth()
        )

        // ── Koordinat ───────────────────────────────────────────────────────────
        JudulBagian("Koordinat lokasi *")
        Text(
            "Buka Google Maps → tahan titik lokasi → salin angka latitude,longitude",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            OutlinedTextField(
                value = latStr,
                onValueChange = { latStr = it },
                label = { Text("Latitude") },
                placeholder = { Text("-8.1689") },
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                singleLine = true,
                modifier = Modifier.weight(1f)
            )
            OutlinedTextField(
                value = lngStr,
                onValueChange = { lngStr = it },
                label = { Text("Longitude") },
                placeholder = { Text("113.702") },
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                singleLine = true,
                modifier = Modifier.weight(1f)
            )
        }

        // ── Jam operasional ─────────────────────────────────────────────────────
        JudulBagian("Jam operasional *")
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            OutlinedTextField(
                value = jamBuka,
                onValueChange = { jamBuka = it },
                label = { Text("Jam buka") },
                placeholder = { Text("07:00") },
                singleLine = true,
                modifier = Modifier.weight(1f)
            )
            OutlinedTextField(
                value = jamTutup,
                onValueChange = { jamTutup = it },
                label = { Text("Jam tutup") },
                placeholder = { Text("21:00") },
                singleLine = true,
                modifier = Modifier.weight(1f)
            )
        }

        // ── Kontak & layanan ────────────────────────────────────────────────────
        JudulBagian("Kontak & layanan")

        OutlinedTextField(
            value = nomorWA,
            onValueChange = { nomorWA = it },
            label = { Text("Nomor WhatsApp *") },
            placeholder = { Text("08123456789") },
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone),
            singleLine = true,
            modifier = Modifier.fillMaxWidth()
        )

        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text("Layanan antar", modifier = Modifier.weight(1f))
            Switch(checked = layananAntar, onCheckedChange = { layananAntar = it })
        }

        // ── Foto ────────────────────────────────────────────────────────────────
        HorizontalDivider()
        KomponenFoto(
            fotoBytes = fotoBytes,
            proses = uploadProses,
            pesan = uploadPesan,
            onFotoBytes = { bytes -> fotoBytes = bytes },
            onHapus = { fotoBytes = null; fotoUrl = "" }
        )

        // ── Menu ────────────────────────────────────────────────────────────────
        HorizontalDivider()
        JudulBagian("Menu (opsional)")
        Text(
            "Tambahkan menu beserta harga aslinya. Harga tidak dipotong komisi.",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )

        menus.forEachIndexed { i, baris ->
            MenuBarisForm(
                index = i + 1,
                baris = baris,
                onChange = { menus[i] = it },
                onHapus = { menus.removeAt(i) }
            )
        }

        TextButton(
            onClick = { menus.add(MenuBaris()) },
            modifier = Modifier.fillMaxWidth()
        ) {
            Text("+ Tambah menu")
        }

        // ── Error & Simpan ──────────────────────────────────────────────────────
        if (error.isNotBlank()) {
            PesanError(error)
        }

        Spacer(Modifier.height(4.dp))

        Button(
            onClick = { simpan() },
            enabled = !loading && !uploadProses,
            modifier = Modifier.fillMaxWidth()
        ) {
            if (loading) {
                CircularProgressIndicator(modifier = Modifier.padding(end = 8.dp), strokeWidth = 2.dp)
            }
            Text(
                when {
                    uploadProses -> "Mengunggah foto..."
                    loading -> "Menyimpan..."
                    isEdit -> "Simpan Perubahan"
                    tipeAkun == "pembeli" -> "Ajukan Warung (+50 XP)"
                    else -> "Kirim & Tunggu Verifikasi"
                }
            )
        }

        Spacer(Modifier.height(16.dp))
    }
}

@Composable
private fun MenuBarisForm(
    index: Int,
    baris: MenuBaris,
    onChange: (MenuBaris) -> Unit,
    onHapus: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                "Menu $index",
                style = MaterialTheme.typography.labelMedium,
                fontWeight = FontWeight.SemiBold,
                modifier = Modifier.weight(1f)
            )
            TextButton(onClick = onHapus) {
                Text("Hapus", color = MaterialTheme.colorScheme.error)
            }
        }
        OutlinedTextField(
            value = baris.namaMenu,
            onValueChange = { onChange(baris.copy(namaMenu = it)) },
            label = { Text("Nama menu") },
            singleLine = true,
            modifier = Modifier.fillMaxWidth()
        )
        Row(
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.fillMaxWidth()
        ) {
            OutlinedTextField(
                value = baris.harga,
                onValueChange = { onChange(baris.copy(harga = it)) },
                label = { Text("Harga (Rp)") },
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                singleLine = true,
                modifier = Modifier.weight(1f)
            )
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text("Tersedia", style = MaterialTheme.typography.bodySmall)
                Switch(
                    checked = baris.tersedia,
                    onCheckedChange = { onChange(baris.copy(tersedia = it)) },
                    modifier = Modifier.padding(start = 4.dp)
                )
            }
        }
    }
}
