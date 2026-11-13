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
import androidx.compose.foundation.layout.imePadding
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
import androidx.compose.material.icons.filled.Email
import androidx.compose.material.icons.filled.Storefront
import androidx.compose.material.icons.filled.VolunteerActivism
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
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
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.carijajan.app.AppContainer
import com.carijajan.app.ui.components.bounceClick
import com.carijajan.app.ui.components.bounceEffect
import com.carijajan.app.ui.theme.*
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
    var tipeAkun by remember { mutableStateOf("mitra") } // "mitra" atau "pengguna"
    var loading by remember { mutableStateOf(false) }
    var error by remember { mutableStateOf("") }

    // State untuk Dialog OTP Verifikasi Gmail
    var otpDialogTerbuka by remember { mutableStateOf(false) }
    var otpInput by remember { mutableStateOf("") }
    var devOtpKode by remember { mutableStateOf<String?>(null) }
    var otpLoading by remember { mutableStateOf(false) }
    var otpError by remember { mutableStateOf("") }
    var otpPesan by remember { mutableStateOf("") }
    var registeredEmail by remember { mutableStateOf("") }
    var registeredPassword by remember { mutableStateOf("") }

    fun submit() {
        val cleanEmail = email.trim().lowercase()
        when {
            nama.isBlank() -> { error = "Nama wajib diisi."; return }
            cleanEmail.isBlank() -> { error = "Email wajib diisi."; return }
            !cleanEmail.endsWith("@gmail.com") -> {
                error = "Wajib menggunakan email resmi @gmail.com (mencegah akun bot/palsu)."; return
            }
            password.length < 8 -> { error = "Password minimal 8 karakter."; return }
        }
        loading = true
        error = ""
        scope.launch {
            val daftar = container.authRepository.register(
                nama = nama.trim(),
                email = cleanEmail,
                password = password,
                tipeAkun = tipeAkun
            )
            loading = false
            if (daftar.isFailure) {
                error = daftar.exceptionOrNull()?.message ?: "Gagal mendaftar, coba lagi."
                return@launch
            }

            val regRes = daftar.getOrNull()
            registeredEmail = cleanEmail
            registeredPassword = password
            devOtpKode = regRes?.devOtp
            otpInput = ""
            otpError = ""
            otpPesan = regRes?.pesan ?: "Kode verifikasi 6-digit telah dikirim ke email Anda."
            otpDialogTerbuka = true
        }
    }

    fun verifikasiOtp() {
        if (otpInput.trim().length != 6) {
            otpError = "Masukkan 6 digit kode OTP yang dikirim ke email Anda."
            return
        }
        otpLoading = true
        otpError = ""
        scope.launch {
            val resVerif = container.authRepository.verifikasiOtpEmail(
                email = registeredEmail,
                kodeOtp = otpInput.trim()
            )
            if (resVerif.isFailure) {
                otpLoading = false
                otpError = resVerif.exceptionOrNull()?.message ?: "Kode OTP salah atau kadaluarsa."
                return@launch
            }

            // Setelah email terverifikasi, otomatis login
            val masuk = container.authRepository.login(registeredEmail, registeredPassword)
            otpLoading = false
            masuk.onSuccess {
                container.authRepository.setTipeAkun(tipeAkun)
                otpDialogTerbuka = false
                onSuccess()
            }.onFailure {
                otpDialogTerbuka = false
                error = "Verifikasi email berhasil! Silakan masuk dengan akun Anda."
            }
        }
    }

    fun kirimUlangOtp() {
        otpLoading = true
        otpError = ""
        scope.launch {
            val res = container.authRepository.kirimOtpEmail(registeredEmail)
            otpLoading = false
            res.onSuccess { actionRes ->
                devOtpKode = actionRes.devOtp ?: devOtpKode
                otpPesan = actionRes.message.ifBlank { "Kode OTP baru berhasil dikirim ke email." }
            }.onFailure {
                otpError = it.message ?: "Gagal mengirim ulang OTP. Tunggu beberapa saat."
            }
        }
    }

    // ── Dialog Verifikasi OTP Gmail ──────────────────────────────────────────
    if (otpDialogTerbuka) {
        AlertDialog(
            onDismissRequest = { /* Pengguna wajib verifikasi atau klik batal */ },
            icon = {
                Icon(
                    Icons.Filled.Email,
                    contentDescription = null,
                    tint = BrandOrange,
                    modifier = Modifier.size(36.dp)
                )
            },
            title = {
                Text(
                    text = "Verifikasi Akun Gmail",
                    fontWeight = FontWeight.Bold,
                    fontSize = 18.sp,
                    textAlign = TextAlign.Center
                )
            },
            text = {
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Text(
                        text = "Masukkan 6-digit kode verifikasi yang dikirim ke:",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Text(
                        text = registeredEmail,
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.Bold,
                        color = BrandOrange
                    )

                    // Banner Kode OTP untuk Kemudahan Penjurian Hackathon / Dev Mode
                    if (!devOtpKode.isNullOrBlank()) {
                        Surface(
                            color = Color(0xFFFEF3C7),
                            shape = RoundedCornerShape(8.dp),
                            border = BorderStroke(1.dp, Color(0xFFFDE68A)),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Column(modifier = Modifier.padding(10.dp)) {
                                Text(
                                    text = "💡 Mode Penilaian / Pengujian Cepat:",
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = Color(0xFF92400E)
                                )
                                Text(
                                    text = "Kode OTP: $devOtpKode",
                                    fontSize = 15.sp,
                                    fontWeight = FontWeight.ExtraBold,
                                    fontFamily = FontFamily.Monospace,
                                    color = Color(0xFFB45309),
                                    modifier = Modifier.padding(top = 2.dp)
                                )
                            }
                        }
                    }

                    if (otpPesan.isNotBlank()) {
                        Text(
                            text = otpPesan,
                            fontSize = 12.sp,
                            color = BrandOrange
                        )
                    }

                    OutlinedTextField(
                        value = otpInput,
                        onValueChange = { if (it.length <= 6 && it.all { c -> c.isDigit() }) otpInput = it },
                        label = { Text("Kode OTP (6 Angka)") },
                        placeholder = { Text("123456") },
                        singleLine = true,
                        textStyle = TextStyle(
                            fontSize = 22.sp,
                            fontWeight = FontWeight.Bold,
                            fontFamily = FontFamily.Monospace,
                            textAlign = TextAlign.Center,
                            letterSpacing = 6.sp
                        ),
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier.fillMaxWidth()
                    )

                    if (otpError.isNotBlank()) {
                        Surface(
                            color = MaterialTheme.colorScheme.errorContainer,
                            shape = RoundedCornerShape(6.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text(
                                text = otpError,
                                color = MaterialTheme.colorScheme.onErrorContainer,
                                style = MaterialTheme.typography.bodySmall,
                                modifier = Modifier.padding(8.dp)
                            )
                        }
                    }

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.Center
                    ) {
                        TextButton(
                            onClick = { kirimUlangOtp() },
                            enabled = !otpLoading
                        ) {
                            Text("Kirim Ulang Kode OTP", fontSize = 12.sp, color = Color(0xFF2563EB))
                        }
                    }
                }
            },
            confirmButton = {
                Button(
                    onClick = { verifikasiOtp() },
                    enabled = !otpLoading && otpInput.length == 6,
                    colors = ButtonDefaults.buttonColors(containerColor = BrandOrange),
                    modifier = Modifier.bounceEffect()
                ) {
                    if (otpLoading) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(16.dp),
                            color = Color.White,
                            strokeWidth = 2.dp
                        )
                        Spacer(Modifier.width(6.dp))
                    }
                    Text("Verifikasi & Masuk")
                }
            },
            dismissButton = {
                OutlinedButton(
                    onClick = { otpDialogTerbuka = false },
                    enabled = !otpLoading
                ) {
                    Text("Batal")
                }
            }
        )
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .statusBarsPadding()
            .imePadding()
            .verticalScroll(rememberScrollState())
            .padding(20.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            IconButton(
                onClick = onBack,
                modifier = Modifier.bounceEffect()
            ) {
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

        // ── Pilihan Peran Akun: Mitra vs Pengguna / Member ──────────────────────
        Text("Pilih Jenis Akun Anda:", fontWeight = FontWeight.SemiBold, fontSize = 14.sp)

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            // Opsi 1: Mitra / Pemilik Warung
            val isMitra = tipeAkun == "mitra"
            Surface(
                shape = RoundedCornerShape(14.dp),
                border = BorderStroke(
                    width = if (isMitra) 2.dp else 1.dp,
                    color = if (isMitra) BrandOrange else Color(0xFFE7E5E4)
                ),
                color = if (isMitra) BrandOrangeSurface else Color.White,
                modifier = Modifier
                    .weight(1f)
                    .bounceClick { tipeAkun = "mitra" }
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
                                .background(if (isMitra) BrandOrange else Color(0xFFF5F5F4)),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                Icons.Filled.Storefront,
                                contentDescription = null,
                                tint = if (isMitra) Color.White else Color(0xFF78716C),
                                modifier = Modifier.size(20.dp)
                            )
                        }
                        if (isMitra) {
                            Icon(
                                Icons.Filled.CheckCircle,
                                contentDescription = null,
                                tint = BrandOrange,
                                modifier = Modifier.size(18.dp)
                            )
                        }
                    }
                    Spacer(Modifier.height(10.dp))
                    Text("Mitra / Pemilik Warung", fontWeight = FontWeight.Bold, fontSize = 12.sp, color = if (isMitra) BrandOrangeDark else Color(0xFF1C1917))
                    Text(
                        "Kelola 1 warung resmi, menu, statistik, & diskon darurat Zero Waste Food.",
                        fontSize = 10.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        lineHeight = 14.sp,
                        modifier = Modifier.padding(top = 2.dp)
                    )
                }
            }

            // Opsi 2: Pengguna / Member Komunitas
            val isPengguna = tipeAkun == "pengguna"
            Surface(
                shape = RoundedCornerShape(14.dp),
                border = BorderStroke(
                    width = if (isPengguna) 2.dp else 1.dp,
                    color = if (isPengguna) BrandMaroon else Color(0xFFE7E5E4)
                ),
                color = if (isPengguna) BrandMaroonContainer else Color.White,
                modifier = Modifier
                    .weight(1f)
                    .bounceClick { tipeAkun = "pengguna" }
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
                                .background(if (isPengguna) BrandMaroon else Color(0xFFF5F5F4)),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                Icons.Filled.VolunteerActivism,
                                contentDescription = null,
                                tint = if (isPengguna) Color.White else Color(0xFF78716C),
                                modifier = Modifier.size(20.dp)
                            )
                        }
                        if (isPengguna) {
                            Icon(
                                Icons.Filled.CheckCircle,
                                contentDescription = null,
                                tint = BrandMaroon,
                                modifier = Modifier.size(18.dp)
                            )
                        }
                    }
                    Spacer(Modifier.height(10.dp))
                    Text("Pengguna / Warga", fontWeight = FontWeight.Bold, fontSize = 12.sp, color = if (isPengguna) BrandMaroon else Color(0xFF1C1917))
                    Text(
                        "Cari warung, pesan via WA, ajukan warung baru tersembunyi & raih XP level!",
                        fontSize = 10.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        lineHeight = 14.sp,
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

        Column(modifier = Modifier.fillMaxWidth()) {
            OutlinedTextField(
                value = email,
                onValueChange = { email = it },
                label = { Text("Email Resmi (@gmail.com)") },
                placeholder = { Text("nama@gmail.com") },
                singleLine = true,
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email),
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier.fillMaxWidth()
            )
            Text(
                text = "🛡️ Hanya menerima akun resmi @gmail.com untuk mencegah akun palsu/spam.",
                fontSize = 11.sp,
                color = BrandOrange,
                modifier = Modifier.padding(start = 4.dp, top = 4.dp)
            )
        }

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
            shape = RoundedCornerShape(14.dp),
            colors = ButtonDefaults.buttonColors(
                containerColor = if (tipeAkun == "mitra" || tipeAkun == "penjual") BrandOrange else BrandMaroon
            ),
            modifier = Modifier
                .fillMaxWidth()
                .height(50.dp)
                .bounceEffect()
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
                fontWeight = FontWeight.Bold,
                fontSize = 15.sp
            )
        }

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.Center,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text("Sudah punya akun?", style = MaterialTheme.typography.bodyMedium)
            TextButton(
                onClick = onBack,
                modifier = Modifier.bounceEffect()
            ) {
                Text("Masuk di Sini", fontWeight = FontWeight.Bold, color = BrandOrange)
            }
        }
    }
}
