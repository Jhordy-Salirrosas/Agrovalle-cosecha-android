package com.agrovalle.cosecha.model

import java.io.Serializable

data class Parihuela(
    val id: Long = 0L,
    val codigoMarbete: String,
    val cuartel: String,
    val variedad: String,
    val clp: String,
    val estado: String = "ABIERTA" // "ABIERTA", "CERRADA"
) : Serializable
