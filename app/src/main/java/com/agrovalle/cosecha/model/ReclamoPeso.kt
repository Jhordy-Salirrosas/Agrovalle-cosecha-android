package com.agrovalle.cosecha.model

import java.io.Serializable

enum class EstadoReclamoPeso {
    DENTRO_TOLERANCIA,
    COMPENSADO
}

/**
 * Registro de la bitácora de reclamos de peso (AGV-015 escenarios 2.0 y 3.0).
 */
data class ReclamoPeso(
    val id: Long = 0L,
    val fecha: String,
    val hora: String,
    val timestamp: Long,
    val jornaleroId: Long,
    val jornaleroNombre: String,
    val pesajeOriginalId: Long,
    val pesoOriginalNetoKg: Double,
    val pesoBrutoReclamoKg: Double,
    val taraAplicadaKg: Double,
    val pesoNuevoNetoKg: Double,
    val diferenciaKg: Double,
    val diferenciaGramos: Double,
    val diferenciaAbsolutaGramos: Double,
    val excedeTolerancia: Boolean,
    val estado: EstadoReclamoPeso,
    val pesajeCompensatorioId: Long? = null,
    val pesoCompensatorioKg: Double? = null,
    val capatazAutorizador: String? = null,
    val codigoQrCapataz: String? = null
) : Serializable
