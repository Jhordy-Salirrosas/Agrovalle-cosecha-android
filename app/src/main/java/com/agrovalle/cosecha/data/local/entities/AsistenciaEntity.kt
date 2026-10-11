package com.agrovalle.cosecha.data.local.entities

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

/**
 * Entidad Room: Registro de asistencia de un jornalero.
 * Tipo de ingreso: QR (escaneo de fotocheck) o MANUAL (búsqueda por DNI).
 */
@Entity(
    tableName = "asistencias",
    foreignKeys = [
        ForeignKey(
            entity = JornaleroEntity::class,
            parentColumns = ["id"],
            childColumns = ["jornalero_id"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [Index(value = ["jornalero_id"])]
)
data class AsistenciaEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,

    @ColumnInfo(name = "jornalero_id")
    val jornaleroId: Long,

    @ColumnInfo(name = "fecha_hora")
    val fechaHora: String,

    @ColumnInfo(name = "tipo_ingreso")
    val tipoIngreso: String, // "QR" o "MANUAL"

    @ColumnInfo(name = "observacion")
    val observacion: String? = null
)
