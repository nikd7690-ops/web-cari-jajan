package com.carijajan.app.ui.screen

import android.Manifest
import android.location.Location
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
import androidx.compose.material.icons.filled.FormatListBulleted
import androidx.compose.material.icons.filled.Map
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.outlined.FormatListBulleted
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
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
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
import com.carijajan.app.util.ArahKompas
import com.carijajan.app.util.Format
import com.carijajan.app.util.Lokasi
import com.carijajan.app.util.Titik
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch
import org.maplibre.android.geometry.LatLng

// Titik referensi default awal
private val JEMBER_PUSAT = LatLng(-8.1689, 113.702)
private const val RADIUS_MAKS = 10000

private val DAFTAR_KATEGORI = listOf(
    "semua" to "Semua",
    "pkl" to "Gerobak PKL",
    "keliling" to "Keliling (Motor)",
    "warung" to "Warung Tetap",
    "rumah_makan" to "Rumah Makan"
)

// Helper penghitung jarak geodesik presisi dalam meter
private fun hitungJarakMeter(lat1: Double, lng1: Double, lat2: Double, lng2: Double): Double {
    val hasil = FloatArray(1)
    Location.distanceBetween(lat1, lng1, lat2, lng2, hasil)
    return hasil[0].toDouble()
}

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
    var userLocationRealtime by remember { mutableStateOf<LatLng?>(null) }
    var userHeading by remember { mutableFloatStateOf(0f) }

    var masterVendors by remember { mutableStateOf<Map<String, VendorDto>>(emptyMap()) }
    var loading by remember { mutableStateOf(false) }
    var error by remember { mutableStateOf("") }
    var query by remember { mutableStateOf("") }
    var kategoriFilter by remember { mutableStateOf("semua") }
    var areaTerbuka by remember { mutableStateOf(false) }
    var daftarWarungTerbuka by remember { mutableStateOf(false) }
    var geoLoading by remember { mutableStateOf(false) }
    var pesanLokasi by remember { mutableStateOf("") }

    // Pemicu kontrol kamera eksplisit (mencegah rubbering saat user menggeser/zoom/rotasi)
    var cameraMoveTrigger by remember { mutableIntStateOf(0) }
    var cameraTarget by remember { mutableStateOf<LatLng?>(null) }
    var cameraZoom by remember { mutableStateOf<Double?>(null) }
    var initialLocationMoved by remember { mutableStateOf(false) }

    var bearingResetTrigger by remember { mutableIntStateOf(0) }
    var tabDipilih by remember { mutableStateOf("peta") }
    var izinLokasiDiberikan by remember { mutableStateOf(Lokasi.punyaIzin(context)) }

    val permissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { hasil ->
        if (hasil.values.any { it }) {
            izinLokasiDiberikan = true
        } else {
            pesanLokasi = "Izin lokasi ditolak. Menampilkan area Jember."
            lokasi = Titik(JEMBER_PUSAT.latitude, JEMBER_PUSAT.longitude, "Pusat Jember")
            cameraTarget = JEMBER_PUSAT
            cameraZoom = 15.0
            cameraMoveTrigger++
        }
    }

    fun mintaIzinLokasi() {
        if (Lokasi.punyaIzin(context)) {
            izinLokasiDiberikan = true
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
        if (!Lokasi.punyaIzin(context)) {
            mintaIzinLokasi()
        }
    }

    // ── 1. Pemantau Lokasi Real-Time (GPS & Network Provider) ────────────────
    LaunchedEffect(izinLokasiDiberikan) {
        if (!izinLokasiDiberikan) return@LaunchedEffect
        Lokasi.pantauLokasi(context).collectLatest { loc ->
            val latLng = LatLng(loc.latitude, loc.longitude)
            userLocationRealtime = latLng

            if (loc.hasBearing() && loc.speed > 0.5f) {
                userHeading = loc.bearing
            }

            if (!initialLocationMoved) {
                initialLocationMoved = true
                lokasi = Titik(loc.latitude, loc.longitude, "Lokasi saya")
                cameraTarget = latLng
                cameraZoom = 15.5
                cameraMoveTrigger++
            }
        }
    }

    // ── 2. Pemantau Arah Hadap Kompas Real-Time (dengan filter getaran) ──────
    LaunchedEffect(Unit) {
        ArahKompas.pantauArah(context).collectLatest { heading ->
            userHeading = heading
        }
    }

    // ── 3. Muat Data Warung Multi-Kawasan ────────────────────────────────────
    LaunchedEffect(lokasi) {
        val titik = lokasi ?: Titik(JEMBER_PUSAT.latitude, JEMBER_PUSAT.longitude, "Jember")
        loading = true
        error = ""

        val resAktif = container.vendorRepository.nearby(titik.lat, titik.lng, RADIUS_MAKS)
        val dataAktif = resAktif.getOrDefault(emptyList())

        val dataJember = if (kotlin.math.abs(titik.lat - JEMBER_PUSAT.latitude) > 0.1 ||
            kotlin.math.abs(titik.lng - JEMBER_PUSAT.longitude) > 0.1) {
            val resJember = container.vendorRepository.nearby(JEMBER_PUSAT.latitude, JEMBER_PUSAT.longitude, RADIUS_MAKS)
            resJember.getOrDefault(emptyList())
        } else {
            emptyList()
        }

        val gabungan = (masterVendors.values + dataAktif + dataJember)
            .distinctBy { it.id }
            .associateBy { it.id }

        masterVendors = gabungan
        loading = false
    }

    // ── 4. Filter Warung berdasarkan Pencarian dan Kategori ──────────────────
    val vendorsTampil = remember(masterVendors, query, kategoriFilter) {
        val kata = query.trim().lowercase()
        masterVendors.values.filter { vendor ->
            val cocokKategori = when (kategoriFilter) {
                "semua" -> true
                "pkl" -> vendor.kategori == "pkl"
                "keliling" -> vendor.kategori == "keliling"
                "warung" -> vendor.kategori == "warung"
                "rumah_makan" -> vendor.kategori == "rumah_makan" || vendor.kategori == "cafe"
                else -> true
            }

            val cocokKata = if (kata.isBlank()) true else {
                vendor.namaToko.lowercase().contains(kata) ||
                    vendor.alamatPatokan.lowercase().contains(kata) ||
                    vendor.menus.any { it.namaMenu.lowercase().contains(kata) }
            }

            cocokKategori && cocokKata
        }
    }

    // ── 5. Urutkan Warung dari yang Terdekat berdasarkan Lokasi Pengguna ─────
    val userPos = userLocationRealtime ?: lokasi?.let { LatLng(it.lat, it.lng) }
    val vendorsDenganJarak = remember(vendorsTampil, userPos) {
        vendorsTampil.map { vendor ->
            val jarakMeter = if (userPos != null && vendor.lat != 0.0 && vendor.lng != 0.0) {
                hitungJarakMeter(userPos.latitude, userPos.longitude, vendor.lat, vendor.lng)
            } else {
                vendor.jarak ?: 999999.0
            }
            Pair(vendor, jarakMeter)
        }.sortedBy { it.second } // Diurutkan dari yang terdekat!
    }

    // Kamera meluncur ke warung yang dicari
    LaunchedEffect(query) {
        val kata = query.trim()
        if (kata.length >= 3) {
            val ketemu = vendorsTampil.firstOrNull()
            if (ketemu != null && ketemu.lat != 0.0 && ketemu.lng != 0.0) {
                cameraTarget = LatLng(ketemu.lat, ketemu.lng)
                cameraZoom = 16.0
                cameraMoveTrigger++
            }
        }
    }

    Box(modifier = Modifier.fillMaxSize()) {
        // ── Peta Penuh MapLibre: Bebas Geser, Zoom, dan Rotasi Mulus ──────────
        PetaWarung(
            modifier = Modifier.fillMaxSize(),
            initialCenter = JEMBER_PUSAT,
            initialZoom = 14.5,
            cameraTarget = cameraTarget,
            cameraZoom = cameraZoom,
            cameraMoveTrigger = cameraMoveTrigger,
            bearingResetTrigger = bearingResetTrigger,
            userLocation = userLocationRealtime ?: lokasi?.let { LatLng(it.lat, it.lng) },
            userHeading = userHeading,
            vendors = vendorsTampil,
            selectedId = null,
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
            // Search Bar
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

            // Baris Filter Kategori
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
            visible = vendorsDenganJarak.isNotEmpty(),
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
                        text = "${vendorsDenganJarak.size} warung terdekat",
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

        // ── Bagian Bawah: 5 Tombol Navigasi ─────────────────────────────────
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
                // 1. Peta
                NavigationBarItem(
                    selected = (tabDipilih == "peta" && !daftarWarungTerbuka),
                    onClick = {
                        tabDipilih = "peta"
                        daftarWarungTerbuka = false
                    },
                    icon = {
                        Icon(
                            imageVector = if (tabDipilih == "peta" && !daftarWarungTerbuka) Icons.Default.Map else Icons.Outlined.Map,
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

                // 2. Lokasi (Kamera fokus kembali ke posisi pengguna terkini)
                NavigationBarItem(
                    selected = false,
                    onClick = {
                        val pos = userLocationRealtime ?: lokasi?.let { LatLng(it.lat, it.lng) }
                        if (pos != null) {
                            cameraTarget = pos
                            cameraZoom = 16.0
                            cameraMoveTrigger++
                        } else {
                            areaTerbuka = true
                        }
                    },
                    icon = {
                        Icon(
                            imageVector = Icons.Outlined.LocationOn,
                            contentDescription = "Lokasi Saya"
                        )
                    },
                    label = { Text("Lokasi", fontSize = 11.sp) }
                )

                // 3. Tambah Warung
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

                // 4. ⭐ Tombol List Warung (di sebelah kanan tombol Tambah)
                NavigationBarItem(
                    selected = daftarWarungTerbuka,
                    onClick = {
                        daftarWarungTerbuka = !daftarWarungTerbuka
                    },
                    icon = {
                        Icon(
                            imageVector = if (daftarWarungTerbuka) Icons.Default.FormatListBulleted else Icons.Outlined.FormatListBulleted,
                            contentDescription = "List Warung Terdekat"
                        )
                    },
                    label = {
                        Text(
                            "Daftar",
                            fontSize = 11.sp,
                            fontWeight = if (daftarWarungTerbuka) FontWeight.Bold else FontWeight.Normal
                        )
                    },
                    colors = NavigationBarItemDefaults.colors(
                        selectedIconColor = MaterialTheme.colorScheme.primary,
                        selectedTextColor = MaterialTheme.colorScheme.primary,
                        indicatorColor = MaterialTheme.colorScheme.primaryContainer
                    )
                )

                // 5. Akun
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

    // ── Bottom Sheet: Daftar Warung dari yang Terdekat + Kategori ────────────
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
                // Header Sheet
                Row(
                    modifier = Modifier.fillMaxWidth().padding(bottom = 6.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "Daftar Warung Terdekat",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = "Diurutkan dari posisi terdekatmu • ${vendorsDenganJarak.size} warung",
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

                // Baris Kategori di dalam Sheet
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 4.dp)
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
                                selectedLabelColor = MaterialTheme.colorScheme.onPrimaryContainer
                            )
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
                        TextButton(onClick = { mintaIzinLokasi() }) { Text("Coba lagi") }
                    }
                }

                if (!loading && error.isBlank() && vendorsDenganJarak.isEmpty()) {
                    Text(
                        text = if (query.isNotBlank()) "Tidak ada warung yang cocok dengan \"$query\"."
                        else "Tidak ada warung pada kategori ini.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(vertical = 16.dp)
                    )
                }

                // List Warung Terdekat
                LazyColumn(modifier = Modifier.heightIn(max = 420.dp)) {
                    items(vendorsDenganJarak, key = { it.first.id }) { (vendor, jarakMeter) ->
                        VendorRowTerdekat(
                            vendor = vendor,
                            jarakMeter = jarakMeter,
                            onLihatDiPeta = {
                                daftarWarungTerbuka = false
                                if (vendor.lat != 0.0 && vendor.lng != 0.0) {
                                    cameraTarget = LatLng(vendor.lat, vendor.lng)
                                    cameraZoom = 16.5
                                    cameraMoveTrigger++
                                }
                            },
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

    // ── Bottom Sheet: Pilih Area Manual ─────────────────────────────────────
    if (areaTerbuka) {
        PilihAreaSheet(
            container = container,
            lokasiAktif = lokasi?.label.orEmpty(),
            loadingLokasi = geoLoading,
            pesanLokasi = pesanLokasi,
            onGunakanLokasi = {
                mintaIzinLokasi()
                val pos = userLocationRealtime
                if (pos != null) {
                    lokasi = Titik(pos.latitude, pos.longitude, "Lokasi saya")
                    cameraTarget = pos
                    cameraZoom = 15.5
                    cameraMoveTrigger++
                    areaTerbuka = false
                }
            },
            onPilih = { titik ->
                pesanLokasi = ""
                lokasi = titik
                cameraTarget = LatLng(titik.lat, titik.lng)
                cameraZoom = 15.0
                cameraMoveTrigger++
                areaTerbuka = false
            },
            onDismiss = { areaTerbuka = false }
        )
    }
}

/**
 * Baris item warung terdekat dengan badge kategori, jarak terhitung, dan tombol aksi.
 */
@Composable
private fun VendorRowTerdekat(
    vendor: VendorDto,
    jarakMeter: Double,
    onLihatDiPeta: () -> Unit,
    onClick: () -> Unit
) {
    Surface(
        shape = RoundedCornerShape(14.dp),
        tonalElevation = 2.dp,
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 5.dp)
            .clickable(onClick = onClick)
    ) {
        Column(modifier = Modifier.padding(12.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.Top
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = vendor.namaToko,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )

                    Spacer(modifier = Modifier.height(2.dp))

                    // Badge Kategori & Jam Buka
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Surface(
                            color = MaterialTheme.colorScheme.secondaryContainer,
                            shape = RoundedCornerShape(6.dp)
                        ) {
                            Text(
                                text = when (vendor.kategori) {
                                    "pkl" -> "🛒 Gerobak PKL"
                                    "keliling" -> "🛵 Keliling"
                                    "warung" -> "🏠 Warung Tetap"
                                    "rumah_makan", "cafe" -> "🍽️ Rumah Makan"
                                    else -> Format.labelKategori(vendor.kategori)
                                },
                                style = MaterialTheme.typography.labelSmall,
                                fontWeight = FontWeight.SemiBold,
                                color = MaterialTheme.colorScheme.onSecondaryContainer,
                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                            )
                        }

                        Text(
                            text = "Jam ${vendor.jamBuka} - ${vendor.jamTutup}",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }

                    Spacer(modifier = Modifier.height(4.dp))

                    Text(
                        text = vendor.alamatPatokan,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }

                Spacer(modifier = Modifier.width(8.dp))

                // Jarak & Status Buka/Tutup
                Column(horizontalAlignment = Alignment.End) {
                    Surface(
                        color = if (vendor.buka) MaterialTheme.colorScheme.primaryContainer
                        else MaterialTheme.colorScheme.errorContainer,
                        shape = RoundedCornerShape(50)
                    ) {
                        Text(
                            text = if (vendor.buka) "Buka" else "Tutup",
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = FontWeight.Bold,
                            color = if (vendor.buka) MaterialTheme.colorScheme.onPrimaryContainer
                            else MaterialTheme.colorScheme.onErrorContainer,
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp)
                        )
                    }

                    if (jarakMeter < 900000) {
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = Format.jarak(jarakMeter),
                            style = MaterialTheme.typography.labelMedium,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.primary
                        )
                    }
                }
            }

            // Tombol Aksi Cepat
            Spacer(modifier = Modifier.height(8.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.End,
                verticalAlignment = Alignment.CenterVertically
            ) {
                OutlinedButton(
                    onClick = onLihatDiPeta,
                    modifier = Modifier.height(32.dp),
                    shape = RoundedCornerShape(8.dp)
                ) {
                    Text("Lihat di Peta", fontSize = 11.sp)
                }
                Spacer(modifier = Modifier.width(6.dp))
                TextButton(
                    onClick = onClick,
                    modifier = Modifier.height(32.dp)
                ) {
                    Text("Buka Menu →", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                }
            }
        }
    }
}
