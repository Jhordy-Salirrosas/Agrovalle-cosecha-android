package com.dapm.android.agrovalle.data.local.entities

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey

/**
 * Entidad que almacena la calibración de tara de jabas (AGV-015).
 * Se pesan 5 jabas vacías de muestra y se calcula el promedio.
 * Rango válido: 0.350 kg a 0.400 kg (nominal 0.375 kg).
 * Tolerancia: ±20 gramos (0.020 kg).
 */
@Entity(tableName = "calibraciones_tara")
data class CalibracionTaraEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,

    @ColumnInfo(name = "marca_tiempo")
    val marcaTiempo: Long = System.currentTimeMillis(),

    @ColumnInfo(name = "peso_muestra_5_jabas")
    val pesoMuestra5Jabas: Double,

    @ColumnInfo(name = "tara_promedio_calculada")
    val taraPromedioCalculada: Double,

    @ColumnInfo(name = "tolerancia_gramos")
    val toleranciaGramos: Double = 0.020,

    @ColumnInfo(name = "esta_aprobado")
    val estaAprobado: Boolean,

    @ColumnInfo(name = "nombre_asistente")
    val nombreAsistente: String = "Maribel Cárdenas Tello"
)
