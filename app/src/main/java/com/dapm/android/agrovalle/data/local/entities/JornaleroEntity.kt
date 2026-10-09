package com.dapm.android.agrovalle.data.local.entities

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

/**
 * Entidad que representa a un jornalero (trabajador de campo) en el sistema.
 * El DNI es único e indexado para búsquedas rápidas por documento.
 * El código QR corresponde al fotocheck impreso del jornalero.
 */
@Entity(
    tableName = "jornaleros",
    indices = [Index(value = ["dni"], unique = true)]
)
data class JornaleroEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,

    @ColumnInfo(name = "dni")
    val dni: String,

    @ColumnInfo(name = "codigo_qr")
    val codigoQr: String,

    @ColumnInfo(name = "nombres")
    val nombres: String,

    @ColumnInfo(name = "apellidos")
    val apellidos: String,

    @ColumnInfo(name = "codigo_cuadrilla")
    val codigoCuadrilla: String,

    @ColumnInfo(name = "esta_activo")
    val estaActivo: Boolean = true
) {
    /** Retorna el nombre completo del jornalero. */
    fun nombreCompleto(): String = "$nombres $apellidos"
}
