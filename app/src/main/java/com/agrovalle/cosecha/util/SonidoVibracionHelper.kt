package com.agrovalle.cosecha.util

import android.content.Context
import android.media.AudioManager
import android.media.ToneGenerator
import android.os.Build
import android.os.VibrationEffect
import android.os.Vibrator
import android.os.VibratorManager

/**
 * Helper para confirmación multisensorial de operaciones.
 * Combina retroalimentación sonora (ToneGenerator) y háptica (Vibrator)
 * para confirmar al usuario operaciones exitosas, advertencias o errores
 * en ambientes ruidosos de campo.
 */
class SonidoVibracionHelper(private val contexto: Context) {

    private val vibrador: Vibrator by lazy {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            val manager = contexto.getSystemService(Context.VIBRATOR_MANAGER_SERVICE) as VibratorManager
            manager.defaultVibrator
        } else {
            @Suppress("DEPRECATION")
            contexto.getSystemService(Context.VIBRATOR_SERVICE) as Vibrator
        }
    }

    /**
     * Confirmación de registro EXITOSO (asistencia o pesaje).
     * Tono agudo corto + vibración breve.
     */
    fun confirmarExito() {
        reproducirTono(ToneGenerator.TONE_PROP_ACK, 150)
        vibrar(longArrayOf(0, 100, 50, 100))
    }

    /**
     * Señal de ADVERTENCIA (ej: tara por defecto).
     * Doble tono medio + vibración larga.
     */
    fun alertarAdvertencia() {
        reproducirTono(ToneGenerator.TONE_PROP_PROMPT, 300)
        vibrar(longArrayOf(0, 200, 100, 200))
    }

    /**
     * Señal de ERROR (ej: jornalero no encontrado, duplicado).
     * Tono grave + vibración triple.
     */
    fun notificarError() {
        reproducirTono(ToneGenerator.TONE_PROP_NACK, 400)
        vibrar(longArrayOf(0, 150, 80, 150, 80, 150))
    }

    /**
     * Señal de ESCANEO QR detectado.
     * Tono corto tipo beep de escáner.
     */
    fun beepEscaneo() {
        reproducirTono(ToneGenerator.TONE_PROP_BEEP, 100)
        vibrar(longArrayOf(0, 50))
    }

    private fun reproducirTono(tipoTono: Int, duracionMs: Int) {
        try {
            val generador = ToneGenerator(AudioManager.STREAM_NOTIFICATION, 100)
            generador.startTone(tipoTono, duracionMs)
            android.os.Handler(contexto.mainLooper).postDelayed({
                generador.release()
            }, duracionMs.toLong() + 50)
        } catch (e: Exception) {
            // Silenciar si el hardware de audio no está disponible
            e.printStackTrace()
        }
    }

    private fun vibrar(patron: LongArray) {
        if (!vibrador.hasVibrator()) return
        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                val amplitudes = IntArray(patron.size) {
                    if (it % 2 == 0) 0 else VibrationEffect.DEFAULT_AMPLITUDE
                }
                vibrador.vibrate(VibrationEffect.createWaveform(patron, amplitudes, -1))
            } else {
                @Suppress("DEPRECATION")
                vibrador.vibrate(patron, -1)
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }
}
