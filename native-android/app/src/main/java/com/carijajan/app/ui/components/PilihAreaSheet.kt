package com.carijajan.app.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.MyLocation
import androidx.compose.material.icons.filled.Place
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.outlined.NearMe
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.carijajan.app.AppContainer
import com.carijajan.app.data.GeocodeItem
import com.carijajan.app.util.Lokasi
import com.carijajan.app.util.Titik
import java.util.Locale
import kotlinx.coroutines.delay
import org.maplibre.android.geometry.LatLng

data class TempatAreaPilihan(
    val nama: String,
    val kota: String,
    val lat: Double,
    val lng: Double,
    val deskripsi: String = ""
)

private val DAFTAR_TEMPAT_KOTA = listOf(
    // Jember & Sekitarnya
    TempatAreaPilihan("Alun-alun Jember", "Jember", -8.1689, 113.7020, "Pusat Kota & Kuliner Malam"),
    TempatAreaPilihan("Tegal Boto / UNEJ", "Jember", -8.1640, 113.7170, "Kawasan Mahasiswa & Jajanan"),
    TempatAreaPilihan("Kaliwates", "Jember", -8.1830, 113.6770, "Pusat Belanja & Kuliner"),
    TempatAreaPilihan("Sumbersari", "Jember", -8.1580, 113.7150, "Area Kampus & Kafe"),
    TempatAreaPilihan("Patrang", "Jember", -8.1450, 113.7000, "Area Kuliner Tradisional"),
    TempatAreaPilihan("Pasar Tanjung", "Jember", -8.1750, 113.7080, "Pasar Induk & Sentra Jajanan Pasar"),
    TempatAreaPilihan("Mangli", "Jember", -8.1950, 113.6480, "Jalan Utama & Pusat Oleh-Oleh"),

    // Surabaya
    TempatAreaPilihan("Tunjungan / Pusat", "Surabaya", -7.2618, 112.7383, "Pusat Kuliner & Wisata Sejarah"),
    TempatAreaPilihan("Gubeng", "Surabaya", -7.2736, 112.7538, "Sentra Warung & Dekat Stasiun"),
    TempatAreaPilihan("Rungkut", "Surabaya", -7.3204, 112.7753, "Kawasan Padat Kuliner & Kampus UPN"),
    TempatAreaPilihan("Wonokromo", "Surabaya", -7.3005, 112.7388, "Dekat DTC & Kuliner Malam"),
    TempatAreaPilihan("Darmo / Taman Bungkul", "Surabaya", -7.2915, 112.7380, "Sentra PKL Taman Bungkul"),
    TempatAreaPilihan("Ketintang / Unesa", "Surabaya", -7.3115, 112.7245, "Area Mahasiswa & Kuliner Hemat"),

    // Malang
    TempatAreaPilihan("Alun-alun Kota Malang", "Malang", -7.9826, 112.6308, "Pusat Kota & Kuliner Khas Malang"),
    TempatAreaPilihan("Soekarno-Hatta (Suhat)", "Malang", -7.9445, 112.6162, "Pusat Kafe & UMKM Populer"),
    TempatAreaPilihan("Lowokwaru / UB", "Malang", -7.9526, 112.6145, "Kawasan Kampus Universitas Brawijaya"),
    TempatAreaPilihan("Klojen", "Malang", -7.9785, 112.6280, "Stasiun Kota Baru & Kuliner Legendaris"),
    TempatAreaPilihan("Ijen Boulevard", "Malang", -7.9715, 112.6225, "Kawasan Wisata & Kuliner Heritage"),

    // Yogyakarta
    TempatAreaPilihan("Malioboro", "Yogyakarta", -7.7928, 110.3658, "Pusat Wisata & Lesehan Gudeg"),
    TempatAreaPilihan("UGM / Bulaksumur", "Yogyakarta", -7.7713, 110.3775, "Sentra Jajanan Mahasiswa UGM"),
    TempatAreaPilihan("Kotabaru", "Yogyakarta", -7.7865, 110.3745, "Kafe & Warung Heritage"),
    TempatAreaPilihan("Prawirotaman", "Yogyakarta", -7.8185, 110.3712, "Area Wisata & Kuliner Multikultural"),
    TempatAreaPilihan("Tugu Jogja", "Yogyakarta", -7.7828, 110.3670, "Ikon Kota & Sentra Kopi Joss"),

    // Bandung
    TempatAreaPilihan("Alun-alun Bandung", "Bandung", -6.9218, 107.6071, "Pusat Kota & Jalan Asia Afrika"),
    TempatAreaPilihan("Dago / Ir. H. Juanda", "Bandung", -6.8835, 107.6142, "Pusat Kafe & Wisata Kuliner"),
    TempatAreaPilihan("Dipatiukur / Unpad", "Bandung", -6.8925, 107.6178, "Sentra Kuliner Mahasiswa"),
    TempatAreaPilihan("Braga", "Bandung", -6.9175, 107.6095, "Jalan Bersejarah & Toko Roti Legendaris"),

    // Jakarta
    TempatAreaPilihan("Monas / Gambir", "Jakarta", -6.1754, 106.8272, "Pusat Ibukota & Lenggang Jakarta"),
    TempatAreaPilihan("Senopati / SCBD", "Jakarta", -6.2285, 106.8095, "Pusat Kuliner & Resto Populer"),
    TempatAreaPilihan("Tebet", "Jakarta", -6.2365, 106.8525, "Kawasan Kuliner Anak Muda & UMKM"),
    TempatAreaPilihan("Blok M", "Jakarta", -6.2440, 106.7985, "Sentra Kuliner Melawai & MRT"),

    // Denpasar / Bali
    TempatAreaPilihan("Renon / Puputan", "Bali", -8.6705, 115.2335, "Pusat Kota Denpasar & Warung Lokal"),
    TempatAreaPilihan("Sanur", "Bali", -8.6925, 115.2625, "Wisata Pantai & Kuliner Tradisional"),
    TempatAreaPilihan("Kuta", "Bali", -8.7185, 115.1695, "Sentra Wisatawan & Aneka Kuliner"),

    // Semarang & Solo
    TempatAreaPilihan("Simpang Lima", "Semarang", -6.9904, 110.4229, "Pusat Kuliner Tahu Gimbal & Oleh-oleh"),
    TempatAreaPilihan("Tembalang / Undip", "Semarang", -7.0505, 110.4385, "Kawasan Mahasiswa & Kuliner Hemat"),
    TempatAreaPilihan("Manahan", "Solo", -7.5535, 110.8065, "Pusat Kuliner Selat Solo & Stadion"),
    TempatAreaPilihan("Slamet Riyadi", "Solo", -7.5685, 110.8175, "Pusat Kota & Kuliner Pasar Malam")
)

private val KATEGORI_KOTA = listOf(
    "Sekitar Saya",
    "Jember",
    "Surabaya",
    "Malang",
    "Yogyakarta",
    "Bandung",
    "Jakarta",
    "Bali",
    "Semarang",
    "Solo"
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PilihAreaSheet(
    container: AppContainer,
    lokasiAktif: String,
    userLocation: LatLng? = null,
    loadingLokasi: Boolean,
    pesanLokasi: String,
    onGunakanLokasi: () -> Unit,
    onPilih: (Titik) -> Unit,
    onDismiss: () -> Unit
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState
    ) {
        PilihAreaContent(
            container = container,
            lokasiAktif = lokasiAktif,
            userLocation = userLocation,
            loadingLokasi = loadingLokasi,
            pesanLokasi = pesanLokasi,
            onGunakanLokasi = onGunakanLokasi,
            onPilih = onPilih,
            onDismiss = onDismiss
        )
    }
}

/**
 * Panel inline untuk memilih area di atas peta dengan Bottom Navigation Bar tetap tampil di depan.
 */
@Composable
fun PilihAreaPanel(
    container: AppContainer,
    lokasiAktif: String,
    userLocation: LatLng? = null,
    loadingLokasi: Boolean,
    pesanLokasi: String,
    modifier: Modifier = Modifier,
    onGunakanLokasi: () -> Unit,
    onPilih: (Titik) -> Unit,
    onDismiss: () -> Unit
) {
    Surface(
        shape = RoundedCornerShape(topStart = 22.dp, topEnd = 22.dp),
        color = MaterialTheme.colorScheme.surface,
        tonalElevation = 6.dp,
        shadowElevation = 16.dp,
        modifier = modifier
            .fillMaxWidth()
            .heightIn(max = 560.dp)
            .padding(bottom = 74.dp)
            .navigationBarsPadding()
    ) {
        PilihAreaContent(
            container = container,
            lokasiAktif = lokasiAktif,
            userLocation = userLocation,
            loadingLokasi = loadingLokasi,
            pesanLokasi = pesanLokasi,
            onGunakanLokasi = onGunakanLokasi,
            onPilih = onPilih,
            onDismiss = onDismiss
        )
    }
}

@Composable
fun PilihAreaContent(
    container: AppContainer,
    lokasiAktif: String,
    userLocation: LatLng? = null,
    loadingLokasi: Boolean,
    pesanLokasi: String,
    onGunakanLokasi: () -> Unit,
    onPilih: (Titik) -> Unit,
    onDismiss: () -> Unit
) {
    var query by remember { mutableStateOf("") }
    var hasilGeocode by remember { mutableStateOf<List<GeocodeItem>>(emptyList()) }
    var loadingGeocode by remember { mutableStateOf(false) }
    var errorGeocode by remember { mutableStateOf("") }

    // Kategori kota terpilih (Default "Sekitar Saya" jika GPS aktif, atau "Jember")
    var kotaTerpilih by remember {
        mutableStateOf(if (userLocation != null) "Sekitar Saya" else "Jember")
    }

    // Pencarian geocoding real-time ke API backend (OpenStreetMap Nominatim Indonesia)
    LaunchedEffect(query) {
        val kata = query.trim()
        if (kata.length < 3) {
            hasilGeocode = emptyList()
            errorGeocode = ""
            loadingGeocode = false
            return@LaunchedEffect
        }
        loadingGeocode = true
        errorGeocode = ""
        delay(400)
        val res = container.vendorRepository.geocode(kata)
        res.onSuccess {
            hasilGeocode = it
            loadingGeocode = false
        }.onFailure {
            errorGeocode = it.message ?: "Gagal mencari daerah."
            hasilGeocode = emptyList()
            loadingGeocode = false
        }
    }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 18.dp)
            .padding(bottom = 20.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        // Drag Handle
        Box(
            modifier = Modifier
                .align(Alignment.CenterHorizontally)
                .padding(top = 8.dp)
                .size(width = 36.dp, height = 4.dp)
                .background(MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.4f), CircleShape)
        )

        // ── Header Judul & Status Lokasi Aktif ───────────────────────────
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = "Pilih Kota & Daerah",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = if (lokasiAktif.isBlank()) "Belum ada lokasi dipilih" else "📍 Area aktif: $lokasiAktif",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.primary,
                    fontWeight = FontWeight.Medium
                )
            }
            IconButton(onClick = onDismiss, modifier = Modifier.size(28.dp)) {
                Icon(Icons.Default.Close, contentDescription = "Tutup", modifier = Modifier.size(18.dp))
            }
        }

            // ── Card Lokasi GPS Saya Saat Ini ────────────────────────────────
            Surface(
                shape = RoundedCornerShape(14.dp),
                color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.5f),
                tonalElevation = 2.dp,
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable(enabled = !loadingLokasi, onClick = onGunakanLokasi)
            ) {
                Row(
                    modifier = Modifier.padding(14.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Surface(
                        shape = RoundedCornerShape(10.dp),
                        color = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(38.dp)
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            if (loadingLokasi) {
                                CircularProgressIndicator(
                                    color = MaterialTheme.colorScheme.onPrimary,
                                    modifier = Modifier.size(20.dp),
                                    strokeWidth = 2.dp
                                )
                            } else {
                                Icon(
                                    imageVector = Icons.Default.MyLocation,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.onPrimary,
                                    modifier = Modifier.size(22.dp)
                                )
                            }
                        }
                    }

                    Spacer(modifier = Modifier.width(12.dp))

                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "Gunakan Lokasi GPS Saya",
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onPrimaryContainer
                        )
                        Text(
                            text = if (userLocation != null) {
                                "GPS Terkunci: ${String.format(Locale.US, "%.4f, %.4f", userLocation.latitude, userLocation.longitude)}"
                            } else {
                                "Otomatis mengarah ke titik koordinat perangkat Anda"
                            },
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.8f)
                        )
                    }
                }
            }

            if (pesanLokasi.isNotBlank()) {
                Text(
                    text = pesanLokasi,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.error
                )
            }

            // ── Input Pencarian Kota / Tempat Se-Indonesia ────────────────────
            OutlinedTextField(
                value = query,
                onValueChange = { query = it },
                label = { Text("Cari kota, kecamatan, atau tempat...") },
                placeholder = { Text("Contoh: Kaliwates, Surabaya, Malioboro, Bandung") },
                leadingIcon = {
                    Icon(imageVector = Icons.Default.Search, contentDescription = "Cari")
                },
                trailingIcon = {
                    if (query.isNotEmpty()) {
                        IconButton(onClick = { query = "" }) {
                            Icon(imageVector = Icons.Default.Close, contentDescription = "Hapus")
                        }
                    }
                },
                shape = RoundedCornerShape(14.dp),
                modifier = Modifier.fillMaxWidth(),
                singleLine = true
            )

            // ── Hasil Pencarian Dinamis (Nominatim Indonesia) ─────────────────
            if (loadingGeocode) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = Modifier.padding(vertical = 4.dp)
                ) {
                    CircularProgressIndicator(modifier = Modifier.size(16.dp), strokeWidth = 2.dp)
                    Text("Mencari tempat di seluruh Indonesia...", style = MaterialTheme.typography.bodySmall)
                }
            }

            if (errorGeocode.isNotBlank()) {
                Text(
                    text = errorGeocode,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.error
                )
            }

            if (hasilGeocode.isNotEmpty()) {
                Text(
                    text = "Hasil Pencarian:",
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.SemiBold
                )
                LazyColumn(
                    modifier = Modifier.heightIn(max = 200.dp),
                    verticalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    items(hasilGeocode) { item ->
                        Surface(
                            shape = RoundedCornerShape(10.dp),
                            color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                            modifier = Modifier
                                .fillMaxWidth()
                                .bounceClick {
                                    onPilih(Titik(item.lat, item.lng, item.nama))
                                }
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 12.dp, vertical = 10.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Place,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.primary,
                                    modifier = Modifier.size(20.dp)
                                )
                                Spacer(modifier = Modifier.width(10.dp))
                                Text(
                                    text = item.nama,
                                    style = MaterialTheme.typography.bodySmall,
                                    fontWeight = FontWeight.Medium,
                                    maxLines = 2,
                                    overflow = TextOverflow.Ellipsis
                                )
                            }
                        }
                    }
                }
            }

            // ── Filter Baris Kategori Kota & Sekitar ──────────────────────────
            Text(
                text = "Pilih Daerah Populer & Sekitar:",
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.SemiBold
            )

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                KATEGORI_KOTA.forEach { kota ->
                    val terpilih = (kotaTerpilih == kota)
                    FilterChip(
                        selected = terpilih,
                        onClick = { kotaTerpilih = kota },
                        label = {
                            Text(
                                text = kota,
                                fontSize = 12.sp,
                                fontWeight = if (terpilih) FontWeight.Bold else FontWeight.Normal
                            )
                        },
                        leadingIcon = if (kota == "Sekitar Saya") {
                            {
                                Icon(
                                    imageVector = Icons.Outlined.NearMe,
                                    contentDescription = null,
                                    modifier = Modifier.size(16.dp)
                                )
                            }
                        } else null,
                        colors = FilterChipDefaults.filterChipElevation().let {
                            FilterChipDefaults.filterChipColors(
                                selectedContainerColor = MaterialTheme.colorScheme.primaryContainer,
                                selectedLabelColor = MaterialTheme.colorScheme.onPrimaryContainer
                            )
                        }
                    )
                }
            }

            // ── Daftar Tempat / Kecamatan Berdasarkan Kota yang Dipilih ──────
            val tempatDitampilkan = remember(kotaTerpilih, userLocation) {
                if (kotaTerpilih == "Sekitar Saya") {
                    if (userLocation != null) {
                        DAFTAR_TEMPAT_KOTA.map { tempat ->
                            val jarak = Lokasi.hitungJarakKm(
                                userLocation.latitude,
                                userLocation.longitude,
                                tempat.lat,
                                tempat.lng
                            )
                            tempat to jarak
                        }.sortedBy { it.second }.take(8)
                    } else {
                        // Jika GPS belum dapat, tampilkan pusat Jember sebagai default awal
                        DAFTAR_TEMPAT_KOTA.filter { it.kota == "Jember" }.map { it to null }
                    }
                } else {
                    DAFTAR_TEMPAT_KOTA.filter { it.kota.equals(kotaTerpilih, ignoreCase = true) }
                        .map { tempat ->
                            val jarak = if (userLocation != null) {
                                Lokasi.hitungJarakKm(
                                    userLocation.latitude,
                                    userLocation.longitude,
                                    tempat.lat,
                                    tempat.lng
                                )
                            } else null
                            tempat to jarak
                        }
                }
            }

            LazyColumn(
                modifier = Modifier.heightIn(max = 240.dp),
                verticalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                items(tempatDitampilkan) { (tempat, jarakKm) ->
                    Surface(
                        shape = RoundedCornerShape(12.dp),
                        tonalElevation = 1.dp,
                        color = MaterialTheme.colorScheme.surface,
                        modifier = Modifier
                            .fillMaxWidth()
                            .bounceClick {
                                onPilih(Titik(tempat.lat, tempat.lng, "${tempat.nama}, ${tempat.kota}"))
                            }
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 14.dp, vertical = 10.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Surface(
                                shape = RoundedCornerShape(8.dp),
                                color = MaterialTheme.colorScheme.primary.copy(alpha = 0.12f),
                                modifier = Modifier.size(34.dp)
                            ) {
                                Box(contentAlignment = Alignment.Center) {
                                    Icon(
                                        imageVector = Icons.Default.LocationOn,
                                        contentDescription = null,
                                        tint = MaterialTheme.colorScheme.primary,
                                        modifier = Modifier.size(18.dp)
                                    )
                                }
                            }

                            Spacer(modifier = Modifier.width(12.dp))

                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = tempat.nama,
                                    style = MaterialTheme.typography.bodyMedium,
                                    fontWeight = FontWeight.Bold,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                                Text(
                                    text = if (tempat.deskripsi.isNotBlank()) "${tempat.kota} · ${tempat.deskripsi}" else tempat.kota,
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    fontSize = 11.sp,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                            }

                            if (jarakKm != null) {
                                Spacer(modifier = Modifier.width(8.dp))
                                Surface(
                                    shape = RoundedCornerShape(6.dp),
                                    color = MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.7f),
                                    modifier = Modifier.padding(start = 4.dp)
                                ) {
                                    Text(
                                        text = if (jarakKm < 1.0) "${(jarakKm * 1000).toInt()} m" else "${String.format(Locale.US, "%.1f", jarakKm)} km",
                                        style = MaterialTheme.typography.labelSmall,
                                        color = MaterialTheme.colorScheme.onSecondaryContainer,
                                        fontWeight = FontWeight.SemiBold,
                                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 3.dp)
                                    )
                                }
                            }
                        }
                    }
                }
            }

            Text(
                text = "Pencarian daerah: © kontributor OpenStreetMap (Nominatim)",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f),
                fontSize = 10.sp
            )
        }
}
