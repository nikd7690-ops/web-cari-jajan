package com.carijajan.app.ui.screen

import android.Manifest
import android.location.Location
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.BorderStroke
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
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AddCircle
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Explore
import androidx.compose.material.icons.filled.Flag
import androidx.compose.material.icons.filled.FormatListBulleted
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.Map
import androidx.compose.material.icons.filled.Navigation
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Storefront
import androidx.compose.material.icons.outlined.FormatListBulleted
import androidx.compose.material.icons.outlined.LocationOn
import androidx.compose.material.icons.outlined.Map
import androidx.compose.material.icons.outlined.Person
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import com.carijajan.app.ui.components.bounceClick
import com.carijajan.app.ui.components.bounceEffect
import com.carijajan.app.ui.theme.*
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
import coil3.compose.AsyncImage
import com.carijajan.app.AppContainer
import com.carijajan.app.data.VendorDto
import com.carijajan.app.ui.components.CariJajanBottomBar
import com.carijajan.app.ui.components.DialogLaporkanWarung
import com.carijajan.app.ui.components.PetaWarung
import com.carijajan.app.ui.components.PilihAreaPanel
import com.carijajan.app.ui.components.PilihAreaSheet
import com.carijajan.app.util.ArahKompas
import com.carijajan.app.util.Format
import com.carijajan.app.util.Lokasi
import com.carijajan.app.util.Navigasi
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

private fun hitungJarakMeter(lat1: Double, lng1: Double, lat2: Double, lng2: Double): Double {
    val hasil = FloatArray(1)
    Location.distanceBetween(lat1, lng1, lat2, lng2, hasil)
    return hasil[0].toDouble()
}

private fun apakahBukaDiWaktu(jamBuka: String, jamTutup: String, waktu: String): Boolean {
    if (waktu == "semua") return true

    fun toMinutes(jam: String): Int {
        val parts = jam.split(":")
        val h = parts.getOrNull(0)?.toIntOrNull() ?: 0
        val m = parts.getOrNull(1)?.toIntOrNull() ?: 0
        return h * 60 + m
    }

    val b = toMinutes(jamBuka)
    val t = toMinutes(jamTutup)

    // Jika buka dan tutup sama, berarti buka 24 jam penuh
    if (b == t) return true

    val targetMulai = when (waktu) {
        "pagi" -> 5 * 60    // 05:00
        "siang" -> 11 * 60  // 11:00
        "malam" -> 17 * 60  // 17:00
        else -> return true
    }
    val targetSelesai = when (waktu) {
        "pagi" -> 11 * 60   // 11:00
        "siang" -> 17 * 60  // 17:00
        "malam" -> 24 * 60  // 24:00
        else -> return true
    }

    return if (t > b) {
        // Rentang normal: overlap jika buka sebelum target selesai dan tutup setelah target mulai
        b < targetSelesai && t > targetMulai
    } else {
        // Lewat tengah malam (misal 17:00 s.d 02:00)
        if (waktu == "malam") true
        else t > targetMulai
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun BerandaScreen(
    container: AppContainer,
    onLogin: () -> Unit = {},
    onDashboard: () -> Unit = {},
    onAdd: () -> Unit,
    onVendor: (String) -> Unit,
    onRegister: () -> Unit = {},
    onEdit: (String) -> Unit = {}
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
    var filterWaktu by remember { mutableStateOf("semua") } // "semua", "pagi", "siang", "malam"
    var vendorTerpilihPreview by remember { mutableStateOf<VendorDto?>(null) }
    var areaTerbuka by remember { mutableStateOf(false) }
    var daftarWarungTerbuka by remember { mutableStateOf(false) }
    var akunTerbuka by remember { mutableStateOf(false) }
    var laporanWarungTarget by remember { mutableStateOf<VendorDto?>(null) }
    var geoLoading by remember { mutableStateOf(false) }
    var pesanLokasi by remember { mutableStateOf("") }

    BackHandler(enabled = akunTerbuka) {
        akunTerbuka = false
    }

    // Fitur Navigasi Rute
    var ruteAktifVendor by remember { mutableStateOf<VendorDto?>(null) }
    var ruteTitikList by remember { mutableStateOf<List<LatLng>?>(null) }

    // Pemicu kontrol kamera eksplisit
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

    // ── 1. Pemantau Lokasi Real-Time ─────────────────────────────────────────
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
                scope.launch {
                    val label = Lokasi.dapatkanNamaArea(context, loc.latitude, loc.longitude)
                    lokasi = Titik(loc.latitude, loc.longitude, label)
                }
                cameraTarget = latLng
                cameraZoom = 15.5
                cameraMoveTrigger++
            }
        }
    }

    // ── 2. Pemantau Arah Hadap Kompas Real-Time ──────────────────────────────
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

    // ── 4. Filter Warung berdasarkan Pencarian, Kategori & Waktu (Pagi/Siang/Malam) ──
    val vendorsTampil = remember(masterVendors, query, kategoriFilter, filterWaktu) {
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

            val cocokWaktu = apakahBukaDiWaktu(vendor.jamBuka, vendor.jamTutup, filterWaktu)

            cocokKategori && cocokKata && cocokWaktu
        }
    }

    // ── 5. Urutkan Warung dari yang Terdekat ─────────────────────────────────
    val userPos = userLocationRealtime ?: lokasi?.let { LatLng(it.lat, it.lng) }
    val vendorsDenganJarak = remember(vendorsTampil, userPos) {
        vendorsTampil.map { vendor ->
            val jarakMeter = if (userPos != null && vendor.lat != 0.0 && vendor.lng != 0.0) {
                hitungJarakMeter(userPos.latitude, userPos.longitude, vendor.lat, vendor.lng)
            } else {
                vendor.jarak ?: 999999.0
            }
            Pair(vendor, jarakMeter)
        }.sortedBy { it.second }
    }

    // Fungsi aktivasi navigasi rute ke warung
    fun mulaiNavigasiKeWarung(vendor: VendorDto) {
        ruteAktifVendor = vendor
        val pos = userLocationRealtime ?: lokasi?.let { LatLng(it.lat, it.lng) }
        if (pos != null && vendor.lat != 0.0 && vendor.lng != 0.0) {
            scope.launch {
                val titikList = Navigasi.ambilTitikRute(pos.latitude, pos.longitude, vendor.lat, vendor.lng)
                ruteTitikList = titikList
            }
        }
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
        // ── Peta Penuh MapLibre ──────────────────────────────────────────────
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
            selectedId = vendorTerpilihPreview?.id,
            routePoints = ruteTitikList,
            onSelectVendor = { id ->
                val v = masterVendors[id] ?: vendorsTampil.firstOrNull { it.id == id }
                if (v != null) {
                    vendorTerpilihPreview = v
                    if (v.lat != 0.0 && v.lng != 0.0) {
                        cameraTarget = LatLng(v.lat, v.lng)
                        cameraMoveTrigger++
                    }
                }
            }
        )

        // ── Bagian Atas: Info Lokasi, Pencarian & Baris Kategori ─────────────
        Column(
            modifier = Modifier
                .align(Alignment.TopCenter)
                .fillMaxWidth()
                .statusBarsPadding()
                .padding(horizontal = 16.dp, vertical = 8.dp),
            verticalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            // ── Baris Info Lokasi Aktif & Pemilih Kota / Waktu Operasional ───────
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 2.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Surface(
                    shape = RoundedCornerShape(20.dp),
                    color = MaterialTheme.colorScheme.surface.copy(alpha = 0.95f),
                    tonalElevation = 4.dp,
                    shadowElevation = 3.dp,
                    modifier = Modifier.clickable { areaTerbuka = true }
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.Default.LocationOn,
                            contentDescription = "Pilih Lokasi",
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(15.dp)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Column(modifier = Modifier.widthIn(max = 110.dp)) {
                            Text(
                                text = "Lokasi Peta:",
                                fontSize = 8.5.sp,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Text(
                                text = lokasi?.label?.ifBlank { "Pusat Jember" } ?: "Pusat Jember",
                                style = MaterialTheme.typography.labelSmall,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onSurface,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                        Spacer(modifier = Modifier.width(2.dp))
                        Icon(
                            imageVector = Icons.Default.ArrowDropDown,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.size(16.dp)
                        )
                    }
                }

                Spacer(modifier = Modifier.weight(1f))

                // ── Tombol Filter Waktu: Pagi, Siang, Malam (Ikon Saja) ─────
                Surface(
                    shape = RoundedCornerShape(20.dp),
                    color = MaterialTheme.colorScheme.surface.copy(alpha = 0.95f),
                    tonalElevation = 4.dp,
                    shadowElevation = 3.dp
                ) {
                    Row(
                        modifier = Modifier.padding(3.dp),
                        horizontalArrangement = Arrangement.spacedBy(2.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        listOf(
                            Triple("semua", "Semua", "🕒"),
                            Triple("pagi", "Pagi", "🌅"),
                            Triple("siang", "Siang", "☀️"),
                            Triple("malam", "Malam", "🌙")
                        ).forEach { (key, label, emoji) ->
                            val terpilih = filterWaktu == key
                            Surface(
                                shape = CircleShape,
                                color = if (terpilih) BrandOrange else Color.Transparent,
                                modifier = Modifier
                                    .size(30.dp)
                                    .bounceClick { filterWaktu = key }
                            ) {
                                Box(
                                    contentAlignment = Alignment.Center,
                                    modifier = Modifier.fillMaxSize()
                                ) {
                                    Text(
                                        text = emoji,
                                        fontSize = 13.sp
                                    )
                                }
                            }
                        }
                    }
                }
            }

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

        // ── Tombol Kompas: Reset Arah ke Utara & Pusatkan ke Titik Pengguna ──
        Surface(
            shape = CircleShape,
            color = MaterialTheme.colorScheme.surface,
            tonalElevation = 6.dp,
            shadowElevation = 6.dp,
            modifier = Modifier
                .align(Alignment.BottomEnd)
                .padding(end = 16.dp, bottom = if (ruteAktifVendor != null) 175.dp else 105.dp)
                .size(48.dp)
                .clickable {
                    if (!izinLokasiDiberikan) {
                        mintaIzinLokasi()
                    }
                    val pos = userLocationRealtime ?: lokasi?.let { LatLng(it.lat, it.lng) }
                    if (pos != null) {
                        cameraTarget = pos
                        cameraZoom = 16.5
                        cameraMoveTrigger++
                    }
                    bearingResetTrigger++
                }
        ) {
            Box(contentAlignment = Alignment.Center) {
                Icon(
                    imageVector = Icons.Default.Explore,
                    contentDescription = "Pusatkan ke Titik Pengguna & Reset Arah",
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(26.dp)
                )
            }
        }

        // ── Kartu Navigasi Rute Aktif (Muncul saat Memilih Warung / Tekan Rute)
        AnimatedVisibility(
            visible = ruteAktifVendor != null,
            enter = fadeIn(),
            exit = fadeOut(),
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .padding(horizontal = 16.dp)
                .padding(bottom = 92.dp)
                .fillMaxWidth()
        ) {
            val vendor = ruteAktifVendor
            if (vendor != null) {
                val jarak = if (userPos != null && vendor.lat != 0.0 && vendor.lng != 0.0) {
                    hitungJarakMeter(userPos.latitude, userPos.longitude, vendor.lat, vendor.lng)
                } else {
                    vendor.jarak ?: 0.0
                }

                Surface(
                    shape = RoundedCornerShape(18.dp),
                    color = MaterialTheme.colorScheme.surface,
                    tonalElevation = 6.dp,
                    shadowElevation = 10.dp,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(14.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = Icons.Default.Navigation,
                                contentDescription = "Rute Navigasi",
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(22.dp)
                            )
                            Spacer(Modifier.width(8.dp))
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = vendor.namaToko,
                                    style = MaterialTheme.typography.titleSmall,
                                    fontWeight = FontWeight.Bold,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                                Text(
                                    text = "Jarak ${Format.jarak(jarak)} • 🛵 ${Navigasi.estimasiWaktuMotor(jarak)} • 🚶 ${Navigasi.estimasiWaktuJalan(jarak)}",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.primary,
                                    fontWeight = FontWeight.SemiBold
                                )
                            }
                            IconButton(
                                onClick = {
                                    ruteAktifVendor = null
                                    ruteTitikList = null
                                },
                                modifier = Modifier.size(28.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Close,
                                    contentDescription = "Tutup Rute",
                                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                    modifier = Modifier.size(18.dp)
                                )
                            }
                        }

                        Spacer(Modifier.height(10.dp))

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            OutlinedButton(
                                onClick = { onVendor(vendor.id) },
                                modifier = Modifier.weight(1f),
                                shape = RoundedCornerShape(10.dp)
                            ) {
                                Text("Buka Menu", fontSize = 12.sp)
                            }

                            Button(
                                onClick = {
                                    Navigasi.bukaNavigasi(context, vendor.lat, vendor.lng, vendor.namaToko)
                                },
                                modifier = Modifier.weight(1.4f),
                                shape = RoundedCornerShape(10.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Navigation,
                                    contentDescription = "Mulai Navigasi",
                                    modifier = Modifier.size(16.dp)
                                )
                                Spacer(Modifier.width(6.dp))
                                Text("Navigasi GPS ↗", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                            }
                        }
                    }
                }
            }
        }

        // ── Pill Akses Cepat Warung Terdekat (Tampil jika Tidak Ada Rute & Tidak Ada Preview)
        AnimatedVisibility(
            visible = vendorsDenganJarak.isNotEmpty() && ruteAktifVendor == null && vendorTerpilihPreview == null && !daftarWarungTerbuka && !areaTerbuka && !akunTerbuka,
            enter = fadeIn(),
            exit = fadeOut(),
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .navigationBarsPadding()
                .padding(bottom = 96.dp)
        ) {
            Surface(
                shape = RoundedCornerShape(22.dp),
                color = MaterialTheme.colorScheme.surface,
                tonalElevation = 6.dp,
                shadowElevation = 8.dp,
                border = BorderStroke(1.dp, BrandOrangeLight),
                modifier = Modifier.bounceClick { daftarWarungTerbuka = true }
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(
                        modifier = Modifier
                            .size(10.dp)
                            .background(com.carijajan.app.ui.theme.BrandOrange, CircleShape)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "${vendorsDenganJarak.size} warung terdekat",
                        style = MaterialTheme.typography.labelMedium,
                        fontWeight = FontWeight.Bold
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = "• Lihat daftar ↗",
                        style = MaterialTheme.typography.labelSmall,
                        color = com.carijajan.app.ui.theme.BrandOrange,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        }

        // ── Kartu Pop-up Preview Pin Warung Saat di-Klik ────────────────────
        if (vendorTerpilihPreview != null) {
            val preview = vendorTerpilihPreview!!
            val previewJarak = remember(preview, userPos) {
                if (userPos != null && preview.lat != 0.0 && preview.lng != 0.0) {
                    hitungJarakMeter(userPos.latitude, userPos.longitude, preview.lat, preview.lng)
                } else {
                    preview.jarak
                }
            }

            Surface(
                shape = RoundedCornerShape(20.dp),
                color = MaterialTheme.colorScheme.surface,
                shadowElevation = 16.dp,
                tonalElevation = 8.dp,
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .fillMaxWidth()
                    .padding(horizontal = 10.dp)
                    .navigationBarsPadding()
                    .padding(bottom = 90.dp)
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(14.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.Top
                    ) {
                        Row(
                            modifier = Modifier.weight(1f),
                            horizontalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            // Foto Warung
                            Surface(
                                shape = RoundedCornerShape(12.dp),
                                color = MaterialTheme.colorScheme.surfaceVariant,
                                modifier = Modifier.size(68.dp)
                            ) {
                                if (preview.foto.isNotBlank()) {
                                    AsyncImage(
                                        model = preview.foto,
                                        contentDescription = preview.namaToko,
                                        contentScale = androidx.compose.ui.layout.ContentScale.Crop,
                                        modifier = Modifier.fillMaxSize()
                                    )
                                } else {
                                    Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                                        Text(preview.kategoriEmoji, fontSize = 28.sp)
                                    }
                                }
                            }

                            // Info Teks Warung
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = preview.namaToko,
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.Bold,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )

                                Text(
                                    text = preview.alamatPatokan,
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis,
                                    modifier = Modifier.padding(top = 1.dp)
                                )

                                // Siapa yang mengkontribusikan
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    modifier = Modifier.padding(top = 4.dp)
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Person,
                                        contentDescription = null,
                                        tint = com.carijajan.app.ui.theme.BrandMaroon,
                                        modifier = Modifier.size(13.dp)
                                    )
                                    Spacer(Modifier.width(4.dp))
                                    Text(
                                        text = "Diajukan oleh: ${preview.namaKontributor ?: "Warga Komunitas"}",
                                        fontSize = 11.sp,
                                        color = com.carijajan.app.ui.theme.BrandMaroon,
                                        fontWeight = FontWeight.SemiBold,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                }
                            }
                        }

                        // Aksi Laporkan & Tutup Preview
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            IconButton(
                                onClick = { laporanWarungTarget = preview },
                                modifier = Modifier.size(28.dp)
                            ) {
                                Icon(Icons.Filled.Flag, contentDescription = "Laporkan Warung", tint = Color(0xFFEF4444), modifier = Modifier.size(16.dp))
                            }
                            IconButton(
                                onClick = { vendorTerpilihPreview = null },
                                modifier = Modifier.size(28.dp)
                            ) {
                                Icon(Icons.Default.Close, contentDescription = "Tutup", modifier = Modifier.size(18.dp))
                            }
                        }
                    }

                    Spacer(Modifier.height(10.dp))

                    // Status Buka/Tutup & Jam Operasional
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Surface(
                                shape = RoundedCornerShape(12.dp),
                                color = if (preview.buka) Color(0xFFDCFCE7) else Color(0xFFFEE2E2),
                                border = BorderStroke(1.dp, if (preview.buka) Color(0xFF86EFAC) else Color(0xFFFECACA))
                            ) {
                                Text(
                                    text = if (preview.buka) "● Sedang Buka" else "● Sudah Tutup",
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = if (preview.buka) Color(0xFF15803D) else Color(0xFFB91C1C),
                                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
                                )
                            }
                            Spacer(Modifier.width(8.dp))
                            Text(
                                text = "Buka: ${preview.jamBuka} - ${preview.jamTutup}",
                                fontSize = 11.sp,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }

                        if (previewJarak != null) {
                            Text(
                                text = Format.jarak(previewJarak),
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.primary
                            )
                        }
                    }

                    Spacer(Modifier.height(12.dp))

                    // 2 Tombol Aksi: Navigasi Langsung & Lihat Menu/Profil
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        OutlinedButton(
                            onClick = {
                                mulaiNavigasiKeWarung(preview)
                                vendorTerpilihPreview = null
                            },
                            shape = RoundedCornerShape(14.dp),
                            border = BorderStroke(1.5.dp, BrandOrange),
                            colors = ButtonDefaults.outlinedButtonColors(
                                contentColor = BrandOrange
                            ),
                            modifier = Modifier
                                .weight(1f)
                                .height(46.dp)
                                .bounceEffect()
                        ) {
                            Icon(Icons.Default.Navigation, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(Modifier.width(6.dp))
                            Text("Navigasi", fontWeight = FontWeight.Bold, fontSize = 13.sp)
                        }

                        Button(
                            onClick = {
                                onVendor(preview.id)
                            },
                            shape = RoundedCornerShape(14.dp),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = BrandOrange,
                                contentColor = Color.White
                            ),
                            modifier = Modifier
                                .weight(1.1f)
                                .height(46.dp)
                                .bounceEffect()
                        ) {
                            Icon(Icons.Default.Storefront, contentDescription = null, modifier = Modifier.size(17.dp))
                            Spacer(Modifier.width(6.dp))
                            Text("Menu & Profil", fontWeight = FontWeight.Bold, fontSize = 13.sp)
                        }
                    }
                }
            }
        }

        // ── Panel Sheet: Daftar Warung Terdekat (Tampil di Atas Peta, di Belakang Bottom Navigation Bar) ──
        AnimatedVisibility(
            visible = daftarWarungTerbuka,
            enter = slideInVertically(initialOffsetY = { it }) + fadeIn(),
            exit = slideOutVertically(targetOffsetY = { it }) + fadeOut(),
            modifier = Modifier.align(Alignment.BottomCenter)
        ) {
            Surface(
                shape = RoundedCornerShape(topStart = 22.dp, topEnd = 22.dp),
                color = MaterialTheme.colorScheme.surface,
                tonalElevation = 6.dp,
                shadowElevation = 16.dp,
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(max = 520.dp)
                    .padding(bottom = 76.dp)
                    .navigationBarsPadding()
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 10.dp)
                ) {
                    // Drag Handle
                    Box(
                        modifier = Modifier
                            .align(Alignment.CenterHorizontally)
                            .size(width = 36.dp, height = 4.dp)
                            .background(MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.4f), CircleShape)
                    )
                    Spacer(Modifier.height(8.dp))

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
                            Spacer(Modifier.width(8.dp))
                        }
                        IconButton(
                            onClick = { daftarWarungTerbuka = false },
                            modifier = Modifier.size(28.dp)
                        ) {
                            Icon(Icons.Default.Close, contentDescription = "Tutup", modifier = Modifier.size(18.dp))
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
                    LazyColumn(modifier = Modifier.fillMaxWidth()) {
                        items(vendorsDenganJarak, key = { it.first.id }) { (vendor, jarakMeter) ->
                            VendorRowTerdekat(
                                vendor = vendor,
                                jarakMeter = jarakMeter,
                                onMulaiRute = {
                                    daftarWarungTerbuka = false
                                    mulaiNavigasiKeWarung(vendor)
                                },
                                onLihatDiPeta = {
                                    daftarWarungTerbuka = false
                                    vendorTerpilihPreview = vendor
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

        // ── Panel Sheet: Pilih Area Manual & Kota/Tempat Sekitar ───────────────
        AnimatedVisibility(
            visible = areaTerbuka,
            enter = slideInVertically(initialOffsetY = { it }) + fadeIn(),
            exit = slideOutVertically(targetOffsetY = { it }) + fadeOut(),
            modifier = Modifier.align(Alignment.BottomCenter)
        ) {
            PilihAreaPanel(
                container = container,
                lokasiAktif = lokasi?.label.orEmpty(),
                userLocation = userLocationRealtime ?: lokasi?.let { LatLng(it.lat, it.lng) },
                loadingLokasi = geoLoading,
                pesanLokasi = pesanLokasi,
                onGunakanLokasi = {
                    mintaIzinLokasi()
                    val pos = userLocationRealtime
                    if (pos != null) {
                        scope.launch {
                            val label = Lokasi.dapatkanNamaArea(context, pos.latitude, pos.longitude)
                            lokasi = Titik(pos.latitude, pos.longitude, label)
                        }
                        cameraTarget = pos
                        cameraZoom = 16.0
                        cameraMoveTrigger++
                        bearingResetTrigger++
                        areaTerbuka = false
                    }
                },
                onPilih = { titik ->
                    pesanLokasi = ""
                    lokasi = titik
                    cameraTarget = LatLng(titik.lat, titik.lng)
                    cameraZoom = 15.5
                    cameraMoveTrigger++
                    areaTerbuka = false
                },
                onDismiss = { areaTerbuka = false }
            )
        }

        // ── Panel Akun: Masuk / Dashboard Terbuka di Atas Beranda (Bottom Bar Tetap Stay) ──
        AnimatedVisibility(
            visible = akunTerbuka,
            enter = slideInHorizontally(
                initialOffsetX = { it },
                animationSpec = tween(320)
            ) + fadeIn(animationSpec = tween(320)),
            exit = slideOutHorizontally(
                targetOffsetX = { it },
                animationSpec = tween(320)
            ) + fadeOut(animationSpec = tween(320)),
            modifier = Modifier.fillMaxSize()
        ) {
            Surface(
                modifier = Modifier.fillMaxSize(),
                color = MaterialTheme.colorScheme.background
            ) {
                if (user == null) {
                    MasukScreen(
                        container = container,
                        onSuccess = {
                            // User otomatis terefresh via collectAsStateWithLifecycle
                        },
                        onRegister = {
                            akunTerbuka = false
                            onRegister()
                        },
                        onBack = { akunTerbuka = false },
                        tampilkanBottomBar = false
                    )
                } else {
                    DashboardScreen(
                        container = container,
                        onBack = { akunTerbuka = false },
                        onAdd = {
                            akunTerbuka = false
                            onAdd()
                        },
                        onEdit = { id ->
                            akunTerbuka = false
                            onEdit(id)
                        },
                        onVendor = { id ->
                            akunTerbuka = false
                            onVendor(id)
                        },
                        onLogout = {
                            // Sesi dibersihkan, berganti ke MasukScreen
                        },
                        tampilkanBottomBar = false
                    )
                }
            }
        }

        // ── Bagian Bawah: Bottom Navigation Bar Utama (Selalu di Lapisan Paling Depan) ──
        CariJajanBottomBar(
            tabAktif = when {
                akunTerbuka -> "akun"
                daftarWarungTerbuka -> "daftar"
                areaTerbuka -> "lokasi"
                else -> "peta"
            },
            sudahLogin = (user != null),
            modifier = Modifier.align(Alignment.BottomCenter),
            onTabSelected = { tab ->
                when (tab) {
                    "peta" -> {
                        akunTerbuka = false
                        daftarWarungTerbuka = false
                        areaTerbuka = false
                    }
                    "lokasi" -> {
                        akunTerbuka = false
                        daftarWarungTerbuka = false
                        areaTerbuka = !areaTerbuka
                    }
                    "daftar" -> {
                        akunTerbuka = false
                        areaTerbuka = false
                        daftarWarungTerbuka = !daftarWarungTerbuka
                    }
                    "tambah" -> {
                        akunTerbuka = false
                        daftarWarungTerbuka = false
                        areaTerbuka = false
                        onAdd()
                    }
                    "akun" -> {
                        daftarWarungTerbuka = false
                        areaTerbuka = false
                        akunTerbuka = !akunTerbuka
                    }
                }
            }
        )
    }

    // ── Dialog Laporkan Warung (Ketidaksesuaian Peta / Tutup) ────────────────
    if (laporanWarungTarget != null) {
        DialogLaporkanWarung(
            container = container,
            vendorId = laporanWarungTarget!!.id,
            namaToko = laporanWarungTarget!!.namaToko,
            onDismiss = { laporanWarungTarget = null }
        )
    }
}

/**
 * Baris item warung terdekat dengan badge kategori, jarak terhitung, tombol rute dan aksi.
 */
@Composable
private fun VendorRowTerdekat(
    vendor: VendorDto,
    jarakMeter: Double,
    onMulaiRute: () -> Unit,
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

            // Tombol Aksi: Rute Navigasi, Lihat di Peta, Buka Menu
            Spacer(modifier = Modifier.height(8.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.End,
                verticalAlignment = Alignment.CenterVertically
            ) {
                OutlinedButton(
                    onClick = onMulaiRute,
                    modifier = Modifier.height(32.dp),
                    shape = RoundedCornerShape(8.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Navigation,
                        contentDescription = "Rute",
                        modifier = Modifier.size(13.dp)
                    )
                    Spacer(Modifier.width(4.dp))
                    Text("Rute", fontSize = 11.sp, fontWeight = FontWeight.SemiBold)
                }
                Spacer(modifier = Modifier.width(6.dp))
                OutlinedButton(
                    onClick = onLihatDiPeta,
                    modifier = Modifier.height(32.dp),
                    shape = RoundedCornerShape(8.dp)
                ) {
                    Text("Peta", fontSize = 11.sp)
                }
                Spacer(modifier = Modifier.width(6.dp))
                TextButton(
                    onClick = onClick,
                    modifier = Modifier.height(32.dp)
                ) {
                    Text("Menu →", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                }
            }
        }
    }
}
