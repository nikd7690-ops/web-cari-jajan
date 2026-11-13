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
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.DeliveryDining
import androidx.compose.material.icons.filled.Eco
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.Navigation
import androidx.compose.material.icons.filled.Phone
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material.icons.filled.Storefront
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
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
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import coil3.compose.AsyncImage
import com.carijajan.app.AppContainer
import com.carijajan.app.data.MenuDto
import com.carijajan.app.data.VendorDto
import com.carijajan.app.ui.components.PesanError
import com.carijajan.app.ui.components.bounceClick
import com.carijajan.app.ui.components.bounceEffect
import com.carijajan.app.ui.theme.BrandMaroon
import com.carijajan.app.ui.theme.BrandMaroonContainer
import com.carijajan.app.ui.theme.BrandOrange
import com.carijajan.app.ui.theme.BrandOrangeDark
import com.carijajan.app.ui.theme.BrandOrangeLight
import com.carijajan.app.ui.theme.BrandOrangeSurface
import com.carijajan.app.ui.theme.WarmBg
import com.carijajan.app.ui.theme.WarmBorder
import com.carijajan.app.util.CartItem
import com.carijajan.app.util.Format
import com.carijajan.app.util.Keranjang
import com.carijajan.app.util.Navigasi
import com.carijajan.app.util.WhatsApp

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

    val zeroWasteAktif by container.tokenStore.isZeroWasteActive(vendorId).collectAsStateWithLifecycle(initialValue = false)
    val zeroWasteDiskon by container.tokenStore.getZeroWasteDiscount(vendorId).collectAsStateWithLifecycle(initialValue = 30)
    val zeroWasteCatatan by container.tokenStore.getZeroWasteNote(vendorId).collectAsStateWithLifecycle(initialValue = "")

    LaunchedEffect(vendorId) {
        container.tokenStore.recordVendorView(vendorId)
        loading = true
        val res = container.vendorRepository.detail(vendorId)
        res.onSuccess {
            vendor = it
            mode = if (it.layananAntar) "Diantar" else "Ambil sendiri"
        }.onFailure { error = it.message ?: "Gagal mengambil data warung" }
        loading = false
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(WarmBg)
            .statusBarsPadding()
    ) {
        Column(
            modifier = Modifier.fillMaxSize()
        ) {
            // Header Top Bar
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Surface(
                    shape = CircleShape,
                    color = Color.White,
                    shadowElevation = 2.dp,
                    modifier = Modifier
                        .size(40.dp)
                        .bounceClick { onBack() }
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Icon(
                            imageVector = Icons.Default.ArrowBack,
                            contentDescription = "Kembali",
                            tint = BrandMaroon,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                }
                Spacer(Modifier.width(12.dp))
                Text(
                    text = vendor?.namaToko ?: "Detail Warung",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = BrandMaroon,
                    maxLines = 1
                )
            }

            if (loading) {
                Column(
                    modifier = Modifier.fillMaxSize(),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center
                ) {
                    CircularProgressIndicator(color = BrandOrange)
                    Text("Memuat info warung...", modifier = Modifier.padding(top = 10.dp), color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
                return@Column
            }

            val toko = vendor
            if (toko == null) {
                Box(Modifier.padding(16.dp)) {
                    PesanError(error.ifBlank { "Warung tidak ditemukan" })
                }
                return@Column
            }

            LazyColumn(
                modifier = Modifier
                    .weight(1f)
                    .padding(horizontal = 16.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                // Banner Foto Warung
                if (toko.foto.isNotBlank()) {
                    item {
                        Surface(
                            shape = RoundedCornerShape(18.dp),
                            shadowElevation = 3.dp,
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(190.dp)
                        ) {
                            AsyncImage(
                                model = toko.foto,
                                contentDescription = "Foto ${toko.namaToko}",
                                contentScale = ContentScale.Crop,
                                modifier = Modifier.fillMaxSize()
                            )
                        }
                    }
                }

                // Info Utama Toko Card
                item {
                    Surface(
                        shape = RoundedCornerShape(18.dp),
                        color = Color.White,
                        border = BorderStroke(1.dp, WarmBorder),
                        shadowElevation = 1.dp,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(modifier = Modifier.padding(16.dp)) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.Top
                            ) {
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        text = toko.namaToko,
                                        style = MaterialTheme.typography.titleLarge,
                                        fontWeight = FontWeight.ExtraBold,
                                        color = BrandMaroon
                                    )
                                    Spacer(Modifier.height(4.dp))
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Icon(
                                            imageVector = Icons.Default.LocationOn,
                                            contentDescription = null,
                                            tint = BrandOrange,
                                            modifier = Modifier.size(16.dp)
                                        )
                                        Spacer(Modifier.width(4.dp))
                                        Text(
                                            text = "${Format.labelKategori(toko.kategori)} · ${toko.alamatPatokan}",
                                            style = MaterialTheme.typography.bodySmall,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                    }
                                }

                                // Status Buka / Tutup Badge
                                Surface(
                                    color = if (toko.bukaAktif) BrandOrangeSurface else Color(0xFFFEE2E2),
                                    shape = RoundedCornerShape(20.dp),
                                    border = BorderStroke(1.dp, if (toko.bukaAktif) BrandOrangeLight else Color(0xFFFECACA))
                                ) {
                                    Text(
                                        text = if (toko.bukaAktif) "● Buka" else "○ Tutup",
                                        color = if (toko.bukaAktif) BrandOrangeDark else Color(0xFFDC2626),
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 11.sp,
                                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
                                    )
                                }
                            }

                            Spacer(Modifier.height(8.dp))
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(
                                    imageVector = Icons.Default.Schedule,
                                    contentDescription = null,
                                    tint = BrandMaroon.copy(alpha = 0.8f),
                                    modifier = Modifier.size(14.dp)
                                )
                                Spacer(Modifier.width(4.dp))
                                Text(
                                    text = "Jam Operasional: ${toko.jamBuka} - ${toko.jamTutup}",
                                    style = MaterialTheme.typography.labelMedium,
                                    color = BrandMaroon.copy(alpha = 0.8f)
                                )
                            }

                            if (!toko.bukaAktif) {
                                Spacer(Modifier.height(6.dp))
                                Surface(
                                    color = Color(0xFFFEF2F2),
                                    shape = RoundedCornerShape(8.dp),
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    Text(
                                        text = "Warung sedang tutup, pesanan baru akan diproses saat buka.",
                                        color = Color(0xFFDC2626),
                                        style = MaterialTheme.typography.bodySmall,
                                        modifier = Modifier.padding(8.dp)
                                    )
                                }
                            }

                            Spacer(Modifier.height(12.dp))

                            // Tombol Navigasi GPS
                            Button(
                                onClick = {
                                    Navigasi.bukaNavigasi(context, toko.lat, toko.lng, toko.namaToko)
                                },
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = BrandOrangeSurface,
                                    contentColor = BrandOrangeDark
                                ),
                                border = BorderStroke(1.dp, BrandOrangeLight),
                                shape = RoundedCornerShape(12.dp),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .bounceEffect()
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Navigation,
                                    contentDescription = "Navigasi",
                                    tint = BrandOrange,
                                    modifier = Modifier.size(18.dp)
                                )
                                Spacer(Modifier.width(8.dp))
                                Text(
                                    "Petunjuk Arah (Navigasi GPS)",
                                    fontWeight = FontWeight.SemiBold,
                                    fontSize = 13.sp
                                )
                            }
                        }
                    }
                }

                // Banner Zero Waste Food jika aktif
                if (zeroWasteAktif) {
                    item {
                        Surface(
                            color = Color(0xFFFFFBEB),
                            shape = RoundedCornerShape(16.dp),
                            border = BorderStroke(1.dp, Color(0xFFFDE68A)),
                            shadowElevation = 1.dp,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Row(
                                modifier = Modifier.padding(14.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Surface(
                                    shape = CircleShape,
                                    color = Color(0xFFFEF3C7),
                                    modifier = Modifier.size(42.dp)
                                ) {
                                    Box(contentAlignment = Alignment.Center) {
                                        Icon(
                                            imageVector = Icons.Filled.Eco,
                                            contentDescription = null,
                                            tint = Color(0xFFD97706),
                                            modifier = Modifier.size(24.dp)
                                        )
                                    }
                                }
                                Spacer(Modifier.width(12.dp))
                                Column {
                                    Text(
                                        "Promo Zero Waste Food (-$zeroWasteDiskon%)",
                                        fontWeight = FontWeight.Bold,
                                        color = Color(0xFF92400E),
                                        fontSize = 13.sp
                                    )
                                    Text(
                                        zeroWasteCatatan.ifBlank { "Diskon khusus porsi lezat sebelum warung tutup agar tidak terbuang!" },
                                        style = MaterialTheme.typography.bodySmall,
                                        color = Color(0xFFB45309),
                                        fontSize = 11.sp
                                    )
                                }
                            }
                        }
                    }
                }

                // Header Bagian Menu
                item {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(top = 8.dp, bottom = 4.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(
                            "Daftar Menu",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = BrandMaroon
                        )
                        Text(
                            "${toko.menus.size} item",
                            style = MaterialTheme.typography.labelMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }

                if (toko.menus.isEmpty()) {
                    item {
                        Surface(
                            shape = RoundedCornerShape(12.dp),
                            color = Color.White,
                            border = BorderStroke(1.dp, WarmBorder),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text(
                                "Belum ada menu yang didaftarkan.",
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.padding(16.dp)
                            )
                        }
                    }
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

                // Opsi Pengambilan
                item {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 8.dp)
                    ) {
                        Text(
                            "Opsi Pengambilan",
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.SemiBold,
                            color = BrandMaroon
                        )
                        Row(
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            modifier = Modifier.padding(top = 6.dp)
                        ) {
                            if (toko.layananAntar) {
                                FilterChip(
                                    selected = mode == "Diantar",
                                    onClick = { mode = "Diantar" },
                                    label = { Text("Diantar Penjual") },
                                    colors = FilterChipDefaults.filterChipColors(
                                        selectedContainerColor = BrandOrangeLight,
                                        selectedLabelColor = BrandOrangeDark
                                    ),
                                    modifier = Modifier.bounceClick { mode = "Diantar" }
                                )
                            }
                            FilterChip(
                                selected = mode == "Ambil sendiri",
                                onClick = { mode = "Ambil sendiri" },
                                label = { Text("Ambil Sendiri di Warung") },
                                colors = FilterChipDefaults.filterChipColors(
                                    selectedContainerColor = BrandOrangeLight,
                                    selectedLabelColor = BrandOrangeDark
                                ),
                                modifier = Modifier.bounceClick { mode = "Ambil sendiri" }
                            )
                        }
                    }
                }

                item { Spacer(Modifier.height(80.dp)) }
            }

            // Sticky Bottom Checkout Bar
            Surface(
                shape = RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp),
                color = Color.White,
                shadowElevation = 12.dp,
                border = BorderStroke(1.dp, WarmBorder),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(
                    modifier = Modifier
                        .padding(horizontal = 20.dp, vertical = 14.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = "${Keranjang.hitungJumlahItem(cart)} item di keranjang",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Text(
                                text = Format.rupiah(Keranjang.hitungTotal(cart)),
                                style = MaterialTheme.typography.titleLarge,
                                fontWeight = FontWeight.ExtraBold,
                                color = BrandOrange
                            )
                        }

                        if (toko.nomorWA.isNotBlank()) {
                            Button(
                                onClick = {
                                    val teks = Keranjang.buatTeksPesanan(toko.namaToko, cart, mode)
                                    WhatsApp.open(context, toko.nomorWA, teks)
                                },
                                enabled = cart.isNotEmpty(),
                                shape = RoundedCornerShape(14.dp),
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = BrandOrange,
                                    contentColor = Color.White
                                ),
                                modifier = Modifier.bounceEffect()
                            ) {
                                Text(
                                    if (cart.isEmpty()) "Pilih menu dulu" else "Pesan WhatsApp",
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 13.sp
                                )
                            }
                        }
                    }

                    if (toko.nomorWA.isBlank()) {
                        Surface(
                            color = BrandOrangeSurface,
                            shape = RoundedCornerShape(10.dp),
                            border = BorderStroke(1.dp, BrandOrangeLight),
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(top = 8.dp)
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                modifier = Modifier.padding(10.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Info,
                                    contentDescription = null,
                                    tint = BrandOrangeDark,
                                    modifier = Modifier.size(16.dp)
                                )
                                Spacer(Modifier.width(6.dp))
                                Text(
                                    text = "Warung ini melayani beli langsung di tempat (tanpa kontak WhatsApp online).",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = BrandOrangeDark
                                )
                            }
                        }
                    }
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
        color = if (menu.tersedia) Color.White else Color(0xFFF9FAFB),
        shape = RoundedCornerShape(16.dp),
        border = BorderStroke(1.dp, if (menu.tersedia) WarmBorder else Color(0xFFE5E7EB)),
        shadowElevation = if (menu.tersedia) 1.dp else 0.dp,
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 3.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 14.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = menu.namaMenu,
                    fontWeight = FontWeight.Bold,
                    fontSize = 14.sp,
                    color = if (menu.tersedia) BrandMaroon else Color.Gray
                )
                Spacer(Modifier.height(2.dp))
                Text(
                    text = if (menu.tersedia) Format.rupiah(menu.harga) else "${Format.rupiah(menu.harga)} · Stok Habis",
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.SemiBold,
                    color = if (menu.tersedia) BrandOrange else Color(0xFF9CA3AF)
                )
            }

            if (menu.tersedia) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    if (cartItem != null && cartItem.qty > 0) {
                        Surface(
                            shape = CircleShape,
                            color = BrandOrangeLight,
                            border = BorderStroke(1.dp, BrandOrange),
                            modifier = Modifier
                                .size(32.dp)
                                .bounceClick { onRemove() }
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Text(
                                    "−",
                                    fontWeight = FontWeight.ExtraBold,
                                    color = BrandOrangeDark,
                                    fontSize = 16.sp
                                )
                            }
                        }

                        Text(
                            text = "${cartItem.qty}",
                            fontWeight = FontWeight.Bold,
                            fontSize = 14.sp,
                            color = BrandMaroon,
                            modifier = Modifier.padding(horizontal = 4.dp)
                        )
                    }

                    Surface(
                        shape = CircleShape,
                        color = BrandOrange,
                        shadowElevation = 2.dp,
                        modifier = Modifier
                            .size(32.dp)
                            .bounceClick { onAdd() }
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Text(
                                "+",
                                fontWeight = FontWeight.ExtraBold,
                                color = Color.White,
                                fontSize = 16.sp
                            )
                        }
                    }
                }
            } else {
                Surface(
                    shape = RoundedCornerShape(6.dp),
                    color = Color(0xFFF3F4F6)
                ) {
                    Text(
                        text = "Habis",
                        fontSize = 11.sp,
                        color = Color(0xFF9CA3AF),
                        fontWeight = FontWeight.Medium,
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                    )
                }
            }
        }
    }
}

