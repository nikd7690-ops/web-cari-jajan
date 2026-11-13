package com.carijajan.app.ui.components

import android.view.Gravity
import androidx.collection.LruCache
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
import androidx.compose.runtime.rememberUpdatedState
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
import kotlin.math.roundToInt
import org.maplibre.android.MapLibre
import org.maplibre.android.annotations.Icon
import org.maplibre.android.annotations.IconFactory
import org.maplibre.android.annotations.Marker
import org.maplibre.android.annotations.MarkerOptions
import org.maplibre.android.annotations.Polyline
import org.maplibre.android.annotations.PolylineOptions
import org.maplibre.android.camera.CameraPosition
import org.maplibre.android.camera.CameraUpdateFactory
import org.maplibre.android.geometry.LatLng
import org.maplibre.android.geometry.LatLngBounds
import org.maplibre.android.maps.MapLibreMap
import org.maplibre.android.maps.MapView

private class UserPuckTracker(
    var heading: Int = -1,
    var position: LatLng? = null
)

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
    routePoints: List<LatLng>? = null,
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

    val currentLoc by rememberUpdatedState(userLocation)
    val currentHeading by rememberUpdatedState(userHeading)
    val currentVendors by rememberUpdatedState(vendors)
    val currentOnSelectVendor by rememberUpdatedState(onSelectVendor)

    var mapRef by remember { mutableStateOf<MapLibreMap?>(null) }
    var styleSiap by remember { mutableStateOf(false) }

    // Referensi marker lokal
    var userMarkerRef by remember { mutableStateOf<Marker?>(null) }
    val vendorMarkers = remember { mutableListOf<Marker>() }
    val markerIdToVendorId = remember { mutableMapOf<Long, String>() }
    var polylineRef by remember { mutableStateOf<Polyline?>(null) }

    // Cache icon puck untuk 120 sudut unik (tiap 3 derajat) untuk performa 60-120 FPS
    val puckIconCache = remember { LruCache<Int, Icon>(120) }
    val puckTracker = remember { UserPuckTracker() }

    val updatePuck = {
        val map = mapRef
        if (map != null && styleSiap) {
            val loc = currentLoc
            if (loc == null) {
                userMarkerRef?.let { map.removeMarker(it) }
                userMarkerRef = null
                puckTracker.position = null
                puckTracker.heading = -1
            } else {
                // Rotasi panah heading relatif terhadap rotasi kamera peta
                // Ketika peta diputar bearing θ, panah di layar menyesuaikan (userHeading - bearing)
                val mapBearing = map.cameraPosition.bearing.toFloat()
                val relativeAngle = ((currentHeading - mapBearing) % 360f + 360f) % 360f
                val quantized = (((relativeAngle / 3f).roundToInt() * 3) % 360 + 360) % 360

                val posChanged = puckTracker.position != loc
                val headingChanged = puckTracker.heading != quantized

                if (posChanged || headingChanged || userMarkerRef == null) {
                    var icon = puckIconCache.get(quantized)
                    if (icon == null) {
                        val puckBitmap = PinGenerator.dapatkanPuckLokasi(context, quantized.toFloat())
                        icon = IconFactory.recreate("user_puck_$quantized", puckBitmap)
                        puckIconCache.put(quantized, icon)
                    }

                    val existing = userMarkerRef
                    if (existing != null) {
                        if (posChanged) existing.position = loc
                        if (headingChanged) existing.icon = icon
                    } else {
                        userMarkerRef = map.addMarker(
                            MarkerOptions()
                                .position(loc)
                                .icon(icon)
                                .title("Lokasi saya")
                        )
                    }

                    puckTracker.position = loc
                    puckTracker.heading = quantized
                }
            }
        }
    }

    val updatePuckAction = rememberUpdatedState(updatePuck)

    Box(modifier = modifier) {
        AndroidView(
            factory = { ctx ->
                mapView.apply {
                    getMapAsync { map ->
                        mapRef = map

                        // ── Gestur Penuh: Bebas Geser, Zoom, Rotasi, Tilt ─────
                        map.uiSettings.isScrollGesturesEnabled = true
                        map.uiSettings.isZoomGesturesEnabled = true
                        map.uiSettings.isRotateGesturesEnabled = true
                        map.uiSettings.isTiltGesturesEnabled = true
                        map.uiSettings.isDoubleTapGesturesEnabled = true
                        map.uiSettings.isQuickZoomGesturesEnabled = true

                        // Batas zoom optimal: mencegah GPU lag/overdraw saat zoom dekat ekstrem
                        map.setMinZoomPreference(3.5)
                        map.setMaxZoomPreference(18.5)

                        map.cameraPosition = CameraPosition.Builder()
                            .target(initialCenter)
                            .zoom(initialZoom)
                            .build()

                        val density = ctx.resources.displayMetrics.density
                        // Nonaktifkan kompas kecil bawaan MapLibre agar digantikan oleh FAB Kompas navigasi utama
                        map.uiSettings.isCompassEnabled = false

                        // Putar arah panah lokasi seketika saat peta diputar oleh dua jari pengguna
                        map.addOnCameraMoveListener {
                            updatePuckAction.value.invoke()
                        }

                        map.setStyle(BuildConfig.MAP_STYLE_URL) { _ ->
                            styleSiap = true
                        }

                        map.setOnMarkerClickListener { marker ->
                            if (marker == userMarkerRef) return@setOnMarkerClickListener true

                            val activeList = currentVendors
                            val foundId = markerIdToVendorId[marker.id]
                                ?: activeList.firstOrNull { it.namaToko == marker.title }?.id
                                ?: activeList.firstOrNull {
                                    kotlin.math.abs(it.lat - marker.position.latitude) < 0.0001 &&
                                    kotlin.math.abs(it.lng - marker.position.longitude) < 0.0001
                                }?.id

                            if (foundId != null) {
                                currentOnSelectVendor(foundId)
                                true
                            } else {
                                false
                            }
                        }
                    }
                }
            },
            modifier = Modifier.fillMaxSize()
        )

        // Atribusi OpenStreetMap dan OpenFreeMap (wajib ada)
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

    // ── Update Marker Warung ────────────────────────────────────────────────
    LaunchedEffect(styleSiap, vendors) {
        val map = mapRef ?: return@LaunchedEffect
        if (!styleSiap) return@LaunchedEffect

        vendorMarkers.forEach { map.removeMarker(it) }
        vendorMarkers.clear()
        markerIdToVendorId.clear()

        val iconFactory = IconFactory.getInstance(context)

        vendors.forEach { vendor ->
            if (vendor.lat == 0.0 && vendor.lng == 0.0) return@forEach

            val diskon = if (vendor.zeroWaste?.aktif == true) (vendor.zeroWaste.diskonPersen.takeIf { it > 0 } ?: 30) else 0
            val pinBitmap = PinGenerator.dapatkanPin(
                context = context,
                namaToko = vendor.namaToko,
                kategori = vendor.kategori,
                buka = vendor.buka,
                diskonPersen = diskon
            )
            val customIcon = iconFactory.fromBitmap(pinBitmap)

            val m = map.addMarker(
                MarkerOptions()
                    .position(LatLng(vendor.lat, vendor.lng))
                    .icon(customIcon)
                    .title(vendor.namaToko)
                    .snippet(Format.labelKategori(vendor.kategori))
            )
            markerIdToVendorId[m.id] = vendor.id
            vendorMarkers.add(m)
        }

        // Pastikan listener klik marker terpasang dengan referensi terkini
        map.setOnMarkerClickListener { marker ->
            if (marker == userMarkerRef) return@setOnMarkerClickListener true

            val activeList = currentVendors
            val foundId = markerIdToVendorId[marker.id]
                ?: activeList.firstOrNull { it.namaToko == marker.title }?.id
                ?: activeList.firstOrNull {
                    kotlin.math.abs(it.lat - marker.position.latitude) < 0.0001 &&
                    kotlin.math.abs(it.lng - marker.position.longitude) < 0.0001
                }?.id

            if (foundId != null) {
                currentOnSelectVendor(foundId)
                true
            } else {
                false
            }
        }
    }

    // ── Update Posisi & Arah Panah Pengguna Real-Time ────────────────────────
    LaunchedEffect(styleSiap, userLocation, userHeading) {
        updatePuck()
    }

    // ── Menggambar Garis Rute Navigasi (Polyline) ───────────────────────────
    LaunchedEffect(styleSiap, routePoints) {
        val map = mapRef ?: return@LaunchedEffect
        if (!styleSiap) return@LaunchedEffect

        polylineRef?.let { map.removePolyline(it) }
        polylineRef = null

        if (!routePoints.isNullOrEmpty() && routePoints.size >= 2) {
            val opts = PolylineOptions()
                .color(android.graphics.Color.parseColor("#059669"))
                .width(5.5f)
            routePoints.forEach { opts.add(it) }
            polylineRef = map.addPolyline(opts)

            // Zoom otomatis memuat seluruh rute perjalanan
            try {
                val builder = LatLngBounds.Builder()
                routePoints.forEach { builder.include(it) }
                val bounds = builder.build()
                val density = context.resources.displayMetrics.density
                val padding = (70 * density).toInt()
                map.animateCamera(CameraUpdateFactory.newLatLngBounds(bounds, padding), 600)
            } catch (_: Exception) {}
        }
    }

    // ── Kontrol Kamera ──────────────────────────────────────────────────────
    LaunchedEffect(cameraMoveTrigger) {
        if (cameraMoveTrigger > 0 && cameraTarget != null) {
            val map = mapRef ?: return@LaunchedEffect
            val targetZoom = cameraZoom ?: map.cameraPosition.zoom
            map.animateCamera(CameraUpdateFactory.newLatLngZoom(cameraTarget, targetZoom), 600)
        }
    }

    // ── Kompas Reset ke Utara & Pusatkan Langsung ke Titik Pengguna ──────────
    LaunchedEffect(bearingResetTrigger) {
        if (bearingResetTrigger > 0) {
            val map = mapRef ?: return@LaunchedEffect
            val target = userLocation ?: cameraTarget ?: map.cameraPosition.target
            val targetZoom = if (userLocation != null) 16.5.coerceAtLeast(map.cameraPosition.zoom) else map.cameraPosition.zoom
            val pos = CameraPosition.Builder()
                .target(target)
                .bearing(0.0)
                .tilt(0.0)
                .zoom(targetZoom)
                .build()
            map.animateCamera(CameraUpdateFactory.newCameraPosition(pos), 650)
        }
    }
}
