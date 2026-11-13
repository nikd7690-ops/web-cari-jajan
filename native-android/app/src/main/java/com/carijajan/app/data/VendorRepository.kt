package com.carijajan.app.data

import okhttp3.MediaType.Companion.toMediaType
import okhttp3.MultipartBody
import okhttp3.RequestBody.Companion.toRequestBody

class VendorRepository(private val api: ApiService) {

    suspend fun nearby(lat: Double, lng: Double, radius: Int = 3000): Result<List<VendorDto>> = call {
        val res = api.vendors(lat, lng, radius)
        if (!res.isSuccessful) throw ApiException(ApiErrors.message(res))
        res.body()?.data ?: emptyList()
    }

    suspend fun detail(id: String): Result<VendorDto> = call {
        val res = api.vendor(id)
        if (!res.isSuccessful) throw ApiException(ApiErrors.message(res))
        res.body()?.data ?: throw ApiException("Warung tidak ditemukan")
    }

    suspend fun mine(): Result<List<VendorDto>> = call {
        val res = api.myVendors()
        if (!res.isSuccessful) throw ApiException(ApiErrors.message(res))
        res.body()?.data ?: emptyList()
    }

    suspend fun create(input: VendorInput): Result<VendorDto> = call {
        val res = api.createVendor(input)
        if (!res.isSuccessful) throw ApiException(ApiErrors.message(res))
        res.body()?.data ?: throw ApiException("Gagal menyimpan warung")
    }

    suspend fun update(id: String, input: VendorInput): Result<VendorDto> = call {
        val res = api.updateVendor(id, input)
        if (!res.isSuccessful) throw ApiException(ApiErrors.message(res))
        res.body()?.data ?: throw ApiException("Gagal menyimpan warung")
    }

    suspend fun delete(id: String): Result<Unit> = call {
        val res = api.deleteVendor(id)
        if (!res.isSuccessful) throw ApiException(ApiErrors.message(res))
        Unit
    }

    suspend fun geocode(query: String): Result<List<GeocodeItem>> = call {
        val res = api.geocode(query)
        if (!res.isSuccessful) throw ApiException(ApiErrors.message(res))
        res.body()?.data ?: emptyList()
    }

    suspend fun uploadJpeg(bytes: ByteArray, fileName: String = "warung.jpg"): Result<String> = call {
        val body = bytes.toRequestBody("image/jpeg".toMediaType())
        val part = MultipartBody.Part.createFormData("file", fileName, body)
        val res = api.upload(part)
        if (!res.isSuccessful) throw ApiException(ApiErrors.message(res))
        res.body()?.url ?: throw ApiException("Gagal mengunggah foto")
    }

    suspend fun scanMenuFoto(bytes: ByteArray, fileName: String = "menu.jpg"): Result<List<AiMenuItemDto>> = call {
        val body = bytes.toRequestBody("image/jpeg".toMediaType())
        val part = MultipartBody.Part.createFormData("file", fileName, body)
        val res = api.scanMenuFoto(part)
        if (!res.isSuccessful) throw ApiException(ApiErrors.message(res))
        res.body()?.items ?: emptyList()
    }

    suspend fun kirimLaporan(vendorId: String, alasan: String, deskripsi: String): Result<Unit> = call {
        val res = api.kirimLaporan(KirimLaporanRequest(vendorId, alasan, deskripsi))
        if (!res.isSuccessful) throw ApiException(ApiErrors.message(res))
        Unit
    }

    suspend fun daftarLaporan(): Result<List<LaporanDto>> = call {
        val res = api.daftarLaporan()
        if (!res.isSuccessful) throw ApiException(ApiErrors.message(res))
        res.body()?.data ?: emptyList()
    }

    suspend fun adminMembers(): Result<AdminMembersData> = call {
        val res = api.adminMembers()
        if (!res.isSuccessful) throw ApiException(ApiErrors.message(res))
        res.body()?.data ?: AdminMembersData()
    }

    suspend fun adminVendors(status: String = "semua"): Result<List<VendorDto>> = call {
        if (status == "semua") {
            val res = api.adminVendors("semua")
            if (res.isSuccessful && res.body()?.data != null) {
                return@call res.body()!!.data
            }

            // Fallback backward-compatibility jika backend di Vercel belum dideploy
            // dan masih menggunakan enum lama (hanya menerima pending, approved, rejected)
            val pRes = api.adminVendors("pending")
            val aRes = api.adminVendors("approved")
            val rRes = api.adminVendors("rejected")

            val combined = mutableListOf<VendorDto>()
            pRes.body()?.data?.let { combined.addAll(it) }
            aRes.body()?.data?.let { combined.addAll(it) }
            rRes.body()?.data?.let { combined.addAll(it) }

            if (combined.isNotEmpty() || (pRes.isSuccessful && aRes.isSuccessful)) {
                return@call combined
            }

            if (!res.isSuccessful) throw ApiException(ApiErrors.message(res))
            emptyList()
        } else {
            val res = api.adminVendors(status)
            if (!res.isSuccessful) throw ApiException(ApiErrors.message(res))
            res.body()?.data ?: emptyList()
        }
    }

    suspend fun adminVendorAction(id: String, aksi: String): Result<Unit> = call {
        val res = api.adminVendorAction(id, AdminVendorActionRequest(aksi))
        if (!res.isSuccessful) throw ApiException(ApiErrors.message(res))
        Unit
    }

    suspend fun banMember(id: String, banned: Boolean, reason: String = ""): Result<Unit> = call {
        val res = api.banMember(id, AdminBanMemberRequest(banned, reason))
        if (!res.isSuccessful) throw ApiException(ApiErrors.message(res))
        Unit
    }

    suspend fun deleteMember(id: String): Result<Unit> = call {
        val res = api.deleteMember(id)
        if (!res.isSuccessful) throw ApiException(ApiErrors.message(res))
        Unit
    }

    private inline fun <T> call(block: () -> T): Result<T> = try {
        Result.success(block())
    } catch (e: Exception) {
        val errorMsg = e.message.orEmpty()
        val friendlyEx = if (errorMsg.contains("<!DOCTYPE") || errorMsg.contains("<html") || errorMsg.contains("had '<' instead")) {
            ApiException("Server belum memperbarui endpoint API ini. Mohon tunggu proses deploy server selesai.")
        } else {
            e
        }
        Result.failure(friendlyEx)
    }
}
