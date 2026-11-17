package com.agrovalle.cosecha.data.repository

import com.agrovalle.cosecha.data.DatosMemoria

/**
 * Opción seleccionable de jornalero con asistencia registrada en la jornada (AGV-006).
 */
data class JornaleroAsistenciaOpcion(
    val id: Long,
    val nombreCompleto: String,
    val cuartelActual: String?
) {
    val etiqueta: String
        get() = if (cuartelActual.isNullOrBlank()) {
            nombreCompleto
        } else {
            "$nombreCompleto ($cuartelActual)"
        }
}

/**
 * Proveedor y validador del selector de jornaleros alimentado por la asistencia del día (AGV-006).
 */
object SelectorJornalerosAsistencia {

    fun cargarParaFecha(fecha: String): List<JornaleroAsistenciaOpcion> {
        return DatosMemoria.jornalerosConAsistencia(fecha).map { jornalero ->
            JornaleroAsistenciaOpcion(
                id = jornalero.id,
                nombreCompleto = jornalero.nombreCompleto,
                cuartelActual = DatosMemoria.obtenerCuartelAsignado(jornalero.id)
            )
        }
    }

    fun puedeRegistrarPesaje(
        disponibles: List<JornaleroAsistenciaOpcion>,
        seleccionado: String?
    ): Boolean {
        if (disponibles.isEmpty() || seleccionado.isNullOrBlank()) return false
        return disponibles.any {
            it.nombreCompleto.equals(seleccionado.trim(), ignoreCase = true)
        }
    }
}
