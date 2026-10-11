package com.agrovalle.cosecha.data.local.entities

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey

/**
 * Entidad Room: Calibración de tara (peso de jabas vacías).
 * Se pesan 5 jabas para obtener un promedio.
 * Si se omite, se usa tara por defecto (0.375 kg).
 */
@Entity(tableName = "calibraciones_tara")
data class CalibracionTaraEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,

    @ColumnInfo(name = "fecha_hora")
    val fechaHora: String,

    @ColumnInfo(name = "responsable")
    val responsable: String,

    @ColumnInfo(name = "peso_jaba_1")
    val pesoJaba1: Double? = null,

    @ColumnInfo(name = "peso_jaba_2")
    val pesoJaba2: Double? = null,

    @ColumnInfo(name = "peso_jaba_3")
    val pesoJaba3: Double? = null,

    @ColumnInfo(name = "peso_jaba_4")
    val pesoJaba4: Double? = null,

    @ColumnInfo(name = "peso_jaba_5")
    val pesoJaba5: Double? = null,

    @ColumnInfo(name = "tara_promedio")
    val taraPromedio: Double = 0.375,

    @ColumnInfo(name = "es_omitida")
    val esOmitida: Boolean = false
)
