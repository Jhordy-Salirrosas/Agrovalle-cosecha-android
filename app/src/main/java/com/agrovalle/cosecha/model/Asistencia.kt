package com.agrovalle.cosecha.model

import java.io.Serializable

data class Asistencia(
    val id: Long = 0L,
    val jornaleroId: Long,
    val fecha: String,
    val horaIngreso: String,
    val estado: String = "PRESENTE",
    val metodo: String = "QR" // "QR" o "DNI"
) : Serializable
