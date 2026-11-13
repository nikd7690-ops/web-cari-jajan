package com.carijajan.app.data

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class Coordinates(
    val type: String = "Point",
    val coordinates: List<Double> = emptyList()
)

@Serializable
data class MenuDto(
    @SerialName("_id") val id: String? = null,
    val namaMenu: String = "",
    val harga: Double = 0.0,
    val tersedia: Boolean = true
)

@Serializable
data class ZeroWasteDto(
    val aktif: Boolean = false,
    val diskonPersen: Int = 0,
    val keterangan: String = ""
)

@Serializable
data class VendorDto(
    @SerialName("_id") val id: String = "",
    val owner: String? = null,
    val submittedBy: String? = null,
    val status: String = "approved",
    val namaToko: String = "",
    val kategori: String = "warung",
    val lokasi: Coordinates? = null,
    val alamatPatokan: String = "",
    val jamBuka: String = "00:00",
    val jamTutup: String = "00:00",
    val nomorWA: String = "",
    val layananAntar: Boolean = false,
    val foto: String = "",
    val menus: List<MenuDto> = emptyList(),
    val jarak: Double? = null,
    val buka: Boolean = false,
    val zeroWaste: ZeroWasteDto? = null,
    val namaKontributor: String? = null,
    val createdAt: String? = null
) {
    val lat: Double get() = lokasi?.coordinates?.getOrNull(1) ?: 0.0
    val lng: Double get() = lokasi?.coordinates?.getOrNull(0) ?: 0.0
    val kategoriEmoji: String get() = when (kategori) {
        "pkl" -> "🍢"
        "keliling" -> "🚲"
        "rumah_makan" -> "🍽️"
        else -> "🏪"
    }
}

@Serializable
data class UserDto(
    val id: String = "",
    val name: String = "",
    val email: String = "",
    val role: String = "user",
    val tipeAkun: String = "pengguna",
    val xp: Int = 0,
    val level: Int = 1,
    val emailVerified: Boolean = false
)

@Serializable
data class LoginRequest(val email: String, val password: String)

@Serializable
data class RegisterRequest(
    val nama: String,
    val email: String,
    val password: String,
    val tipeAkun: String = "pengguna"
)

@Serializable
data class LoginResponse(val token: String, val user: UserDto)

@Serializable
data class SessionResponse(val user: UserDto? = null)

@Serializable
data class RegisterResponse(
    val data: UserDto? = null,
    val pesan: String? = null,
    val devOtp: String? = null
)

@Serializable
data class KirimOtpEmailRequest(val email: String)

@Serializable
data class VerifikasiOtpEmailRequest(val email: String, val kodeOtp: String)

@Serializable
data class OtpActionResponse(
    val message: String = "",
    val emailVerified: Boolean = false,
    val devOtp: String? = null,
    val user: UserDto? = null,
    val error: String? = null
)

@Serializable
data class VendorListResponse(val data: List<VendorDto> = emptyList())

@Serializable
data class VendorResponse(val data: VendorDto? = null)

@Serializable
data class UploadResponse(val url: String? = null)

@Serializable
data class GeocodeItem(val nama: String = "", val lat: Double = 0.0, val lng: Double = 0.0)

@Serializable
data class GeocodeResponse(val data: List<GeocodeItem> = emptyList())

@Serializable
data class DetailError(val field: String? = null, val message: String? = null)

@Serializable
data class ApiError(
    val error: String? = null,
    val details: List<DetailError>? = null
)

@Serializable
data class MenuInput(
    val namaMenu: String,
    val harga: Double,
    val tersedia: Boolean = true
)

@Serializable
data class VendorInput(
    val namaToko: String,
    val kategori: String,
    val lat: Double,
    val lng: Double,
    val alamatPatokan: String,
    val jamBuka: String,
    val jamTutup: String,
    val nomorWA: String,
    val layananAntar: Boolean = false,
    val foto: String = "",
    val menus: List<MenuInput> = emptyList(),
    val userLat: Double? = null,
    val userLng: Double? = null,
    val zeroWaste: ZeroWasteDto? = null
)

@Serializable
data class AiMenuItemDto(
    val namaMenu: String = "",
    val harga: Double? = null,
    val yakin: Boolean = true
)

@Serializable
data class MenuScanAiResponse(
    val items: List<AiMenuItemDto> = emptyList(),
    val error: String? = null
)
