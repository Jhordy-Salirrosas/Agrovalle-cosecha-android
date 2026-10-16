package com.agrovalle.cosecha.util

/**
 * Calculos puros del pesaje (AGV-003) y de la calibracion (AGV-015).
 *
 * Se aislaron del ViewModel para poder verificarlos con pruebas unitarias JUnit
 * sin depender del framework de Android.
 */
object CalculadoraPesaje {

    /** Tara total aplicada = tara por jaba * cantidad de jabas. */
    fun calcularTaraTotal(cantidadJabas: Int, taraPorJaba: Double): Double =
        redondear2(taraPorJaba * cantidadJabas)

    /**
     * Peso Neto = Peso Bruto - Tara Total, nunca negativo.
     * Se descuenta la tara total ya redondeada para que el resultado coincida
     * exactamente con la formula que se muestra en pantalla.
     */
    fun calcularPesoNeto(pesoBruto: Double, cantidadJabas: Int, taraPorJaba: Double): Double {
        val taraTotal = calcularTaraTotal(cantidadJabas, taraPorJaba)
        return redondear2((pesoBruto - taraTotal).coerceAtLeast(0.0))
    }

    /** Validacion de rangos para evitar errores de tipeo (AGV-003). */
    fun rangoValido(pesoBruto: Double, cantidadJabas: Int): Boolean =
        pesoBruto in Constantes.PESO_BRUTO_MIN..Constantes.PESO_BRUTO_MAX && cantidadJabas > 0

    /** Promedio de la muestra de jabas vacias (AGV-015). */
    fun promedioMuestras(pesos: List<Double>): Double =
        if (pesos.isEmpty()) Constantes.TARA_POR_DEFECTO else redondear3(pesos.average())

    /** La tara calibrada es valida solo dentro del rango de 350 a 400 g (AGV-015). */
    fun taraValida(taraPorJaba: Double): Boolean =
        taraPorJaba in Constantes.TARA_MIN..Constantes.TARA_MAX

    private fun redondear2(valor: Double): Double = Math.round(valor * 100.0) / 100.0

    private fun redondear3(valor: Double): Double = Math.round(valor * 1000.0) / 1000.0
}
