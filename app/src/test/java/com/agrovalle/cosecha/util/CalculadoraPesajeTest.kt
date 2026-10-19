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
 *
 * NOTA: los nombres de los metodos NO usan comillas invertidas ni espacios,
 * para que la tarea de Gradle `testDebugUnitTest` pueda ejecutarlos tambien en
 * Windows sin lanzar java.lang.ClassNotFoundException.
 *
 * Solo se prueba codigo puro de Kotlin (CalculadoraPesaje y Constantes), sin
 * dependencias del framework de Android.
 */
class CalculadoraPesajeTest {

    private val tara = 0.375

    // ------------------------------------------------------------------
    // AGV-003: Peso Neto = Peso Bruto - (Tara x Jabas)
    // ------------------------------------------------------------------

    @Test
    fun testPesaje4Jabas() {
        // Escenario 1 de AGV-003: 9.50 - (4 x 0.375) = 8.00 kg
        assertEquals(8.00, CalculadoraPesaje.calcularPesoNeto(9.50, 4, tara), 0.0001)
    }

    @Test
    fun testPesaje3Jabas() {
        // Tara total = 1.125 -> 1.13 ; 9.50 - 1.13 = 8.37
        assertEquals(8.37, CalculadoraPesaje.calcularPesoNeto(9.50, 3, tara), 0.0001)
    }

    @Test
    fun testPesaje6Jabas() {
        // Tara total = 2.25 ; 9.50 - 2.25 = 7.25
        assertEquals(7.25, CalculadoraPesaje.calcularPesoNeto(9.50, 6, tara), 0.0001)
    }

    // ------------------------------------------------------------------
    // Caso borde: el peso neto JAMAS puede ser negativo
    // ------------------------------------------------------------------

    @Test
    fun testPesoNetoNuncaNegativo() {
        assertEquals(0.00, CalculadoraPesaje.calcularPesoNeto(1.00, 6, tara), 0.0001)
    }

    @Test
    fun testPesoBrutoMenorQueTaraSeRecortaACero() {
        // 1.00 - (10 x 0.375 = 3.75) seria -2.75 -> se recorta a 0.00
        assertEquals(0.00, CalculadoraPesaje.calcularPesoNeto(1.00, 10, tara), 0.0001)
    }

    @Test
    fun testPesoBrutoCeroDaPesoNetoCero() {
        assertEquals(0.00, CalculadoraPesaje.calcularPesoNeto(0.00, 4, tara), 0.0001)
    }

    // ------------------------------------------------------------------
    // Tara total
    // ------------------------------------------------------------------

    @Test
    fun testTaraTotal4Jabas() {
        assertEquals(1.50, CalculadoraPesaje.calcularTaraTotal(4, tara), 0.0001)
    }

    @Test
    fun testTaraTotalCeroJabas() {
        assertEquals(0.00, CalculadoraPesaje.calcularTaraTotal(0, tara), 0.0001)
    }

    @Test
    fun testPesoNetoCoincideConBrutoMenosTaraTotal() {
        // Coherencia de la formula que se pinta en pantalla:
        // "bruto - taraTotal = neto"
        val jabas = 5
        val bruto = 12.30
        val taraTotal = CalculadoraPesaje.calcularTaraTotal(jabas, tara)
        val neto = CalculadoraPesaje.calcularPesoNeto(bruto, jabas, tara)
        assertEquals(1.88, taraTotal, 0.0001)   // 5 x 0.375 = 1.875 -> 1.88
        assertEquals(10.42, neto, 0.0001)       // 12.30 - 1.88 = 10.42
        assertEquals(neto, bruto - taraTotal, 0.0001)
    }

    // ------------------------------------------------------------------
    // Validacion de rango (anti errores de tipeo)
    // ------------------------------------------------------------------

    @Test
    fun testValidaRangoDePesoYCantidadDeJabas() {
        assertTrue(CalculadoraPesaje.rangoValido(9.50, 4))
        assertFalse(CalculadoraPesaje.rangoValido(0.00, 4))   // sin peso
        assertFalse(CalculadoraPesaje.rangoValido(9.50, 0))   // sin jabas
        assertFalse(CalculadoraPesaje.rangoValido(99.00, 4))  // fuera de rango
    }

    @Test
    fun testLimitesExactosDePesoBrutoSonValidos() {
        assertTrue(CalculadoraPesaje.rangoValido(Constantes.PESO_BRUTO_MIN, 1))
        assertTrue(CalculadoraPesaje.rangoValido(Constantes.PESO_BRUTO_MAX, 1))
    }

    @Test
    fun testPesoFueraDeLosLimitesEsRechazado() {
        assertFalse(CalculadoraPesaje.rangoValido(Constantes.PESO_BRUTO_MIN - 0.01, 1))
        assertFalse(CalculadoraPesaje.rangoValido(Constantes.PESO_BRUTO_MAX + 0.01, 1))
    }

    @Test
    fun testCantidadDeJabasNegativaEsInvalida() {
        assertFalse(CalculadoraPesaje.rangoValido(9.50, -1))
    }

    // ------------------------------------------------------------------
    // AGV-015: calibracion de tara
    // ------------------------------------------------------------------

    @Test
    fun testPromedioDe5JabasVacias() {
        val muestras = listOf(0.370, 0.380, 0.375, 0.378, 0.372)
        assertEquals(0.375, CalculadoraPesaje.promedioMuestras(muestras), 0.0001)
    }

    @Test
    fun testSinMuestrasSeUsaTaraPorDefecto() {
        assertEquals(
            Constantes.TARA_POR_DEFECTO,
            CalculadoraPesaje.promedioMuestras(emptyList()),
            0.0001
        )
    }

    @Test
    fun testPromedioDeUnaSolaMuestra() {
        assertEquals(0.372, CalculadoraPesaje.promedioMuestras(listOf(0.372)), 0.0001)
    }

    @Test
    fun testTaraCalibradaValidaEntre350Y400Gramos() {
        assertTrue(CalculadoraPesaje.taraValida(0.375))
        assertTrue(CalculadoraPesaje.taraValida(0.350))
        assertTrue(CalculadoraPesaje.taraValida(0.400))
        assertFalse(CalculadoraPesaje.taraValida(0.300))
        assertFalse(CalculadoraPesaje.taraValida(0.450))
    }

    /** Caso borde: tara calibrada FUERA de rango (jabas muy pesadas). */
    @Test
    fun testCalibracionConJabasPesadasQuedaFueraDeRango() {
        // Jabas de ~850 g: muy por encima del maximo de 400 g
        val muestras = listOf(0.850, 0.852, 0.848, 0.851, 0.849)
        val promedio = CalculadoraPesaje.promedioMuestras(muestras)
        assertEquals(0.850, promedio, 0.0001)
        assertFalse(CalculadoraPesaje.taraValida(promedio))
    }

    /** Caso borde: tara calibrada FUERA de rango (jabas muy livianas). */
    @Test
    fun testCalibracionConJabasLivianasQuedaFueraDeRango() {
        // Jabas de ~200 g: por debajo del minimo de 350 g
        val muestras = listOf(0.200, 0.202, 0.198, 0.201, 0.199)
        val promedio = CalculadoraPesaje.promedioMuestras(muestras)
        assertEquals(0.200, promedio, 0.0001)
        assertFalse(CalculadoraPesaje.taraValida(promedio))
    }

    @Test
    fun testTaraPorDefectoDelTurnoEsValida() {
        assertTrue(CalculadoraPesaje.taraValida(Constantes.TARA_POR_DEFECTO))
    }

    // ------------------------------------------------------------------
    // Constantes de negocio
    // ------------------------------------------------------------------

    /**
     * Se valida solo el valor de la constante (codigo puro de Kotlin).
     *
     * FIX JVM: YA NO se toca DatosMemoria, porque su inicializacion arrastra
     * dependencias de Android y la tarea `testDebugUnitTest` fallaba con
     * java.lang.ClassNotFoundException / InitializationError.
     */
    @Test
    fun testTotalCuadrillaEs30() {
        assertEquals(30, Constantes.TOTAL_CUADRILLA)
    }
}
