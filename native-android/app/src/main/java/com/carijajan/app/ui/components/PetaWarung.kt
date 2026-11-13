package com.carijajan.app.ui.components

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
import org.maplibre.android.annotations.MarkerOptions
import org.maplibre.android.camera.CameraPosition
import org.maplibre.android.camera.CameraUpdateFactory
import org.maplibre.android.geometry.LatLng
import org.maplibre.android.maps.MapLibreMap
import org.maplibre.android.maps.MapView
import org.maplibre.android.maps.Style

@Composable
fun PetaWarung(
    modifier: Modifier = Modifier,
    center: LatLng,
    zoom: Double,
    userLocation: LatLng?,
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

    Box(modifier = modifier) {
        AndroidView(
            factory = { mapView },
            modifier = Modifier.fillMaxSize()
        ) { view ->
            view.getMapAsync { map ->
                mapRef = map
                map.cameraPosition = CameraPosition.Builder().target(center).zoom(zoom).build()
                map.setStyle(BuildConfig.MAP_STYLE_URL) { style ->
                    styleSiap = true
                    tampilkanMarker(map, vendors, userLocation, selectedId)
                }
                map.setOnMarkerClickListener { marker ->
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

        Text(
            text = "© kontributor OpenStreetMap · OpenFreeMap",
            color = Color(0xFF3F3F46),
            fontSize = 9.sp,
            modifier = Modifier
                .align(Alignment.BottomStart)
                .padding(start = 6.dp, bottom = 4.dp)
                .background(Color.White.copy(alpha = 0.75f), RoundedCornerShape(4.dp))
                .padding(horizontal = 4.dp, vertical = 1.dp)
        )
    }

    LaunchedEffect(styleSiap, vendors, userLocation, selectedId, center, zoom) {
        val map = mapRef ?: return@LaunchedEffect
        if (!styleSiap) return@LaunchedEffect
        tampilkanMarker(map, vendors, userLocation, selectedId)
    }

    LaunchedEffect(center, zoom) {
        val map = mapRef ?: return@LaunchedEffect
        if (!styleSiap) return@LaunchedEffect
        map.animateCamera(CameraUpdateFactory.newLatLngZoom(center, zoom), 600)
    }
}

private fun tampilkanMarker(
    map: MapLibreMap,
    vendors: List<VendorDto>,
    userLocation: LatLng?,
    selectedId: String?
) {
    map.clear()
    userLocation?.let { lokasi ->
        map.addMarker(
            MarkerOptions()
                .position(lokasi)
                .title("Lokasi saya")
        )
    }
    vendors.forEach { vendor ->
        if (vendor.lat == 0.0 && vendor.lng == 0.0) return@forEach
        map.addMarker(
            MarkerOptions()
                .position(LatLng(vendor.lat, vendor.lng))
                .title(vendor.namaToko)
                .snippet(Format.labelKategori(vendor.kategori))
        )
    }
}
