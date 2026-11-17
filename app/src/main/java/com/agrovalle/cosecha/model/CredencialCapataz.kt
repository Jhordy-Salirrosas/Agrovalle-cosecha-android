package com.agrovalle.cosecha.model

import java.io.Serializable

/** Credencial de capataz autorizada para aprobar pesajes compensatorios (AGV-015). */
data class CredencialCapataz(
    val id: Long,
    val nombre: String,
    val codigoQr: String,
    val cuadrillaCodigo: String
) : Serializable
