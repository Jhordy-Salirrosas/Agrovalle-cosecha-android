package com.agrovalle.cosecha.util

import com.agrovalle.cosecha.data.DatosMemoria
import com.agrovalle.cosecha.model.Asistencia
import com.agrovalle.cosecha.model.Jornalero
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Assert.fail
import org.junit.Before
import org.junit.Test

class CalculadoraCierreTurnoTest {

    @Before
    fun setUp() {
        DatosMemoria.reiniciarTurno()
    }

    @Test
    fun porcentajeManualesSinAsistenciasDevuelveCero() {
        assertEquals(0.0, CalculadoraCierreTurno.calcularPorcentajeManuales(0, 0), 0.001)
        assertEquals(0.0, CalculadoraCierreTurno.calcularPorcentajeManuales(2, 0), 0.001)
    }

    @Test
    fun porcentajeManualesSinIngresosManualesDevuelveCero() {
        assertEquals(0.0, CalculadoraCierreTurno.calcularPorcentajeManuales(0, 12), 0.001)
    }

    @Test
    fun porcentajeManualesCalculaExactoConDosDecimales() {
        assertEquals(25.0, CalculadoraCierreTurno.calcularPorcentajeManuales(1, 4), 0.001)
        assertEquals(33.33, CalculadoraCierreTurno.calcularPorcentajeManuales(1, 3), 0.001)
        assertEquals(100.0, CalculadoraCierreTurno.calcularPorcentajeManuales(5, 5), 0.001)
    }

    @Test
    fun resumirPorCuadrillaSinAsistenciasMantieneCuadrillaEnCero() {
        val resumen = CalculadoraCierreTurno.resumirPorCuadrilla(
            asistenciasDelDia = emptyList(),
            jornaleros = DatosMemoria.listaJornaleros,
            cuadrillasBase = listOf(DatosMemoria.CODIGO_CUADRILLA)
        )

        assertEquals(1, resumen.size)
        val item = resumen.single()
        assertEquals("CUA-01", item.cuadrillaCodigo)
        assertEquals(0, item.totalAsistencias)
        assertEquals(0, item.cantidadIngresosManuales)
        assertEquals(0.0, item.porcentajeIngresosManuales, 0.001)
    }

    @Test
    fun resumirPorCuadrillaAgrupaQrYManualesPorCuadrilla() {
        val padron = listOf(
            Jornalero(1L, "40000001", "Ana", "Lopez", "QR_1", "CUA-01"),
            Jornalero(2L, "40000002", "Luis", "Perez", "QR_2", "CUA-01"),
            Jornalero(3L, "40000003", "Rosa", "Diaz", "QR_3", "CUA-02"),
            Jornalero(4L, "40000004", "Pedro", "Rojas", "QR_4", "CUA-02")
        )
        val asistencias = listOf(
            Asistencia(1L, 1L, "2026-11-17", "06:02:00", "PUNTUAL", "QR_SCAN"),
            Asistencia(2L, 2L, "2026-11-17", "06:08:00", "PUNTUAL", "DNI_MANUAL"),
            Asistencia(3L, 3L, "2026-11-17", "06:10:00", "PUNTUAL", "QR_SCAN"),
            Asistencia(4L, 4L, "2026-11-17", "06:12:00", "PUNTUAL", "QR_SCAN")
        )

        val resumen = CalculadoraCierreTurno.resumirPorCuadrilla(asistencias, padron)

        assertEquals(2, resumen.size)
        val cua1 = resumen.first { it.cuadrillaCodigo == "CUA-01" }
        assertEquals(2, cua1.totalAsistencias)
        assertEquals(1, cua1.cantidadIngresosManuales)
        assertEquals(50.0, cua1.porcentajeIngresosManuales, 0.001)

        val cua2 = resumen.first { it.cuadrillaCodigo == "CUA-02" }
        assertEquals(2, cua2.totalAsistencias)
        assertEquals(0, cua2.cantidadIngresosManuales)
        assertEquals(0.0, cua2.porcentajeIngresosManuales, 0.001)
    }

    @Test
    fun bitacoraYResumenDatosMemoriaRegistranMotivoHoraYCapatazSinContarQrEnBitacora() {
        val fecha = "2026-11-17"
        val jQr1 = DatosMemoria.listaJornaleros[0]
        val jQr2 = DatosMemoria.listaJornaleros[1]
        val jManual = DatosMemoria.listaJornaleros[2]

        DatosMemoria.registrarAsistencia(jQr1.id, fecha, "06:01:00", "PUNTUAL", "QR_SCAN")
        DatosMemoria.registrarAsistencia(jQr2.id, fecha, "06:03:00", "PUNTUAL", "QR_SCAN")
        DatosMemoria.registrarAsistencia(jManual.id, fecha, "06:09:15", "PUNTUAL", "DNI_MANUAL")
        DatosMemoria.registrarBitacoraIngresoManual(
            jornalero = jManual,
            fecha = fecha,
            hora = "06:09:15",
            motivo = "Fotocheck olvidado o extraviado",
            capatazResponsable = "Capataz de cuadrilla"
        )
        // Otra fecha distinta no debe contaminar el reporte del día
        DatosMemoria.registrarAsistencia(DatosMemoria.listaJornaleros[3].id, "2026-11-16", "06:00:00", "PUNTUAL", "DNI_MANUAL")
        DatosMemoria.registrarBitacoraIngresoManual(
            jornalero = DatosMemoria.listaJornaleros[3],
            fecha = "2026-11-16",
            hora = "06:00:00",
            motivo = "Código QR dañado o ilegible",
            capatazResponsable = "Otro Capataz"
        )

        val resumenDia = DatosMemoria.obtenerResumenCierrePorCuadrilla(fecha).single()
        assertEquals("CUA-01", resumenDia.cuadrillaCodigo)
        assertEquals(3, resumenDia.totalAsistencias)
        assertEquals(1, resumenDia.cantidadIngresosManuales)
        assertEquals(33.33, resumenDia.porcentajeIngresosManuales, 0.001)

        val bitacoraDia = DatosMemoria.obtenerBitacoraIngresosManuales(fecha)
        assertEquals(1, bitacoraDia.size)
        val entrada = bitacoraDia.single()
        assertEquals(jManual.nombreCompleto, entrada.jornaleroNombre)
        assertEquals("06:09:15", entrada.hora)
        assertEquals("Fotocheck olvidado o extraviado", entrada.motivo)
        assertEquals("Capataz de cuadrilla", entrada.capatazResponsable)
    }

    @Test
    fun bitacoraIngresoManualExigeMotivoYCapatazNoVacios() {
        val j = DatosMemoria.listaJornaleros.first()
        try {
            DatosMemoria.registrarBitacoraIngresoManual(j, "2026-11-17", "06:10:00", "  ", "Capataz")
            fail("Debió rechazar motivo en blanco")
        } catch (expected: IllegalArgumentException) {
            // Esperado
        }

        try {
            DatosMemoria.registrarBitacoraIngresoManual(j, "2026-11-17", "06:10:00", "Falla cámara", "  ")
            fail("Debió rechazar capataz en blanco")
        } catch (expected: IllegalArgumentException) {
            // Esperado
        }

        assertTrue(DatosMemoria.bitacoraIngresosManuales.isEmpty())
    }
}
