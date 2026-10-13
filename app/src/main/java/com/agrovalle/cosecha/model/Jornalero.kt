package com.agrovalle.cosecha.model

import java.io.Serializable

data class Jornalero(
    val id: Long = 0L,
    val dni: String,
    val nombres: String,
    val apellidos: String,
    val codigoQr: String,
    val cuadrillaCodigo: String
) : Serializable {
    val nombreCompleto: String
        get() = "$nombres $apellidos"
}
