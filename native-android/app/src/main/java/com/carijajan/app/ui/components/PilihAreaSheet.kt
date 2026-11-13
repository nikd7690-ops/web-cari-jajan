package com.carijajan.app.ui.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.AssistChip
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.carijajan.app.AppContainer
import com.carijajan.app.data.GeocodeItem
import com.carijajan.app.util.Titik
import kotlinx.coroutines.delay

private val KAWASAN_JEMBER = listOf(
    Titik(-8.1689, 113.702, "Alun-alun Jember"),
    Titik(-8.1640, 113.717, "Tegal Boto"),
    Titik(-8.1830, 113.677, "Kaliwates"),
    Titik(-8.1580, 113.715, "Sumbersari"),
    Titik(-8.1450, 113.700, "Patrang"),
    Titik(-8.1750, 113.708, "Pasar Tanjung")
)

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun PilihAreaSheet(
    container: AppContainer,
    lokasiAktif: String,
    loadingLokasi: Boolean,
    pesanLokasi: String,
    onGunakanLokasi: () -> Unit,
    onPilih: (Titik) -> Unit,
    onDismiss: () -> Unit
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    var query by remember { mutableStateOf("") }
    var hasil by remember { mutableStateOf<List<GeocodeItem>>(emptyList()) }
    var loading by remember { mutableStateOf(false) }
    var error by remember { mutableStateOf("") }

    LaunchedEffect(query) {
        val kata = query.trim()
        if (kata.length < 3) {
            hasil = emptyList()
            error = ""
            loading = false
            return@LaunchedEffect
        }
        loading = true
        error = ""
        delay(500)
        val res = container.vendorRepository.geocode(kata)
        res.onSuccess {
            hasil = it
            loading = false
        }.onFailure {
            error = it.message ?: "Gagal mencari kawasan."
            hasil = emptyList()
            loading = false
        }
    }

    ModalBottomSheet(onDismissRequest = onDismiss, sheetState = sheetState) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 18.dp)
                .padding(bottom = 24.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Text(
                text = "Pilih area",
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold
            )
            Text(
                text = if (lokasiAktif.isBlank()) "Belum ada area dipilih" else "Area aktif: $lokasiAktif",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            Button(
                onClick = onGunakanLokasi,
                enabled = !loadingLokasi,
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(if (loadingLokasi) "Mengambil lokasi..." else "Gunakan lokasiku")
            }

            if (pesanLokasi.isNotBlank()) {
                Text(
                    text = pesanLokasi,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.error
                )
            }

            OutlinedTextField(
                value = query,
                onValueChange = { query = it },
                label = { Text("Cari kawasan") },
                placeholder = { Text("Contoh: Kaliwates, Bandung") },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true
            )

            if (loading) {
                Text("Mencari kawasan...", style = MaterialTheme.typography.bodySmall)
            }
            if (error.isNotBlank()) {
                Text(
                    text = error,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.error
                )
            }

            if (hasil.isNotEmpty()) {
                LazyColumn(modifier = Modifier.heightIn(max = 220.dp)) {
                    items(hasil) { item ->
                        TextButton(
                            onClick = { onPilih(Titik(item.lat, item.lng, item.nama)) },
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text(item.nama, style = MaterialTheme.typography.bodySmall)
                        }
                    }
                }
            }

            Text(
                text = "Kawasan cepat (Jember)",
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.SemiBold
            )
            FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                KAWASAN_JEMBER.forEach { area ->
                    AssistChip(
                        onClick = { onPilih(area) },
                        label = { Text(area.label) }
                    )
                }
            }

            Text(
                text = "Pencarian area: © kontributor OpenStreetMap (Nominatim)",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}
