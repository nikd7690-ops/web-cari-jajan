package com.carijajan.app.util

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.location.Location
import android.location.LocationListener
import android.location.LocationManager
import android.os.Handler
import android.os.Looper
import androidx.core.content.ContextCompat
import java.util.concurrent.atomic.AtomicBoolean
import kotlin.coroutines.resume
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.withContext

object Lokasi {
    fun punyaIzin(context: Context): Boolean {
        val fine = ContextCompat.checkSelfPermission(
            context,
            Manifest.permission.ACCESS_FINE_LOCATION
        ) == PackageManager.PERMISSION_GRANTED
        val coarse = ContextCompat.checkSelfPermission(
            context,
            Manifest.permission.ACCESS_COARSE_LOCATION
        ) == PackageManager.PERMISSION_GRANTED
        return fine || coarse
    }

    /**
     * Mengambil lokasi sekali (one-shot).
     */
    suspend fun ambil(context: Context): Location? = withContext(Dispatchers.IO) {
        if (!punyaIzin(context)) return@withContext null

        val manager = context.getSystemService(Context.LOCATION_SERVICE) as? LocationManager
            ?: return@withContext null

        val providers = listOf(
            LocationManager.GPS_PROVIDER,
            LocationManager.NETWORK_PROVIDER
        ).filter { runCatching { manager.isProviderEnabled(it) }.getOrDefault(false) }

        if (providers.isEmpty()) return@withContext null

        var best: Location? = null
        for (provider in providers) {
            val last = runCatching { manager.getLastKnownLocation(provider) }.getOrNull()
            if (last != null && (best == null || last.time > best!!.time)) {
                best = last
            }
        }
        if (best != null) return@withContext best

        val provider = providers.first()
        suspendCancellableCoroutine { continuation ->
            val done = AtomicBoolean(false)
            val handler = Handler(Looper.getMainLooper())

            val listener = object : LocationListener {
                override fun onLocationChanged(location: Location) {
                    if (done.compareAndSet(false, true)) {
                        manager.removeUpdates(this)
                        handler.removeCallbacksAndMessages(null)
                        if (continuation.isActive) continuation.resume(location)
                    }
                }

                override fun onProviderEnabled(provider: String) {}
                override fun onProviderDisabled(provider: String) {}
            }

            try {
                @Suppress("MissingPermission")
                manager.requestLocationUpdates(
                    provider,
                    1000L,
                    0f,
                    listener,
                    Looper.getMainLooper()
                )
            } catch (_: Exception) {
                if (continuation.isActive) continuation.resume(null)
                return@suspendCancellableCoroutine
            }

            handler.postDelayed({
                if (done.compareAndSet(false, true)) {
                    manager.removeUpdates(listener)
                    if (continuation.isActive) continuation.resume(null)
                }
            }, 12000L)

            continuation.invokeOnCancellation {
                manager.removeUpdates(listener)
                handler.removeCallbacksAndMessages(null)
            }
        }
    }

    /**
     * Memantau lokasi pengguna secara REAL-TIME.
     * Mengirimkan koordinat setiap kali perangkat berpindah (interval 1000ms / 1 meter).
     */
    fun pantauLokasi(context: Context): Flow<Location> = callbackFlow {
        if (!punyaIzin(context)) {
            close()
            return@callbackFlow
        }
        val manager = context.getSystemService(Context.LOCATION_SERVICE) as? LocationManager
        if (manager == null) {
            close()
            return@callbackFlow
        }

        // Kirim lokasi terakhir yang diketahui segera agar tidak menunggu GPS lock pertama
        val lastGps = runCatching { manager.getLastKnownLocation(LocationManager.GPS_PROVIDER) }.getOrNull()
        val lastNet = runCatching { manager.getLastKnownLocation(LocationManager.NETWORK_PROVIDER) }.getOrNull()
        val best = if (lastGps != null && (lastNet == null || lastGps.time > lastNet.time)) lastGps else lastNet
        if (best != null) {
            trySend(best)
        }

        val listener = object : LocationListener {
            override fun onLocationChanged(location: Location) {
                trySend(location)
            }

            override fun onProviderEnabled(provider: String) {}
            override fun onProviderDisabled(provider: String) {}
        }

        try {
            if (manager.isProviderEnabled(LocationManager.GPS_PROVIDER)) {
                manager.requestLocationUpdates(
                    LocationManager.GPS_PROVIDER,
                    1000L,
                    1f,
                    listener,
                    Looper.getMainLooper()
                )
            }
            if (manager.isProviderEnabled(LocationManager.NETWORK_PROVIDER)) {
                manager.requestLocationUpdates(
                    LocationManager.NETWORK_PROVIDER,
                    1000L,
                    1f,
                    listener,
                    Looper.getMainLooper()
                )
            }
        } catch (_: SecurityException) {
            close()
            return@callbackFlow
        }

        awaitClose {
            manager.removeUpdates(listener)
        }
    }
}
