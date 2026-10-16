package com.agrovalle.cosecha.util

import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

/**
 * Utilidades de fecha y hora para marcas horarias exactas y deteccion de tardanza.
 *
 * Se usa SimpleDateFormat / Calendar (disponibles desde API 24) para no depender
 * de java.time, que requiere API 26 o desugaring.
 */
object FechaHoraUtil {

    private const val FORMATO_FECHA = "yyyy-MM-dd"
    private const val FORMATO_HORA = "HH:mm:ss"

    /** Hora de inicio de la jornada manana y minuto a partir del cual es tardanza. */
    private const val MIN_INICIO_MANANA = 6 * 60          // 06:00
    private const val MIN_TARDANZA_MANANA = 6 * 60 + 15   // 06:15

    /** Hora de inicio de la jornada tarde y su minuto de tardanza. */
    private const val MIN_INICIO_TARDE = 14 * 60          // 14:00
    private const val MIN_TARDANZA_TARDE = 14 * 60 + 15   // 14:15

    fun hoy(): String = SimpleDateFormat(FORMATO_FECHA, Locale.US).format(Date())

    fun ahoraHora(): String = SimpleDateFormat(FORMATO_HORA, Locale.US).format(Date())

    fun timestampActual(): Long = System.currentTimeMillis()

    fun inicioDelDia(millis: Long = System.currentTimeMillis()): Long {
        val cal = Calendar.getInstance()
        cal.timeInMillis = millis
        cal.set(Calendar.HOUR_OF_DAY, 0)
        cal.set(Calendar.MINUTE, 0)
        cal.set(Calendar.SECOND, 0)
        cal.set(Calendar.MILLISECOND, 0)
        return cal.timeInMillis
    }

    fun finDelDia(millis: Long = System.currentTimeMillis()): Long =
        inicioDelDia(millis) + 24L * 60L * 60L * 1000L - 1L

    /** Convierte "HH:mm:ss" a minutos transcurridos del dia. */
    fun enMinutos(hora: String): Int {
        val partes = hora.split(":")
        if (partes.size < 2) return 0
        val hh = partes[0].toIntOrNull() ?: return 0
        val mm = partes[1].toIntOrNull() ?: return 0
        return hh * 60 + mm
    }

    /**
     * Regla de AGV-001: antes del inicio de jornada (+15 min de gracia) es PUNTUAL;
     * a partir de ahi se registra como TARDANZA con la hora real.
     */
    fun esTardanza(horaIngreso: String, esJornadaTarde: Boolean): Boolean {
        val limite = if (esJornadaTarde) MIN_TARDANZA_TARDE else MIN_TARDANZA_MANANA
        return enMinutos(horaIngreso) >= limite
    }

    /** Etiqueta legible de la hora de inicio del turno. */
    fun horaInicioJornada(esJornadaTarde: Boolean): Int =
        if (esJornadaTarde) MIN_INICIO_TARDE else MIN_INICIO_MANANA
}
