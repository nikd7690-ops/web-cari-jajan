package com.carijajan.app.ui.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Flag
import androidx.compose.material.icons.filled.Group
import androidx.compose.material.icons.filled.Place
import androidx.compose.material.icons.filled.RestaurantMenu
import androidx.compose.material.icons.filled.Store
import androidx.compose.material.icons.filled.Warning
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.carijajan.app.AppContainer
import com.carijajan.app.ui.theme.*
import kotlinx.coroutines.launch

@Composable
fun DialogLaporkanWarung(
    container: AppContainer,
    vendorId: String,
    namaToko: String,
    onDismiss: () -> Unit
) {
    val scope = rememberCoroutineScope()
    var alasanTerpilih by remember { mutableStateOf("lokasi_salah") }
    var deskripsi by remember { mutableStateOf("") }
    var loading by remember { mutableStateOf(false) }
    var sukses by remember { mutableStateOf(false) }
    var error by remember { mutableStateOf("") }

    data class OpsiAlasan(val key: String, val label: String, val icon: ImageVector)
    val daftarAlasan = listOf(
        OpsiAlasan("lokasi_salah", "Titik lokasi tidak sesuai di peta", Icons.Default.Place),
        OpsiAlasan("sudah_tutup", "Toko / warung sudah tutup permanen", Icons.Default.Store),
        OpsiAlasan("menu_salah", "Menu atau harga tidak akurat", Icons.Default.RestaurantMenu),
        OpsiAlasan("duplikat", "Warung duplikat / ganda", Icons.Default.Group),
        OpsiAlasan("lainnya", "Masalah informasi lainnya", Icons.Default.Warning)
    )

    fun kirim() {
        loading = true
        error = ""
        scope.launch {
            val res = container.vendorRepository.kirimLaporan(vendorId, alasanTerpilih, deskripsi.trim())
            loading = false
            res.onSuccess {
                sukses = true
            }.onFailure {
                error = it.message ?: "Gagal mengirim laporan. Coba lagi."
            }
        }
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        icon = {
            Icon(
                imageVector = if (sukses) Icons.Filled.CheckCircle else Icons.Filled.Flag,
                contentDescription = null,
                tint = if (sukses) BrandOrange else MaterialTheme.colorScheme.error,
                modifier = Modifier.size(32.dp)
            )
        },
        title = {
            Text(
                text = if (sukses) "Laporan Diterima" else "Laporkan Warung",
                fontWeight = FontWeight.Bold,
                fontSize = 18.sp
            )
        },
        text = {
            if (sukses) {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(
                        text = "Terima kasih telah membantu komunitas! Laporan Anda untuk '$namaToko' telah dikirimkan ke admin untuk ditinjau.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = Color(0xFF065F46)
                    )
                }
            } else {
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Text(
                        text = "Ada ketidaksesuaian pada warung '$namaToko'?",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )

                    // Pilihan Alasan
                    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                        daftarAlasan.forEach { opsi ->
                            val terpilih = (alasanTerpilih == opsi.key)
                            Surface(
                                shape = RoundedCornerShape(8.dp),
                                border = BorderStroke(1.dp, if (terpilih) MaterialTheme.colorScheme.primary else Color(0xFFE4E4E7)),
                                color = if (terpilih) MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.3f) else Color.Transparent,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .bounceEffect()
                                    .clickable { alasanTerpilih = opsi.key }
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 6.dp)
                                ) {
                                    RadioButton(
                                        selected = terpilih,
                                        onClick = { alasanTerpilih = opsi.key },
                                        modifier = Modifier.size(20.dp)
                                    )
                                    Spacer(Modifier.width(8.dp))
                                    Icon(
                                        imageVector = opsi.icon,
                                        contentDescription = null,
                                        tint = if (terpilih) MaterialTheme.colorScheme.primary else Color.Gray,
                                        modifier = Modifier.size(16.dp)
                                    )
                                    Spacer(Modifier.width(8.dp))
                                    Text(
                                        text = opsi.label,
                                        fontSize = 12.sp,
                                        fontWeight = if (terpilih) FontWeight.Bold else FontWeight.Normal
                                    )
                                }
                            }
                        }
                    }

                    OutlinedTextField(
                        value = deskripsi,
                        onValueChange = { deskripsi = it },
                        label = { Text("Keterangan Tambahan (Opsional)") },
                        placeholder = { Text("Contoh: Titik peta geser 100 meter ke utara") },
                        minLines = 2,
                        maxLines = 3,
                        modifier = Modifier.fillMaxWidth()
                    )

                    if (error.isNotBlank()) {
                        Text(
                            text = error,
                            color = MaterialTheme.colorScheme.error,
                            fontSize = 12.sp
                        )
                    }
                }
            }
        },
        confirmButton = {
            if (sukses) {
                Button(
                    onClick = onDismiss,
                    modifier = Modifier.bounceEffect()
                ) { Text("Tutup") }
            } else {
                Button(
                    onClick = { kirim() },
                    enabled = !loading,
                    modifier = Modifier.bounceEffect(),
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error)
                ) {
                    if (loading) {
                        CircularProgressIndicator(color = Color.White, modifier = Modifier.size(16.dp), strokeWidth = 2.dp)
                        Spacer(Modifier.width(6.dp))
                    }
                    Text("Kirim Laporan")
                }
            }
        },
        dismissButton = {
            if (!sukses) {
                OutlinedButton(
                    onClick = onDismiss,
                    enabled = !loading,
                    modifier = Modifier.bounceEffect()
                ) {
                    Text("Batal")
                }
            }
        }
    )
}
