package com.agrovalle.cosecha.data.local.entity

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

/**
 * Pesaje / entrega registrada (tabla `entregas_pesaje`).
 *
 * Peso Neto = Peso Bruto - (Tara * Jabas)  ->  almacenado en [pesoNeto].
 * [esTaraPorDefecto] marca los pesajes hechos sin calibración previa (AGV-003, escenario 2).
 */
@Entity(
    tableName = "entregas_pesaje",
    foreignKeys = [
        ForeignKey(
            entity = JornaleroEntity::class,
            parentColumns = ["id"],
            childColumns = ["jornalero_id"],
            onDelete = ForeignKey.CASCADE
        ),
        ForeignKey(
            entity = ParihuelaEntity::class,
            parentColumns = ["id"],
            childColumns = ["parihuela_id"],
            onDelete = ForeignKey.SET_NULL
        )
    ],
    indices = [
        Index(value = ["jornalero_id"]),
        Index(value = ["parihuela_id"])
    ]
)
data class PesajeEntity(
    @PrimaryKey(autoGenerate = true)
    @ColumnInfo(name = "id")
    val id: Long = 0L,

    @ColumnInfo(name = "jornalero_id")
    val jornaleroId: Long,

    @ColumnInfo(name = "parihuela_id")
    val parihuelaId: Long?,

    /** Cantidad de jabas (típicamente 3, 4 o 6). */
    @ColumnInfo(name = "cantidad_jabas")
    val cantidadJabas: Int,

    @ColumnInfo(name = "peso_bruto")
    val pesoBruto: Double,

    /** Tara total aplicada = tara por jaba * cantidad de jabas. */
    @ColumnInfo(name = "tara_total")
    val taraTotal: Double,

    @ColumnInfo(name = "peso_neto")
    val pesoNeto: Double,

    @ColumnInfo(name = "timestamp")
    val timestamp: Long,

    @ColumnInfo(name = "es_tara_defecto")
    val esTaraPorDefecto: Boolean
)
