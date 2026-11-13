package com.carijajan.app.ui.screen

import android.Manifest
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
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
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
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

private val INDONESIA = LatLng(-2.5, 118.0)
private const val RADIUS = 3000

@Composable
fun BerandaScreen(
    container: AppContainer,
    onLogin: () -> Unit,
    onDashboard: () -> Unit,
    onVendor: (String) -> Unit
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val user by container.authRepository.user.collectAsStateWithLifecycle()

    var lokasi by remember { mutableStateOf<Titik?>(null) }
    var vendors by remember { mutableStateOf<List<VendorDto>>(emptyList()) }
    var loading by remember { mutableStateOf(false) }
    var error by remember { mutableStateOf("") }
    var query by remember { mutableStateOf("") }
    var areaTerbuka by remember { mutableStateOf(false) }
    var geoLoading by remember { mutableStateOf(false) }
    var pesanLokasi by remember { mutableStateOf("") }

    suspend fun ambilLokasi() {
        geoLoading = true
        pesanLokasi = ""
        val lok = Lokasi.ambil(context)
        geoLoading = false
        if (lok == null) {
            pesanLokasi = "Gagal membaca lokasi. Pilih area manual."
            areaTerbuka = true
            return
        }
        lokasi = Titik(lok.latitude, lok.longitude, "Lokasi saya")
        areaTerbuka = false
    }

    val permissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { hasil ->
        if (hasil.values.any { it }) {
            scope.launch { ambilLokasi() }
        } else {
            pesanLokasi = "Izin lokasi ditolak. Pilih area manual."
            areaTerbuka = true
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
            areaTerbuka = true
        }
    }

    LaunchedEffect(lokasi) {
        val titik = lokasi ?: return@LaunchedEffect
        loading = true
        error = ""
        val res = container.vendorRepository.nearby(titik.lat, titik.lng, RADIUS)
        res.onSuccess { vendors = it }
            .onFailure {
                error = it.message ?: "Gagal mengambil data warung"
                vendors = emptyList()
            }
        loading = false
    }

    val vendorsTampil = remember(vendors, query) {
        val kata = query.trim()
        if (kata.isBlank()) vendors
        else vendors.filter { it.namaToko.contains(kata, ignoreCase = true) }
    }

    val center = lokasi?.let { LatLng(it.lat, it.lng) } ?: INDONESIA
    val zoom = if (lokasi != null) 15.0 else 5.0

    Box(modifier = Modifier.fillMaxSize()) {
        PetaWarung(
            modifier = Modifier.fillMaxSize(),
            center = center,
            zoom = zoom,
            userLocation = lokasi?.let { LatLng(it.lat, it.lng) },
            vendors = vendorsTampil,
            selectedId = null,
            onSelectVendor = onVendor
        )

        Column(
            modifier = Modifier
                .align(Alignment.TopCenter)
                .fillMaxWidth()
                .statusBarsPadding()
                .padding(12.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Surface(
                shape = RoundedCornerShape(16.dp),
                tonalElevation = 4.dp,
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "Cari Jajan",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = lokasi?.label?.ifBlank { "Area dipilih" } ?: "Atur lokasi",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                    TextButton(onClick = { areaTerbuka = true }) { Text("Lokasi") }
                    TextButton(onClick = { if (user == null) onLogin() else onDashboard() }) {
                        Text(if (user == null) "Masuk" else "Dashboard")
                    }
                }
            }

            OutlinedTextField(
                value = query,
                onValueChange = { query = it },
                placeholder = { Text("Cari warung...") },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true,
                shape = RoundedCornerShape(16.dp)
            )
        }

        Surface(
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .fillMaxWidth()
                .navigationBarsPadding(),
            shape = RoundedCornerShape(topStart = 20.dp, topEnd = 20.dp),
            tonalElevation = 8.dp
        ) {
            Column(modifier = Modifier.padding(horizontal = 14.dp, vertical = 10.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = "Warung terdekat",
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.SemiBold,
                        modifier = Modifier.weight(1f)
                    )
                    Text(
                        text = "${vendorsTampil.size} warung · 3 km",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

                if (loading) {
                    LinearProgressIndicator(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(top = 8.dp)
                    )
                }

                if (error.isNotBlank()) {
                    Row(
                        modifier = Modifier.fillMaxWidth().padding(top = 6.dp),
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
                        text = if (lokasi == null) "Atur lokasi untuk melihat warung terdekat."
                        else if (query.isNotBlank()) "Tidak ada warung yang cocok."
                        else "Belum ada warung dalam radius 3 km.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(vertical = 14.dp)
                    )
                }

                LazyColumn(modifier = Modifier.heightIn(max = 260.dp)) {
                    items(vendorsTampil) { vendor ->
                        VendorRow(vendor = vendor, onClick = { onVendor(vendor.id) })
                    }
                }
            }
        }
    }

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
