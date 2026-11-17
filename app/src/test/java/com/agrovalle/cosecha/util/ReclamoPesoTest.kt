package com.agrovalle.cosecha.util

import com.agrovalle.cosecha.data.DatosMemoria
import com.agrovalle.cosecha.model.EstadoReclamoPeso
import com.agrovalle.cosecha.model.Pesaje
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Assert.fail
import org.junit.Before
import org.junit.Test

class ReclamoPesoTest {

    @Before
    fun setUp() {
        DatosMemoria.reiniciarTurno()
    }

    @Test
    fun `evaluarReclamo con diferencia de exactamente 20 gramos no excede tolerancia`() {
        val eval = CalculadoraReclamoPeso.evaluarReclamo(
            pesoOriginalNetoKg = 8.000,
            pesoNuevoNetoKg = 8.020
        )

        assertFalse(eval.excedeTolerancia)
        assertEquals(20.0, eval.diferenciaAbsolutaGramos, 0.001)
        assertEquals(0.020, eval.diferenciaKg, 0.0001)
        assertEquals(0.0, eval.pesoCompensatorioKg, 0.0001)
    }

    @Test
    fun `evaluarReclamo con diferencia de 21 gramos si excede tolerancia y calcula compensatorio`() {
        val eval = CalculadoraReclamoPeso.evaluarReclamo(
            pesoOriginalNetoKg = 8.000,
            pesoNuevoNetoKg = 8.021
        )

        assertTrue(eval.excedeTolerancia)
        assertEquals(21.0, eval.diferenciaAbsolutaGramos, 0.001)
        assertEquals(0.021, eval.diferenciaKg, 0.0001)
        assertEquals(0.021, eval.pesoCompensatorioKg, 0.0001)
    }

    @Test
    fun `reclamo dentro de 20 gramos mantiene pesaje original intacto y registra bitacora`() {
        val jornalero = DatosMemoria.listaJornaleros.first()
        val original = DatosMemoria.registrarPesaje(
            Pesaje(
                jornaleroId = jornalero.id,
                cantidadJabas = 4,
                pesoBruto = 9.50,
                taraTotal = 1.50,
                pesoNeto = 8.00,
                timestamp = 1_700_000_000_000L,
                esTaraPorDefecto = true
            )
        )

        // Nuevo bruto 9.515 - tara (4 * 0.375 = 1.500) = 8.015 kg (+15 g <= 20 g)
        val resultado = DatosMemoria.registrarReclamoPeso(
            pesajeOriginalId = original.id,
            nuevoPesoBrutoKg = 9.515,
            taraPorJabaKg = Constantes.TARA_POR_DEFECTO,
            fecha = "2026-11-17",
            hora = "10:15:00",
            timestamp = 1_700_000_100_000L
        )

        assertTrue(resultado is DatosMemoria.ResultadoRegistroReclamo.DentroDeTolerancia)
        // Solo existe el pesaje original (sin compensatorio) y sus valores no fueron editados
        assertEquals(1, DatosMemoria.pesajes.size)
        val persistido = DatosMemoria.buscarPesajePorId(original.id)!!
        assertEquals(8.00, persistido.pesoNeto, 0.0001)
        assertEquals(9.50, persistido.pesoBruto, 0.0001)
        assertFalse(persistido.esCompensatorio)

        val bitacora = DatosMemoria.obtenerBitacoraReclamos()
        assertEquals(1, bitacora.size)
        val reg = bitacora.first()
        assertEquals("10:15:00", reg.hora)
        assertEquals(jornalero.nombreCompleto, reg.jornaleroNombre)
        assertEquals(original.id, reg.pesajeOriginalId)
        assertEquals(8.00, reg.pesoOriginalNetoKg, 0.0001)
        assertEquals(15.0, reg.diferenciaGramos, 0.001)
        assertEquals(EstadoReclamoPeso.DENTRO_TOLERANCIA, reg.estado)
        assertNull(reg.pesajeCompensatorioId)
    }

    @Test
    fun `reclamo mayor a 20 gramos exige credencial QR de capataz y rechaza QR invalido o de jornalero`() {
        val jornalero = DatosMemoria.listaJornaleros.first()
        val original = DatosMemoria.registrarPesaje(
            Pesaje(
                jornaleroId = jornalero.id,
                cantidadJabas = 4,
                pesoBruto = 9.50,
                taraTotal = 1.50,
                pesoNeto = 8.00
            )
        )

        // Sin QR de capataz -> rechazado
        try {
            DatosMemoria.registrarReclamoPeso(
                pesajeOriginalId = original.id,
                nuevoPesoBrutoKg = 9.80,
                taraPorJabaKg = Constantes.TARA_POR_DEFECTO,
                codigoQrCapataz = null
            )
            fail("Debió exigir QR del capataz cuando la diferencia supera 20 g")
        } catch (expected: IllegalStateException) {
            // Esperado
        }

        // Con fotocheck de jornalero (no es capataz) -> rechazado
        try {
            DatosMemoria.registrarReclamoPeso(
                pesajeOriginalId = original.id,
                nuevoPesoBrutoKg = 9.80,
                taraPorJabaKg = Constantes.TARA_POR_DEFECTO,
                codigoQrCapataz = jornalero.codigoQr
            )
            fail("Debió rechazar un QR de jornalero como credencial de capataz")
        } catch (expected: IllegalArgumentException) {
            // Esperado
        }

        assertEquals(1, DatosMemoria.pesajes.size)
        assertTrue(DatosMemoria.obtenerBitacoraReclamos().isEmpty())
    }

    @Test
    fun `reclamo mayor a 20 gramos con QR de capataz crea pesaje compensatorio vinculado sin alterar original`() {
        val jornalero = DatosMemoria.listaJornaleros.first()
        val now = 1_700_000_000_000L
        val original = DatosMemoria.registrarPesaje(
            Pesaje(
                jornaleroId = jornalero.id,
                cantidadJabas = 4,
                pesoBruto = 9.50,
                taraTotal = 1.50,
                pesoNeto = 8.00,
                timestamp = now
            )
        )

        // Nuevo bruto 9.850 - tara 1.500 = 8.350 kg (+350 g > 20 g)
        val resultado = DatosMemoria.registrarReclamoPeso(
            pesajeOriginalId = original.id,
            nuevoPesoBrutoKg = 9.85,
            taraPorJabaKg = Constantes.TARA_POR_DEFECTO,
            codigoQrCapataz = "QR_CAPATAZ_001",
            fecha = "2026-11-17",
            hora = "11:20:00",
            timestamp = now + 60_000L
        )

        assertTrue(resultado is DatosMemoria.ResultadoRegistroReclamo.CompensacionRegistrada)
        val compRes = resultado as DatosMemoria.ResultadoRegistroReclamo.CompensacionRegistrada

        // El original sigue intacto y coexiste con el compensatorio
        assertEquals(2, DatosMemoria.pesajes.size)
        val originalPersistido = DatosMemoria.buscarPesajePorId(original.id)!!
        assertEquals(8.00, originalPersistido.pesoNeto, 0.0001)
        assertFalse(originalPersistido.esCompensatorio)
        assertNull(originalPersistido.pesajeOriginalId)

        val comp = compRes.pesajeCompensatorio
        assertTrue(comp.esCompensatorio)
        assertEquals(original.id, comp.pesajeOriginalId)
        assertEquals(0.350, comp.pesoNeto, 0.0001)
        assertEquals(jornalero.id, comp.jornaleroId)

        // El acumulado diario refleja el peso corregido (8.00 + 0.35 = 8.35 kg)
        val acumulado = DatosMemoria.acumuladoDiario(jornalero.id, now - 10_000L, now + 120_000L)
        assertEquals(8.350, acumulado, 0.0001)

        // Bitácora registra el reclamo y la vinculación al compensatorio + capataz
        val bitacora = DatosMemoria.obtenerBitacoraReclamos()
        assertEquals(1, bitacora.size)
        val reg = bitacora.first()
        assertEquals("11:20:00", reg.hora)
        assertEquals(jornalero.nombreCompleto, reg.jornaleroNombre)
        assertEquals(original.id, reg.pesajeOriginalId)
        assertEquals(comp.id, reg.pesajeCompensatorioId)
        assertNotNull(reg.pesoCompensatorioKg)
        assertEquals(0.350, reg.pesoCompensatorioKg!!, 0.0001)
        assertEquals(350.0, reg.diferenciaGramos, 0.001)
        assertEquals("QR_CAPATAZ_001", reg.codigoQrCapataz)
        assertEquals("Carlos Mendez (Capataz)", reg.capatazAutorizador)
        assertEquals(EstadoReclamoPeso.COMPENSADO, reg.estado)
    }
}
