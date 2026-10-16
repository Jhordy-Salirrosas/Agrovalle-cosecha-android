package com.agrovalle.cosecha.model

import java.io.Serializable

data class CalibracionTara(
    val id: Long = 0L,
    val taraPromedioCalculada: Double,
    val pesoMuestra5Jabas: Double,
    val asistenteNombre: String,
    val timestamp: Long = System.currentTimeMillis(),
    val aprobado: Boolean
) : Serializable
