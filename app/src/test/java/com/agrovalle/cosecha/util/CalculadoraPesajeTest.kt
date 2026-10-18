package com.agrovalle.cosecha.util

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Pruebas unitarias del calculo de tara y peso neto (tarea del Sprint Backlog:
 * "Pruebas unitarias del calculo de tara y peso neto (3, 4 y 6 jabas)").
 *
 * Cubre AGV-003 (peso neto y validacion de rango) y AGV-015 (calibracion de
 * tara), incluyendo los casos borde: tara fuera de rango, peso neto negativo y
 * valores limite exactos.
 */
class CalculadoraPesajeTest {

    private val tara = 0.375

    // ------------------------------------------------------------------
    // AGV-003: Peso Neto = Peso Bruto - (Tara x Jabas)
    // ------------------------------------------------------------------

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

    // ------------------------------------------------------------------
    // Caso borde: el peso neto JAMAS puede ser negativo
    // ------------------------------------------------------------------

    @Test
    fun `el peso neto nunca es negativo`() {
        assertEquals(0.00, CalculadoraPesaje.calcularPesoNeto(1.00, 6, tara), 0.0001)
    }

    @Test
    fun `un peso bruto menor que la tara total se recorta a cero`() {
        // 1.00 - (10 x 0.375 = 3.75) seria -2.75 -> se recorta a 0.00
        assertEquals(0.00, CalculadoraPesaje.calcularPesoNeto(1.00, 10, tara), 0.0001)
    }

    @Test
    fun `peso bruto cero da peso neto cero`() {
        assertEquals(0.00, CalculadoraPesaje.calcularPesoNeto(0.00, 4, tara), 0.0001)
    }

    // ------------------------------------------------------------------
    // Tara total
    // ------------------------------------------------------------------

    @Test
    fun `la tara total con 4 jabas es 1 50 kg`() {
        assertEquals(1.50, CalculadoraPesaje.calcularTaraTotal(4, tara), 0.0001)
    }

    @Test
    fun `la tara total con cero jabas es cero`() {
        assertEquals(0.00, CalculadoraPesaje.calcularTaraTotal(0, tara), 0.0001)
    }

    @Test
    fun `el peso neto coincide con bruto menos la tara total mostrada`() {
        // Coherencia de la formula que se pinta en pantalla:
        // "bruto - taraTotal = neto"
        val jabas = 5
        val bruto = 12.30
        val taraTotal = CalculadoraPesaje.calcularTaraTotal(jabas, tara)
        val neto = CalculadoraPesaje.calcularPesoNeto(bruto, jabas, tara)
        assertEquals(1.88, taraTotal, 0.0001)   // 5 x 0.375 = 1.875 -> 1.88
        assertEquals(10.42, neto, 0.0001)        // 12.30 - 1.88 = 10.42
        assertEquals(neto, bruto - taraTotal, 0.0001)
    }

    // ------------------------------------------------------------------
    // Validacion de rango (anti errores de tipeo)
    // ------------------------------------------------------------------

    @Test
    fun `valida el rango de peso y la cantidad de jabas`() {
        assertTrue(CalculadoraPesaje.rangoValido(9.50, 4))
        assertFalse(CalculadoraPesaje.rangoValido(0.00, 4))   // sin peso
        assertFalse(CalculadoraPesaje.rangoValido(9.50, 0))   // sin jabas
        assertFalse(CalculadoraPesaje.rangoValido(99.00, 4))  // fuera de rango
    }

    @Test
    fun `los limites exactos de peso bruto son validos`() {
        assertTrue(CalculadoraPesaje.rangoValido(Constantes.PESO_BRUTO_MIN, 1))
        assertTrue(CalculadoraPesaje.rangoValido(Constantes.PESO_BRUTO_MAX, 1))
    }

    @Test
    fun `un peso apenas fuera de los limites es rechazado`() {
        assertFalse(CalculadoraPesaje.rangoValido(Constantes.PESO_BRUTO_MIN - 0.01, 1))
        assertFalse(CalculadoraPesaje.rangoValido(Constantes.PESO_BRUTO_MAX + 0.01, 1))
    }

    @Test
    fun `una cantidad de jabas negativa es invalida`() {
        assertFalse(CalculadoraPesaje.rangoValido(9.50, -1))
    }

    // ------------------------------------------------------------------
    // AGV-015: calibracion de tara
    // ------------------------------------------------------------------

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
    fun `el promedio de una sola muestra es esa muestra`() {
        assertEquals(0.372, CalculadoraPesaje.promedioMuestras(listOf(0.372)), 0.0001)
    }

    @Test
    fun `la tara calibrada solo es valida entre 350 y 400 gramos`() {
        assertTrue(CalculadoraPesaje.taraValida(0.375))
        assertTrue(CalculadoraPesaje.taraValida(0.350))
        assertTrue(CalculadoraPesaje.taraValida(0.400))
        assertFalse(CalculadoraPesaje.taraValida(0.300))
        assertFalse(CalculadoraPesaje.taraValida(0.450))
    }

    /** Caso borde pedido: tara calibrada FUERA de rango (jabas muy pesadas). */
    @Test
    fun `una calibracion con jabas pesadas queda fuera de rango`() {
        // Jabas de ~850 g: muy por encima del maximo de 400 g
        val muestras = listOf(0.850, 0.852, 0.848, 0.851, 0.849)
        val promedio = CalculadoraPesaje.promedioMuestras(muestras)
        assertEquals(0.850, promedio, 0.0001)
        assertFalse(CalculadoraPesaje.taraValida(promedio))
    }

    /** Caso borde pedido: tara calibrada FUERA de rango (jabas muy livianas). */
    @Test
    fun `una calibracion con jabas livianas queda fuera de rango`() {
        // Jabas de ~200 g: por debajo del minimo de 350 g
        val muestras = listOf(0.200, 0.202, 0.198, 0.201, 0.199)
        val promedio = CalculadoraPesaje.promedioMuestras(muestras)
        assertEquals(0.200, promedio, 0.0001)
        assertFalse(CalculadoraPesaje.taraValida(promedio))
    }

    @Test
    fun `la tara por defecto del turno siempre es valida`() {
        assertTrue(CalculadoraPesaje.taraValida(Constantes.TARA_POR_DEFECTO))
    }

    // ------------------------------------------------------------------
    // Coherencia de constantes con el padron en memoria (Fix 1)
    // ------------------------------------------------------------------

    @Test
    fun `el tamano de la cuadrilla coincide con el padron precargado`() {
        assertEquals(30, Constantes.TOTAL_CUADRILLA)
        assertEquals(30, com.agrovalle.cosecha.data.DatosMemoria.TOTAL_PADRON)
        assertEquals(
            com.agrovalle.cosecha.data.DatosMemoria.TOTAL_PADRON,
            com.agrovalle.cosecha.data.DatosMemoria.listaJornaleros.size
        )
    }
}
