package com.carijajan.app.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.MyLocation
import androidx.compose.material.icons.filled.Place
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableDoubleStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import com.carijajan.app.BuildConfig
import com.carijajan.app.util.Lokasi
import java.util.Locale
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import org.maplibre.android.MapLibre
import org.maplibre.android.camera.CameraPosition
import org.maplibre.android.camera.CameraUpdateFactory
import org.maplibre.android.geometry.LatLng
import org.maplibre.android.maps.MapLibreMap
import org.maplibre.android.maps.MapView

@Composable
fun PilihTitikPetaDialog(
    initialLat: Double?,
    initialLng: Double?,
    onPilih: (lat: Double, lng: Double, alamat: String) -> Unit,
    onDismiss: () -> Unit
) {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current
    val scope = rememberCoroutineScope()

    val defaultLat = initialLat ?: -8.1689
    val defaultLng = initialLng ?: 113.7020

    var mapRef by remember { mutableStateOf<MapLibreMap?>(null) }
    var currentLat by remember { mutableDoubleStateOf(defaultLat) }
    var currentLng by remember { mutableDoubleStateOf(defaultLng) }
    var alamatPreview by remember { mutableStateOf("Memuat alamat lokasi...") }
    var geocodeJob by remember { mutableStateOf<Job?>(null) }

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

    // Debounced reverse-geocoding saat kamera berhenti bergeser
    fun perbaruiAlamat(lat: Double, lng: Double) {
        geocodeJob?.cancel()
        geocodeJob = scope.launch {
            delay(350)
            val nama = Lokasi.dapatkanNamaArea(context, lat, lng)
            alamatPreview = nama
        }
    }

    LaunchedEffect(Unit) {
        perbaruiAlamat(defaultLat, defaultLng)
    }

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Color.Black)
        ) {
            // ── Peta MapLibre ────────────────────────────────────────────────
            AndroidView(
                factory = { ctx ->
                    mapView.apply {
                        getMapAsync { map ->
                            mapRef = map
                            map.uiSettings.isRotateGesturesEnabled = true
                            map.uiSettings.isTiltGesturesEnabled = false
                            map.uiSettings.isCompassEnabled = false

                            map.cameraPosition = CameraPosition.Builder()
                                .target(LatLng(defaultLat, defaultLng))
                                .zoom(16.0)
                                .build()

                            map.setStyle(BuildConfig.MAP_STYLE_URL)

                            map.addOnCameraMoveListener {
                                val center = map.cameraPosition.target ?: return@addOnCameraMoveListener
                                currentLat = center.latitude
                                currentLng = center.longitude
                            }

                            map.addOnCameraIdleListener {
                                val center = map.cameraPosition.target ?: return@addOnCameraIdleListener
                                currentLat = center.latitude
                                currentLng = center.longitude
                                perbaruiAlamat(center.latitude, center.longitude)
                            }
                        }
                    }
                },
                modifier = Modifier.fillMaxSize()
            )

            // ── Pin Marker Statis Tepat di Tengah Layar ───────────────────────
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(bottom = 36.dp), // Offset agar ujung lancip pin tepat di titik tengah
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Default.Place,
                    contentDescription = "Pin Tengah Peta",
                    tint = Color(0xFFDC2626), // Merah mencolok
                    modifier = Modifier.size(46.dp)
                )
            }

            // ── Top Bar Kontrol & Batal ──────────────────────────────────────
            Surface(
                color = MaterialTheme.colorScheme.surface.copy(alpha = 0.95f),
                shadowElevation = 4.dp,
                modifier = Modifier
                    .fillMaxWidth()
                    .align(Alignment.TopCenter)
                    .statusBarsPadding()
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 14.dp, vertical = 8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    IconButton(onClick = onDismiss) {
                        Icon(imageVector = Icons.Default.Close, contentDescription = "Tutup")
                    }
                    Spacer(modifier = Modifier.width(4.dp))
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "Geser Peta ke Titik Warung",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = "Arahkan pin merah tepat ke lokasi warung",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }

            // ── Tombol GPS Saya (Floating) ───────────────────────────────────
            Surface(
                shape = CircleShape,
                color = MaterialTheme.colorScheme.surface,
                shadowElevation = 6.dp,
                modifier = Modifier
                    .align(Alignment.BottomEnd)
                    .padding(end = 16.dp, bottom = 185.dp)
                    .size(46.dp)
            ) {
                IconButton(
                    onClick = {
                        scope.launch {
                            val loc = Lokasi.ambil(context)
                            if (loc != null) {
                                val target = LatLng(loc.latitude, loc.longitude)
                                mapRef?.animateCamera(CameraUpdateFactory.newLatLngZoom(target, 16.5), 600)
                            }
                        }
                    }
                ) {
                    Icon(
                        imageVector = Icons.Default.MyLocation,
                        contentDescription = "Pusatkan ke GPS Saya",
                        tint = MaterialTheme.colorScheme.primary
                    )
                }
            }

            // ── Bottom Sheet Card: Konfirmasi Titik Terpilih ──────────────────
            Surface(
                color = MaterialTheme.colorScheme.surface,
                shape = RoundedCornerShape(topStart = 20.dp, topEnd = 20.dp),
                shadowElevation = 8.dp,
                modifier = Modifier
                    .fillMaxWidth()
                    .align(Alignment.BottomCenter)
                    .navigationBarsPadding()
            ) {
                Column(
                    modifier = Modifier.padding(18.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Row(verticalAlignment = Alignment.Top) {
                        Icon(
                            imageVector = Icons.Default.Place,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier
                                .size(24.dp)
                                .padding(top = 2.dp)
                        )
                        Spacer(modifier = Modifier.width(10.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = alamatPreview,
                                style = MaterialTheme.typography.titleSmall,
                                fontWeight = FontWeight.Bold,
                                maxLines = 2
                            )
                            Spacer(modifier = Modifier.height(2.dp))
                            Text(
                                text = "Koordinat: ${String.format(Locale.US, "%.5f, %.5f", currentLat, currentLng)}",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                fontSize = 11.sp
                            )
                        }
                    }

                    Button(
                        onClick = {
                            onPilih(currentLat, currentLng, alamatPreview)
                            onDismiss()
                        },
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Icon(imageVector = Icons.Default.Check, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Pilih Titik Ini")
                    }
                }
            }
        }
    }
}
