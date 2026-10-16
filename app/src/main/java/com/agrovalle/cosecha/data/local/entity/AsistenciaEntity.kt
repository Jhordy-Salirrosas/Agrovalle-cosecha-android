package com.agrovalle.cosecha.data.local.entity

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

/**
 * Registro de asistencia diaria (tabla `asistencias`).
 *
 * [estado]: PUNTUAL | TARDANZA
 * [metodo]: QR_SCAN | DNI_MANUAL
 *
 * El índice único (jornalero_id, fecha) impide duplicar la asistencia
 * del mismo jornalero en un mismo día (regla de AGV-001).
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
    indices = [
        Index(value = ["jornalero_id", "fecha"], unique = true)
    ]
)
data class AsistenciaEntity(
    @PrimaryKey(autoGenerate = true)
    @ColumnInfo(name = "id")
    val id: Long = 0L,

    @ColumnInfo(name = "jornalero_id")
    val jornaleroId: Long,

    /** Fecha de la jornada en formato yyyy-MM-dd. */
    @ColumnInfo(name = "fecha")
    val fecha: String,

    /** Hora exacta de ingreso en formato HH:mm:ss. */
    @ColumnInfo(name = "hora_ingreso")
    val horaIngreso: String,

    /** PUNTUAL | TARDANZA */
    @ColumnInfo(name = "estado")
    val estado: String,

    /** QR_SCAN | DNI_MANUAL */
    @ColumnInfo(name = "metodo")
    val metodo: String
)
