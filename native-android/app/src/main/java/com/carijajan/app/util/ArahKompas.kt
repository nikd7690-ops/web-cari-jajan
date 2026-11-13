package com.carijajan.app.util

import android.content.Context
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlin.math.abs

object ArahKompas {
    /**
     * Memantau arah hadap (azimuth/heading) perangkat secara real-time dalam derajat (0°..360°).
     * Dilengkapi filter threshold agar bebas getaran/jitter sensor halus sehingga rendering mulus.
     */
    fun pantauArah(context: Context): Flow<Float> = callbackFlow {
        val sensorManager = context.getSystemService(Context.SENSOR_SERVICE) as? SensorManager
        if (sensorManager == null) {
            close()
            return@callbackFlow
        }

        val rotationSensor = sensorManager.getDefaultSensor(Sensor.TYPE_ROTATION_VECTOR)
        val accelSensor = sensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER)
        val magnetSensor = sensorManager.getDefaultSensor(Sensor.TYPE_MAGNETIC_FIELD)

        val rotationMatrix = FloatArray(9)
        val orientationAngles = FloatArray(3)
        val lastAccelerometer = FloatArray(3)
        val lastMagnetometer = FloatArray(3)
        var lastAccelSet = false
        var lastMagnetSet = false
        var lastEmittedHeading = -999f

        fun emitIfChanged(degrees: Float) {
            val normalized = (degrees % 360f + 360f) % 360f
            val diff = abs(normalized - lastEmittedHeading)
            // Hanya kirim jika perubahan sudut >= 2.5° agar tidak membebani UI thread saat diam
            if (lastEmittedHeading < 0 || (diff >= 2.5f && (360f - diff) >= 2.5f)) {
                lastEmittedHeading = normalized
                trySend(normalized)
            }
        }

        val listener = object : SensorEventListener {
            override fun onSensorChanged(event: SensorEvent) {
                if (event.sensor.type == Sensor.TYPE_ROTATION_VECTOR) {
                    SensorManager.getRotationMatrixFromVector(rotationMatrix, event.values)
                    SensorManager.getOrientation(rotationMatrix, orientationAngles)
                    val degrees = (Math.toDegrees(orientationAngles[0].toDouble()) + 360) % 360
                    emitIfChanged(degrees.toFloat())
                } else if (event.sensor.type == Sensor.TYPE_ACCELEROMETER) {
                    System.arraycopy(event.values, 0, lastAccelerometer, 0, event.values.size)
                    lastAccelSet = true
                } else if (event.sensor.type == Sensor.TYPE_MAGNETIC_FIELD) {
                    System.arraycopy(event.values, 0, lastMagnetometer, 0, event.values.size)
                    lastMagnetSet = true
                }

                if (event.sensor.type != Sensor.TYPE_ROTATION_VECTOR && lastAccelSet && lastMagnetSet) {
                    if (SensorManager.getRotationMatrix(rotationMatrix, null, lastAccelerometer, lastMagnetometer)) {
                        SensorManager.getOrientation(rotationMatrix, orientationAngles)
                        val degrees = (Math.toDegrees(orientationAngles[0].toDouble()) + 360) % 360
                        emitIfChanged(degrees.toFloat())
                    }
                }
            }

            override fun onAccuracyChanged(sensor: Sensor?, accuracy: Int) {}
        }

        if (rotationSensor != null) {
            sensorManager.registerListener(listener, rotationSensor, SensorManager.SENSOR_DELAY_UI)
        } else {
            accelSensor?.let { sensorManager.registerListener(listener, it, SensorManager.SENSOR_DELAY_UI) }
            magnetSensor?.let { sensorManager.registerListener(listener, it, SensorManager.SENSOR_DELAY_UI) }
        }

        awaitClose {
            sensorManager.unregisterListener(listener)
        }
    }
}
