package com.agrovalle.cosecha.data.local.entity

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey
import java.util.Date

/**
 * Entidad Room que registra la reasignación de un jornalero entre cuarteles (AGV-005).
 */
@Entity(
    tableName = "reasignaciones",
    indices = [Index(value = ["jornalero"])]
)
data class ReasignacionEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0L,
    val jornalero: String,
    @ColumnInfo(name = "cuartel_origen")
    val cuartelOrigen: String,
    @ColumnInfo(name = "cuartel_destino")
    val cuartelDestino: String,
    val motivo: String,
    val hora: Date,
    val sincronizado: Boolean = false
)
