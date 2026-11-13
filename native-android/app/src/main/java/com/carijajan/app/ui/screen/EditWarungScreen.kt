package com.carijajan.app.ui.screen

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.CameraAlt
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Map
import androidx.compose.material.icons.filled.MyLocation
import androidx.compose.material.icons.filled.VolunteerActivism
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.carijajan.app.AppContainer
import com.carijajan.app.data.MenuInput
import com.carijajan.app.data.VendorInput
import com.carijajan.app.data.ZeroWasteDto
import com.carijajan.app.ui.components.JudulBagian
import com.carijajan.app.ui.components.KameraDialog
import com.carijajan.app.ui.components.KomponenFoto
import com.carijajan.app.ui.components.PesanError
import com.carijajan.app.ui.components.PilihTitikPetaDialog
import com.carijajan.app.util.Gambar
import com.carijajan.app.util.Lokasi
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
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val isEdit = vendorId != null
    val tipeAkun by container.authRepository.tipeAkun.collectAsStateWithLifecycle()
    val isPengguna = tipeAkun == "pengguna" || tipeAkun == "pembeli"
    val bisaUbahKetersediaan = !isPengguna

    // ── Zero Waste Promo ────────────────────────────────────────────────────────
    var zeroWasteAktif by remember { mutableStateOf(false) }
    var zeroWasteDiskon by remember { mutableStateOf(30) }
    var zeroWasteKet by remember { mutableStateOf("Diskon porsi lezat sebelum warung tutup") }

    // ── Form state ──────────────────────────────────────────────────────────────
    var namaToko by remember { mutableStateOf("") }
    var kategori by remember { mutableStateOf("warung") }
    var latStr by remember { mutableStateOf("") }
    var lngStr by remember { mutableStateOf("") }
    var alamatPatokan by remember { mutableStateOf("") }
    var jamBuka by remember { mutableStateOf("07:00") }
    var jamTutup by remember { mutableStateOf("21:00") }
    var adaWhatsApp by remember { mutableStateOf(true) }
    var nomorWA by remember { mutableStateOf("") }
    var layananAntar by remember { mutableStateOf(false) }
    var fotoUrl by remember { mutableStateOf("") }
    val menus = remember { mutableStateListOf<MenuBaris>() }

    // ── Pilihan 2 Mode Tambah (Kamera Langsung vs Manual) ────────────────────────
    var modeTambahKamera by remember { mutableStateOf(false) }
    var kameraWarungTerbuka by remember { mutableStateOf(false) }
    var gpsOtomatisLoading by remember { mutableStateOf(false) }
    var pesanOtomatis by remember { mutableStateOf("") }

    // ── Dialog Pilih Titik di Peta ──────────────────────────────────────────────
    var petaPickerTerbuka by remember { mutableStateOf(false) }

    // ── AI Scan Foto Menu ───────────────────────────────────────────────────────
    var kameraMenuTerbuka by remember { mutableStateOf(false) }
    var aiScanning by remember { mutableStateOf(false) }
    var aiScanPesan by remember { mutableStateOf("") }
    var aiDraftNotice by remember { mutableStateOf(false) }

    // ── Foto upload state ────────────────────────────────────────────────────────
    var fotoBytes by remember { mutableStateOf<ByteArray?>(null) }
    var uploadProses by remember { mutableStateOf(false) }
    var uploadPesan by remember { mutableStateOf("") }

    // ── UI state ────────────────────────────────────────────────────────────────
    var loading by remember { mutableStateOf(false) }
    var loadingData by remember { mutableStateOf(isEdit) }
    var error by remember { mutableStateOf("") }
    var kategoriExpanded by remember { mutableStateOf(false) }

    val cameraPermissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { granted ->
        if (granted) kameraWarungTerbuka = true
    }

    val cameraMenuPermissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { granted ->
        if (granted) kameraMenuTerbuka = true
    }

    val galleryMenuLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.PickVisualMedia()
    ) { uri ->
        if (uri != null) {
            val bytes = Gambar.kompresUri(context, uri)
            if (bytes != null) {
                // Panggil AI OCR Gemini
                scope.launch {
                    aiScanning = true
                    aiScanPesan = "Gemini AI sedang membaca menu dari foto..."
                    error = ""
                    val res = container.vendorRepository.scanMenuFoto(bytes)
                    aiScanning = false
                    aiScanPesan = ""
                    res.onSuccess { hasil ->
                        if (hasil.isNotEmpty()) {
                            menus.addAll(hasil.map {
                                MenuBaris(
                                    namaMenu = it.namaMenu,
                                    harga = it.harga?.toLong()?.toString() ?: "",
                                    tersedia = true
                                )
                            })
                            aiDraftNotice = true
                        } else {
                            error = "AI tidak mendeteksi teks menu. Silakan tambahkan menu manual."
                        }
                    }.onFailure {
                        error = "Gagal memindai foto: ${it.message}. Silakan tambahkan menu manual."
                    }
                }
            }
        }
    }

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
            adaWhatsApp = v.nomorWA.isNotBlank()
            layananAntar = v.layananAntar
            fotoUrl = v.foto
            v.zeroWaste?.let {
                zeroWasteAktif = it.aktif
                zeroWasteDiskon = it.diskonPersen
                zeroWasteKet = it.keterangan.ifBlank { zeroWasteKet }
            }
            menus.clear()
            menus.addAll(v.menus.map { MenuBaris(it.namaMenu, it.harga.toLong().toString(), it.tersedia) })
        }.onFailure { error = it.message ?: "Gagal memuat data warung" }
        loadingData = false
    }

    // ── Helper ambil GPS + Alamat Otomatis ───────────────────────────────────────
    fun isiGpsDanAlamatOtomatis() {
        gpsOtomatisLoading = true
        scope.launch {
            val loc = Lokasi.ambil(context)
            if (loc != null) {
                latStr = loc.latitude.toString()
                lngStr = loc.longitude.toString()
                val namaArea = Lokasi.dapatkanNamaArea(context, loc.latitude, loc.longitude)
                if (alamatPatokan.isBlank()) {
                    alamatPatokan = namaArea
                }
                pesanOtomatis = "Koordinat GPS ($namaArea) berhasil terisi otomatis!"
            } else {
                error = "Gagal mendeteksi sinyal GPS. Pastikan izin lokasi aktif."
            }
            gpsOtomatisLoading = false
        }
    }

    // ── Upload foto lalu simpan ─────────────────────────────────────────────────
    fun simpan() {
        if (isEdit && isPengguna) {
            error = "Pengguna/member tidak memiliki izin untuk mengubah data warung. Data yang diajukan hanya dikurasi dan dikonfirmasi oleh admin."
            return
        }
        if (namaToko.isBlank()) { error = "Nama toko wajib diisi."; return }
        val lat = latStr.toDoubleOrNull()
        val lng = lngStr.toDoubleOrNull()
        if (lat == null || lng == null) {
            error = "Koordinat lokasi wajib diisi (gunakan GPS atau pilih titik di peta)."
            return
        }
        if (adaWhatsApp && nomorWA.isBlank()) {
            error = "Nomor WhatsApp wajib diisi jika opsi WhatsApp aktif, atau matikan opsi WhatsApp."
            return
        }
        if (jamBuka.isBlank() || jamTutup.isBlank()) { error = "Jam buka dan tutup wajib diisi."; return }

        loading = true
        error = ""

        scope.launch {
            // Ambil titik GPS fisik perangkat saat ini untuk validasi Proof of Presence (maksimal 50 meter)
            var deviceLoc = Lokasi.ambil(context)
            if (!isEdit && deviceLoc != null) {
                val jarakMeter = Lokasi.hitungJarakKm(
                    deviceLoc.latitude,
                    deviceLoc.longitude,
                    lat,
                    lng
                ) * 1000.0

                if (jarakMeter > 50.0) {
                    loading = false
                    error = "Proof of Presence Gagal: Posisi Anda berada sejauh ${jarakMeter.toInt()} meter dari titik warung. Anda wajib berada maksimal 50 meter dari lokasi fisik warung untuk mendaftarkannya."
                    return@launch
                }
            }

            // Upload foto jika ada bytes baru
            var finalFotoUrl = fotoUrl
            if (fotoBytes != null) {
                uploadProses = true
                uploadPesan = "Mengunggah foto warung..."
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
                        tersedia = if (bisaUbahKetersediaan) baris.tersedia else true
                    )
                }

            val input = VendorInput(
                namaToko = namaToko.trim(),
                kategori = kategori,
                lat = lat,
                lng = lng,
                alamatPatokan = alamatPatokan.trim().ifBlank { "Lokasi terdaftar via Cari Jajan" },
                jamBuka = jamBuka.trim(),
                jamTutup = jamTutup.trim(),
                nomorWA = if (adaWhatsApp) nomorWA.trim() else "",
                layananAntar = layananAntar,
                foto = finalFotoUrl,
                menus = menuInputs,
                userLat = deviceLoc?.latitude ?: lat,
                userLng = deviceLoc?.longitude ?: lng,
                zeroWaste = ZeroWasteDto(
                    aktif = zeroWasteAktif,
                    diskonPersen = zeroWasteDiskon,
                    keterangan = zeroWasteKet.trim()
                )
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

    // ── UI Loading ──────────────────────────────────────────────────────────────
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

        // Banner peringatan jika mode edit dibuka oleh Pengguna / Member
        if (isEdit && isPengguna) {
            Surface(
                color = Color(0xFFFEF2F2),
                shape = RoundedCornerShape(10.dp),
                border = BorderStroke(1.dp, Color(0xFFFECACA)),
                modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp)
            ) {
                Row(modifier = Modifier.padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Filled.Warning, contentDescription = null, tint = Color(0xFFDC2626), modifier = Modifier.size(24.dp))
                    Spacer(Modifier.width(10.dp))
                    Column {
                        Text("Hak Akses Terbatas (Pengguna/Member)", fontWeight = FontWeight.Bold, fontSize = 13.sp, color = Color(0xFF991B1B))
                        Text(
                            "Akun Pengguna/Member hanya dapat mengajukan warung baru. Anda tidak dapat mengubah data warung karena kurasi dilakukan langsung oleh admin.",
                            fontSize = 11.sp,
                            color = Color(0xFFB91C1C),
                            lineHeight = 15.sp
                        )
                    }
                }
            }
        }

        // Banner informasi sesuai tipe pengguna
        if (!isEdit && isPengguna) {
            Surface(
                color = Color(0xFFEFF6FF),
                shape = RoundedCornerShape(10.dp),
                modifier = Modifier.fillMaxWidth().padding(vertical = 2.dp)
            ) {
                Row(modifier = Modifier.padding(10.dp), verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Filled.VolunteerActivism, contentDescription = null, tint = Color(0xFF2563EB), modifier = Modifier.size(20.dp))
                    Spacer(Modifier.width(8.dp))
                    Column {
                        Text("Kontribusi Komunitas Bebas (+50 XP)", fontWeight = FontWeight.Bold, fontSize = 12.sp, color = Color(0xFF1E40AF))
                        Text("Ajukan warung baru dengan bebas. Admin akan mengurasi & mengonfirmasi data sebelum tayang.", fontSize = 11.sp, color = Color(0xFF3B82F6))
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
                        Text("Mitra Warung Resmi (Batas 1 Warung)", fontWeight = FontWeight.Bold, fontSize = 12.sp, color = Color(0xFF92400E))
                        Text("Sebagai mitra, Anda dapat mengelola 1 warung utama beserta fitur diskon Zero Waste Food.", fontSize = 11.sp, color = Color(0xFFB45309))
                    }
                }
            }
        }

        // ── 2 Pilihan Metode Tambah: Kamera Langsung vs Manual (Khusus Tambah Baru) ──
        if (!isEdit) {
            JudulBagian("Pilih Cara Tambah Warung")
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Surface(
                    shape = RoundedCornerShape(12.dp),
                    color = if (modeTambahKamera) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                    border = if (modeTambahKamera) androidx.compose.foundation.BorderStroke(1.5.dp, MaterialTheme.colorScheme.primary) else null,
                    modifier = Modifier
                        .weight(1f)
                        .clickable {
                            modeTambahKamera = true
                            if (ContextCompat.checkSelfPermission(context, Manifest.permission.CAMERA) == PackageManager.PERMISSION_GRANTED) {
                                kameraWarungTerbuka = true
                            } else {
                                cameraPermissionLauncher.launch(Manifest.permission.CAMERA)
                            }
                        }
                ) {
                    Column(
                        modifier = Modifier.padding(12.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Icon(
                            imageVector = Icons.Default.CameraAlt,
                            contentDescription = null,
                            tint = if (modeTambahKamera) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.size(28.dp)
                        )
                        Spacer(Modifier.height(4.dp))
                        Text(
                            text = "Langsung Kamera",
                            fontWeight = FontWeight.Bold,
                            fontSize = 13.sp,
                            color = if (modeTambahKamera) MaterialTheme.colorScheme.onPrimaryContainer else MaterialTheme.colorScheme.onSurface
                        )
                        Text(
                            text = "Foto warung → GPS & alamat otomatis",
                            fontSize = 10.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            textAlign = androidx.compose.ui.text.style.TextAlign.Center
                        )
                    }
                }

                Surface(
                    shape = RoundedCornerShape(12.dp),
                    color = if (!modeTambahKamera) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                    border = if (!modeTambahKamera) androidx.compose.foundation.BorderStroke(1.5.dp, MaterialTheme.colorScheme.primary) else null,
                    modifier = Modifier
                        .weight(1f)
                        .clickable { modeTambahKamera = false }
                ) {
                    Column(
                        modifier = Modifier.padding(12.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Icon(
                            imageVector = Icons.Default.Edit,
                            contentDescription = null,
                            tint = if (!modeTambahKamera) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.size(28.dp)
                        )
                        Spacer(Modifier.height(4.dp))
                        Text(
                            text = "Input Manual",
                            fontWeight = FontWeight.Bold,
                            fontSize = 13.sp,
                            color = if (!modeTambahKamera) MaterialTheme.colorScheme.onPrimaryContainer else MaterialTheme.colorScheme.onSurface
                        )
                        Text(
                            text = "Ketik bertahap & pilih titik peta",
                            fontSize = 10.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            textAlign = androidx.compose.ui.text.style.TextAlign.Center
                        )
                    }
                }
            }

            if (pesanOtomatis.isNotBlank()) {
                Surface(
                    color = Color(0xFFECFDF5),
                    shape = RoundedCornerShape(8.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier.padding(8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(Icons.Default.CheckCircle, contentDescription = null, tint = Color(0xFF059669), modifier = Modifier.size(16.dp))
                        Spacer(Modifier.width(6.dp))
                        Text(pesanOtomatis, fontSize = 11.sp, color = Color(0xFF065F46), fontWeight = FontWeight.Medium)
                    }
                }
            }
        }

        // ── Info Dasar ──────────────────────────────────────────────────────────
        JudulBagian("Info Dasar")

        OutlinedTextField(
            value = namaToko,
            onValueChange = { namaToko = it },
            label = { Text("Nama toko / warung *") },
            placeholder = { Text("Contoh: Nasi Goreng Pak Slamet") },
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
            label = { Text("Patokan Alamat") },
            placeholder = { Text("Contoh: Depan Indomaret Jl. Kalimantan, Sumbersari") },
            singleLine = true,
            modifier = Modifier.fillMaxWidth()
        )

        // ── Koordinat Lokasi & Pilihan Titik Manual ─────────────────────────────
        JudulBagian("Titik Lokasi Warung *")
        Text(
            "Gunakan lokasi GPS saat ini atau pilih titik langsung di peta interaktif.",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Button(
                onClick = { isiGpsDanAlamatOtomatis() },
                enabled = !gpsOtomatisLoading,
                modifier = Modifier.weight(1f)
            ) {
                if (gpsOtomatisLoading) {
                    CircularProgressIndicator(modifier = Modifier.size(16.dp), strokeWidth = 2.dp)
                    Spacer(Modifier.width(6.dp))
                } else {
                    Icon(imageVector = Icons.Default.MyLocation, contentDescription = null, modifier = Modifier.size(18.dp))
                    Spacer(Modifier.width(6.dp))
                }
                Text("Isi GPS Saya", fontSize = 12.sp)
            }

            OutlinedButton(
                onClick = { petaPickerTerbuka = true },
                modifier = Modifier.weight(1f)
            ) {
                Icon(imageVector = Icons.Default.Map, contentDescription = null, modifier = Modifier.size(18.dp))
                Spacer(Modifier.width(6.dp))
                Text("Pilih di Peta", fontSize = 12.sp)
            }
        }

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

        // Badge Proof of Presence
        Surface(
            color = Color(0xFFF0FDF4),
            shape = RoundedCornerShape(8.dp),
            border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFFBBF7D0)),
            modifier = Modifier.fillMaxWidth()
        ) {
            Row(
                modifier = Modifier.padding(10.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    imageVector = Icons.Filled.CheckCircle,
                    contentDescription = null,
                    tint = Color(0xFF16A34A),
                    modifier = Modifier.size(18.dp)
                )
                Spacer(Modifier.width(8.dp))
                Text(
                    text = "Keamanan Lokasi: Sistem Proof of Presence aktif. Pengajuan warung wajib berada maksimal 50 meter dari titik fisik GPS perangkat Anda.",
                    fontSize = 11.sp,
                    color = Color(0xFF15803D),
                    lineHeight = 15.sp
                )
            }
        }

        // ── Jam Operasional ─────────────────────────────────────────────────────
        JudulBagian("Jam Operasional *")
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

        // ── Opsi WhatsApp (Bisa Dihidupkan / Dimatikan) ──────────────────────────
        JudulBagian("Kontak WhatsApp & Pemesanan")

        Surface(
            shape = RoundedCornerShape(12.dp),
            color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(modifier = Modifier.padding(12.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "Sediakan Nomor WhatsApp",
                            fontWeight = FontWeight.Bold,
                            fontSize = 13.sp
                        )
                        Text(
                            text = if (adaWhatsApp) "Pembeli dapat memesan via WhatsApp" else "Warung hanya melayani beli langsung di tempat",
                            fontSize = 11.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    Switch(
                        checked = adaWhatsApp,
                        onCheckedChange = {
                            adaWhatsApp = it
                            if (!it) nomorWA = ""
                        }
                    )
                }

                if (adaWhatsApp) {
                    Spacer(Modifier.height(8.dp))
                    OutlinedTextField(
                        value = nomorWA,
                        onValueChange = { nomorWA = it },
                        label = { Text("Nomor WhatsApp *") },
                        placeholder = { Text("08123456789") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone),
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            }
        }

        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text("Layanan antar penjual", modifier = Modifier.weight(1f))
            Switch(checked = layananAntar, onCheckedChange = { layananAntar = it })
        }

        // ── Foto Warung ─────────────────────────────────────────────────────────
        HorizontalDivider()
        KomponenFoto(
            fotoBytes = fotoBytes,
            proses = uploadProses,
            pesan = uploadPesan,
            onFotoBytes = { bytes -> fotoBytes = bytes },
            onHapus = { fotoBytes = null; fotoUrl = "" }
        )

        // ── Bagian Menu & Scan Foto AI ──────────────────────────────────────────
        HorizontalDivider()
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            JudulBagian("Menu Makanan & Minuman")
        }

        Text(
            "Tambahkan menu secara manual atau gunakan AI untuk memindai daftar harga dari foto.",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )

        // Tombol Aksi Tambah Menu & Scan Foto AI
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            OutlinedButton(
                onClick = { menus.add(MenuBaris()) },
                modifier = Modifier.weight(1f)
            ) {
                Text("+ Menu Manual", fontSize = 12.sp)
            }

            Button(
                onClick = {
                    if (ContextCompat.checkSelfPermission(context, Manifest.permission.CAMERA) == PackageManager.PERMISSION_GRANTED) {
                        kameraMenuTerbuka = true
                    } else {
                        cameraMenuPermissionLauncher.launch(Manifest.permission.CAMERA)
                    }
                },
                enabled = !aiScanning,
                modifier = Modifier.weight(1f)
            ) {
                Icon(Icons.Default.AutoAwesome, contentDescription = null, modifier = Modifier.size(16.dp))
                Spacer(Modifier.width(6.dp))
                Text("Scan Foto AI", fontSize = 12.sp)
            }
        }

        TextButton(
            onClick = {
                galleryMenuLauncher.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly))
            },
            modifier = Modifier.align(Alignment.CenterHorizontally)
        ) {
            Text("Atau pilih foto daftar menu dari galeri", fontSize = 11.sp)
        }

        if (aiScanning) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier.padding(vertical = 4.dp)
            ) {
                CircularProgressIndicator(modifier = Modifier.size(18.dp), strokeWidth = 2.dp)
                Text(aiScanPesan, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.primary)
            }
        }

        // Label Draf AI Sesuai Aturan AGENTS.md
        if (aiDraftNotice) {
            Surface(
                color = Color(0xFFFEF3C7),
                shape = RoundedCornerShape(10.dp),
                modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp)
            ) {
                Row(modifier = Modifier.padding(10.dp), verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.Warning, contentDescription = null, tint = Color(0xFFD97706), modifier = Modifier.size(20.dp))
                    Spacer(Modifier.width(8.dp))
                    Column {
                        Text("Draf hasil AI, periksa dulu!", fontWeight = FontWeight.Bold, fontSize = 12.sp, color = Color(0xFF92400E))
                        Text("Silakan periksa atau sesuaikan nama menu dan harga hasil scan sebelum disimpan.", fontSize = 11.sp, color = Color(0xFFB45309))
                    }
                }
            }
        }

        // Daftar Item Menu
        menus.forEachIndexed { i, baris ->
            MenuBarisForm(
                index = i + 1,
                baris = baris,
                bisaUbahKetersediaan = bisaUbahKetersediaan,
                onChange = { menus[i] = it },
                onHapus = { menus.removeAt(i) }
            )
        }

        // ── Fitur Khusus Mitra: Zero Waste Food (Diskon Sewaktu-waktu) ───────
        if (!isPengguna) {
            JudulBagian("🌿 Zero Waste Food (Penyelamat Makanan)")
            Surface(
                shape = RoundedCornerShape(12.dp),
                color = Color(0xFFFFFBEB),
                border = BorderStroke(1.dp, Color(0xFFFDE68A)),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text("Promo Diskon Zero Waste", fontWeight = FontWeight.Bold, fontSize = 13.sp, color = Color(0xFF92400E))
                            Text("Beri diskon mendadak sewaktu-waktu untuk sisa porsi sebelum warung tutup.", fontSize = 11.sp, color = Color(0xFFB45309))
                        }
                        Switch(
                            checked = zeroWasteAktif,
                            onCheckedChange = { zeroWasteAktif = it }
                        )
                    }

                    if (zeroWasteAktif) {
                        HorizontalDivider(color = Color(0xFFFDE68A))
                        Text("Pilih Besaran Diskon:", fontSize = 12.sp, fontWeight = FontWeight.SemiBold, color = Color(0xFF92400E))
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            listOf(20, 30, 50).forEach { disc ->
                                Surface(
                                    shape = RoundedCornerShape(8.dp),
                                    color = if (zeroWasteDiskon == disc) Color(0xFFD97706) else Color.White,
                                    border = BorderStroke(1.dp, if (zeroWasteDiskon == disc) Color(0xFFD97706) else Color(0xFFFDE68A)),
                                    modifier = Modifier.clickable { zeroWasteDiskon = disc }
                                ) {
                                    Text(
                                        text = "-$disc%",
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 12.sp,
                                        color = if (zeroWasteDiskon == disc) Color.White else Color(0xFF92400E),
                                        modifier = Modifier.padding(horizontal = 14.dp, vertical = 6.dp)
                                    )
                                }
                            }
                        }

                        OutlinedTextField(
                            value = zeroWasteKet,
                            onValueChange = { zeroWasteKet = it },
                            label = { Text("Keterangan Promo") },
                            placeholder = { Text("Contoh: Promo jelang tutup porsi lezat diskon 30%") },
                            singleLine = true,
                            modifier = Modifier.fillMaxWidth()
                        )
                    }
                }
            }
        }

        // ── Error & Simpan ──────────────────────────────────────────────────────
        if (error.isNotBlank()) {
            PesanError(error)
        }

        Spacer(Modifier.height(4.dp))

        Button(
            onClick = { simpan() },
            enabled = !loading && !uploadProses && !aiScanning && !(isEdit && isPengguna),
            modifier = Modifier.fillMaxWidth()
        ) {
            if (loading) {
                CircularProgressIndicator(modifier = Modifier.padding(end = 8.dp), strokeWidth = 2.dp)
            }
            Text(
                when {
                    uploadProses -> "Mengunggah foto..."
                    loading -> "Menyimpan..."
                    isEdit && isPengguna -> "Pengguna Tidak Bisa Mengubah Warung"
                    isEdit -> "Simpan Perubahan"
                    isPengguna -> "Ajukan Warung (+50 XP)"
                    else -> "Kirim & Tunggu Verifikasi"
                }
            )
        }

        Spacer(Modifier.height(16.dp))
    }

    // ── Dialog Kamera Langsung Tambah Warung (Foto + Auto GPS) ────────────────
    if (kameraWarungTerbuka) {
        KameraDialog(
            onDismiss = { kameraWarungTerbuka = false },
            onBytes = { bytes ->
                kameraWarungTerbuka = false
                fotoBytes = bytes
                // Otomatis isi GPS dan Alamat
                isiGpsDanAlamatOtomatis()
            }
        )
    }

    // ── Dialog Kamera Scan Daftar Menu (AI Gemini OCR) ────────────────────────
    if (kameraMenuTerbuka) {
        KameraDialog(
            onDismiss = { kameraMenuTerbuka = false },
            onBytes = { bytes ->
                kameraMenuTerbuka = false
                scope.launch {
                    aiScanning = true
                    aiScanPesan = "Gemini AI sedang membaca daftar menu & harga dari foto..."
                    error = ""
                    val res = container.vendorRepository.scanMenuFoto(bytes)
                    aiScanning = false
                    aiScanPesan = ""
                    res.onSuccess { hasil ->
                        if (hasil.isNotEmpty()) {
                            menus.addAll(hasil.map {
                                MenuBaris(
                                    namaMenu = it.namaMenu,
                                    harga = it.harga?.toLong()?.toString() ?: "",
                                    tersedia = true
                                )
                            })
                            aiDraftNotice = true
                        } else {
                            error = "AI tidak mendeteksi teks menu. Silakan tambahkan menu manual."
                        }
                    }.onFailure {
                        error = "Gagal memindai foto: ${it.message}. Silakan tambahkan menu manual."
                    }
                }
            }
        )
    }

    // ── Dialog Interaktif Pilih Titik di Peta ────────────────────────────────
    if (petaPickerTerbuka) {
        PilihTitikPetaDialog(
            initialLat = latStr.toDoubleOrNull(),
            initialLng = lngStr.toDoubleOrNull(),
            onPilih = { lat, lng, alamat ->
                latStr = lat.toString()
                lngStr = lng.toString()
                alamatPatokan = alamat
                pesanOtomatis = "Titik lokasi berhasil dipilih dari peta!"
            },
            onDismiss = { petaPickerTerbuka = false }
        )
    }
}

@Composable
private fun MenuBarisForm(
    index: Int,
    baris: MenuBaris,
    bisaUbahKetersediaan: Boolean,
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
            placeholder = { Text("Contoh: Es Teh Manis") },
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
                placeholder = { Text("5000") },
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                singleLine = true,
                modifier = Modifier.weight(1f)
            )

            if (bisaUbahKetersediaan) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text("Tersedia", style = MaterialTheme.typography.bodySmall)
                    Switch(
                        checked = baris.tersedia,
                        onCheckedChange = { onChange(baris.copy(tersedia = it)) },
                        modifier = Modifier.padding(start = 4.dp)
                    )
                }
            } else {
                Surface(
                    color = Color(0xFFF1F5F9),
                    shape = RoundedCornerShape(6.dp),
                    modifier = Modifier.padding(start = 4.dp)
                ) {
                    Text(
                        text = "Tersedia",
                        fontSize = 11.sp,
                        color = Color(0xFF475569),
                        fontWeight = FontWeight.Medium,
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 6.dp)
                    )
                }
            }
        }
    }
}
