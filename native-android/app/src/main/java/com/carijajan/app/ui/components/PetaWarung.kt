package com.carijajan.app.ui.components

import android.view.Gravity
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import com.carijajan.app.BuildConfig
import com.carijajan.app.data.VendorDto
import com.carijajan.app.util.Format
import org.maplibre.android.MapLibre
import org.maplibre.android.annotations.IconFactory
import org.maplibre.android.annotations.Marker
import org.maplibre.android.annotations.MarkerOptions
import org.maplibre.android.camera.CameraPosition
import org.maplibre.android.camera.CameraUpdateFactory
import org.maplibre.android.geometry.LatLng
import org.maplibre.android.maps.MapLibreMap
import org.maplibre.android.maps.MapView

@Composable
fun PetaWarung(
    modifier: Modifier = Modifier,
    initialCenter: LatLng,
    initialZoom: Double,
    cameraTarget: LatLng?,
    cameraZoom: Double?,
    cameraMoveTrigger: Int = 0,
    bearingResetTrigger: Int = 0,
    userLocation: LatLng?,
    userHeading: Float = 0f,
    vendors: List<VendorDto>,
    selectedId: String?,
    onSelectVendor: (String) -> Unit
) {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current
    val mapView = remember {
        MapLibre.getInstance(context)
        MapView(context).apply { onCreate(null) }
    }

    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            when (event) {
                Lifecycle.Event.ON_START -> mapView.onStart()
                Lifecycle.Event.ON_RESUME -> mapView.onResume()
                Lifecycle.Event.ON_PAUSE -> mapView.onPause()
                Lifecycle.Event.ON_STOP -> mapView.onStop()
                Lifecycle.Event.ON_DESTROY -> mapView.onDestroy()
                else -> Unit
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose {
            lifecycleOwner.lifecycle.removeObserver(observer)
        }
    }

    var mapRef by remember { mutableStateOf<MapLibreMap?>(null) }
    var styleSiap by remember { mutableStateOf(false) }

    // Referensi marker lokal agar pembaruan tidak saling menghapus
    var userMarkerRef by remember { mutableStateOf<Marker?>(null) }
    val vendorMarkers = remember { mutableListOf<Marker>() }

    Box(modifier = modifier) {
        AndroidView(
            factory = { ctx ->
                mapView.apply {
                    getMapAsync { map ->
                        mapRef = map

                        // ── Aktifkan Gestur Penuh: Geser, Zoom, Rotasi, Tilt ───
                        map.uiSettings.isScrollGesturesEnabled = true   // Geser bebas (pan)
                        map.uiSettings.isZoomGesturesEnabled = true     // Zoom (cubit / pinch)
                        map.uiSettings.isRotateGesturesEnabled = true   // Rotasi 2 jari bebas
                        map.uiSettings.isTiltGesturesEnabled = true     // Kemiringan 2 jari (pitch)
                        map.uiSettings.isDoubleTapGesturesEnabled = true
                        map.uiSettings.isQuickZoomGesturesEnabled = true

                        // Atur posisi awal sekali saja saat peta pertama kali diinisialisasi
                        map.cameraPosition = CameraPosition.Builder()
                            .target(initialCenter)
                            .zoom(initialZoom)
                            .build()

                        // Kompas bawaan MapLibre (sebelah kanan bawah)
                        val density = ctx.resources.displayMetrics.density
                        map.uiSettings.isCompassEnabled = true
                        map.uiSettings.compassGravity = Gravity.BOTTOM or Gravity.END
                        map.uiSettings.setCompassMargins(0, 0, (16 * density).toInt(), (110 * density).toInt())

                        map.setStyle(BuildConfig.MAP_STYLE_URL) { _ ->
                            styleSiap = true
                        }

                        map.setOnMarkerClickListener { marker ->
                            // Abaikan klik pada titik lokasi pengguna sendiri
                            if (marker == userMarkerRef) return@setOnMarkerClickListener true

                            val found = vendors.firstOrNull { it.namaToko == marker.title }
                            if (found != null) {
                                onSelectVendor(found.id)
                                true
                            } else {
                                false
                            }
                        }
                    }
                }
            },
            modifier = Modifier.fillMaxSize()
            // JANGAN gunakan lambda update di sini agar kamera tidak terus-menerus direset saat recompose!
        )

        // Atribusi OpenStreetMap dan OpenFreeMap (wajib ada dan tidak boleh ditutupi)
        Text(
            text = "© kontributor OpenStreetMap · OpenFreeMap",
            color = Color(0xFF3F3F46),
            fontSize = 9.sp,
            modifier = Modifier
                .align(Alignment.BottomStart)
                .padding(start = 10.dp, bottom = 90.dp)
                .background(Color.White.copy(alpha = 0.85f), RoundedCornerShape(4.dp))
                .padding(horizontal = 5.dp, vertical = 2.dp)
        )
    }

    // ── Update Marker Warung saat Data Berubah (Tanpa Mengganggu Posisi User) ─
    LaunchedEffect(styleSiap, vendors) {
        val map = mapRef ?: return@LaunchedEffect
        if (!styleSiap) return@LaunchedEffect

        // Hapus marker warung yang lama
        vendorMarkers.forEach { map.removeMarker(it) }
        vendorMarkers.clear()

        val iconFactory = IconFactory.getInstance(context)

        // Pasang marker warung baru
        vendors.forEach { vendor ->
            if (vendor.lat == 0.0 && vendor.lng == 0.0) return@forEach

            val pinBitmap = PinGenerator.dapatkanPin(
                context = context,
                namaToko = vendor.namaToko,
                kategori = vendor.kategori,
                buka = vendor.buka
            )
            val customIcon = iconFactory.fromBitmap(pinBitmap)

            val m = map.addMarker(
                MarkerOptions()
                    .position(LatLng(vendor.lat, vendor.lng))
                    .icon(customIcon)
                    .title(vendor.namaToko)
                    .snippet(Format.labelKategori(vendor.kategori))
            )
            vendorMarkers.add(m)
        }
    }

    // ── Update Posisi & Arah Panah Pengguna Real-Time (Tanpa Mereset Kamera) ──
    LaunchedEffect(styleSiap, userLocation, userHeading) {
        val map = mapRef ?: return@LaunchedEffect
        if (!styleSiap || userLocation == null) return@LaunchedEffect

        val iconFactory = IconFactory.getInstance(context)
        val puckBitmap = PinGenerator.dapatkanPuckLokasi(context, userHeading)
        val customPuck = iconFactory.fromBitmap(puckBitmap)

        val existing = userMarkerRef
        if (existing != null) {
            existing.position = userLocation
            existing.icon = customPuck
        } else {
            userMarkerRef = map.addMarker(
                MarkerOptions()
                    .position(userLocation)
                    .icon(customPuck)
                    .title("Lokasi saya")
            )
        }
    }

    // ── Animasi Kamera HANYA Berjalan saat Ada Pemicu Tindakan Eksplisit ──────
    LaunchedEffect(cameraMoveTrigger) {
        if (cameraMoveTrigger > 0 && cameraTarget != null) {
            val map = mapRef ?: return@LaunchedEffect
            val targetZoom = cameraZoom ?: map.cameraPosition.zoom
            map.animateCamera(CameraUpdateFactory.newLatLngZoom(cameraTarget, targetZoom), 600)
        }
    }

    // ── Animasi Kompas Reset ke Utara ────────────────────────────────────────
    LaunchedEffect(bearingResetTrigger) {
        if (bearingResetTrigger > 0) {
            val map = mapRef ?: return@LaunchedEffect
            map.animateCamera(CameraUpdateFactory.bearingTo(0.0), 500)
        }
    }
}
