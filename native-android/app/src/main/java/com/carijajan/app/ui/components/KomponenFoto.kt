package com.carijajan.app.ui.components

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.view.ViewGroup
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.camera.core.CameraSelector
import androidx.camera.core.ImageCapture
import androidx.camera.core.ImageCaptureException
import androidx.camera.core.Preview
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.view.PreviewView
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.content.ContextCompat
import androidx.lifecycle.LifecycleOwner
import com.carijajan.app.util.Gambar
import java.io.File
import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors
import kotlinx.coroutines.launch

@Composable
fun KomponenFoto(
    fotoBytes: ByteArray?,
    proses: Boolean,
    pesan: String,
    onFotoBytes: (ByteArray) -> Unit,
    onHapus: () -> Unit
) {
    var kameraTerbuka by remember { mutableStateOf(false) }
    val context = LocalContext.current
    val galleryLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.PickVisualMedia()
    ) { uri ->
        if (uri != null) {
            val bytes = Gambar.kompresUri(context, uri)
            if (bytes != null) onFotoBytes(bytes)
        }
    }
    val permissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { granted ->
        if (granted) kameraTerbuka = true
    }

    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Text("Foto warung (opsional)", style = MaterialTheme.typography.titleSmall)
        Text("Foto dikompres otomatis. Maksimal 2 MB.", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)

        val bitmap = remember(fotoBytes) {
            fotoBytes?.let { android.graphics.BitmapFactory.decodeByteArray(it, 0, it.size)?.asImageBitmap() }
        }
        if (bitmap != null) {
            Image(bitmap = bitmap, contentDescription = "Pratinjau foto warung", modifier = Modifier.fillMaxWidth().size(190.dp))
        } else {
            Box(
                modifier = Modifier.fillMaxWidth().size(150.dp).background(MaterialTheme.colorScheme.surfaceVariant),
                contentAlignment = Alignment.Center
            ) {
                Text("Belum ada foto", color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }

        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            Button(
                enabled = !proses,
                onClick = {
                    if (ContextCompat.checkSelfPermission(context, Manifest.permission.CAMERA) == PackageManager.PERMISSION_GRANTED) {
                        kameraTerbuka = true
                    } else {
                        permissionLauncher.launch(Manifest.permission.CAMERA)
                    }
                }
            ) { Text("Ambil foto") }
            OutlinedButton(
                enabled = !proses,
                onClick = { galleryLauncher.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)) }
            ) { Text("Pilih galeri") }
            if (fotoBytes != null) {
                TextButton(enabled = !proses, onClick = onHapus) { Text("Hapus") }
            }
        }

        if (proses) CircularProgressIndicator(modifier = Modifier.size(24.dp))
        if (pesan.isNotBlank()) Text(pesan, color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodySmall)
    }

    if (kameraTerbuka) {
        KameraDialog(
            onDismiss = { kameraTerbuka = false },
            onBytes = { bytes ->
                kameraTerbuka = false
                onFotoBytes(bytes)
            }
        )
    }
}

@Composable
private fun KameraDialog(
    onDismiss: () -> Unit,
    onBytes: (ByteArray) -> Unit
) {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current
    val executor = remember { Executors.newSingleThreadExecutor() }
    var imageCapture by remember { mutableStateOf<ImageCapture?>(null) }
    var error by remember { mutableStateOf("") }
    var captureLoading by remember { mutableStateOf(false) }
    var lensFacing by remember { mutableStateOf(CameraSelector.LENS_FACING_BACK) }

    DisposableEffect(Unit) {
        onDispose { executor.shutdown() }
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Ambil foto") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                CameraPreview(
                    context = context,
                    lifecycleOwner = lifecycleOwner,
                    lensFacing = lensFacing,
                    executor = executor,
                    onCaptureReady = { imageCapture = it },
                    modifier = Modifier.fillMaxWidth().size(320.dp)
                )
                if (error.isNotBlank()) Text(error, color = MaterialTheme.colorScheme.error)
                TextButton(onClick = {
                    lensFacing = if (lensFacing == CameraSelector.LENS_FACING_BACK) CameraSelector.LENS_FACING_FRONT else CameraSelector.LENS_FACING_BACK
                }) { Text("Ganti kamera") }
            }
        },
        confirmButton = {
            Button(
                enabled = !captureLoading && imageCapture != null,
                onClick = {
                    val capture = imageCapture ?: return@Button
                    captureLoading = true
                    error = ""
                    val file = File(context.cacheDir, "camera-${System.currentTimeMillis()}.jpg")
                    val options = ImageCapture.OutputFileOptions.Builder(file).build()
                    capture.takePicture(
                        options,
                        executor,
                        object : ImageCapture.OnImageSavedCallback {
                            override fun onImageSaved(output: ImageCapture.OutputFileResults) {
                                val bytes = Gambar.kompresFile(file)
                                file.delete()
                                android.os.Handler(android.os.Looper.getMainLooper()).post {
                                    captureLoading = false
                                    if (bytes == null) error = "Gagal memproses gambar."
                                    else onBytes(bytes)
                                }
                            }

                            override fun onError(exception: ImageCaptureException) {
                                android.os.Handler(android.os.Looper.getMainLooper()).post {
                                    captureLoading = false
                                    error = "Gagal mengambil foto. Coba lagi."
                                }
                            }
                        }
                    )
                }
            ) {
                if (captureLoading) CircularProgressIndicator(modifier = Modifier.size(18.dp)) else Text("Ambil")
            }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Batal") } }
    )
}

@Composable
private fun CameraPreview(
    context: Context,
    lifecycleOwner: LifecycleOwner,
    lensFacing: Int,
    executor: ExecutorService,
    onCaptureReady: (ImageCapture) -> Unit,
    modifier: Modifier = Modifier
) {
    AndroidView(
        modifier = modifier,
        factory = { ctx ->
            PreviewView(ctx).apply {
                layoutParams = ViewGroup.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT)
                scaleType = PreviewView.ScaleType.FILL_CENTER
                implementationMode = PreviewView.ImplementationMode.COMPATIBLE
            }
        },
        update = { previewView ->
            val providerFuture = ProcessCameraProvider.getInstance(context)
            providerFuture.addListener({
                val provider = providerFuture.get()
                val preview = Preview.Builder().build().also {
                    it.surfaceProvider = previewView.surfaceProvider
                }
                val capture = ImageCapture.Builder()
                    .setCaptureMode(ImageCapture.CAPTURE_MODE_MINIMIZE_LATENCY)
                    .build()
                val selector = CameraSelector.Builder().requireLensFacing(lensFacing).build()
                try {
                    provider.unbindAll()
                    provider.bindToLifecycle(lifecycleOwner, selector, preview, capture)
                    onCaptureReady(capture)
                } catch (_: Exception) {
                    onCaptureReady(capture)
                }
            }, ContextCompat.getMainExecutor(context))
        }
    )
}

