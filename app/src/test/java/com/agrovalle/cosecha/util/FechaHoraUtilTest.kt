package com.agrovalle.cosecha.util

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Pruebas unitarias de las marcas horarias y la deteccion de tardanza (AGV-001):
 * antes de las 06:15 es PUNTUAL; desde las 06:15 es TARDANZA.
 *
 * Tardanza = el ingreso ocurre EN o DESPUES del minuto limite (incluye el
 * segundo exacto, p. ej. 06:15:00 ya es tardanza).
 *
 * NOTA: los nombres de los metodos NO usan comillas invertidas ni espacios,
 * para que la tarea de Gradle `testDebugUnitTest` pueda ejecutarlos tambien en
 * Windows sin lanzar java.lang.ClassNotFoundException.
 */
class FechaHoraUtilTest {

    // ------------------------------------------------------------------
    // Conversion de "HH:mm:ss" a minutos del dia
    // ------------------------------------------------------------------

    @Test
    fun testConvierteHoraAMinutosDelDia() {
        assertEquals(0, FechaHoraUtil.enMinutos("00:00:00"))
        assertEquals(375, FechaHoraUtil.enMinutos("06:15:00"))
        assertEquals(840, FechaHoraUtil.enMinutos("14:00:00"))
        assertEquals(1439, FechaHoraUtil.enMinutos("23:59:00"))
    }

    @Test
    fun testHoraInvalidaNoRompeElCalculo() {
        assertEquals(0, FechaHoraUtil.enMinutos(""))
        assertEquals(0, FechaHoraUtil.enMinutos("abc"))
    }

    @Test
    fun testHoraSinSegundosTambienSeInterpreta() {
        assertEquals(390, FechaHoraUtil.enMinutos("06:30"))
    }

    // ------------------------------------------------------------------
    // Jornada manana: limite 06:15
    // ------------------------------------------------------------------

    @Test
    fun testIngresoAntesDeLas615EsPuntual() {
        assertFalse(FechaHoraUtil.esTardanza("05:55:00", esJornadaTarde = false))
        assertFalse(FechaHoraUtil.esTardanza("06:14:00", esJornadaTarde = false))
    }

    @Test
    fun testIngresoDesdeLas615EsTardanza() {
        assertTrue(FechaHoraUtil.esTardanza("06:15:00", esJornadaTarde = false))
        assertTrue(FechaHoraUtil.esTardanza("06:30:00", esJornadaTarde = false))
    }

    /** Caso borde: un segundo antes del limite sigue siendo PUNTUAL. */
    @Test
    fun testUnSegundoAntesDelLimiteEsPuntual() {
        assertFalse(FechaHoraUtil.esTardanza("06:14:59", esJornadaTarde = false))
    }

    // ------------------------------------------------------------------
    // Jornada tarde: limite 14:15
    // ------------------------------------------------------------------

    @Test
    fun testJornadaTardeUsaSuPropioUmbral() {
        assertFalse(FechaHoraUtil.esTardanza("14:10:00", esJornadaTarde = true))
        assertTrue(FechaHoraUtil.esTardanza("14:15:00", esJornadaTarde = true))
    }

    /** Una hora de manana NO debe contar como tardanza en la jornada tarde. */
    @Test
    fun testUmbralDeMananaNoAplicaALaJornadaTarde() {
        assertFalse(FechaHoraUtil.esTardanza("06:30:00", esJornadaTarde = true))
    }

    /**
     * Caso borde: extremos del dia.
     *
     * OJO: las 00:00 NO son tardanza, porque ocurren ANTES del limite de las
     * 06:15. El dia termina en tardanza porque las 23:59 si superan el limite.
     */
    @Test
    fun testExtremosDelDiaSeEvaluanContraElLimite() {
        assertFalse(FechaHoraUtil.esTardanza("00:00:00", esJornadaTarde = false))
        assertTrue(FechaHoraUtil.esTardanza("23:59:00", esJornadaTarde = false))
        assertTrue(FechaHoraUtil.esTardanza("23:59:00", esJornadaTarde = true))
    }

    // ------------------------------------------------------------------
    // Rango del dia
    // ------------------------------------------------------------------

    @Test
    fun testRangoDelDiaEsCoherente() {
        val inicio = FechaHoraUtil.inicioDelDia()
        val fin = FechaHoraUtil.finDelDia()
        assertTrue(fin > inicio)
        assertEquals(24L * 60L * 60L * 1000L - 1L, fin - inicio)
    }

    @Test
    fun testHoraActualTieneFormatoHHmmss() {
        val hora = FechaHoraUtil.ahoraHora()
        assertTrue(
            "Formato inesperado: $hora",
            Regex("""^\d{2}:\d{2}:\d{2}$""").matches(hora)
        )
    }

    @Test
    fun testFechaDeHoyTieneFormatoISO() {
        val fecha = FechaHoraUtil.hoy()
        assertTrue(
            "Formato inesperado: $fecha",
            Regex("""^\d{4}-\d{2}-\d{2}$""").matches(fecha)
        )
    }
}
