package com.carijajan.app.ui.screen

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import coil3.compose.AsyncImage
import com.carijajan.app.AppContainer
import com.carijajan.app.data.VendorDto
import com.carijajan.app.util.Format
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
    var vendors by remember { mutableStateOf<List<VendorDto>>(emptyList()) }
    var loading by remember { mutableStateOf(true) }
    var error by remember { mutableStateOf("") }
    var deleteTarget by remember { mutableStateOf<VendorDto?>(null) }
    var deleteLoading by remember { mutableStateOf(false) }

    suspend fun load() {
        loading = true
        val result = container.vendorRepository.mine()
        result.onSuccess { vendors = it }
            .onFailure { error = it.message ?: "Gagal mengambil data warung" }
        loading = false
    }

    LaunchedEffect(Unit) { load() }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .statusBarsPadding()
            .padding(horizontal = 16.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            TextButton(onClick = onBack) { Text("←") }
            Column(modifier = Modifier.weight(1f)) {
                Text("Dashboard", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
                val namaUser = user?.name?.takeIf { it.isNotBlank() } ?: user?.email.orEmpty()
                Text("Halo, $namaUser", style = MaterialTheme.typography.bodySmall)
            }
            TextButton(onClick = {
                scope.launch {
                    container.authRepository.logout()
                    onLogout()
                }
            }) { Text("Keluar") }
        }

        Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.padding(bottom = 10.dp)) {
            Surface(color = MaterialTheme.colorScheme.primaryContainer, shape = RoundedCornerShape(50)) {
                Text(
                    "Level ${user?.level ?: 1}",
                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp),
                    style = MaterialTheme.typography.labelMedium
                )
            }
            Text(
                "${user?.xp ?: 0} XP",
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.align(Alignment.CenterVertically)
            )
        }

        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(
                "Warung milikmu",
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.SemiBold,
                modifier = Modifier.weight(1f)
            )
            Button(onClick = onAdd) { Text("+ Tambah") }
        }

        when {
            loading -> Column(
                Modifier.fillMaxWidth().padding(24.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) { CircularProgressIndicator() }

            error.isNotBlank() -> Text(
                error,
                color = MaterialTheme.colorScheme.error,
                modifier = Modifier.padding(12.dp)
            )

            vendors.isEmpty() -> Text(
                "Belum ada warung. Tekan Tambah untuk mendaftarkan warung.",
                modifier = Modifier.padding(vertical = 16.dp),
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            else -> LazyColumn(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                items(vendors, key = { it.id }) { vendor ->
                    VendorDashboardRow(
                        vendor = vendor,
                        onView = { onVendor(vendor.id) },
                        onEdit = { onEdit(vendor.id) },
                        onDelete = { deleteTarget = vendor }
                    )
                }
            }
        }
    }

    deleteTarget?.let { vendor ->
        AlertDialog(
            onDismissRequest = { if (!deleteLoading) deleteTarget = null },
            title = { Text("Hapus warung?") },
            text = { Text("Warung ${vendor.namaToko} akan dihapus permanen.") },
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
                    Text(
                        if (deleteLoading) "Menghapus..." else "Hapus",
                        color = MaterialTheme.colorScheme.error
                    )
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

@Composable
private fun VendorDashboardRow(
    vendor: VendorDto,
    onView: () -> Unit,
    onEdit: () -> Unit,
    onDelete: () -> Unit
) {
    Surface(
        shape = RoundedCornerShape(14.dp),
        tonalElevation = 1.dp,
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(modifier = Modifier.padding(12.dp), verticalAlignment = Alignment.Top) {
            if (vendor.foto.isNotBlank()) {
                AsyncImage(
                    model = vendor.foto,
                    contentDescription = "Foto ${vendor.namaToko}",
                    modifier = Modifier.padding(end = 10.dp)
                )
            }
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    vendor.namaToko,
                    fontWeight = FontWeight.SemiBold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Text(
                    "${Format.labelKategori(vendor.kategori)} · ${vendor.jamBuka}-${vendor.jamTutup}",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Text(
                    vendor.alamatPatokan,
                    style = MaterialTheme.typography.bodySmall,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Surface(
                    color = when (vendor.status) {
                        "approved" -> MaterialTheme.colorScheme.primaryContainer
                        "rejected" -> MaterialTheme.colorScheme.errorContainer
                        else -> MaterialTheme.colorScheme.tertiaryContainer
                    },
                    shape = RoundedCornerShape(50),
                    modifier = Modifier.padding(top = 5.dp)
                ) {
                    Text(
                        Format.statusLabel(vendor.status),
                        style = MaterialTheme.typography.labelSmall,
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
                    )
                }
                Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                    TextButton(onClick = onView) { Text("Lihat") }
                    TextButton(onClick = onEdit) { Text("Edit") }
                    TextButton(onClick = onDelete) {
                        Text("Hapus", color = MaterialTheme.colorScheme.error)
                    }
                }
            }
        }
    }
}
