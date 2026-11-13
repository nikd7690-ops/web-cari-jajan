package com.carijajan.app.data

import okhttp3.MultipartBody
import retrofit2.Response
import retrofit2.http.Body
import retrofit2.http.DELETE
import retrofit2.http.GET
import retrofit2.http.Multipart
import retrofit2.http.POST
import retrofit2.http.PUT
import retrofit2.http.Part
import retrofit2.http.Path
import retrofit2.http.Query

interface ApiService {
    @POST("api/login")
    suspend fun login(@Body body: LoginRequest): Response<LoginResponse>

    @POST("api/register")
    suspend fun register(@Body body: RegisterRequest): Response<RegisterResponse>

    @POST("api/auth/kirim-otp-email")
    suspend fun kirimOtpEmail(@Body body: KirimOtpEmailRequest): Response<OtpActionResponse>

    @POST("api/auth/verifikasi-otp-email")
    suspend fun verifikasiOtpEmail(@Body body: VerifikasiOtpEmailRequest): Response<OtpActionResponse>

    @GET("api/session")
    suspend fun session(): Response<SessionResponse>

    @GET("api/vendors")
    suspend fun vendors(
        @Query("lat") lat: Double,
        @Query("lng") lng: Double,
        @Query("radius") radius: Int = 3000
    ): Response<VendorListResponse>

    @GET("api/vendors/{id}")
    suspend fun vendor(@Path("id") id: String): Response<VendorResponse>

    @GET("api/vendors/mine")
    suspend fun myVendors(): Response<VendorListResponse>

    @POST("api/vendors")
    suspend fun createVendor(@Body body: VendorInput): Response<VendorResponse>

    @PUT("api/vendors/{id}")
    suspend fun updateVendor(
        @Path("id") id: String,
        @Body body: VendorInput
    ): Response<VendorResponse>

    @DELETE("api/vendors/{id}")
    suspend fun deleteVendor(@Path("id") id: String): Response<Unit>

    @GET("api/geocode")
    suspend fun geocode(@Query("q") q: String): Response<GeocodeResponse>

    @Multipart
    @POST("api/upload")
    suspend fun upload(@Part file: MultipartBody.Part): Response<UploadResponse>

    @Multipart
    @POST("api/menu-dari-foto")
    suspend fun scanMenuFoto(@Part file: MultipartBody.Part): Response<MenuScanAiResponse>
}
