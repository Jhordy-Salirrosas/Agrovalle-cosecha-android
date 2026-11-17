package com.agrovalle.cosecha.data.local.entity

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey
import java.util.Date

/**
 * Entidad Room que registra eventos de bitacora vinculados a una parihuela.
 */
@Entity(
    tableName = "bitacora",
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
data class BitacoraEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0L,
    @ColumnInfo(name = "parihuela_id")
    val parihuelaId: Long,
    val evento: String,
    val detalle: String,
    @ColumnInfo(name = "fecha_hora")
    val fechaHora: Date
)
