package com.carijajan.app.ui.screen

import androidx.compose.foundation.BorderStroke
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
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.HelpOutline
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Eco
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.LocationSearching
import androidx.compose.material.icons.filled.Logout
import androidx.compose.material.icons.filled.Radar
import androidx.compose.material.icons.filled.Storefront
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VolunteerActivism
import androidx.compose.material.icons.filled.WorkspacePremium
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import coil3.compose.AsyncImage
import com.carijajan.app.AppContainer
import com.carijajan.app.data.VendorDto
import com.carijajan.app.util.Format
import com.carijajan.app.util.Gamifikasi
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch

@Composable
fun DashboardScreen(
    container: AppContainer,
    onBack: () -> Unit,
    onAdd: () -> Unit,
    onEdit: (String) -> Unit,
    onVendor: (String) -> Unit,
    onLogout: () -> Unit
) {
    val scope = rememberCoroutineScope()
    val user by container.authRepository.user.collectAsStateWithLifecycle()
    val defaultTipeAkun by container.authRepository.tipeAkun.collectAsStateWithLifecycle()

    // Mode tampilan: "penjual" atau "pembeli" (pengguna bisa bebas beralih untuk demonstrasi)
    var modeDashboard by remember { mutableStateOf(defaultTipeAkun) }
    LaunchedEffect(defaultTipeAkun) {
        modeDashboard = defaultTipeAkun
    }

    var vendors by remember { mutableStateOf<List<VendorDto>>(emptyList()) }
    var loading by remember { mutableStateOf(true) }
    var error by remember { mutableStateOf("") }
    var deleteTarget by remember { mutableStateOf<VendorDto?>(null) }
    var deleteLoading by remember { mutableStateOf(false) }

    // Dialog usulan revisi untuk pembeli
    var dialogRevisiWarung by remember { mutableStateOf<VendorDto?>(null) }
    var dialogRevisiTerkirim by remember { mutableStateOf(false) }

    // State Zero Waste untuk Penjual
    var zeroWasteAktif by remember { mutableStateOf(false) }
    var zeroWasteDiskon by remember { mutableIntStateOf(30) }
    var zeroWasteCatatan by remember { mutableStateOf("Diskon 30% jelang tutup! Makanan enak, hemat, dan cegah makanan terbuang.") }
    var zeroWasteDisimpan by remember { mutableStateOf(false) }

    // Views & impressions
    var totalViews by remember { mutableIntStateOf(142) }
    var totalRadar by remember { mutableIntStateOf(86) }

    suspend fun load() {
        loading = true
        val result = container.vendorRepository.mine()
        result.onSuccess { list ->
            vendors = list
            if (list.isNotEmpty()) {
                val firstId = list.first().id
                zeroWasteAktif = container.tokenStore.isZeroWasteActive(firstId).first()
                zeroWasteDiskon = container.tokenStore.getZeroWasteDiscount(firstId).first()
                zeroWasteCatatan = container.tokenStore.getZeroWasteNote(firstId).first()
                totalViews = container.tokenStore.getVendorViews(firstId).first()
                totalRadar = container.tokenStore.getRadarImpressions(firstId).first()
            }
        }.onFailure {
            error = it.message ?: "Gagal mengambil data warung"
        }
        loading = false
    }

    LaunchedEffect(Unit) { load() }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .statusBarsPadding()
            .padding(horizontal = 16.dp)
    ) {
        // ── Header Bar ────────────────────────────────────────────────────────
        Row(
            modifier = Modifier.fillMaxWidth().padding(top = 8.dp, bottom = 12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(onClick = onBack) {
                Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Kembali")
            }
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = if (modeDashboard == "penjual") "Dashboard Pemilik Warung" else "Dashboard Pembeli & Kontributor",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
                val namaUser = user?.name?.takeIf { it.isNotBlank() } ?: user?.email.orEmpty()
                Text("Halo, $namaUser", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            IconButton(onClick = {
                scope.launch {
                    container.authRepository.logout()
                    onLogout()
                }
            }) {
                Icon(Icons.Filled.Logout, contentDescription = "Keluar", tint = MaterialTheme.colorScheme.error)
            }
        }

        // ── Switcher Mode Dashboard (Penjual vs Pembeli) ────────────────────────
        Surface(
            color = Color(0xFFF4F4F5),
            shape = RoundedCornerShape(12.dp),
            modifier = Modifier.fillMaxWidth().padding(bottom = 14.dp)
        ) {
            Row(modifier = Modifier.padding(4.dp)) {
                val isPjl = modeDashboard == "penjual"
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .clip(RoundedCornerShape(9.dp))
                        .background(if (isPjl) Color(0xFF059669) else Color.Transparent)
                        .clickable {
                            modeDashboard = "penjual"
                            scope.launch { container.authRepository.setTipeAkun("penjual") }
                        }
                        .padding(vertical = 8.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            Icons.Filled.Storefront,
                            contentDescription = null,
                            tint = if (isPjl) Color.White else Color(0xFF52525B),
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(Modifier.width(6.dp))
                        Text(
                            "Mode Penjual",
                            fontSize = 12.sp,
                            fontWeight = if (isPjl) FontWeight.Bold else FontWeight.Medium,
                            color = if (isPjl) Color.White else Color(0xFF52525B)
                        )
                    }
                }

                val isPbl = modeDashboard == "pembeli"
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .clip(RoundedCornerShape(9.dp))
                        .background(if (isPbl) Color(0xFF2563EB) else Color.Transparent)
                        .clickable {
                            modeDashboard = "pembeli"
                            scope.launch { container.authRepository.setTipeAkun("pembeli") }
                        }
                        .padding(vertical = 8.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            Icons.Filled.VolunteerActivism,
                            contentDescription = null,
                            tint = if (isPbl) Color.White else Color(0xFF52525B),
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(Modifier.width(6.dp))
                        Text(
                            "Mode Pembeli",
                            fontSize = 12.sp,
                            fontWeight = if (isPbl) FontWeight.Bold else FontWeight.Medium,
                            color = if (isPbl) Color.White else Color(0xFF52525B)
                        )
                    }
                }
            }
        }

        LazyColumn(
            verticalArrangement = Arrangement.spacedBy(14.dp),
            modifier = Modifier.weight(1f)
        ) {
            // =========================================================================
            // 🍲 TAMPILAN DASHBOARD PEMBELI & KONTRIBUTOR KOMUNITAS
            // =========================================================================
            if (modeDashboard == "pembeli") {
                val xp = user?.xp ?: 0
                val level = Gamifikasi.levelDariXp(xp)
                val gelar = Gamifikasi.gelarLevel(level)
                val progress = Gamifikasi.progressLevel(xp)
                val sisaXp = Gamifikasi.XP_PER_LEVEL - (xp % Gamifikasi.XP_PER_LEVEL)

                // 1. Kartu Gamifikasi Kontributor (Level & XP)
                item {
                    Surface(
                        shape = RoundedCornerShape(16.dp),
                        color = Color(0xFFEFF6FF),
                        border = BorderStroke(1.dp, Color(0xFFBFDBFE)),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(modifier = Modifier.padding(16.dp)) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.SpaceBetween,
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Box(
                                        modifier = Modifier
                                            .size(44.dp)
                                            .clip(CircleShape)
                                            .background(Color(0xFF2563EB)),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Icon(
                                            Icons.Filled.WorkspacePremium,
                                            contentDescription = null,
                                            tint = Color.White,
                                            modifier = Modifier.size(24.dp)
                                        )
                                    }
                                    Spacer(Modifier.width(12.dp))
                                    Column {
                                        Text("Level $level", fontWeight = FontWeight.Bold, fontSize = 17.sp, color = Color(0xFF1E3A8A))
                                        Text(gelar, fontSize = 12.sp, fontWeight = FontWeight.SemiBold, color = Color(0xFF2563EB))
                                    }
                                }

                                Surface(
                                    color = Color.White,
                                    shape = RoundedCornerShape(20.dp),
                                    border = BorderStroke(1.dp, Color(0xFF93C5FD))
                                ) {
                                    Text(
                                        "$xp XP Total",
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 13.sp,
                                        color = Color(0xFF1D4ED8),
                                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
                                    )
                                }
                            }

                            Spacer(Modifier.height(14.dp))
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Text("Progres Level Berikutnya", fontSize = 11.sp, color = Color(0xFF3B82F6))
                                Text("$sisaXp XP lagi menuju Level ${level + 1}", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = Color(0xFF1E40AF))
                            }
                            Spacer(Modifier.height(6.dp))
                            LinearProgressIndicator(
                                progress = { progress },
                                color = Color(0xFF2563EB),
                                trackColor = Color(0xFFDBEAFE),
                                modifier = Modifier.fillMaxWidth().height(8.dp).clip(RoundedCornerShape(4.dp))
                            )

                            Spacer(Modifier.height(10.dp))
                            Text(
                                "🎁 Setiap warung baru yang Anda ajukan dan lolos verifikasi admin memberikan +50 XP!",
                                fontSize = 11.sp,
                                color = Color(0xFF1E3A8A)
                            )
                        }
                    }
                }

                // 2. Tombol Aksi Tambah / Ajukan Warung Baru
                item {
                    Button(
                        onClick = onAdd,
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF2563EB)),
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier.fillMaxWidth().height(46.dp)
                    ) {
                        Icon(Icons.Filled.Add, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(Modifier.width(8.dp))
                        Text("Ajukan Lokasi Warung Baru (+50 XP)", fontWeight = FontWeight.Bold)
                    }
                }

                // 3. Daftar Warung yang Diajukan oleh Kontributor
                item {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.padding(top = 4.dp)
                    ) {
                        Text(
                            "Warung yang Anda Ajukan",
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.weight(1f)
                        )
                        Text("${vendors.size} Warung", fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                }

                if (loading) {
                    item {
                        Box(Modifier.fillMaxWidth().padding(24.dp), contentAlignment = Alignment.Center) {
                            CircularProgressIndicator(color = Color(0xFF2563EB))
                        }
                    }
                } else if (vendors.isEmpty()) {
                    item {
                        Surface(
                            shape = RoundedCornerShape(14.dp),
                            color = Color(0xFFF9FAFB),
                            border = BorderStroke(1.dp, Color(0xFFE5E7EB)),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Column(
                                modifier = Modifier.padding(20.dp),
                                horizontalAlignment = Alignment.CenterHorizontally
                            ) {
                                Icon(Icons.Filled.LocationSearching, contentDescription = null, tint = Color(0xFF9CA3AF), modifier = Modifier.size(36.dp))
                                Spacer(Modifier.height(8.dp))
                                Text("Belum Ada Warung yang Diajukan", fontWeight = FontWeight.SemiBold, fontSize = 14.sp)
                                Text(
                                    "Temukan pedagang kaki lima atau warung UMKM tersembunyi di sekitar Anda dan jadilah yang pertama membagikannya!",
                                    fontSize = 12.sp,
                                    color = Color(0xFF6B7280),
                                    textAlign = androidx.compose.ui.text.style.TextAlign.Center,
                                    modifier = Modifier.padding(top = 4.dp)
                                )
                            }
                        }
                    }
                } else {
                    items(vendors, key = { it.id }) { vendor ->
                        PembeliVendorCard(
                            vendor = vendor,
                            onView = { onVendor(vendor.id) },
                            onRequestEdit = { dialogRevisiWarung = vendor }
                        )
                    }
                }
            }

            // =========================================================================
            // 🏪 TAMPILAN DASHBOARD PENJUAL / PEMILIK WARUNG
            // =========================================================================
            else {
                // 1. Kartu Statistik Pengunjung & Radar
                item {
                    Surface(
                        shape = RoundedCornerShape(16.dp),
                        color = Color(0xFFECFDF5),
                        border = BorderStroke(1.dp, Color(0xFFA7F3D0)),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(modifier = Modifier.padding(16.dp)) {
                            Text("Statistik Performa Warung", fontWeight = FontWeight.Bold, fontSize = 15.sp, color = Color(0xFF065F46))
                            Spacer(Modifier.height(12.dp))

                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(10.dp)
                            ) {
                                Surface(
                                    color = Color.White,
                                    shape = RoundedCornerShape(12.dp),
                                    modifier = Modifier.weight(1f)
                                ) {
                                    Column(modifier = Modifier.padding(12.dp)) {
                                        Row(verticalAlignment = Alignment.CenterVertically) {
                                            Icon(Icons.Filled.Visibility, contentDescription = null, tint = Color(0xFF059669), modifier = Modifier.size(16.dp))
                                            Spacer(Modifier.width(6.dp))
                                            Text("Dilihat Pembeli", fontSize = 11.sp, color = Color(0xFF047857))
                                        }
                                        Spacer(Modifier.height(6.dp))
                                        Text("$totalViews", fontWeight = FontWeight.ExtraBold, fontSize = 20.sp, color = Color(0xFF064E3B))
                                        Text("kali dilihat", fontSize = 10.sp, color = Color(0xFF6B7280))
                                    }
                                }

                                Surface(
                                    color = Color.White,
                                    shape = RoundedCornerShape(12.dp),
                                    modifier = Modifier.weight(1f)
                                ) {
                                    Column(modifier = Modifier.padding(12.dp)) {
                                        Row(verticalAlignment = Alignment.CenterVertically) {
                                            Icon(Icons.Filled.Radar, contentDescription = null, tint = Color(0xFF2563EB), modifier = Modifier.size(16.dp))
                                            Spacer(Modifier.width(6.dp))
                                            Text("Masuk Radar", fontSize = 11.sp, color = Color(0xFF1D4ED8))
                                        }
                                        Spacer(Modifier.height(6.dp))
                                        Text("$totalRadar", fontWeight = FontWeight.ExtraBold, fontSize = 20.sp, color = Color(0xFF1E3A8A))
                                        Text("radius pembeli", fontSize = 10.sp, color = Color(0xFF6B7280))
                                    }
                                }
                            }
                        }
                    }
                }

                // 2. Fitur Zero Waste Food (Penyelamat Makanan & Diskon Laris)
                item {
                    Surface(
                        shape = RoundedCornerShape(16.dp),
                        color = Color(0xFFFFFBEB),
                        border = BorderStroke(1.dp, Color(0xFFFDE68A)),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(modifier = Modifier.padding(16.dp)) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Box(
                                        modifier = Modifier
                                            .size(34.dp)
                                            .clip(CircleShape)
                                            .background(Color(0xFFD97706)),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Icon(Icons.Filled.Eco, contentDescription = null, tint = Color.White, modifier = Modifier.size(18.dp))
                                    }
                                    Spacer(Modifier.width(10.dp))
                                    Column {
                                        Text("Zero Waste Food", fontWeight = FontWeight.Bold, fontSize = 14.sp, color = Color(0xFF92400E))
                                        Text("Cegah makanan terbuang & cepat laku", fontSize = 11.sp, color = Color(0xFFB45309))
                                    }
                                }

                                Switch(
                                    checked = zeroWasteAktif,
                                    onCheckedChange = { zeroWasteAktif = it },
                                    colors = SwitchDefaults.colors(
                                        checkedThumbColor = Color.White,
                                        checkedTrackColor = Color(0xFFD97706)
                                    )
                                )
                            }

                            if (zeroWasteAktif) {
                                Spacer(Modifier.height(10.dp))
                                HorizontalDivider(color = Color(0xFFFDE68A))
                                Spacer(Modifier.height(10.dp))

                                Text("Pilih Potongan Diskon Jelang Tutup:", fontSize = 12.sp, fontWeight = FontWeight.SemiBold, color = Color(0xFF92400E))
                                Row(
                                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                                    modifier = Modifier.padding(vertical = 6.dp)
                                ) {
                                    listOf(20, 30, 50).forEach { diskon ->
                                        FilterChip(
                                            selected = zeroWasteDiskon == diskon,
                                            onClick = { zeroWasteDiskon = diskon },
                                            label = { Text("Diskon $diskon%", fontSize = 12.sp) },
                                            colors = FilterChipDefaults.filterChipColors(
                                                selectedContainerColor = Color(0xFFD97706),
                                                selectedLabelColor = Color.White
                                            )
                                        )
                                    }
                                }

                                OutlinedTextField(
                                    value = zeroWasteCatatan,
                                    onValueChange = { zeroWasteCatatan = it },
                                    label = { Text("Pesan Promo untuk Pembeli", fontSize = 11.sp) },
                                    modifier = Modifier.fillMaxWidth().padding(top = 4.dp),
                                    shape = RoundedCornerShape(10.dp)
                                )

                                Spacer(Modifier.height(8.dp))
                                Button(
                                    onClick = {
                                        if (vendors.isNotEmpty()) {
                                            val id = vendors.first().id
                                            scope.launch {
                                                container.tokenStore.saveZeroWaste(
                                                    vendorId = id,
                                                    active = zeroWasteAktif,
                                                    discount = zeroWasteDiskon,
                                                    note = zeroWasteCatatan
                                                )
                                                zeroWasteDisimpan = true
                                            }
                                        }
                                    },
                                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFD97706)),
                                    shape = RoundedCornerShape(10.dp),
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    Text(if (zeroWasteDisimpan) "✓ Promo Berhasil Diaktifkan" else "Simpan Promo Zero Waste", fontWeight = FontWeight.Bold, fontSize = 12.sp)
                                }
                            }
                        }
                    }
                }

                // 3. Tombol Tambah Warung Baru
                item {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.padding(top = 4.dp)
                    ) {
                        Text(
                            "Warung Milik Anda",
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.weight(1f)
                        )
                        Button(
                            onClick = onAdd,
                            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF059669)),
                            shape = RoundedCornerShape(10.dp)
                        ) {
                            Text("+ Tambah Warung", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                        }
                    }
                }

                if (loading) {
                    item {
                        Box(Modifier.fillMaxWidth().padding(24.dp), contentAlignment = Alignment.Center) {
                            CircularProgressIndicator(color = Color(0xFF059669))
                        }
                    }
                } else if (vendors.isEmpty()) {
                    item {
                        Surface(
                            shape = RoundedCornerShape(14.dp),
                            color = Color(0xFFF9FAFB),
                            border = BorderStroke(1.dp, Color(0xFFE5E7EB)),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Column(
                                modifier = Modifier.padding(20.dp),
                                horizontalAlignment = Alignment.CenterHorizontally
                            ) {
                                Icon(Icons.Filled.Storefront, contentDescription = null, tint = Color(0xFF9CA3AF), modifier = Modifier.size(36.dp))
                                Spacer(Modifier.height(8.dp))
                                Text("Belum Ada Warung Terdaftar", fontWeight = FontWeight.SemiBold, fontSize = 14.sp)
                                Text(
                                    "Daftarkan warung atau gerobak Anda sekarang agar muncul di peta pembeli terdekat!",
                                    fontSize = 12.sp,
                                    color = Color(0xFF6B7280),
                                    textAlign = androidx.compose.ui.text.style.TextAlign.Center,
                                    modifier = Modifier.padding(top = 4.dp)
                                )
                            }
                        }
                    }
                } else {
                    items(vendors, key = { it.id }) { vendor ->
                        PenjualVendorCard(
                            vendor = vendor,
                            zeroWasteAktif = zeroWasteAktif,
                            zeroWasteDiskon = zeroWasteDiskon,
                            onView = { onVendor(vendor.id) },
                            onEdit = { onEdit(vendor.id) },
                            onDelete = { deleteTarget = vendor }
                        )
                    }
                }
            }
        }
    }

    // ── Dialog Usul Revisi untuk Pembeli ──────────────────────────────────────
    dialogRevisiWarung?.let { v ->
        AlertDialog(
            onDismissRequest = {
                dialogRevisiWarung = null
                dialogRevisiTerkirim = false
            },
            icon = { Icon(Icons.AutoMirrored.Filled.HelpOutline, contentDescription = null, tint = Color(0xFF2563EB)) },
            title = { Text("Ajukan Usulan Perubahan") },
            text = {
                if (dialogRevisiTerkirim) {
                    Text(
                        "✓ Terima kasih! Usulan revisi untuk warung '${v.namaToko}' telah dikirim ke kurator admin & pemilik warung untuk ditinjau.",
                        color = Color(0xFF059669),
                        fontWeight = FontWeight.Medium
                    )
                } else {
                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Text(
                            "Sebagai pembeli/kontributor komunitas, Anda tidak dapat mengubah data warung '${v.namaToko}' secara sepihak demi menjaga keaslian data.",
                            style = MaterialTheme.typography.bodyMedium
                        )
                        Text(
                            "Apakah Anda ingin mengirim usulan perubahan data (lokasi/jam buka/menu) ke kurator admin dan pemilik warung?",
                            fontSize = 12.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            },
            confirmButton = {
                if (!dialogRevisiTerkirim) {
                    Button(
                        onClick = { dialogRevisiTerkirim = true },
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF2563EB))
                    ) {
                        Text("Kirim Usulan Revisi")
                    }
                } else {
                    Button(
                        onClick = {
                            dialogRevisiWarung = null
                            dialogRevisiTerkirim = false
                        }
                    ) {
                        Text("Tutup")
                    }
                }
            },
            dismissButton = {
                if (!dialogRevisiTerkirim) {
                    TextButton(onClick = { dialogRevisiWarung = null }) { Text("Batal") }
                }
            }
        )
    }

    // ── Dialog Hapus Warung untuk Penjual ─────────────────────────────────────
    deleteTarget?.let { vendor ->
        AlertDialog(
            onDismissRequest = { if (!deleteLoading) deleteTarget = null },
            title = { Text("Hapus Warung?") },
            text = { Text("Warung '${vendor.namaToko}' akan dihapus permanen dari daftar Anda.") },
            confirmButton = {
                TextButton(
                    enabled = !deleteLoading,
                    onClick = {
                        deleteLoading = true
                        scope.launch {
                            val result = container.vendorRepository.delete(vendor.id)
                            deleteLoading = false
                            result.onSuccess {
                                deleteTarget = null
                                load()
                            }.onFailure { error = it.message ?: "Gagal menghapus warung" }
                        }
                    }
                ) {
                    Text(if (deleteLoading) "Menghapus..." else "Hapus", color = MaterialTheme.colorScheme.error)
                }
            },
            dismissButton = {
                TextButton(
                    onClick = { deleteTarget = null },
                    enabled = !deleteLoading
                ) { Text("Batal") }
            }
        )
    }
}

// ── Komponen Kartu Warung Khusus Pembeli (Kontributor) ────────────────────────
@Composable
private fun PembeliVendorCard(
    vendor: VendorDto,
    onView: () -> Unit,
    onRequestEdit: () -> Unit
) {
    Surface(
        shape = RoundedCornerShape(14.dp),
        border = BorderStroke(1.dp, Color(0xFFE4E4E7)),
        color = Color.White,
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.padding(14.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                if (vendor.foto.isNotBlank()) {
                    AsyncImage(
                        model = vendor.foto,
                        contentDescription = null,
                        modifier = Modifier.size(54.dp).clip(RoundedCornerShape(8.dp))
                    )
                    Spacer(Modifier.width(10.dp))
                }
                Column(modifier = Modifier.weight(1f)) {
                    Text(vendor.namaToko, fontWeight = FontWeight.Bold, fontSize = 15.sp, maxLines = 1, overflow = TextOverflow.Ellipsis)
                    Text(
                        "${Format.labelKategori(vendor.kategori)} · ${vendor.alamatPatokan}",
                        fontSize = 11.sp,
                        color = Color(0xFF71717A),
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )

                    // Badge Status Kurasi
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                        modifier = Modifier.padding(top = 4.dp)
                    ) {
                        val isApproved = vendor.status == "approved"
                        Surface(
                            color = if (isApproved) Color(0xFFECFDF5) else Color(0xFFFEF3C7),
                            shape = RoundedCornerShape(50)
                        ) {
                            Text(
                                text = if (isApproved) "✓ Terverifikasi (+50 XP)" else "⏳ Menunggu Kurasi Admin",
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Bold,
                                color = if (isApproved) Color(0xFF047857) else Color(0xFFB45309),
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp)
                            )
                        }
                    }
                }
            }

            Spacer(Modifier.height(10.dp))
            HorizontalDivider(color = Color(0xFFF4F4F5))
            Spacer(Modifier.height(6.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.End,
                verticalAlignment = Alignment.CenterVertically
            ) {
                TextButton(onClick = onView) { Text("Lihat di Peta", fontSize = 12.sp) }
                Spacer(Modifier.width(4.dp))
                OutlinedButton(
                    onClick = onRequestEdit,
                    shape = RoundedCornerShape(8.dp)
                ) {
                    Icon(Icons.Filled.Edit, contentDescription = null, modifier = Modifier.size(13.dp))
                    Spacer(Modifier.width(4.dp))
                    Text("Ajukan Revisi", fontSize = 11.sp)
                }
            }
        }
    }
}

// ── Komponen Kartu Warung Khusus Pemilik Warung (Penjual) ─────────────────────
@Composable
private fun PenjualVendorCard(
    vendor: VendorDto,
    zeroWasteAktif: Boolean,
    zeroWasteDiskon: Int,
    onView: () -> Unit,
    onEdit: () -> Unit,
    onDelete: () -> Unit
) {
    Surface(
        shape = RoundedCornerShape(14.dp),
        border = BorderStroke(1.dp, Color(0xFFE4E4E7)),
        color = Color.White,
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.padding(14.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                if (vendor.foto.isNotBlank()) {
                    AsyncImage(
                        model = vendor.foto,
                        contentDescription = null,
                        modifier = Modifier.size(56.dp).clip(RoundedCornerShape(8.dp))
                    )
                    Spacer(Modifier.width(10.dp))
                }
                Column(modifier = Modifier.weight(1f)) {
                    Text(vendor.namaToko, fontWeight = FontWeight.Bold, fontSize = 15.sp, maxLines = 1, overflow = TextOverflow.Ellipsis)
                    Text(
                        "${Format.labelKategori(vendor.kategori)} · Jam ${vendor.jamBuka}-${vendor.jamTutup}",
                        fontSize = 11.sp,
                        color = Color(0xFF71717A)
                    )

                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                        modifier = Modifier.padding(top = 4.dp)
                    ) {
                        // Badge Status
                        val isApproved = vendor.status == "approved"
                        Surface(
                            color = if (isApproved) Color(0xFFECFDF5) else Color(0xFFFEF3C7),
                            shape = RoundedCornerShape(50)
                        ) {
                            Text(
                                text = if (isApproved) "✓ Aktif di Peta" else "⏳ Menunggu Verifikasi",
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Bold,
                                color = if (isApproved) Color(0xFF047857) else Color(0xFFB45309),
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp)
                            )
                        }

                        // Badge Zero Waste jika aktif
                        if (zeroWasteAktif) {
                            Surface(
                                color = Color(0xFFFFFBEB),
                                border = BorderStroke(1.dp, Color(0xFFFDE68A)),
                                shape = RoundedCornerShape(50)
                            ) {
                                Text(
                                    "🌿 Zero Waste -$zeroWasteDiskon%",
                                    fontSize = 10.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = Color(0xFFD97706),
                                    modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                )
                            }
                        }
                    }
                }
            }

            // Info Verifikasi Awal
            if (vendor.status != "approved") {
                Surface(
                    color = Color(0xFFFFFBEB),
                    shape = RoundedCornerShape(8.dp),
                    modifier = Modifier.fillMaxWidth().padding(top = 8.dp)
                ) {
                    Row(
                        modifier = Modifier.padding(8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(Icons.Filled.Info, contentDescription = null, tint = Color(0xFFD97706), modifier = Modifier.size(15.dp))
                        Spacer(Modifier.width(6.dp))
                        Text(
                            "Warung baru memerlukan kurasi admin untuk awalan saja sebelum tampil di peta.",
                            fontSize = 11.sp,
                            color = Color(0xFF92400E)
                        )
                    }
                }
            }

            Spacer(Modifier.height(10.dp))
            HorizontalDivider(color = Color(0xFFF4F4F5))
            Spacer(Modifier.height(6.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.End,
                verticalAlignment = Alignment.CenterVertically
            ) {
                TextButton(onClick = onView) { Text("Lihat", fontSize = 12.sp) }
                TextButton(onClick = onEdit) {
                    Text("Edit Warung & Menu", fontSize = 12.sp, color = Color(0xFF059669), fontWeight = FontWeight.Bold)
                }
                TextButton(onClick = onDelete) {
                    Text("Hapus", fontSize = 12.sp, color = MaterialTheme.colorScheme.error)
                }
            }
        }
    }
}
