package com.agrovalle.cosecha.util

import kotlin.math.abs

data class EvaluacionReclamoPeso(
    val pesoOriginalNetoKg: Double,
    val pesoNuevoNetoKg: Double,
    val diferenciaKg: Double,
    val diferenciaGramos: Double,
    val diferenciaAbsolutaGramos: Double,
    val excedeTolerancia: Boolean,
    val pesoCompensatorioKg: Double
)

/**
 * Cálculos puros del flujo de reclamos de peso (AGV-015 escenarios 2.0 y 3.0).
 */
object CalculadoraReclamoPeso {

    /**
     * Calcula el peso neto en kg con precisión de gramo (3 decimales) a partir de
     * peso bruto, jabas y tara por jaba.
     */
    fun calcularPesoNetoPrecisionGramos(
        pesoBrutoKg: Double,
        cantidadJabas: Int,
        taraPorJabaKg: Double
    ): Double {
        require(pesoBrutoKg.isFinite() && pesoBrutoKg > 0.0) {
            "El peso bruto del reclamo debe ser mayor a 0"
        }
        require(cantidadJabas > 0) {
            "La cantidad de jabas debe ser mayor a 0"
        }
        require(taraPorJabaKg.isFinite() && taraPorJabaKg >= 0.0) {
            "La tara por jaba no es válida"
        }
        val taraTotal = redondear3(taraPorJabaKg * cantidadJabas)
        return redondear3((pesoBrutoKg - taraTotal).coerceAtLeast(0.0))
    }

    /**
     * Compara [pesoNuevoNetoKg] contra [pesoOriginalNetoKg] usando [toleranciaGramos] (20 g).
     * Si la diferencia absoluta es <= 20 g se mantiene el original; si es > 20 g
     * se requiere compensatorio autorizado por el capataz.
     */
    fun evaluarReclamo(
        pesoOriginalNetoKg: Double,
        pesoNuevoNetoKg: Double,
        toleranciaGramos: Double = Constantes.TOLERANCIA_RECLAMO_GRAMOS
    ): EvaluacionReclamoPeso {
        require(pesoOriginalNetoKg.isFinite() && pesoOriginalNetoKg >= 0.0) {
            "El peso neto original no es válido"
        }
        require(pesoNuevoNetoKg.isFinite() && pesoNuevoNetoKg >= 0.0) {
            "El nuevo peso neto no es válido"
        }
        require(toleranciaGramos.isFinite() && toleranciaGramos >= 0.0) {
            "La tolerancia de reclamo no es válida"
        }

        val origGramos = Math.round(pesoOriginalNetoKg * 1000.0)
        val nuevoGramos = Math.round(pesoNuevoNetoKg * 1000.0)
        val diffGramos = nuevoGramos - origGramos
        val absDiffGramos = abs(diffGramos)
        val excede = absDiffGramos.toDouble() > toleranciaGramos
        val diffKg = diffGramos / 1000.0

        return EvaluacionReclamoPeso(
            pesoOriginalNetoKg = origGramos / 1000.0,
            pesoNuevoNetoKg = nuevoGramos / 1000.0,
            diferenciaKg = diffKg,
            diferenciaGramos = diffGramos.toDouble(),
            diferenciaAbsolutaGramos = absDiffGramos.toDouble(),
            excedeTolerancia = excede,
            pesoCompensatorioKg = if (excede) diffKg else 0.0
        )
    }

    private fun redondear3(valor: Double): Double = Math.round(valor * 1000.0) / 1000.0
}
