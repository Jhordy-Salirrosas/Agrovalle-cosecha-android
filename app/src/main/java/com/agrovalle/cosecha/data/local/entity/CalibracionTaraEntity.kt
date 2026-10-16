package com.agrovalle.cosecha.data.local.entity

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey

/**
 * Calibración de tara del turno (tabla `calibraciones`) - AGV-015.
 *
 * Se pesan 5 jabas vacías: [pesoMuestra5Jabas] es la suma de las 5 muestras
 * y [taraPromedioCalculada] = pesoMuestra5Jabas / 5.
 */
@Entity(tableName = "calibraciones")
data class CalibracionTaraEntity(
    @PrimaryKey(autoGenerate = true)
    @ColumnInfo(name = "id")
    val id: Long = 0L,

    @ColumnInfo(name = "tara_promedio_calculada")
    val taraPromedioCalculada: Double,

    @ColumnInfo(name = "peso_muestra_5_jabas")
    val pesoMuestra5Jabas: Double,

    @ColumnInfo(name = "asistente_nombre")
    val asistenteNombre: String,

    @ColumnInfo(name = "timestamp")
    val timestamp: Long,

    @ColumnInfo(name = "aprobado")
    val aprobado: Boolean
)
