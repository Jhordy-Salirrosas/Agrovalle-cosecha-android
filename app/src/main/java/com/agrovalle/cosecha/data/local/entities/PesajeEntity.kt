package com.agrovalle.cosecha.data.local.entities

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

/**
 * Entidad Room: Registro de pesaje de un jornalero.
 * Almacena peso bruto, jabas, tara y peso neto calculado.
 * Indica si se usó tara por defecto (sin calibración).
 */
@Entity(
    tableName = "pesajes",
    foreignKeys = [
        ForeignKey(
            entity = ParihuelaEntity::class,
            parentColumns = ["id"],
            childColumns = ["parihuela_id"],
            onDelete = ForeignKey.CASCADE
        ),
        ForeignKey(
            entity = JornaleroEntity::class,
            parentColumns = ["id"],
            childColumns = ["jornalero_id"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [
        Index(value = ["parihuela_id"]),
        Index(value = ["jornalero_id"])
    ]
)
data class PesajeEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,

    @ColumnInfo(name = "parihuela_id")
    val parihuelaId: Long,

    @ColumnInfo(name = "jornalero_id")
    val jornaleroId: Long,

    @ColumnInfo(name = "peso_bruto")
    val pesoBruto: Double,

    @ColumnInfo(name = "cantidad_jabas")
    val cantidadJabas: Int,

    @ColumnInfo(name = "tara_unitaria")
    val taraUnitaria: Double,

    @ColumnInfo(name = "tara_total")
    val taraTotal: Double,

    @ColumnInfo(name = "peso_neto")
    val pesoNeto: Double,

    @ColumnInfo(name = "es_tara_por_defecto")
    val esTaraPorDefecto: Boolean = false,

    @ColumnInfo(name = "fecha_hora")
    val fechaHora: String
)
