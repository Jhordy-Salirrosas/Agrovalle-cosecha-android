package com.agrovalle.cosecha.util

import java.util.Locale
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
     * Resuelve la tara total a descontar en un reclamo (H-10):
     * utiliza la tara total guardada en el pesaje original ([taraTotalOriginalKg]) si es válida (> 0);
     * solo si el registro original no tuviera tara guardada, calcula el respaldo con [taraPorJabaRespaldoKg].
     */
    fun resolverTaraTotal(
        taraTotalOriginalKg: Double,
        cantidadJabas: Int,
        taraPorJabaRespaldoKg: Double
    ): Double {
        if (taraTotalOriginalKg.isFinite() && taraTotalOriginalKg > 0.0) {
            return Math.round(taraTotalOriginalKg * 100.0) / 100.0
        }
        require(cantidadJabas > 0) {
            "La cantidad de jabas debe ser mayor a 0"
        }
        require(taraPorJabaRespaldoKg.isFinite() && taraPorJabaRespaldoKg >= 0.0) {
            "La tara por jaba no es válida"
        }
        return CalculadoraPesaje.calcularTaraTotal(cantidadJabas, taraPorJabaRespaldoKg)
    }

    /**
     * Calcula el peso neto del re-pesaje descontando directamente [taraTotalKg]
     * con la misma precisión (2 decimales) que [CalculadoraPesaje].
     * Exige que el peso bruto supere la tara total (neto > 0).
     */
    fun calcularPesoNetoConTaraTotal(
        pesoBrutoKg: Double,
        taraTotalKg: Double
    ): Double {
        require(pesoBrutoKg.isFinite() && pesoBrutoKg > 0.0) {
            "El peso bruto del reclamo debe ser mayor a 0"
        }
        require(taraTotalKg.isFinite() && taraTotalKg >= 0.0) {
            "La tara total no es válida"
        }
        val taraRedondeada = Math.round(taraTotalKg * 100.0) / 100.0
        require(pesoBrutoKg > taraRedondeada) {
            String.format(
                Locale.US,
                "El peso bruto del re-pesaje (%.2f kg) debe ser mayor a la tara total (%.2f kg)",
                pesoBrutoKg,
                taraRedondeada
            )
        }
        val pesoNeto = Math.round((pesoBrutoKg - taraRedondeada) * 100.0) / 100.0
        require(pesoNeto > 0.0) {
            String.format(
                Locale.US,
                "El peso neto del re-pesaje debe ser mayor a 0 kg (tara total: %.2f kg)",
                taraRedondeada
            )
        }
        return pesoNeto
    }

    /**
     * Calcula el peso neto del re-pesaje con la misma precisión (2 decimales) que
     * [CalculadoraPesaje]. Exige que el peso bruto supere la tara total (neto > 0).
     */
    fun calcularPesoNeto(
        pesoBrutoKg: Double,
        cantidadJabas: Int,
        taraPorJabaKg: Double
    ): Double {
        require(cantidadJabas > 0) {
            "La cantidad de jabas debe ser mayor a 0"
        }
        require(taraPorJabaKg.isFinite() && taraPorJabaKg >= 0.0) {
            "La tara por jaba no es válida"
        }
        val taraTotal = CalculadoraPesaje.calcularTaraTotal(cantidadJabas, taraPorJabaKg)
        return calcularPesoNetoConTaraTotal(pesoBrutoKg, taraTotal)
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
            pesoCompensatorioKg = if (excede) nuevoGramos / 1000.0 else 0.0
        )
    }
}
