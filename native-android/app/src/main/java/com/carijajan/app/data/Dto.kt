package com.carijajan.app.data

import kotlinx.serialization.ExperimentalSerializationApi
import kotlinx.serialization.KSerializer
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.descriptors.PrimitiveKind
import kotlinx.serialization.descriptors.PrimitiveSerialDescriptor
import kotlinx.serialization.descriptors.SerialDescriptor
import kotlinx.serialization.encoding.Decoder
import kotlinx.serialization.encoding.Encoder
import kotlinx.serialization.json.JsonDecoder
import kotlinx.serialization.json.JsonNull
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.contentOrNull

@OptIn(ExperimentalSerializationApi::class)
object FlexibleStringSerializer : KSerializer<String?> {
    override val descriptor: SerialDescriptor =
        PrimitiveSerialDescriptor("FlexibleStringSerializer", PrimitiveKind.STRING)

    override fun deserialize(decoder: Decoder): String? {
        val jsonDecoder = decoder as? JsonDecoder ?: return try {
            decoder.decodeString()
        } catch (_: Exception) {
            null
        }

        return try {
            val element = jsonDecoder.decodeJsonElement()
            when (element) {
                is JsonNull -> null
                is JsonPrimitive -> element.contentOrNull
                is JsonObject -> {
                    val name = element["name"]?.let { if (it is JsonPrimitive) it.contentOrNull else null }
                    val id = element["_id"]?.let { if (it is JsonPrimitive) it.contentOrNull else null }
                        ?: element["id"]?.let { if (it is JsonPrimitive) it.contentOrNull else null }
                    val namaToko = element["namaToko"]?.let { if (it is JsonPrimitive) it.contentOrNull else null }
                    name ?: namaToko ?: id
                }
                else -> null
            }
        } catch (_: Exception) {
            null
        }
    }

    override fun serialize(encoder: Encoder, value: String?) {
        if (value != null) {
            encoder.encodeString(value)
        } else {
            encoder.encodeNull()
        }
    }
}

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
    @Serializable(with = FlexibleStringSerializer::class) val owner: String? = null,
    @Serializable(with = FlexibleStringSerializer::class) val submittedBy: String? = null,
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
    val views: Int = 0,
    val namaKontributor: String? = null,
    val createdAt: String? = null
) {
    val lat: Double get() = lokasi?.coordinates?.getOrNull(1) ?: 0.0
    val lng: Double get() = lokasi?.coordinates?.getOrNull(0) ?: 0.0
    val kategoriLabel: String get() = when (kategori) {
        "pkl" -> "Gerobak PKL"
        "keliling" -> "Keliling"
        "rumah_makan" -> "Rumah Makan"
        else -> "Warung Tetap"
    }

    val diskonPersen: Int get() = if (zeroWaste?.aktif == true) zeroWaste.diskonPersen.coerceIn(0, 100) else 0
    val adaDiskon: Boolean get() = diskonPersen > 0
    fun hitungHargaDiskon(hargaAsli: Double): Double {
        if (!adaDiskon) return hargaAsli
        return (hargaAsli * (100 - diskonPersen) / 100.0).coerceAtLeast(0.0)
    }

    /**
     * Mengecek status operasional buka/tutup secara akurat menggunakan jam lokal perangkat (WIB/WITA/WIT).
     * Jika sekarang jam 07:00 dan jam buka 07:00-12:00, warung dipastikan berstatus buka.
     */
    val bukaAktif: Boolean get() {
        if (jamBuka.isBlank() || jamTutup.isBlank()) return buka
        return try {
            val partsBuka = jamBuka.split(":")
            val partsTutup = jamTutup.split(":")
            if (partsBuka.size < 2 || partsTutup.size < 2) return buka

            val bMin = (partsBuka[0].trim().toIntOrNull() ?: 0) * 60 + (partsBuka[1].trim().toIntOrNull() ?: 0)
            val tMin = (partsTutup[0].trim().toIntOrNull() ?: 0) * 60 + (partsTutup[1].trim().toIntOrNull() ?: 0)

            if (bMin == tMin) return true // Warung buka 24 jam penuh

            val cal = java.util.Calendar.getInstance()
            val sMin = cal.get(java.util.Calendar.HOUR_OF_DAY) * 60 + cal.get(java.util.Calendar.MINUTE)

            if (tMin > bMin) {
                sMin in bMin until tMin
            } else {
                sMin >= bMin || sMin < tMin
            }
        } catch (_: Exception) {
            buka
        }
    }
}

@Serializable
data class UserDto(
    @SerialName("_id") val mongoId: String? = null,
    val id: String? = null,
    val name: String? = null,
    val email: String? = null,
    val role: String? = null,
    val tipeAkun: String? = null,
    val xp: Int? = null,
    val level: Int? = null,
    val emailVerified: Boolean? = null,
    val isBanned: Boolean? = null,
    val totalPengajuan: Int? = null
) {
    val displayId: String get() = id?.ifBlank { mongoId.orEmpty() } ?: mongoId.orEmpty()
    val displayName: String get() = name?.ifBlank { "Member Cari Jajan" } ?: "Member Cari Jajan"
    val displayEmail: String get() = email.orEmpty()
    val displayRole: String get() = role ?: "user"
    val displayTipeAkun: String get() = tipeAkun ?: "pengguna"
    val displayXp: Int get() = xp ?: 0
    val displayLevel: Int get() = level ?: 1
    val displayIsBanned: Boolean get() = isBanned ?: false
    val displayTotalPengajuan: Int get() = totalPengajuan ?: 0
}

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

@Serializable
data class KirimLaporanRequest(
    val vendorId: String,
    val alasan: String,
    val deskripsi: String = ""
)

@Serializable
data class LaporanDto(
    @SerialName("_id") val id: String = "",
    @Serializable(with = FlexibleStringSerializer::class) val vendorId: String? = null,
    val namaToko: String = "",
    val namaPelapor: String = "Warga Komunitas",
    val alasan: String = "lokasi_salah",
    val deskripsi: String = "",
    val status: String = "pending",
    val catatanAdmin: String = "",
    val createdAt: String? = null
) {
    val alasanLabel: String get() = when (alasan) {
        "lokasi_salah" -> "Titik lokasi tidak sesuai peta"
        "sudah_tutup" -> "Toko sudah tutup permanen"
        "menu_salah" -> "Menu/harga tidak akurat"
        "duplikat" -> "Warung duplikat"
        else -> "Masalah lainnya"
    }
}

@Serializable
data class LaporanListResponse(val data: List<LaporanDto> = emptyList())

@Serializable
data class TopKontributorDto(
    @Serializable(with = FlexibleStringSerializer::class) val userId: String? = null,
    val name: String? = null,
    val email: String? = null,
    val xp: Int? = null,
    val level: Int? = null,
    val totalPengajuan: Int? = null,
    val diterima: Int? = null,
    val menunggu: Int? = null
) {
    val displayName: String get() = name?.ifBlank { "Warga Komunitas" } ?: "Warga Komunitas"
    val displayEmail: String get() = email ?: "-"
    val displayXp: Int get() = xp ?: 0
    val displayLevel: Int get() = level ?: 1
    val displayTotalPengajuan: Int get() = totalPengajuan ?: 0
    val displayDiterima: Int get() = diterima ?: 0
    val displayMenunggu: Int get() = menunggu ?: 0
}

@Serializable
data class AdminMembersData(
    val members: List<UserDto>? = emptyList(),
    val topKontributor: List<TopKontributorDto>? = emptyList()
) {
    val safeMembers: List<UserDto> get() = members ?: emptyList()
    val safeTopKontributor: List<TopKontributorDto> get() = topKontributor ?: emptyList()
}

@Serializable
data class AdminMembersResponse(val data: AdminMembersData? = null)

@Serializable
data class AdminVendorActionRequest(val aksi: String)

@Serializable
data class AdminBanMemberRequest(
    val banned: Boolean,
    val reason: String = ""
)

@Serializable
data class RequestOtpRequest(
    val email: String
)

@Serializable
data class RequestOtpResponse(
    val ok: Boolean = false,
    val message: String = "",
    val demoOtp: String? = null
)

@Serializable
data class ResetPasswordRequest(
    val email: String,
    val otp: String,
    val passwordBaru: String
)

@Serializable
data class ResetPasswordResponse(
    val ok: Boolean = false,
    val message: String = ""
)
