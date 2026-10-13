package com.agrovalle.cosecha.model

import java.io.Serializable

data class CalibracionTara(
    val id: Long = 0L,
    val taraPromedio: Double,
    val pesoMuestra: Double,
    val fecha: String,
    val aprobado: Boolean
) : Serializable
