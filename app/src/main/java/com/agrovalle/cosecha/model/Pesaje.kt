package com.agrovalle.cosecha.model

import java.io.Serializable

data class Pesaje(
    val id: Long = 0L,
    val jornaleroId: Long,
    val cantidadJabas: Int,
    val pesoBruto: Double,
    val taraTotal: Double,
    val pesoNeto: Double,
    val timestamp: Long = System.currentTimeMillis(),
    val esTaraPorDefecto: Boolean = false
) : Serializable
