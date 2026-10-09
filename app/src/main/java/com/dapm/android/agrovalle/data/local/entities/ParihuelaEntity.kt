package com.dapm.android.agrovalle.data.local.entities

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey

/**
 * Entidad que representa una parihuela (pallet) de acopio de fruta.
 * Regla de segregación estricta: NO se mezclan variedades ni cuarteles.
 * Estándar de estiba: 100-120 jabas por parihuela.
 */
@Entity(tableName = "parihuelas")
data class ParihuelaEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,

    @ColumnInfo(name = "codigo_marbete")
    val codigoMarbete: String,

    @ColumnInfo(name = "cuartel")
    val cuartel: String,

    @ColumnInfo(name = "variedad")
    val variedad: String,

    @ColumnInfo(name = "codigo_clp")
    val codigoClp: String,

    @ColumnInfo(name = "total_jabas")
    val totalJabas: Int = 0,

    @ColumnInfo(name = "peso_neto_total")
    val pesoNetoTotal: Double = 0.0,

    @ColumnInfo(name = "estado_parihuela")
    val estadoParihuela: String = "ABIERTA",

    @ColumnInfo(name = "hora_apertura")
    val horaApertura: Long = System.currentTimeMillis(),

    @ColumnInfo(name = "hora_sellado")
    val horaSellado: Long? = null,

    @ColumnInfo(name = "esta_sincronizado")
    val estaSincronizado: Boolean = false
)
