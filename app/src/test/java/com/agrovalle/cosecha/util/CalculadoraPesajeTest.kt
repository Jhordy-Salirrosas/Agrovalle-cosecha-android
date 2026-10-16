package com.agrovalle.cosecha.util

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Pruebas unitarias del calculo de tara y peso neto (tarea del Sprint Backlog:
 * "Pruebas unitarias del calculo de tara y peso neto (3, 4 y 6 jabas)").
 */
class CalculadoraPesajeTest {

    private val tara = 0.375

    @Test
    fun `pesaje de 4 jabas y 9 50 kg da 8 00 kg de peso neto`() {
        // Escenario 1 de AGV-003: 9.50 - (4 x 0.375) = 8.00 kg
        assertEquals(8.00, CalculadoraPesaje.calcularPesoNeto(9.50, 4, tara), 0.0001)
    }

    @Test
    fun `pesaje de 3 jabas y 9 50 kg da 8 37 kg de peso neto`() {
        // Tara total = 1.125 -> 1.13 ; 9.50 - 1.13 = 8.37
        assertEquals(8.37, CalculadoraPesaje.calcularPesoNeto(9.50, 3, tara), 0.0001)
    }

    @Test
    fun `pesaje de 6 jabas y 9 50 kg da 7 25 kg de peso neto`() {
        // Tara total = 2.25 ; 9.50 - 2.25 = 7.25
        assertEquals(7.25, CalculadoraPesaje.calcularPesoNeto(9.50, 6, tara), 0.0001)
    }

    @Test
    fun `el peso neto nunca es negativo`() {
        assertEquals(0.00, CalculadoraPesaje.calcularPesoNeto(1.00, 6, tara), 0.0001)
    }

    @Test
    fun `la tara total con 4 jabas es 1 50 kg`() {
        assertEquals(1.50, CalculadoraPesaje.calcularTaraTotal(4, tara), 0.0001)
    }

    @Test
    fun `valida el rango de peso y la cantidad de jabas`() {
        assertTrue(CalculadoraPesaje.rangoValido(9.50, 4))
        assertFalse(CalculadoraPesaje.rangoValido(0.00, 4))   // sin peso
        assertFalse(CalculadoraPesaje.rangoValido(9.50, 0))   // sin jabas
        assertFalse(CalculadoraPesaje.rangoValido(99.00, 4))  // fuera de rango
    }

    @Test
    fun `promedio de 5 jabas vacias`() {
        val muestras = listOf(0.370, 0.380, 0.375, 0.378, 0.372)
        assertEquals(0.375, CalculadoraPesaje.promedioMuestras(muestras), 0.0001)
    }

    @Test
    fun `sin muestras se usa la tara por defecto`() {
        assertEquals(Constantes.TARA_POR_DEFECTO, CalculadoraPesaje.promedioMuestras(emptyList()), 0.0001)
    }

    @Test
    fun `la tara calibrada solo es valida entre 350 y 400 gramos`() {
        assertTrue(CalculadoraPesaje.taraValida(0.375))
        assertTrue(CalculadoraPesaje.taraValida(0.350))
        assertTrue(CalculadoraPesaje.taraValida(0.400))
        assertFalse(CalculadoraPesaje.taraValida(0.300))
        assertFalse(CalculadoraPesaje.taraValida(0.450))
    }
}
