package com.carijajan.app.ui.screen

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
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
import androidx.compose.material.icons.filled.Storefront
import androidx.compose.material.icons.filled.VolunteerActivism
import androidx.compose.material3.AlertDialog
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
import com.carijajan.app.ui.components.bounceClick
import com.carijajan.app.ui.components.bounceEffect
import com.carijajan.app.ui.theme.*
import com.carijajan.app.ui.components.CariJajanBottomBar
import kotlinx.coroutines.launch

@Composable
fun MasukScreen(
    container: AppContainer,
    onSuccess: () -> Unit,
    onRegister: () -> Unit,
    onBack: () -> Unit,
    tampilkanBottomBar: Boolean = true
) {
    val scope = rememberCoroutineScope()
    var email by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    var loading by remember { mutableStateOf(false) }
    var error by remember { mutableStateOf("") }

    // State Lupa Password
    var dialogLupaPassword by remember { mutableStateOf(false) }
    var resetEmail by remember { mutableStateOf("") }
    var resetPasswordBaru by remember { mutableStateOf("") }
    var resetKonfirmasi by remember { mutableStateOf("") }
    var resetLoading by remember { mutableStateOf(false) }
    var resetError by remember { mutableStateOf("") }
    var resetSuksesPesan by remember { mutableStateOf("") }

    fun submit() {
        if (email.isBlank() || password.isBlank()) {
            error = "Email dan password wajib diisi."
            return
        }
        loading = true
        error = ""
        scope.launch {
            val res = container.authRepository.login(email, password)
            loading = false
            res.onSuccess { onSuccess() }
                .onFailure { error = it.message ?: "Gagal masuk, coba lagi." }
        }
    }

    Box(modifier = Modifier.fillMaxSize()) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .statusBarsPadding()
                .imePadding()
                .verticalScroll(rememberScrollState())
                .padding(20.dp)
                .padding(bottom = 76.dp),
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
                text = "Masuk ke Cari Jajan",
                style = MaterialTheme.typography.headlineMedium,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface
            )
            Spacer(Modifier.height(6.dp))
            Text(
                text = "Satu akun untuk Pemilik Warung UMKM dan Pembeli / Kontributor Komunitas.",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }

        // Info Card: Menjelaskan 2 Peran
        Surface(
            color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
            shape = RoundedCornerShape(14.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier
                            .size(32.dp)
                            .clip(RoundedCornerShape(8.dp))
                            .background(BrandOrange),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(Icons.Filled.Storefront, contentDescription = null, tint = Color.White, modifier = Modifier.size(18.dp))
                    }
                    Spacer(Modifier.width(10.dp))
                    Column {
                        Text("Pemilik Warung", fontWeight = FontWeight.SemiBold, fontSize = 13.sp)
                        Text("Kelola warung, pantau pengunjung, & promo Zero Waste", fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                }

                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier
                            .size(32.dp)
                            .clip(RoundedCornerShape(8.dp))
                            .background(BrandMaroon),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(Icons.Filled.VolunteerActivism, contentDescription = null, tint = Color.White, modifier = Modifier.size(18.dp))
                    }
                    Spacer(Modifier.width(10.dp))
                    Column {
                        Text("Pembeli & Kontributor", fontWeight = FontWeight.SemiBold, fontSize = 13.sp)
                        Text("Ajukan warung baru, raih XP, dan naikkan level!", fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                }
            }
        }

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
            label = { Text("Password") },
            singleLine = true,
            visualTransformation = PasswordVisualTransformation(),
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
            shape = RoundedCornerShape(12.dp),
            modifier = Modifier.fillMaxWidth()
        )

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.End
        ) {
            TextButton(
                onClick = {
                    resetEmail = email.trim()
                    resetPasswordBaru = ""
                    resetKonfirmasi = ""
                    resetError = ""
                    resetSuksesPesan = ""
                    dialogLupaPassword = true
                },
                modifier = Modifier.bounceEffect()
            ) {
                Text(
                    text = "Lupa password?",
                    fontSize = 12.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = BrandOrange
                )
            }
        }

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
            colors = ButtonDefaults.buttonColors(containerColor = BrandOrange),
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
            Text(if (loading) "Memproses..." else "Masuk Akun", fontWeight = FontWeight.Bold, fontSize = 15.sp)
        }

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.Center,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text("Belum punya akun?", style = MaterialTheme.typography.bodyMedium)
            TextButton(
                onClick = onRegister,
                modifier = Modifier.bounceEffect()
            ) {
                Text("Daftar Sekarang", fontWeight = FontWeight.Bold, color = BrandOrange)
            }
        }
    }

        if (tampilkanBottomBar) {
            CariJajanBottomBar(
                tabAktif = "akun",
                sudahLogin = false,
                modifier = Modifier.align(Alignment.BottomCenter),
                onTabSelected = { tab ->
                    when (tab) {
                        "peta" -> onBack()
                        "lokasi" -> onBack()
                        "daftar" -> onBack()
                        "tambah" -> onBack()
                        "akun" -> Unit
                    }
                }
            )
        }

        if (dialogLupaPassword) {
            AlertDialog(
                onDismissRequest = { if (!resetLoading) dialogLupaPassword = false },
                title = { Text("Lupa Password", fontWeight = FontWeight.Bold) },
                text = {
                    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                        if (resetSuksesPesan.isNotBlank()) {
                            Surface(
                                color = Color(0xFFECFDF5),
                                shape = RoundedCornerShape(8.dp),
                                border = BorderStroke(1.dp, Color(0xFF86EFAC)),
                                modifier = Modifier.fillMaxWidth().padding(bottom = 4.dp)
                            ) {
                                Text(
                                    resetSuksesPesan,
                                    color = Color(0xFF047857),
                                    fontSize = 12.sp,
                                    modifier = Modifier.padding(10.dp)
                                )
                            }
                        } else {
                            Text(
                                "Masukkan email akun Anda dan buat kata sandi baru (minimal 8 karakter).",
                                fontSize = 12.sp,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            OutlinedTextField(
                                value = resetEmail,
                                onValueChange = { resetEmail = it },
                                label = { Text("Email Akun") },
                                singleLine = true,
                                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email),
                                modifier = Modifier.fillMaxWidth()
                            )
                            OutlinedTextField(
                                value = resetPasswordBaru,
                                onValueChange = { resetPasswordBaru = it },
                                label = { Text("Password Baru") },
                                singleLine = true,
                                visualTransformation = PasswordVisualTransformation(),
                                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
                                modifier = Modifier.fillMaxWidth()
                            )
                            OutlinedTextField(
                                value = resetKonfirmasi,
                                onValueChange = { resetKonfirmasi = it },
                                label = { Text("Konfirmasi Password Baru") },
                                singleLine = true,
                                visualTransformation = PasswordVisualTransformation(),
                                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
                                modifier = Modifier.fillMaxWidth()
                            )
                            if (resetError.isNotBlank()) {
                                Text(
                                    resetError,
                                    color = MaterialTheme.colorScheme.error,
                                    fontSize = 11.sp
                                )
                            }
                        }
                    }
                },
                confirmButton = {
                    if (resetSuksesPesan.isNotBlank()) {
                        Button(
                            onClick = {
                                email = resetEmail
                                password = ""
                                dialogLupaPassword = false
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = BrandOrange)
                        ) {
                            Text("Tutup & Masuk")
                        }
                    } else {
                        Button(
                            onClick = {
                                if (resetEmail.isBlank() || !resetEmail.contains("@")) {
                                    resetError = "Masukkan email akun yang valid."
                                    return@Button
                                }
                                if (resetPasswordBaru.length < 8) {
                                    resetError = "Password baru minimal 8 karakter."
                                    return@Button
                                }
                                if (resetPasswordBaru != resetKonfirmasi) {
                                    resetError = "Konfirmasi password baru tidak cocok."
                                    return@Button
                                }
                                resetLoading = true
                                resetError = ""
                                scope.launch {
                                    val res = container.authRepository.resetPassword(resetEmail, resetPasswordBaru)
                                    resetLoading = false
                                    res.onSuccess {
                                        resetSuksesPesan = it
                                    }.onFailure { err ->
                                        resetError = err.message ?: "Gagal memperbarui password."
                                    }
                                }
                            },
                            enabled = !resetLoading,
                            colors = ButtonDefaults.buttonColors(containerColor = BrandOrange)
                        ) {
                            if (resetLoading) {
                                CircularProgressIndicator(
                                    modifier = Modifier.size(16.dp),
                                    strokeWidth = 2.dp,
                                    color = Color.White
                                )
                                Spacer(Modifier.width(6.dp))
                            }
                            Text("Perbarui Password")
                        }
                    }
                },
                dismissButton = {
                    if (resetSuksesPesan.isBlank()) {
                        TextButton(
                            onClick = { dialogLupaPassword = false },
                            enabled = !resetLoading
                        ) {
                            Text("Batal")
                        }
                    }
                }
            )
        }
    }
}
