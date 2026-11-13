package com.carijajan.app.ui.screen

import android.Manifest
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AddCircle
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Explore
import androidx.compose.material.icons.filled.Map
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.outlined.LocationOn
import androidx.compose.material.icons.outlined.Map
import androidx.compose.material.icons.outlined.Person
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.carijajan.app.AppContainer
import com.carijajan.app.data.VendorDto
import com.carijajan.app.ui.components.PetaWarung
import com.carijajan.app.ui.components.PilihAreaSheet
import com.carijajan.app.util.Format
import com.carijajan.app.util.Lokasi
import com.carijajan.app.util.Titik
import kotlinx.coroutines.launch
import org.maplibre.android.geometry.LatLng

// Titik referensi pusat UMKM Jember agar semua warung contoh selalu ikut termuat
private val JEMBER_PUSAT = LatLng(-8.1689, 113.702)
private val INDONESIA = LatLng(-2.5, 118.0)
private const val RADIUS_MAKS = 10000 // 10 km (maksimum yang didukung backend)

private val DAFTAR_KATEGORI = listOf(
    "semua" to "Semua",
    "pkl" to "Gerobak PKL",
    "keliling" to "Keliling (Motor)",
    "warung" to "Warung Tetap",
    "rumah_makan" to "Rumah Makan"
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun BerandaScreen(
    container: AppContainer,
    onLogin: () -> Unit,
    onDashboard: () -> Unit,
    onAdd: () -> Unit,
    onVendor: (String) -> Unit
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val user by container.authRepository.user.collectAsStateWithLifecycle()

    var lokasi by remember { mutableStateOf<Titik?>(null) }
    // Master list menampung semua warung (lokal GPS + warung kawasan lain) agar tidak hilang saat zoom out
    var masterVendors by remember { mutableStateOf<Map<String, VendorDto>>(emptyMap()) }
    var loading by remember { mutableStateOf(false) }
    var error by remember { mutableStateOf("") }
    var query by remember { mutableStateOf("") }
    var kategoriFilter by remember { mutableStateOf("semua") }
    var areaTerbuka by remember { mutableStateOf(false) }
    var daftarWarungTerbuka by remember { mutableStateOf(false) }
    var geoLoading by remember { mutableStateOf(false) }
    var pesanLokasi by remember { mutableStateOf("") }
    var bearingResetTrigger by remember { mutableIntStateOf(0) }
    var tabDipilih by remember { mutableStateOf("peta") }
    var mapCameraCenter by remember { mutableStateOf(INDONESIA) }
    var mapCameraZoom by remember { mutableStateOf(5.0) }

    suspend fun ambilLokasi() {
        geoLoading = true
        pesanLokasi = ""
        val lok = Lokasi.ambil(context)
        geoLoading = false
        if (lok == null) {
            pesanLokasi = "Gagal membaca lokasi. Menggunakan area Jember."
            lokasi = Titik(JEMBER_PUSAT.latitude, JEMBER_PUSAT.longitude, "Pusat Jember")
            mapCameraCenter = JEMBER_PUSAT
            mapCameraZoom = 15.0
            return
        }
        lokasi = Titik(lok.latitude, lok.longitude, "Lokasi saya")
        mapCameraCenter = LatLng(lok.latitude, lok.longitude)
        mapCameraZoom = 15.0
        areaTerbuka = false
    }

    val permissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { hasil ->
        if (hasil.values.any { it }) {
            scope.launch { ambilLokasi() }
        } else {
            pesanLokasi = "Izin lokasi ditolak. Menampilkan area Jember."
            lokasi = Titik(JEMBER_PUSAT.latitude, JEMBER_PUSAT.longitude, "Pusat Jember")
            mapCameraCenter = JEMBER_PUSAT
            mapCameraZoom = 15.0
        }
    }

    fun mintaLokasi() {
        if (Lokasi.punyaIzin(context)) {
            scope.launch { ambilLokasi() }
        } else {
            permissionLauncher.launch(
                arrayOf(
                    Manifest.permission.ACCESS_FINE_LOCATION,
                    Manifest.permission.ACCESS_COARSE_LOCATION
                )
            )
        }
    }

    LaunchedEffect(Unit) {
        if (Lokasi.punyaIzin(context)) {
            ambilLokasi()
        } else {
            // Jika belum ada izin, gunakan pusat contoh agar warung langsung tampil
            lokasi = Titik(JEMBER_PUSAT.latitude, JEMBER_PUSAT.longitude, "Pusat Jember")
            mapCameraCenter = JEMBER_PUSAT
            mapCameraZoom = 14.5
        }
    }

    // ── Muat semua warung (gabungan dari lokasi aktif + pusat contoh) ────────
    LaunchedEffect(lokasi) {
        val titik = lokasi ?: return@LaunchedEffect
        loading = true
        error = ""

        // 1. Muat warung di sekitar lokasi aktif
        val resAktif = container.vendorRepository.nearby(titik.lat, titik.lng, RADIUS_MAKS)
        val dataAktif = resAktif.getOrDefault(emptyList())

        // 2. Jika lokasi pengguna di luar Jember, muat juga warung Jember agar saat zoom out tetap terlihat
        val dataJember = if (kotlin.math.abs(titik.lat - JEMBER_PUSAT.latitude) > 0.1 ||
            kotlin.math.abs(titik.lng - JEMBER_PUSAT.longitude) > 0.1) {
            val resJember = container.vendorRepository.nearby(JEMBER_PUSAT.latitude, JEMBER_PUSAT.longitude, RADIUS_MAKS)
            resJember.getOrDefault(emptyList())
        } else {
            emptyList()
        }

        // Gabungkan semua ke dalam master map berdasarkan ID unik
        val gabungan = (masterVendors.values + dataAktif + dataJember)
            .distinctBy { it.id }
            .associateBy { it.id }

        masterVendors = gabungan
        loading = false
    }

    // ── Filter warung berdasarkan pencarian dan kategori ────────────────────
    val vendorsTampil = remember(masterVendors, query, kategoriFilter) {
        val kata = query.trim().lowercase()
        masterVendors.values.filter { vendor ->
            // Filter kategori
            val cocokKategori = when (kategoriFilter) {
                "semua" -> true
                "pkl" -> vendor.kategori == "pkl"
                "keliling" -> vendor.kategori == "keliling"
                "warung" -> vendor.kategori == "warung"
                "rumah_makan" -> vendor.kategori == "rumah_makan" || vendor.kategori == "cafe"
                else -> true
            }

            // Filter teks (nama toko, menu, atau alamat patokan)
            val cocokKata = if (kata.isBlank()) true else {
                vendor.namaToko.lowercase().contains(kata) ||
                    vendor.alamatPatokan.lowercase().contains(kata) ||
                    vendor.menus.any { it.namaMenu.lowercase().contains(kata) }
            }

            cocokKategori && cocokKata
        }
    }

    // Ketika user mengetik pencarian dan ditemukan warung, arahkan kamera ke warung yang relevan
    LaunchedEffect(query) {
        val kata = query.trim()
        if (kata.length >= 3) {
            val ketemu = vendorsTampil.firstOrNull()
            if (ketemu != null && ketemu.lat != 0.0 && ketemu.lng != 0.0) {
                mapCameraCenter = LatLng(ketemu.lat, ketemu.lng)
                mapCameraZoom = 16.0
            }
        }
    }

    Box(modifier = Modifier.fillMaxSize()) {
        // ── Peta Penuh MapLibre ──────────────────────────────────────────────
        PetaWarung(
            modifier = Modifier.fillMaxSize(),
            center = mapCameraCenter,
            zoom = mapCameraZoom,
            userLocation = lokasi?.let { LatLng(it.lat, it.lng) },
            vendors = vendorsTampil,
            selectedId = null,
            bearingResetTrigger = bearingResetTrigger,
            onSelectVendor = onVendor
        )

        // ── Bagian Atas: Pencarian & Baris Kategori ──────────────────────────
        Column(
            modifier = Modifier
                .align(Alignment.TopCenter)
                .fillMaxWidth()
                .statusBarsPadding()
                .padding(horizontal = 16.dp, vertical = 8.dp),
            verticalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            // 1. Search Bar Mengambang
            Surface(
                shape = RoundedCornerShape(24.dp),
                tonalElevation = 6.dp,
                shadowElevation = 6.dp,
                color = MaterialTheme.colorScheme.surface,
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 14.dp, vertical = 2.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = Icons.Default.Search,
                        contentDescription = "Cari",
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(22.dp)
                    )
                    Spacer(modifier = Modifier.width(10.dp))
                    BasicTextField(
                        value = query,
                        onValueChange = { query = it },
                        singleLine = true,
                        textStyle = MaterialTheme.typography.bodyMedium.copy(
                            color = MaterialTheme.colorScheme.onSurface,
                            fontWeight = FontWeight.Medium
                        ),
                        decorationBox = { innerTextField ->
                            if (query.isEmpty()) {
                                Text(
                                    text = "Cari warung, menu, atau kategori...",
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                            innerTextField()
                        },
                        modifier = Modifier
                            .weight(1f)
                            .padding(vertical = 12.dp)
                    )
                    if (query.isNotBlank()) {
                        IconButton(
                            onClick = { query = "" },
                            modifier = Modifier.size(32.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Close,
                                contentDescription = "Hapus",
                                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.size(18.dp)
                            )
                        }
                    }
                }
            }

            // 2. Baris Filter Kategori (Bisa digeser horizontal)
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                DAFTAR_KATEGORI.forEach { (idKat, label) ->
                    val terpilih = (kategoriFilter == idKat)
                    FilterChip(
                        selected = terpilih,
                        onClick = {
                            kategoriFilter = if (terpilih && idKat != "semua") "semua" else idKat
                        },
                        label = {
                            Text(
                                text = label,
                                fontSize = 11.sp,
                                fontWeight = if (terpilih) FontWeight.Bold else FontWeight.Normal
                            )
                        },
                        shape = RoundedCornerShape(50),
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = MaterialTheme.colorScheme.primaryContainer,
                            selectedLabelColor = MaterialTheme.colorScheme.onPrimaryContainer,
                            containerColor = MaterialTheme.colorScheme.surface.copy(alpha = 0.95f)
                        ),
                        elevation = FilterChipDefaults.filterChipElevation(elevation = 2.dp)
                    )
                }
            }
        }

        // ── Tombol Kompas: Sebelah Kanan Agak ke Bawah ───────────────────────
        Surface(
            shape = CircleShape,
            color = MaterialTheme.colorScheme.surface,
            tonalElevation = 6.dp,
            shadowElevation = 6.dp,
            modifier = Modifier
                .align(Alignment.BottomEnd)
                .padding(end = 16.dp, bottom = 105.dp)
                .size(46.dp)
                .clickable {
                    // Reset rotasi bearing peta kembali ke Utara (0°)
                    bearingResetTrigger++
                }
        ) {
            Box(contentAlignment = Alignment.Center) {
                Icon(
                    imageVector = Icons.Default.Explore,
                    contentDescription = "Kompas (Reset ke Utara)",
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(26.dp)
                )
            }
        }

        // ── Pill Akses Cepat Warung Terdekat ────────────────────────────────
        AnimatedVisibility(
            visible = vendorsTampil.isNotEmpty(),
            enter = fadeIn(),
            exit = fadeOut(),
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .padding(bottom = 92.dp)
        ) {
            Surface(
                shape = RoundedCornerShape(20.dp),
                color = MaterialTheme.colorScheme.surface,
                tonalElevation = 4.dp,
                shadowElevation = 4.dp,
                modifier = Modifier.clickable { daftarWarungTerbuka = true }
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 14.dp, vertical = 7.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(
                        modifier = Modifier
                            .size(8.dp)
                            .background(MaterialTheme.colorScheme.primary, CircleShape)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "${vendorsTampil.size} warung tampil",
                        style = MaterialTheme.typography.labelMedium,
                        fontWeight = FontWeight.SemiBold
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        text = "• Lihat daftar",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.primary,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        }

        // ── Bagian Bawah: 4 Tombol (Peta, Lokasi, Tambah, Akun) ─────────────
        Surface(
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .fillMaxWidth(),
            tonalElevation = 8.dp,
            shadowElevation = 12.dp,
            shape = RoundedCornerShape(topStart = 18.dp, topEnd = 18.dp),
            color = MaterialTheme.colorScheme.surface
        ) {
            NavigationBar(
                containerColor = Color.Transparent,
                modifier = Modifier
                    .fillMaxWidth()
                    .navigationBarsPadding(),
                tonalElevation = 0.dp
            ) {
                // 1. Tombol Peta
                NavigationBarItem(
                    selected = (tabDipilih == "peta"),
                    onClick = {
                        tabDipilih = "peta"
                        daftarWarungTerbuka = !daftarWarungTerbuka
                    },
                    icon = {
                        Icon(
                            imageVector = if (tabDipilih == "peta") Icons.Default.Map else Icons.Outlined.Map,
                            contentDescription = "Peta"
                        )
                    },
                    label = { Text("Peta", fontSize = 11.sp) },
                    colors = NavigationBarItemDefaults.colors(
                        selectedIconColor = MaterialTheme.colorScheme.primary,
                        selectedTextColor = MaterialTheme.colorScheme.primary,
                        indicatorColor = MaterialTheme.colorScheme.primaryContainer
                    )
                )

                // 2. Tombol Lokasi
                NavigationBarItem(
                    selected = false,
                    onClick = { areaTerbuka = true },
                    icon = {
                        Icon(
                            imageVector = Icons.Outlined.LocationOn,
                            contentDescription = "Lokasi"
                        )
                    },
                    label = { Text("Lokasi", fontSize = 11.sp) }
                )

                // 3. Tombol Tambah
                NavigationBarItem(
                    selected = false,
                    onClick = { onAdd() },
                    icon = {
                        Icon(
                            imageVector = Icons.Filled.AddCircle,
                            contentDescription = "Tambah Warung",
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(26.dp)
                        )
                    },
                    label = {
                        Text(
                            "Tambah",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = MaterialTheme.colorScheme.primary
                        )
                    }
                )

                // 4. Tombol Akun
                NavigationBarItem(
                    selected = false,
                    onClick = {
                        if (user == null) onLogin() else onDashboard()
                    },
                    icon = {
                        Icon(
                            imageVector = if (user != null) Icons.Default.Person else Icons.Outlined.Person,
                            contentDescription = "Akun"
                        )
                    },
                    label = {
                        Text(
                            if (user != null) "Akun" else "Masuk",
                            fontSize = 11.sp
                        )
                    }
                )
            }
        }
    }

    // ── Bottom Sheet: Daftar Warung ─────────────────────────────────────────
    if (daftarWarungTerbuka) {
        val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = false)
        ModalBottomSheet(
            onDismissRequest = { daftarWarungTerbuka = false },
            sheetState = sheetState
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp)
                    .padding(bottom = 24.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth().padding(bottom = 8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "Daftar warung",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = "${vendorsTampil.size} warung ditemukan",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    if (loading) {
                        Text(
                            text = "Memuat...",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.primary
                        )
                    }
                }

                if (loading) {
                    LinearProgressIndicator(
                        modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp)
                    )
                }

                if (error.isNotBlank()) {
                    Row(
                        modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = error,
                            color = MaterialTheme.colorScheme.error,
                            style = MaterialTheme.typography.bodySmall,
                            modifier = Modifier.weight(1f)
                        )
                        TextButton(onClick = { mintaLokasi() }) { Text("Coba lagi") }
                    }
                }

                if (!loading && error.isBlank() && vendorsTampil.isEmpty()) {
                    Text(
                        text = if (query.isNotBlank()) "Tidak ada warung yang cocok dengan \"$query\"."
                        else "Tidak ada warung pada kategori ini.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(vertical = 16.dp)
                    )
                }

                LazyColumn(modifier = Modifier.heightIn(max = 380.dp)) {
                    items(vendorsTampil) { vendor ->
                        VendorRow(
                            vendor = vendor,
                            onClick = {
                                daftarWarungTerbuka = false
                                onVendor(vendor.id)
                            }
                        )
                    }
                }
            }
        }
    }

    // ── Bottom Sheet: Pilih Area ───────────────────────────────────────────
    if (areaTerbuka) {
        PilihAreaSheet(
            container = container,
            lokasiAktif = lokasi?.label.orEmpty(),
            loadingLokasi = geoLoading,
            pesanLokasi = pesanLokasi,
            onGunakanLokasi = { mintaLokasi() },
            onPilih = { titik ->
                pesanLokasi = ""
                lokasi = titik
                mapCameraCenter = LatLng(titik.lat, titik.lng)
                mapCameraZoom = 15.0
                areaTerbuka = false
            },
            onDismiss = { areaTerbuka = false }
        )
    }
}

@Composable
private fun VendorRow(vendor: VendorDto, onClick: () -> Unit) {
    Surface(
        shape = RoundedCornerShape(12.dp),
        tonalElevation = 1.dp,
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp)
            .clickable(onClick = onClick)
    ) {
        Row(modifier = Modifier.padding(12.dp), verticalAlignment = Alignment.Top) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = vendor.namaToko,
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.SemiBold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Text(
                    text = Format.labelKategori(vendor.kategori),
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Text(
                    text = vendor.alamatPatokan,
                    style = MaterialTheme.typography.bodySmall,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
            Spacer(modifier = Modifier.width(8.dp))
            Column(horizontalAlignment = Alignment.End) {
                Surface(
                    color = if (vendor.buka) MaterialTheme.colorScheme.primaryContainer
                    else MaterialTheme.colorScheme.errorContainer,
                    shape = RoundedCornerShape(50)
                ) {
                    Text(
                        text = if (vendor.buka) "Buka" else "Tutup",
                        style = MaterialTheme.typography.labelSmall,
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp)
                    )
                }
                if (vendor.jarak != null) {
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = Format.jarak(vendor.jarak),
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }
    }
}
