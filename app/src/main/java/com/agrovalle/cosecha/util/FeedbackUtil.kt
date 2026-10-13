package com.agrovalle.cosecha.util

import android.content.Context
import android.media.AudioManager
import android.media.ToneGenerator
import android.os.Build
import android.os.VibrationEffect
import android.os.Vibrator

object FeedbackUtil {

    /**
     * Emite una vibración háptica corta de éxito (150ms).
     */
    @Suppress("DEPRECATION")
    fun vibrarExito(context: Context) {
        try {
            val vibrator = context.getSystemService(Context.VIBRATOR_SERVICE) as? Vibrator
            vibrator?.let {
                if (it.hasVibrator()) {
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                        it.vibrate(
                            VibrationEffect.createOneShot(
                                150,
                                VibrationEffect.DEFAULT_AMPLITUDE
                            )
                        )
                    } else {
                        it.vibrate(150)
                    }
                }
            }
        } catch (_: Exception) {
            // Manejo silencioso en dispositivos sin hardware vibrador
        }
    }

    /**
     * Emite un patrón de vibración doble para advertencias o tara por defecto.
     */
    @Suppress("DEPRECATION")
    fun vibrarAlerta(context: Context) {
        try {
            val vibrator = context.getSystemService(Context.VIBRATOR_SERVICE) as? Vibrator
            vibrator?.let {
                if (it.hasVibrator()) {
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                        val pattern = longArrayOf(0, 100, 80, 100)
                        it.vibrate(VibrationEffect.createWaveform(pattern, -1))
                    } else {
                        @Suppress("DEPRECATION")
                        it.vibrate(longArrayOf(0, 100, 80, 100), -1)
                    }
                }
            }
        } catch (_: Exception) {
        }
    }

    /**
     * Reproduce un tono acústico audible tipo confirmación de pesaje.
     */
    fun reproducirBeepExito() {
        try {
            val toneGen = ToneGenerator(AudioManager.STREAM_NOTIFICATION, 100)
            toneGen.startTone(ToneGenerator.TONE_PROP_ACK, 200)
            toneGen.release()
        } catch (_: Exception) {
        }
    }

    /**
     * Reproduce un tono acústico de advertencia.
     */
    fun reproducirBeepAlerta() {
        try {
            val toneGen = ToneGenerator(AudioManager.STREAM_NOTIFICATION, 90)
            toneGen.startTone(ToneGenerator.TONE_PROP_NACK, 250)
            toneGen.release()
        } catch (_: Exception) {
        }
    }

    /**
     * Ejecuta feedback sensorial combinado (sonido + vibración).
     */
    fun ejecutarFeedbackConfirmacion(context: Context, esAlerta: Boolean = false) {
        if (esAlerta) {
            vibrarAlerta(context)
            reproducirBeepAlerta()
        } else {
            vibrarExito(context)
            reproducirBeepExito()
        }
    }
}
