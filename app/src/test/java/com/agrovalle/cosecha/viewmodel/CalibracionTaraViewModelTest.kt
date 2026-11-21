package com.agrovalle.cosecha.viewmodel

import com.agrovalle.cosecha.data.DatosMemoria
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Assert.fail
import org.junit.Before
import org.junit.Test

class CalibracionTaraViewModelTest {

    @Before
    fun setUp() {
        DatosMemoria.reiniciarTurno()
    }

    @Test
    fun `guardar calibracion sin autorizacion de capataz o con QR invalido es rechazado sin guardar nada`() {
        val muestrasValidas = listOf(0.360, 0.365, 0.360, 0.355, 0.360)

        // Sin QR (null)
        try {
            CalibracionTaraViewModel.guardarCalibracionAutorizada(muestrasValidas, null)
            fail("Debió rechazar guardar calibración sin QR de capataz")
        } catch (expected: IllegalStateException) {
            assertTrue(expected.message!!.contains("capataz"))
        }

        // QR en blanco
        try {
            CalibracionTaraViewModel.guardarCalibracionAutorizada(muestrasValidas, "   ")
            fail("Debió rechazar guardar calibración con QR en blanco")
        } catch (expected: IllegalStateException) {
            // Esperado
        }

        // QR de jornalero (no autorizado)
        try {
            CalibracionTaraViewModel.guardarCalibracionAutorizada(
                muestrasValidas,
                DatosMemoria.listaJornaleros.first().codigoQr
            )
            fail("Debió rechazar QR de jornalero para modificar la tara")
        } catch (expected: IllegalArgumentException) {
            assertTrue(expected.message!!.contains("no válida"))
        }

        // QR desconocido
        try {
            CalibracionTaraViewModel.guardarCalibracionAutorizada(muestrasValidas, "QR_INVENTADO_999")
            fail("Debió rechazar QR desconocido para modificar la tara")
        } catch (expected: IllegalArgumentException) {
            // Esperado
        }

        assertTrue(DatosMemoria.calibraciones.isEmpty())
        assertNull(DatosMemoria.ultimaCalibracion())
    }

    @Test
    fun `guardar calibracion con credencial QR valida de capataz o supervisor guarda la nueva tara`() {
        val muestras1 = listOf(0.360, 0.365, 0.360, 0.355, 0.360)
        val guardada1 = CalibracionTaraViewModel.guardarCalibracionAutorizada(
            pesosMuestras = muestras1,
            codigoQrCapataz = "QR_CAPATAZ_001",
            asistenteNombre = "Asistente Turno",
            timestamp = 1_700_000_000_000L
        )

        assertTrue(guardada1.aprobado)
        assertEquals(0.360, guardada1.taraPromedioCalculada, 0.0001)
        assertEquals("Carlos Mendez (Capataz)", guardada1.capatazAutorizador)
        assertEquals("QR_CAPATAZ_001", guardada1.codigoQrCapataz)
        assertEquals(1, DatosMemoria.calibraciones.size)
        assertNotNull(DatosMemoria.ultimaCalibracion())
        assertEquals(0.360, DatosMemoria.ultimaCalibracion()!!.taraPromedioCalculada, 0.0001)

        // También acepta credencial de supervisora (case-insensitive y con espacios)
        val muestras2 = listOf(0.380, 0.380, 0.385, 0.375, 0.380)
        val guardada2 = CalibracionTaraViewModel.guardarCalibracionAutorizada(
            pesosMuestras = muestras2,
            codigoQrCapataz = "  qr_capataz_002 ",
            asistenteNombre = "Asistente Turno",
            timestamp = 1_700_000_100_000L
        )

        assertTrue(guardada2.aprobado)
        assertEquals(0.380, guardada2.taraPromedioCalculada, 0.0001)
        assertEquals("Elena Rojas (Supervisora)", guardada2.capatazAutorizador)
        assertEquals("QR_CAPATAZ_002", guardada2.codigoQrCapataz)
        assertEquals(2, DatosMemoria.calibraciones.size)
        assertEquals(0.380, DatosMemoria.ultimaCalibracion()!!.taraPromedioCalculada, 0.0001)
    }
}
