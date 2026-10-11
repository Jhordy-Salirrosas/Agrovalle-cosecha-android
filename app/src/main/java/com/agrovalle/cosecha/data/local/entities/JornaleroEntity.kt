package com.agrovalle.cosecha.data.local.entities

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

/**
 * Entidad Room: Jornalero (trabajador de campo).
 * El DNI es único e indexado para búsquedas rápidas.
 * El código QR corresponde al fotocheck impreso del jornalero.
 *
 * TODO [Coder 1]: Conectar con Room Database real y sincronización remota.
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

    @ColumnInfo(name = "cuadrilla_id")
    val cuadrillaId: String,

    @ColumnInfo(name = "esta_activo")
    val estaActivo: Boolean = true
) {
    fun nombreCompleto(): String = "$nombres $apellidos"
}
