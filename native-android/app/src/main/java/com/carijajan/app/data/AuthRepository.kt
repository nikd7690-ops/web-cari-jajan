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

    suspend fun login(email: String, password: String): Result<UserDto> = try {
        val res = api.login(LoginRequest(email.trim().lowercase(), password))
        if (res.isSuccessful) {
            val body = res.body()
            if (body == null) {
                Result.failure(ApiException("Balasan login tidak valid"))
            } else {
                tokenStore.saveToken(body.token)
                _user.value = body.user
                Result.success(body.user)
            }
        } else {
            Result.failure(ApiException(ApiErrors.message(res)))
        }
    } catch (e: Exception) {
        Result.failure(e)
    }

    suspend fun register(nama: String, email: String, password: String): Result<Unit> = try {
        val res = api.register(RegisterRequest(nama.trim(), email.trim().lowercase(), password))
        if (res.isSuccessful) {
            Result.success(Unit)
        } else {
            Result.failure(ApiException(ApiErrors.message(res)))
        }
    } catch (e: Exception) {
        Result.failure(e)
    }

    suspend fun restoreSession(): UserDto? {
        val token = tokenStore.getToken()
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
