package com.agrovalle.cosecha.util

import com.agrovalle.cosecha.model.Asistencia
import com.agrovalle.cosecha.model.Jornalero
import com.agrovalle.cosecha.model.ResumenCuadrillaCierre

/**
 * Cálculos puros para el reporte de cierre de turno de ingresos manuales (AGV-016 Escenario 2.0).
 * Aislado del framework de Android para pruebas unitarias en JVM.
 */
object CalculadoraCierreTurno {

    const val METODO_DNI_MANUAL = "DNI_MANUAL"

    /**
     * Calcula el porcentaje de ingresos manuales sobre el total de asistencias (0.0 a 100.0).
     * Devuelve 0.0 cuando no hay asistencias o cuando no hay ingresos manuales.
     */
    fun calcularPorcentajeManuales(
        cantidadManuales: Int,
        totalAsistencias: Int
    ): Double {
        if (totalAsistencias <= 0 || cantidadManuales <= 0) return 0.0
        val porcentaje = (cantidadManuales.toDouble() * 100.0) / totalAsistencias.toDouble()
        return Math.round(porcentaje * 100.0) / 100.0
    }

    /**
     * Agrupa las asistencias del día por cuadrilla y calcula totales y porcentaje de ingresos manuales.
     */
    fun resumirPorCuadrilla(
        asistenciasDelDia: List<Asistencia>,
        jornaleros: List<Jornalero>,
        cuadrillasBase: Collection<String> = emptyList()
    ): List<ResumenCuadrillaCierre> {
        val jornalerosById = jornaleros.associateBy { it.id }
        val asistenciasPorCuadrilla = linkedMapOf<String, MutableList<Asistencia>>()

        cuadrillasBase
            .filter { it.isNotBlank() }
            .forEach { codigo -> asistenciasPorCuadrilla.putIfAbsent(codigo, mutableListOf()) }

        asistenciasDelDia.forEach { asistencia ->
            val cuadrilla = jornalerosById[asistencia.jornaleroId]?.cuadrillaCodigo
                ?.takeIf { it.isNotBlank() }
                ?: "SIN_CUADRILLA"
            asistenciasPorCuadrilla.getOrPut(cuadrilla) { mutableListOf() }.add(asistencia)
        }

        return asistenciasPorCuadrilla.entries
            .sortedBy { it.key }
            .map { (cuadrilla, lista) ->
                val total = lista.size
                val manuales = lista.count { it.metodo.equals(METODO_DNI_MANUAL, ignoreCase = true) }
                ResumenCuadrillaCierre(
                    cuadrillaCodigo = cuadrilla,
                    totalAsistencias = total,
                    cantidadIngresosManuales = manuales,
                    porcentajeIngresosManuales = calcularPorcentajeManuales(manuales, total)
                )
            }
    }
}
