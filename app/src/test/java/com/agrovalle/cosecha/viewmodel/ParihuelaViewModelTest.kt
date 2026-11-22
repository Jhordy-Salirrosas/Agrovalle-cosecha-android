package com.agrovalle.cosecha.viewmodel

import com.agrovalle.cosecha.data.repository.JornaleroAsistenciaOpcion
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Assert.fail
import org.junit.Test

class ParihuelaViewModelTest {

    @Test
    fun `derivarContextoPesaje usa el cuartelActual del jornalero seleccionado y la variedad del catalogo`() {
        val disponibles = listOf(
            JornaleroAsistenciaOpcion(id = 1L, nombreCompleto = "Juan Perez", cuartelActual = "Cuartel 01"),
            JornaleroAsistenciaOpcion(id = 2L, nombreCompleto = "Maria Lopez", cuartelActual = "Cuartel 05"),
            JornaleroAsistenciaOpcion(id = 3L, nombreCompleto = "Luis Torres", cuartelActual = null)
        )

        val ctxJuan = ParihuelaViewModel.derivarContextoPesaje(disponibles, "Juan Perez")
        assertEquals("Cuartel 01", ctxJuan.cuartel)
        assertEquals("Biloxi", ctxJuan.variedad)
        assertTrue(ctxJuan.estaCompleto)

        val ctxMaria = ParihuelaViewModel.derivarContextoPesaje(disponibles, "  maria lopez ")
        assertEquals("Cuartel 05", ctxMaria.cuartel)
        assertEquals("Ventura", ctxMaria.variedad)
        assertTrue(ctxMaria.estaCompleto)

        val ctxSinAsignacion = ParihuelaViewModel.derivarContextoPesaje(disponibles, "Luis Torres")
        assertNull(ctxSinAsignacion.cuartel)
        assertNull(ctxSinAsignacion.variedad)
        assertFalse(ctxSinAsignacion.estaCompleto)

        val ctxNinguno = ParihuelaViewModel.derivarContextoPesaje(disponibles, null)
        assertNull(ctxNinguno.cuartel)
        assertNull(ctxNinguno.variedad)
        assertFalse(ctxNinguno.estaCompleto)
    }

    @Test
    fun `validarCatalogoApertura acepta pares del catalogo y rechaza cuartel o variedad fuera de catalogo`() {
        val (variedadOk, cuartelOk) = ParihuelaViewModel.validarCatalogoApertura("  ventura ", " cuartel 05 ")
        assertEquals("Ventura", variedadOk)
        assertEquals("Cuartel 05", cuartelOk)

        // Cuartel fuera de catálogo
        try {
            ParihuelaViewModel.validarCatalogoApertura("Biloxi", "Cuartel 99")
            fail("Debió rechazar un cuartel fuera de catálogo")
        } catch (expected: IllegalArgumentException) {
            assertTrue(expected.message!!.contains("catálogo"))
        }

        // Variedad fuera de catálogo
        try {
            ParihuelaViewModel.validarCatalogoApertura("Emerald", "Cuartel 01")
            fail("Debió rechazar una variedad fuera de catálogo")
        } catch (expected: IllegalArgumentException) {
            assertTrue(expected.message!!.contains("catálogo"))
        }

        // Variedad de catálogo pero incoherente con el cuartel
        try {
            ParihuelaViewModel.validarCatalogoApertura("Ventura", "Cuartel 01")
            fail("Debió rechazar una variedad que no corresponde al cuartel")
        } catch (expected: IllegalArgumentException) {
            assertTrue(expected.message!!.contains("Biloxi"))
        }
    }
}
