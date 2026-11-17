package com.agrovalle.cosecha.data.local.entity

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey
import java.util.Date

/**
 * Entidad Room que representa una incidencia asociada a una parihuela.
 */
@Entity(
    tableName = "incidencias",
    foreignKeys = [
        ForeignKey(
            entity = ParihuelaEntity::class,
            parentColumns = ["id"],
            childColumns = ["parihuela_id"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [Index(value = ["parihuela_id"])]
)
data class IncidenciaEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0L,
    @ColumnInfo(name = "parihuela_id")
    val parihuelaId: Long,
    val tipo: String,
    val detalle: String,
    @ColumnInfo(name = "fecha_hora")
    val fechaHora: Date,
    val estado: EstadoIncidencia = EstadoIncidencia.PENDIENTE,
    @ColumnInfo(name = "accion_tomada")
    val accionTomada: String? = null,
    @ColumnInfo(name = "hora_revision")
    val horaRevision: Date? = null,
    val responsable: String? = null
)
