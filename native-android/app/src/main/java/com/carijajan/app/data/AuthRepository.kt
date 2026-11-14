package com.carijajan.app.data

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

class AuthRepository(
    private val api: ApiService,
    private val tokenStore: TokenStore
) {
    private val _user = MutableStateFlow<UserDto?>(null)
    val user: StateFlow<UserDto?> = _user.asStateFlow()

    private val _tipeAkun = MutableStateFlow("penjual") // "penjual" atau "pembeli"
    val tipeAkun: StateFlow<String> = _tipeAkun.asStateFlow()

    suspend fun setTipeAkun(tipe: String) {
        tokenStore.saveTipeAkun(tipe)
        _tipeAkun.value = tipe
    }

    suspend fun login(email: String, password: String): Result<UserDto> = try {
        val res = api.login(LoginRequest(email.trim().lowercase(), password))
        if (res.isSuccessful) {
            val body = res.body()
            if (body == null) {
                Result.failure(ApiException("Balasan login tidak valid"))
            } else {
                tokenStore.saveToken(body.token)
                val tipe = body.user.displayTipeAkun.ifBlank { tokenStore.getTipeAkun() }
                tokenStore.saveTipeAkun(tipe)
                _tipeAkun.value = tipe
                _user.value = body.user
                Result.success(body.user)
            }
        } else {
            Result.failure(ApiException(ApiErrors.message(res)))
        }
    } catch (e: Exception) {
        Result.failure(e)
    }

    suspend fun register(
        nama: String,
        email: String,
        password: String,
        tipeAkun: String = "mitra"
    ): Result<RegisterResponse> = try {
        val cleanTipe = if (tipeAkun == "mitra" || tipeAkun == "penjual") "mitra" else "pengguna"
        val res = api.register(RegisterRequest(nama.trim(), email.trim().lowercase(), password, cleanTipe))
        if (res.isSuccessful) {
            tokenStore.saveTipeAkun(cleanTipe)
            _tipeAkun.value = cleanTipe
            Result.success(res.body() ?: RegisterResponse())
        } else {
            Result.failure(ApiException(ApiErrors.message(res)))
        }
    } catch (e: Exception) {
        Result.failure(e)
    }

    suspend fun kirimOtpEmail(email: String): Result<OtpActionResponse> = try {
        val res = api.kirimOtpEmail(KirimOtpEmailRequest(email.trim().lowercase()))
        if (res.isSuccessful) {
            Result.success(res.body() ?: OtpActionResponse())
        } else {
            Result.failure(ApiException(ApiErrors.message(res)))
        }
    } catch (e: Exception) {
        Result.failure(e)
    }

    suspend fun verifikasiOtpEmail(email: String, kodeOtp: String): Result<OtpActionResponse> = try {
        val res = api.verifikasiOtpEmail(VerifikasiOtpEmailRequest(email.trim().lowercase(), kodeOtp.trim()))
        if (res.isSuccessful) {
            val body = res.body() ?: OtpActionResponse()
            if (body.user != null) {
                _user.value = body.user
            }
            Result.success(body)
        } else {
            Result.failure(ApiException(ApiErrors.message(res)))
        }
    } catch (e: Exception) {
        Result.failure(e)
    }

    suspend fun requestOtp(email: String): Result<RequestOtpResponse> = try {
        val res = api.requestOtp(RequestOtpRequest(email.trim().lowercase()))
        if (res.isSuccessful) {
            Result.success(res.body() ?: RequestOtpResponse(ok = true, message = "Kode OTP berhasil dikirim."))
        } else {
            Result.failure(ApiException(ApiErrors.message(res)))
        }
    } catch (e: Exception) {
        Result.failure(e)
    }

    suspend fun resetPassword(email: String, otp: String, passwordBaru: String): Result<String> = try {
        val res = api.resetPassword(ResetPasswordRequest(email.trim().lowercase(), otp.trim(), passwordBaru))
        if (res.isSuccessful) {
            Result.success(res.body()?.message ?: "Password berhasil diperbarui.")
        } else {
            Result.failure(ApiException(ApiErrors.message(res)))
        }
    } catch (e: Exception) {
        Result.failure(e)
    }

    suspend fun restoreSession(): UserDto? {
        val token = tokenStore.getToken()
        _tipeAkun.value = tokenStore.getTipeAkun()
        if (token.isNullOrBlank()) {
            _user.value = null
            return null
        }

        return try {
            val res = api.session()
            if (res.isSuccessful) {
                val user = res.body()?.user
                if (user == null) tokenStore.clear()
                _user.value = user
                user
            } else {
                if (res.code() == 401) tokenStore.clear()
                _user.value = null
                null
            }
        } catch (_: Exception) {
            null
        }
    }

    suspend fun logout() {
        tokenStore.clear()
        _user.value = null
    }
}
