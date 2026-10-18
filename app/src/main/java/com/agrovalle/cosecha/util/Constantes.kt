package com.agrovalle.cosecha.util

/**
 * Constantes de negocio del Sprint 1 (fuente: HU del Product Backlog).
 */
object Constantes {

    /** Tara por defecto por jaba cuando el turno no fue calibrado (AGV-003). */
    const val TARA_POR_DEFECTO = 0.375

    /**
     * Tamano REAL del padron precargado en memoria (AGV-001).
     *
     * Debe coincidir con DatosMemoria.TOTAL_PADRON, porque el contador de la
     * pantalla de asistencia se muestra como "asistentes / TOTAL_CUADRILLA".
     * Si ambos valores se desincronizan el contador nunca llega al 100%.
     */
    const val TOTAL_CUADRILLA = 30

    /** Estandar de estiba de una parihuela (AGV-006, Sprint 2). */
    const val JABAS_ESTANDAR_MIN = 100
    const val JABAS_ESTANDAR_MAX = 120

    /** Rangos validos de peso bruto para evitar errores de tipeo (AGV-003). */
    const val PESO_BRUTO_MIN = 0.10
    const val PESO_BRUTO_MAX = 60.00

    /** Cantidad de jabas que se pesan en la calibracion de tara (AGV-015). */
    const val MUESTRAS_CALIBRACION = 5

    /** Rango valido de tara por jaba: 350 g a 400 g (AGV-015). */
    const val TARA_MIN = 0.350
    const val TARA_MAX = 0.400

    /** Tolerancia de reclamo en gramos (AGV-015). */
    const val TOLERANCIA_RECLAMO_GRAMOS = 20.0
}
