package com.agrovalle.cosecha.model

import java.io.Serializable

/**
 * Entrada de bitácora para un ingreso manual de asistencia por DNI (AGV-016 - Escenario 2.0).
 */
data class RegistroIngresoManual(
    val id: Long = 0L,
    val jornaleroId: Long,
    val jornaleroNombre: String,
    val dni: String,
    val cuadrillaCodigo: String,
    val fecha: String,
    val hora: String,
    val motivo: String,
    val capatazResponsable: String
) : Serializable

/**
 * Resumen de asistencias e ingresos manuales por cuadrilla al cierre del turno.
 */
data class ResumenCuadrillaCierre(
    val cuadrillaCodigo: String,
    val totalAsistencias: Int,
    val cantidadIngresosManuales: Int,
    val porcentajeIngresosManuales: Double
)
