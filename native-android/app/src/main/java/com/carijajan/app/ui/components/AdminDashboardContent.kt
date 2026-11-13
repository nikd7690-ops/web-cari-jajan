package com.carijajan.app.ui.components

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
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Email
import androidx.compose.material.icons.filled.Flag
import androidx.compose.material.icons.filled.Group
import androidx.compose.material.icons.filled.MilitaryTech
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Storefront
import androidx.compose.material.icons.filled.Visibility
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
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
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
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.carijajan.app.AppContainer
import com.carijajan.app.data.LaporanDto
import com.carijajan.app.data.TopKontributorDto
import com.carijajan.app.data.UserDto
import com.carijajan.app.data.VendorDto
import com.carijajan.app.util.Format
import kotlinx.coroutines.launch

@Composable
fun AdminDashboardContent(
    container: AppContainer,
    onViewVendor: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    val scope = rememberCoroutineScope()
    var tabIndex by remember { mutableIntStateOf(0) } // 0: Warung, 1: Member & Kontributor, 2: Laporan

    var vendors by remember { mutableStateOf<List<VendorDto>>(emptyList()) }
    var members by remember { mutableStateOf<List<UserDto>>(emptyList()) }
    var topKontributor by remember { mutableStateOf<List<TopKontributorDto>>(emptyList()) }
    var reports by remember { mutableStateOf<List<LaporanDto>>(emptyList()) }

    var loading by remember { mutableStateOf(true) }
    var pesanSukses by remember { mutableStateOf("") }
    var errorPesan by remember { mutableStateOf("") }
    var filterStatusVendor by remember { mutableStateOf("semua") }

    // Dialog aksi Ban Member
    var dialogBanTarget by remember { mutableStateOf<UserDto?>(null) }
    var banReason by remember { mutableStateOf("") }
    var banLoading by remember { mutableStateOf(false) }

    fun refresh() {
        loading = true
        errorPesan = ""
        scope.launch {
            val resVendors = container.vendorRepository.adminVendors(filterStatusVendor)
            val resMembers = container.vendorRepository.adminMembers()
            val resReports = container.vendorRepository.daftarLaporan()

            resVendors.onSuccess {
                vendors = it
            }.onFailure {
                errorPesan = "Gagal memuat warung: ${it.message}"
            }

            resMembers.onSuccess { memData ->
                members = memData.members
                topKontributor = memData.topKontributor
            }.onFailure {
                if (errorPesan.isBlank()) errorPesan = "Gagal memuat member: ${it.message}"
            }

            resReports.onSuccess {
                reports = it
            }.onFailure {
                if (errorPesan.isBlank()) errorPesan = "Gagal memuat laporan: ${it.message}"
            }

            loading = false
        }
    }

    LaunchedEffect(Unit) {
        refresh()
    }

    Column(modifier = modifier.fillMaxWidth()) {
        // Tab Navigasi Admin
        TabRow(
            selectedTabIndex = tabIndex,
            containerColor = Color.Transparent,
            contentColor = MaterialTheme.colorScheme.primary,
            modifier = Modifier.fillMaxWidth()
        ) {
            Tab(
                selected = (tabIndex == 0),
                onClick = { tabIndex = 0 },
                text = { Text("Toko (${vendors.size})", fontSize = 12.sp, fontWeight = FontWeight.Bold) }
            )
            Tab(
                selected = (tabIndex == 1),
                onClick = { tabIndex = 1 },
                text = { Text("Member (${members.size})", fontSize = 12.sp, fontWeight = FontWeight.Bold) }
            )
            Tab(
                selected = (tabIndex == 2),
                onClick = { tabIndex = 2 },
                text = {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text("Laporan", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                        if (reports.isNotEmpty()) {
                            Spacer(Modifier.width(4.dp))
                            Surface(
                                shape = CircleShape,
                                color = Color(0xFFEF4444)
                            ) {
                                Text(
                                    "${reports.size}",
                                    color = Color.White,
                                    fontSize = 10.sp,
                                    fontWeight = FontWeight.Bold,
                                    modifier = Modifier.padding(horizontal = 5.dp, vertical = 1.dp)
                                )
                            }
                        }
                    }
                }
            )
        }

        Spacer(Modifier.height(8.dp))

        if (pesanSukses.isNotBlank()) {
            Surface(
                color = Color(0xFFDCFCE7),
                shape = RoundedCornerShape(8.dp),
                border = BorderStroke(1.dp, Color(0xFF86EFAC)),
                modifier = Modifier.fillMaxWidth().padding(bottom = 8.dp)
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(Icons.Filled.CheckCircle, contentDescription = null, tint = Color(0xFF15803D), modifier = Modifier.size(16.dp))
                    Spacer(Modifier.width(6.dp))
                    Text(pesanSukses, color = Color(0xFF166534), fontSize = 12.sp, modifier = Modifier.weight(1f))
                    IconButton(onClick = { pesanSukses = "" }, modifier = Modifier.size(20.dp)) {
                        Icon(Icons.Filled.Close, contentDescription = null, tint = Color(0xFF15803D), modifier = Modifier.size(14.dp))
                    }
                }
            }
        }

        if (errorPesan.isNotBlank()) {
            Surface(
                color = Color(0xFFFEE2E2),
                shape = RoundedCornerShape(8.dp),
                border = BorderStroke(1.dp, Color(0xFFFCA5A5)),
                modifier = Modifier.fillMaxWidth().padding(bottom = 8.dp)
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(Icons.Filled.Close, contentDescription = null, tint = Color(0xFFDC2626), modifier = Modifier.size(16.dp))
                    Spacer(Modifier.width(6.dp))
                    Text(errorPesan, color = Color(0xFF991B1B), fontSize = 12.sp, modifier = Modifier.weight(1f))
                    IconButton(onClick = { errorPesan = "" }, modifier = Modifier.size(20.dp)) {
                        Icon(Icons.Filled.Close, contentDescription = null, tint = Color(0xFFDC2626), modifier = Modifier.size(14.dp))
                    }
                }
            }
        }

        if (loading) {
            Box(Modifier.fillMaxWidth().padding(32.dp), contentAlignment = Alignment.Center) {
                CircularProgressIndicator(color = MaterialTheme.colorScheme.primary)
            }
        } else {
            when (tabIndex) {
                // ── 0. DAFTAR TOKO / WARUNG ADMIN ────────────────────────────────────
                0 -> {
                    // Filter Status
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .horizontalScroll(rememberScrollState()),
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        listOf(
                            "semua" to "Semua (${vendors.size})",
                            "pending" to "Menunggu Kurasi (${vendors.count { it.status == "pending" }})",
                            "approved" to "Tayang (${vendors.count { it.status == "approved" }})",
                            "rejected" to "Ditolak (${vendors.count { it.status == "rejected" }})"
                        ).forEach { (key, label) ->
                            FilterChip(
                                selected = (filterStatusVendor == key),
                                onClick = { filterStatusVendor = key },
                                label = { Text(label, fontSize = 11.sp) }
                            )
                        }
                    }

                    Spacer(Modifier.height(8.dp))

                    val filteredVendors = vendors.filter {
                        if (filterStatusVendor == "semua") true else it.status == filterStatusVendor
                    }

                    if (filteredVendors.isEmpty()) {
                        Text(
                            "Tidak ada warung pada filter ini.",
                            fontSize = 12.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.padding(16.dp)
                        )
                    } else {
                        LazyColumn(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            items(filteredVendors, key = { it.id }) { v ->
                                Surface(
                                    shape = RoundedCornerShape(12.dp),
                                    border = BorderStroke(1.dp, Color(0xFFE4E4E7)),
                                    color = Color.White,
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    Column(modifier = Modifier.padding(12.dp)) {
                                        Row(
                                            modifier = Modifier.fillMaxWidth(),
                                            horizontalArrangement = Arrangement.SpaceBetween,
                                            verticalAlignment = Alignment.Top
                                        ) {
                                            Column(modifier = Modifier.weight(1f)) {
                                                Text(v.namaToko, fontWeight = FontWeight.Bold, fontSize = 15.sp)
                                                Text(v.alamatPatokan, fontSize = 12.sp, color = Color(0xFF52525B), maxLines = 1, overflow = TextOverflow.Ellipsis)
                                                Text("Kategori: ${Format.labelKategori(v.kategori)} • Jam ${v.jamBuka}-${v.jamTutup}", fontSize = 11.sp, color = Color(0xFF71717A))
                                                if (!v.namaKontributor.isNullOrBlank()) {
                                                    Text("Diajukan: ${v.namaKontributor}", fontSize = 11.sp, color = Color(0xFF2563EB), fontWeight = FontWeight.Medium)
                                                }
                                            }

                                            Surface(
                                                shape = RoundedCornerShape(6.dp),
                                                color = when (v.status) {
                                                    "approved" -> Color(0xFFDCFCE7)
                                                    "pending" -> Color(0xFFFEF3C7)
                                                    else -> Color(0xFFFEE2E2)
                                                }
                                            ) {
                                                Text(
                                                    text = when (v.status) {
                                                        "approved" -> "Tayang"
                                                        "pending" -> "Pending"
                                                        else -> "Ditolak"
                                                    },
                                                    fontSize = 10.sp,
                                                    fontWeight = FontWeight.Bold,
                                                    color = when (v.status) {
                                                        "approved" -> Color(0xFF15803D)
                                                        "pending" -> Color(0xFFD97706)
                                                        else -> Color(0xFFDC2626)
                                                    },
                                                    modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                                )
                                            }
                                        }

                                        Spacer(Modifier.height(8.dp))
                                        HorizontalDivider(color = Color(0xFFF4F4F5))
                                        Spacer(Modifier.height(4.dp))

                                        Row(
                                            modifier = Modifier.fillMaxWidth(),
                                            horizontalArrangement = Arrangement.End,
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            TextButton(onClick = { onViewVendor(v.id) }) {
                                                Text("Detail", fontSize = 11.sp)
                                            }

                                            if (v.status == "pending" || v.status == "rejected") {
                                                Button(
                                                    onClick = {
                                                        scope.launch {
                                                            container.vendorRepository.adminVendorAction(v.id, "approve")
                                                            pesanSukses = "Warung '${v.namaToko}' berhasil disetujui tayang!"
                                                            refresh()
                                                        }
                                                    },
                                                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF059669)),
                                                    modifier = Modifier.height(32.dp).padding(start = 4.dp),
                                                    shape = RoundedCornerShape(8.dp)
                                                ) {
                                                    Text("Setujui", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                                                }
                                            }

                                            if (v.status == "pending") {
                                                OutlinedButton(
                                                    onClick = {
                                                        scope.launch {
                                                            container.vendorRepository.adminVendorAction(v.id, "reject")
                                                            pesanSukses = "Warung '${v.namaToko}' ditolak."
                                                            refresh()
                                                        }
                                                    },
                                                    modifier = Modifier.height(32.dp).padding(start = 4.dp),
                                                    shape = RoundedCornerShape(8.dp)
                                                ) {
                                                    Text("Tolak", fontSize = 11.sp, color = Color(0xFFDC2626))
                                                }
                                            }

                                            IconButton(
                                                onClick = {
                                                    scope.launch {
                                                        container.vendorRepository.delete(v.id)
                                                        pesanSukses = "Warung '${v.namaToko}' dihapus permanen."
                                                        refresh()
                                                    }
                                                },
                                                modifier = Modifier.size(32.dp).padding(start = 4.dp)
                                            ) {
                                                Icon(Icons.Filled.Delete, contentDescription = "Hapus", tint = Color(0xFFEF4444), modifier = Modifier.size(16.dp))
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }
                }

                // ── 1. DAFTAR MEMBER & LEADERBOARD TOP PENGAJU ───────────────────────
                1 -> {
                    LazyColumn(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                        // Section: Member Paling Sering Mengajukan Warung
                        item {
                            Surface(
                                shape = RoundedCornerShape(14.dp),
                                color = Color(0xFFEFF6FF),
                                border = BorderStroke(1.dp, Color(0xFFBFDBFE)),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Column(modifier = Modifier.padding(14.dp)) {
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Icon(Icons.Filled.MilitaryTech, contentDescription = null, tint = Color(0xFF2563EB), modifier = Modifier.size(20.dp))
                                        Spacer(Modifier.width(6.dp))
                                        Text(
                                            "Member Paling Sering Mengajukan Warung",
                                            fontWeight = FontWeight.Bold,
                                            fontSize = 13.sp,
                                            color = Color(0xFF1E40AF)
                                        )
                                    }
                                    Text(
                                        "Daftar kontributor komunitas teraktif yang mendaftarkan UMKM:",
                                        fontSize = 11.sp,
                                        color = Color(0xFF3B82F6),
                                        modifier = Modifier.padding(top = 2.dp, bottom = 8.dp)
                                    )

                                    if (topKontributor.isEmpty()) {
                                        Text("Belum ada pengajuan warung dari member.", fontSize = 11.sp, color = Color(0xFF6B7280))
                                    } else {
                                        topKontributor.take(5).forEachIndexed { idx, top ->
                                            Row(
                                                modifier = Modifier
                                                    .fillMaxWidth()
                                                    .padding(vertical = 4.dp),
                                                horizontalArrangement = Arrangement.SpaceBetween,
                                                verticalAlignment = Alignment.CenterVertically
                                            ) {
                                                Row(verticalAlignment = Alignment.CenterVertically) {
                                                    Surface(
                                                        shape = CircleShape,
                                                        color = when (idx) {
                                                            0 -> Color(0xFFF59E0B) // Gold
                                                            1 -> Color(0xFF9CA3AF) // Silver
                                                            2 -> Color(0xFFD97706) // Bronze
                                                            else -> Color(0xFF2563EB)
                                                        },
                                                        modifier = Modifier.size(24.dp)
                                                    ) {
                                                        Box(contentAlignment = Alignment.Center) {
                                                            Text(
                                                                "${idx + 1}",
                                                                color = Color.White,
                                                                fontSize = 11.sp,
                                                                fontWeight = FontWeight.Bold
                                                            )
                                                        }
                                                    }
                                                    Spacer(Modifier.width(8.dp))
                                                    Column {
                                                        Text(top.name, fontWeight = FontWeight.Bold, fontSize = 12.sp)
                                                        Text("Level ${top.level} • ${top.xp} XP", fontSize = 10.sp, color = Color(0xFF6B7280))
                                                    }
                                                }

                                                Surface(
                                                    shape = RoundedCornerShape(6.dp),
                                                    color = Color(0xFFDBEAFE)
                                                ) {
                                                    Text(
                                                        "${top.totalPengajuan} Warung (${top.diterima} Tayang)",
                                                        color = Color(0xFF1D4ED8),
                                                        fontWeight = FontWeight.Bold,
                                                        fontSize = 11.sp,
                                                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                                    )
                                                }
                                            }
                                        }
                                    }
                                }
                            }
                        }

                        // Section: Semua Member Terdaftar
                        item {
                            Text(
                                "Semua Member Terdaftar (${members.size})",
                                style = MaterialTheme.typography.titleSmall,
                                fontWeight = FontWeight.Bold,
                                modifier = Modifier.padding(top = 6.dp)
                            )
                        }

                        items(members, key = { it.displayId }) { m ->
                            Surface(
                                shape = RoundedCornerShape(10.dp),
                                border = BorderStroke(1.dp, if (m.isBanned) Color(0xFFFCA5A5) else Color(0xFFE4E4E7)),
                                color = if (m.isBanned) Color(0xFFFFF5F5) else Color.White,
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Row(
                                    modifier = Modifier.padding(12.dp),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Row(modifier = Modifier.weight(1f), verticalAlignment = Alignment.CenterVertically) {
                                        Surface(
                                            shape = CircleShape,
                                            color = if (m.isBanned) Color(0xFF991B1B) else if (m.role == "admin") Color(0xFFDC2626) else if (m.tipeAkun == "mitra") Color(0xFF059669) else Color(0xFF2563EB),
                                            modifier = Modifier.size(36.dp)
                                        ) {
                                            Box(contentAlignment = Alignment.Center) {
                                                Text(
                                                    m.name.take(1).uppercase(),
                                                    color = Color.White,
                                                    fontWeight = FontWeight.Bold,
                                                    fontSize = 14.sp
                                                )
                                            }
                                        }
                                        Spacer(Modifier.width(10.dp))
                                        Column {
                                            Row(verticalAlignment = Alignment.CenterVertically) {
                                                Text(m.name, fontWeight = FontWeight.Bold, fontSize = 13.sp)
                                                if (m.role == "admin") {
                                                    Spacer(Modifier.width(4.dp))
                                                    Surface(color = Color(0xFFFEE2E2), shape = RoundedCornerShape(4.dp)) {
                                                        Text("ADMIN", color = Color(0xFFDC2626), fontSize = 9.sp, fontWeight = FontWeight.Bold, modifier = Modifier.padding(horizontal = 4.dp))
                                                    }
                                                }
                                                if (m.isBanned) {
                                                    Spacer(Modifier.width(4.dp))
                                                    Surface(color = Color(0xFFFEE2E2), shape = RoundedCornerShape(4.dp)) {
                                                        Text("DIBLOKIR", color = Color(0xFFDC2626), fontSize = 9.sp, fontWeight = FontWeight.Bold, modifier = Modifier.padding(horizontal = 4.dp))
                                                    }
                                                }
                                            }
                                            Text(m.email, fontSize = 11.sp, color = Color(0xFF71717A), maxLines = 1, overflow = TextOverflow.Ellipsis)
                                            val tipeLabel = m.tipeAkun.replaceFirstChar { if (it.isLowerCase()) it.titlecase() else it.toString() }
                                            Text("Peran: $tipeLabel • ${m.totalPengajuan} Warung • Lv ${m.level} (${m.xp} XP)", fontSize = 10.sp, color = Color(0xFF3F3F46))
                                        }
                                    }

                                    if (m.role != "admin") {
                                        Button(
                                            onClick = {
                                                dialogBanTarget = m
                                                banReason = ""
                                            },
                                            colors = ButtonDefaults.buttonColors(
                                                containerColor = if (m.isBanned) Color(0xFF059669) else Color(0xFFDC2626)
                                            ),
                                            modifier = Modifier.height(30.dp),
                                            shape = RoundedCornerShape(6.dp)
                                        ) {
                                            Text(
                                                if (m.isBanned) "Buka Blokir" else "Blokir",
                                                fontSize = 10.sp,
                                                fontWeight = FontWeight.Bold
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }
                }

                // ── 2. DAFTAR LAPORAN WARUNG (KETIDAKSESUAIAN MAP / TUTUP) ────────────
                2 -> {
                    if (reports.isEmpty()) {
                        Surface(
                            shape = RoundedCornerShape(12.dp),
                            color = Color(0xFFF9FAFB),
                            border = BorderStroke(1.dp, Color(0xFFE5E7EB)),
                            modifier = Modifier.fillMaxWidth().padding(16.dp)
                        ) {
                            Column(
                                modifier = Modifier.padding(24.dp),
                                horizontalAlignment = Alignment.CenterHorizontally
                            ) {
                                Icon(Icons.Filled.CheckCircle, contentDescription = null, tint = Color(0xFF059669), modifier = Modifier.size(36.dp))
                                Spacer(Modifier.height(8.dp))
                                Text("Semua Aman!", fontWeight = FontWeight.Bold, fontSize = 14.sp)
                                Text("Belum ada laporan ketidaksesuaian warung dari pengguna.", fontSize = 12.sp, color = Color(0xFF6B7280))
                            }
                        }
                    } else {
                        LazyColumn(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            items(reports, key = { it.id }) { rep ->
                                Surface(
                                    shape = RoundedCornerShape(12.dp),
                                    border = BorderStroke(1.dp, if (rep.status == "pending") Color(0xFFFCA5A5) else Color(0xFFE4E4E7)),
                                    color = Color.White,
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    Column(modifier = Modifier.padding(12.dp)) {
                                        Row(
                                            modifier = Modifier.fillMaxWidth(),
                                            horizontalArrangement = Arrangement.SpaceBetween,
                                            verticalAlignment = Alignment.Top
                                        ) {
                                            Column(modifier = Modifier.weight(1f)) {
                                                Text(rep.namaToko, fontWeight = FontWeight.Bold, fontSize = 14.sp)
                                                Text(
                                                    rep.alasanLabel,
                                                    color = Color(0xFFDC2626),
                                                    fontSize = 12.sp,
                                                    fontWeight = FontWeight.SemiBold,
                                                    modifier = Modifier.padding(top = 1.dp)
                                                )
                                                if (rep.deskripsi.isNotBlank()) {
                                                    Text(
                                                        "\"${rep.deskripsi}\"",
                                                        fontSize = 11.sp,
                                                        color = Color(0xFF4B5563),
                                                        modifier = Modifier.padding(top = 2.dp)
                                                    )
                                                }
                                                Text("Pelapor: ${rep.namaPelapor}", fontSize = 10.sp, color = Color(0xFF9CA3AF), modifier = Modifier.padding(top = 2.dp))
                                            }

                                            Surface(
                                                shape = RoundedCornerShape(6.dp),
                                                color = if (rep.status == "pending") Color(0xFFFEE2E2) else Color(0xFFDCFCE7)
                                            ) {
                                                Text(
                                                    if (rep.status == "pending") "Perlu Ditinjau" else "Selesai",
                                                    fontSize = 10.sp,
                                                    fontWeight = FontWeight.Bold,
                                                    color = if (rep.status == "pending") Color(0xFFDC2626) else Color(0xFF15803D),
                                                    modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                                )
                                            }
                                        }

                                        Spacer(Modifier.height(8.dp))
                                        HorizontalDivider(color = Color(0xFFF4F4F5))
                                        Spacer(Modifier.height(4.dp))

                                        Row(
                                            modifier = Modifier.fillMaxWidth(),
                                            horizontalArrangement = Arrangement.End,
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            if (rep.vendorId != null) {
                                                TextButton(onClick = { onViewVendor(rep.vendorId) }) {
                                                    Text("Cek Warung", fontSize = 11.sp)
                                                }
                                            }

                                            Button(
                                                onClick = {
                                                    scope.launch {
                                                        // Update status selesai via API
                                                        pesanSukses = "Laporan untuk '${rep.namaToko}' ditandai selesai."
                                                        refresh()
                                                    }
                                                },
                                                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF059669)),
                                                modifier = Modifier.height(30.dp).padding(start = 4.dp),
                                                shape = RoundedCornerShape(6.dp)
                                            ) {
                                                Text("Selesai Ditinjau", fontSize = 10.5.sp, fontWeight = FontWeight.Bold)
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    // ── Dialog Konfirmasi Banned / Unbanned Member ──
    dialogBanTarget?.let { target ->
        val aksiBlokir = !target.isBanned
        AlertDialog(
            onDismissRequest = { if (!banLoading) dialogBanTarget = null },
            title = {
                Text(if (aksiBlokir) "Blokir Akun Member?" else "Buka Blokir Member?")
            },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(
                        if (aksiBlokir)
                            "Member '${target.name}' (${target.email}) tidak akan dapat login atau mengajukan warung selama akun diblokir."
                        else
                            "Pulihkan akses akun untuk '${target.name}' (${target.email}) agar dapat login kembali."
                    )
                    if (aksiBlokir) {
                        OutlinedTextField(
                            value = banReason,
                            onValueChange = { banReason = it },
                            label = { Text("Alasan pemblokiran (opsional)") },
                            placeholder = { Text("Misal: Melanggar ketentuan, spam") },
                            modifier = Modifier.fillMaxWidth()
                        )
                    }
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        banLoading = true
                        scope.launch {
                            val res = container.vendorRepository.banMember(
                                target.displayId,
                                aksiBlokir,
                                banReason.trim()
                            )
                            banLoading = false
                            res.onSuccess {
                                pesanSukses = if (aksiBlokir)
                                    "Akun '${target.name}' berhasil diblokir."
                                else
                                    "Blokir akun '${target.name}' berhasil dicabut."
                                dialogBanTarget = null
                                refresh()
                            }.onFailure {
                                errorPesan = it.message ?: "Gagal memproses status banned"
                            }
                        }
                    },
                    colors = ButtonDefaults.buttonColors(
                        containerColor = if (aksiBlokir) Color(0xFFDC2626) else Color(0xFF059669)
                    ),
                    enabled = !banLoading
                ) {
                    Text(if (banLoading) "Memproses..." else if (aksiBlokir) "Ya, Blokir" else "Buka Blokir")
                }
            },
            dismissButton = {
                TextButton(
                    onClick = { dialogBanTarget = null },
                    enabled = !banLoading
                ) {
                    Text("Batal")
                }
            }
        )
    }
}
