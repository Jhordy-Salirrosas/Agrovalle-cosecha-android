package com.agrovalle.cosecha.model

import java.io.Serializable

data class Asistencia(
    val id: Long = 0L,
    val jornaleroId: Long,
    val fecha: String,
    val horaIngreso: String,
    val estado: String = "PUNTUAL", // "PUNTUAL" o "TARDANZA"
    val metodo: String = "QR_SCAN"  // "QR_SCAN" o "DNI_MANUAL"
) : Serializable
