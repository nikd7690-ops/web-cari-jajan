package com.carijajan.app.util

import android.content.Context
import android.content.Intent
import android.net.Uri
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import org.json.JSONObject
import org.maplibre.android.geometry.LatLng
import java.util.concurrent.TimeUnit

object Navigasi {
    private val httpClient = OkHttpClient.Builder()
        .connectTimeout(5, TimeUnit.SECONDS)
        .readTimeout(5, TimeUnit.SECONDS)
        .build()

    /**
     * Membuka aplikasi Google Maps langsung ke mode navigasi belok demi belok (Turn-by-Turn GPS Navigation).
     * Jika aplikasi Google Maps tidak ada, otomatis fallback ke browser web maps.
     */
    fun bukaNavigasi(context: Context, lat: Double, lng: Double, namaToko: String) {
        val navUri = Uri.parse("google.navigation:q=$lat,$lng&mode=d")
        val intent = Intent(Intent.ACTION_VIEW, navUri).apply {
            setPackage("com.google.android.apps.maps")
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }
        try {
            context.startActivity(intent)
        } catch (_: Exception) {
            val fallbackUri = Uri.parse("https://www.google.com/maps/dir/?api=1&destination=$lat,$lng")
            val fallbackIntent = Intent(Intent.ACTION_VIEW, fallbackUri).apply {
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            context.startActivity(fallbackIntent)
        }
    }

    /**
     * Mengambil koordinat polyline rute jalan nyata dari OSRM (Open Source Routing Machine).
     * Jika offline atau gagal, otomatis fallback ke garis lurus antara titik asal dan tujuan.
     */
    suspend fun ambilTitikRute(
        asalLat: Double,
        asalLng: Double,
        tujuanLat: Double,
        tujuanLng: Double
    ): List<LatLng> = withContext(Dispatchers.IO) {
        val defaultLine = listOf(LatLng(asalLat, asalLng), LatLng(tujuanLat, tujuanLng))
        try {
            val url = "https://router.project-osrm.org/route/v1/driving/$asalLng,$asalLat;$tujuanLng,$tujuanLat?overview=full&geometries=geojson"
            val request = Request.Builder().url(url).build()
            val response = httpClient.newCall(request).execute()
            if (!response.isSuccessful) return@withContext defaultLine

            val body = response.body?.string() ?: return@withContext defaultLine
            val json = JSONObject(body)
            val routes = json.optJSONArray("routes")
            if (routes == null || routes.length() == 0) return@withContext defaultLine

            val geometry = routes.getJSONObject(0).getJSONObject("geometry")
            val coordinates = geometry.getJSONArray("coordinates")

            val list = mutableListOf<LatLng>()
            for (i in 0 until coordinates.length()) {
                val point = coordinates.getJSONArray(i)
                val lng = point.getDouble(0)
                val lat = point.getDouble(1)
                list.add(LatLng(lat, lng))
            }

            if (list.isNotEmpty()) list else defaultLine
        } catch (_: Exception) {
            defaultLine
        }
    }

    fun estimasiWaktuMotor(jarakMeter: Double): String {
        // Rata-rata motor kota: ~25 km/jam (~416 meter/menit)
        val menit = (jarakMeter / 400.0).toInt().coerceAtLeast(1)
        return if (menit >= 60) "${menit / 60} jam ${menit % 60} mnt" else "$menit mnt"
    }

    fun estimasiWaktuJalan(jarakMeter: Double): String {
        // Rata-rata jalan kaki: ~4.5 km/jam (~75 meter/menit)
        val menit = (jarakMeter / 75.0).toInt().coerceAtLeast(1)
        return if (menit >= 60) "${menit / 60} jam ${menit % 60} mnt" else "$menit mnt"
    }
}
