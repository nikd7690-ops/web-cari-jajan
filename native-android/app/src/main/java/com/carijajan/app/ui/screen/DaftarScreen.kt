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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Storefront
import androidx.compose.material.icons.filled.VolunteerActivism
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.carijajan.app.AppContainer
import kotlinx.coroutines.launch

@Composable
fun DaftarScreen(
    container: AppContainer,
    onSuccess: () -> Unit,
    onBack: () -> Unit
) {
    val scope = rememberCoroutineScope()
    var nama by remember { mutableStateOf("") }
    var email by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    var tipeAkun by remember { mutableStateOf("penjual") } // "penjual" atau "pembeli"
    var loading by remember { mutableStateOf(false) }
    var error by remember { mutableStateOf("") }

    fun submit() {
        when {
            nama.isBlank() -> { error = "Nama wajib diisi."; return }
            !email.contains("@") -> { error = "Email tidak valid."; return }
            password.length < 8 -> { error = "Password minimal 8 karakter."; return }
        }
        loading = true
        error = ""
        scope.launch {
            val daftar = container.authRepository.register(
                nama = nama,
                email = email,
                password = password,
                tipeAkun = tipeAkun
            )
            if (daftar.isFailure) {
                loading = false
                error = daftar.exceptionOrNull()?.message ?: "Gagal mendaftar, coba lagi."
                return@launch
            }
            val masuk = container.authRepository.login(email, password)
            loading = false
            masuk.onSuccess {
                container.authRepository.setTipeAkun(tipeAkun)
                onSuccess()
            }.onFailure {
                error = "Akun berhasil dibuat. Silakan masuk menggunakan email Anda."
            }
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .statusBarsPadding()
            .verticalScroll(rememberScrollState())
            .padding(20.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            IconButton(onClick = onBack) {
                Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Kembali")
            }
            Spacer(Modifier.width(4.dp))
            Text("Kembali", style = MaterialTheme.typography.titleSmall)
        }

        Column {
            Text(
                text = "Buat Akun Cari Jajan",
                style = MaterialTheme.typography.headlineMedium,
                fontWeight = FontWeight.Bold
            )
            Spacer(Modifier.height(6.dp))
            Text(
                text = "Pilih peran Anda untuk mulai menjelajah atau mendaftarkan warung UMKM.",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }

        // ── Pilihan Peran Akun: Penjual vs Pembeli / Kontributor ──────────────────
        Text("Pilih Peran Utama Anda:", fontWeight = FontWeight.SemiBold, fontSize = 14.sp)

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            // Opsi 1: Pemilik Warung
            val isPenjual = tipeAkun == "penjual"
            Surface(
                shape = RoundedCornerShape(14.dp),
                border = BorderStroke(
                    width = if (isPenjual) 2.dp else 1.dp,
                    color = if (isPenjual) Color(0xFF059669) else Color(0xFFE4E4E7)
                ),
                color = if (isPenjual) Color(0xFFECFDF5) else Color.White,
                modifier = Modifier
                    .weight(1f)
                    .clickable { tipeAkun = "penjual" }
            ) {
                Column(modifier = Modifier.padding(12.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Box(
                            modifier = Modifier
                                .size(34.dp)
                                .clip(RoundedCornerShape(8.dp))
                                .background(if (isPenjual) Color(0xFF059669) else Color(0xFFF4F4F5)),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                Icons.Filled.Storefront,
                                contentDescription = null,
                                tint = if (isPenjual) Color.White else Color(0xFF71717A),
                                modifier = Modifier.size(20.dp)
                            )
                        }
                        if (isPenjual) {
                            Icon(
                                Icons.Filled.CheckCircle,
                                contentDescription = null,
                                tint = Color(0xFF059669),
                                modifier = Modifier.size(18.dp)
                            )
                        }
                    }
                    Spacer(Modifier.height(10.dp))
                    Text("Pemilik Warung", fontWeight = FontWeight.Bold, fontSize = 13.sp)
                    Text(
                        "Kelola warung, menu, statistik pembeli & promo Zero Waste.",
                        fontSize = 11.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(top = 2.dp)
                    )
                }
            }

            // Opsi 2: Pembeli & Kontributor Komunitas
            val isPembeli = tipeAkun == "pembeli"
            Surface(
                shape = RoundedCornerShape(14.dp),
                border = BorderStroke(
                    width = if (isPembeli) 2.dp else 1.dp,
                    color = if (isPembeli) Color(0xFF2563EB) else Color(0xFFE4E4E7)
                ),
                color = if (isPembeli) Color(0xFFEFF6FF) else Color.White,
                modifier = Modifier
                    .weight(1f)
                    .clickable { tipeAkun = "pembeli" }
            ) {
                Column(modifier = Modifier.padding(12.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Box(
                            modifier = Modifier
                                .size(34.dp)
                                .clip(RoundedCornerShape(8.dp))
                                .background(if (isPembeli) Color(0xFF2563EB) else Color(0xFFF4F4F5)),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                Icons.Filled.VolunteerActivism,
                                contentDescription = null,
                                tint = if (isPembeli) Color.White else Color(0xFF71717A),
                                modifier = Modifier.size(20.dp)
                            )
                        }
                        if (isPembeli) {
                            Icon(
                                Icons.Filled.CheckCircle,
                                contentDescription = null,
                                tint = Color(0xFF2563EB),
                                modifier = Modifier.size(18.dp)
                            )
                        }
                    }
                    Spacer(Modifier.height(10.dp))
                    Text("Pembeli / Kontributor", fontWeight = FontWeight.Bold, fontSize = 13.sp)
                    Text(
                        "Ajukan warung baru di peta, kumpulkan XP, & naik level!",
                        fontSize = 11.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(top = 2.dp)
                    )
                }
            }
        }

        // Form Fields
        OutlinedTextField(
            value = nama,
            onValueChange = { nama = it },
            label = { Text("Nama Lengkap") },
            placeholder = { Text("Nama Anda atau Usaha Anda") },
            singleLine = true,
            shape = RoundedCornerShape(12.dp),
            modifier = Modifier.fillMaxWidth()
        )

        OutlinedTextField(
            value = email,
            onValueChange = { email = it },
            label = { Text("Email") },
            placeholder = { Text("contoh@email.com") },
            singleLine = true,
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email),
            shape = RoundedCornerShape(12.dp),
            modifier = Modifier.fillMaxWidth()
        )

        OutlinedTextField(
            value = password,
            onValueChange = { password = it },
            label = { Text("Password (minimal 8 karakter)") },
            singleLine = true,
            visualTransformation = PasswordVisualTransformation(),
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
            shape = RoundedCornerShape(12.dp),
            modifier = Modifier.fillMaxWidth()
        )

        if (error.isNotBlank()) {
            Surface(
                color = MaterialTheme.colorScheme.errorContainer,
                shape = RoundedCornerShape(8.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(
                    text = error,
                    color = MaterialTheme.colorScheme.onErrorContainer,
                    style = MaterialTheme.typography.bodySmall,
                    modifier = Modifier.padding(10.dp)
                )
            }
        }

        Button(
            onClick = { submit() },
            enabled = !loading,
            shape = RoundedCornerShape(12.dp),
            colors = ButtonDefaults.buttonColors(
                containerColor = if (tipeAkun == "penjual") Color(0xFF059669) else Color(0xFF2563EB)
            ),
            modifier = Modifier.fillMaxWidth().height(48.dp)
        ) {
            if (loading) {
                CircularProgressIndicator(
                    modifier = Modifier.size(20.dp).padding(end = 8.dp),
                    strokeWidth = 2.dp,
                    color = Color.White
                )
            }
            Text(
                if (loading) "Mendaftarkan..." else "Daftar Akun",
                fontWeight = FontWeight.Bold
            )
        }

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.Center,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text("Sudah punya akun?", style = MaterialTheme.typography.bodyMedium)
            TextButton(onClick = onBack) {
                Text("Masuk di Sini", fontWeight = FontWeight.Bold, color = Color(0xFF059669))
            }
        }
    }
}
