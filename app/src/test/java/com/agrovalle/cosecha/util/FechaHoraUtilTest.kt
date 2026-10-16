package com.agrovalle.cosecha.util

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Pruebas unitarias de las marcas horarias y la deteccion de tardanza (AGV-001):
 * antes de las 06:15 es PUNTUAL; desde las 06:15 es TARDANZA.
 */
class FechaHoraUtilTest {

    @Test
    fun `convierte una hora a minutos del dia`() {
        assertEquals(0, FechaHoraUtil.enMinutos("00:00:00"))
        assertEquals(375, FechaHoraUtil.enMinutos("06:15:00"))
        assertEquals(840, FechaHoraUtil.enMinutos("14:00:00"))
        assertEquals(1439, FechaHoraUtil.enMinutos("23:59:00"))
    }

    @Test
    fun `una hora invalida no rompe el calculo`() {
        assertEquals(0, FechaHoraUtil.enMinutos(""))
        assertEquals(0, FechaHoraUtil.enMinutos("abc"))
    }

    @Test
    fun `el ingreso antes de las 6 15 es puntual`() {
        assertFalse(FechaHoraUtil.esTardanza("05:55:00", esJornadaTarde = false))
        assertFalse(FechaHoraUtil.esTardanza("06:14:00", esJornadaTarde = false))
    }

    @Test
    fun `el ingreso desde las 6 15 es tardanza`() {
        assertTrue(FechaHoraUtil.esTardanza("06:15:00", esJornadaTarde = false))
        assertTrue(FechaHoraUtil.esTardanza("06:30:00", esJornadaTarde = false))
    }

    @Test
    fun `la jornada tarde usa su propio umbral`() {
        assertFalse(FechaHoraUtil.esTardanza("14:10:00", esJornadaTarde = true))
        assertTrue(FechaHoraUtil.esTardanza("14:15:00", esJornadaTarde = true))
    }

    @Test
    fun `el rango del dia es coherente`() {
        val inicio = FechaHoraUtil.inicioDelDia()
        val fin = FechaHoraUtil.finDelDia()
        assertTrue(fin > inicio)
        assertEquals(24L * 60L * 60L * 1000L - 1L, fin - inicio)
    }
}
