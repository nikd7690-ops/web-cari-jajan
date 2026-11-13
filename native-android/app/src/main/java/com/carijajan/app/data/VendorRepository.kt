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
        val res = api.adminVendors(status)
        if (!res.isSuccessful) throw ApiException(ApiErrors.message(res))
        res.body()?.data ?: emptyList()
    }

    suspend fun adminVendorAction(id: String, aksi: String): Result<Unit> = call {
        val res = api.adminVendorAction(id, AdminVendorActionRequest(aksi))
        if (!res.isSuccessful) throw ApiException(ApiErrors.message(res))
        Unit
    }

    private inline fun <T> call(block: () -> T): Result<T> = try {
        Result.success(block())
    } catch (e: Exception) {
        Result.failure(e)
    }
}
