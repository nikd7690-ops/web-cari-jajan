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
}
