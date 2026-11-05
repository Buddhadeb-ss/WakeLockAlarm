package com.antigravity.riselock.mission

import android.content.Context
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import kotlin.math.sqrt

/**
 * Robust step detector with hardware step sensor support and
 * an intelligent accelerometer peak-detection fallback.
 */
class StepDetectorHelper(
    context: Context,
    private val onStepDetected: (totalSteps: Int) -> Unit
) : SensorEventListener {

    private val sensorManager = context.getSystemService(Context.SENSOR_SERVICE) as SensorManager
    private val stepSensor: Sensor? = sensorManager.getDefaultSensor(Sensor.TYPE_STEP_DETECTOR)
    private val accelerometer: Sensor? = sensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER)

    private var currentSteps = 0
    private var isListening = false

    // Accelerometer fallback variables
    private var lastMagnitude = 9.8f
    private var isPeak = false
    private var lastStepTimestamp = 0L
    private val minStepIntervalMs = 280L // Prevent shaking fake steps; realistic human cadence
    private val stepThreshold = 11.8f   // Typical acceleration spike during a physical step

    fun startListening(initialSteps: Int = 0) {
        if (isListening) return
        currentSteps = initialSteps
        isListening = true

        if (stepSensor != null) {
            sensorManager.registerListener(this, stepSensor, SensorManager.SENSOR_DELAY_FASTEST)
        } else if (accelerometer != null) {
            sensorManager.registerListener(this, accelerometer, SensorManager.SENSOR_DELAY_UI)
        }
    }

    fun stopListening() {
        if (!isListening) return
        isListening = false
        sensorManager.unregisterListener(this)
    }

    override fun onSensorChanged(event: SensorEvent?) {
        if (event == null || !isListening) return

        if (event.sensor.type == Sensor.TYPE_STEP_DETECTOR) {
            val stepsInBatch = event.values[0].toInt()
            if (stepsInBatch > 0) {
                currentSteps += stepsInBatch
                onStepDetected(currentSteps)
            }
        } else if (event.sensor.type == Sensor.TYPE_ACCELEROMETER) {
            val x = event.values[0]
            val y = event.values[1]
            val z = event.values[2]
            val magnitude = sqrt((x * x + y * y + z * z).toDouble()).toFloat()

            val now = System.currentTimeMillis()
            if (magnitude > stepThreshold && !isPeak && (now - lastStepTimestamp > minStepIntervalMs)) {
                isPeak = true
                lastStepTimestamp = now
                currentSteps++
                onStepDetected(currentSteps)
            } else if (magnitude < 10.0f) {
                isPeak = false
            }
            lastMagnitude = magnitude
        }
    }

    override fun onAccuracyChanged(sensor: Sensor?, accuracy: Int) {
        // No-op
    }
}

