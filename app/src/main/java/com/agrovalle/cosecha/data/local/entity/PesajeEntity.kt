package com.agrovalle.cosecha.data.local.entity

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey
import java.util.Date

/**
 * Entidad Room que representa un pesaje de jabas asociado a una parihuela y un jornalero.
 */
@Entity(
    tableName = "pesajes",
    foreignKeys = [
        ForeignKey(
            entity = ParihuelaEntity::class,
            parentColumns = ["id"],
            childColumns = ["parihuela_id"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [Index(value = ["parihuela_id"]), Index(value = ["jornalero"])]
)
data class PesajeEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0L,
    @ColumnInfo(name = "parihuela_id")
    val parihuelaId: Long,
    val jornalero: String,
    @ColumnInfo(name = "cantidad_jabas")
    val cantidadJabas: Int,
    @ColumnInfo(name = "peso_kg")
    val pesoKg: Double = 0.0,
    @ColumnInfo(name = "fecha_hora")
    val fechaHora: Date
)
