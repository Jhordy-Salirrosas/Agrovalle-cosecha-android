package com.agrovalle.cosecha.data.repository

import com.agrovalle.cosecha.data.DatosMemoria
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

class SelectorJornalerosAsistenciaTest {

    @Before
    fun setUp() {
        DatosMemoria.reiniciarTurno()
    }

    @Test
    fun `sin asistencias hoy la lista esta vacia y bloquea el registro`() {
        val disponibles = SelectorJornalerosAsistencia.cargarParaFecha("2026-11-16")

        assertTrue(disponibles.isEmpty())
        assertFalse(SelectorJornalerosAsistencia.puedeRegistrarPesaje(disponibles, null))
        assertFalse(SelectorJornalerosAsistencia.puedeRegistrarPesaje(disponibles, "Juan Perez Torres"))
    }

    @Test
    fun `solo lista jornaleros con asistencia registrada hoy y excluye otras fechas`() {
        val jHoy = DatosMemoria.listaJornaleros[0]
        val jAyer = DatosMemoria.listaJornaleros[1]
        DatosMemoria.registrarAsistencia(jHoy.id, "2026-11-16", "06:05:00", "PUNTUAL", "QR_SCAN")
        DatosMemoria.asignarCuartel(jHoy.id, "Cuartel 01")
        DatosMemoria.registrarAsistencia(jAyer.id, "2026-11-15", "06:05:00", "PUNTUAL", "QR_SCAN")

        val disponibles = SelectorJornalerosAsistencia.cargarParaFecha("2026-11-16")

        assertEquals(1, disponibles.size)
        assertEquals(jHoy.nombreCompleto, disponibles.first().nombreCompleto)
        assertEquals("Cuartel 01", disponibles.first().cuartelActual)
    }

    @Test
    fun `no permite registrar sin elegir jornalero de la lista ni con nombre ajeno`() {
        val jHoy = DatosMemoria.listaJornaleros[0]
        val jNoAsistio = DatosMemoria.listaJornaleros[2]
        DatosMemoria.registrarAsistencia(jHoy.id, "2026-11-16", "06:10:00", "PUNTUAL", "QR_SCAN")

        val disponibles = SelectorJornalerosAsistencia.cargarParaFecha("2026-11-16")

        assertFalse(SelectorJornalerosAsistencia.puedeRegistrarPesaje(disponibles, null))
        assertFalse(SelectorJornalerosAsistencia.puedeRegistrarPesaje(disponibles, "   "))
        assertFalse(SelectorJornalerosAsistencia.puedeRegistrarPesaje(disponibles, jNoAsistio.nombreCompleto))
        assertTrue(SelectorJornalerosAsistencia.puedeRegistrarPesaje(disponibles, jHoy.nombreCompleto))
    }

    @Test
    fun `al actualizar lista luego de nueva asistencia aparecen nuevos jornaleros`() {
        assertTrue(SelectorJornalerosAsistencia.cargarParaFecha("2026-11-16").isEmpty())

        val j1 = DatosMemoria.listaJornaleros[0]
        val j2 = DatosMemoria.listaJornaleros[3]
        DatosMemoria.registrarAsistencia(j1.id, "2026-11-16", "06:00:00", "PUNTUAL", "QR_SCAN")
        DatosMemoria.registrarAsistencia(j2.id, "2026-11-16", "06:12:00", "PUNTUAL", "DNI_MANUAL")

        val actualizados = SelectorJornalerosAsistencia.cargarParaFecha("2026-11-16")
        assertEquals(listOf(j1.nombreCompleto, j2.nombreCompleto), actualizados.map { it.nombreCompleto })
        assertTrue(SelectorJornalerosAsistencia.puedeRegistrarPesaje(actualizados, j2.nombreCompleto))
    }
}
