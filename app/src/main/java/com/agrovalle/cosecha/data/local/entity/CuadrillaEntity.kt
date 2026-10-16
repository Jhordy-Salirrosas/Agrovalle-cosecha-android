package com.agrovalle.cosecha.data.local.entity

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

/**
 * Cuadrilla de jornaleros (tabla `cuadrillas`).
 * Relación 1:N con [JornaleroEntity] a través de `codigo`.
 */
@Entity(
    tableName = "cuadrillas",
    indices = [Index(value = ["codigo"], unique = true)]
)
data class CuadrillaEntity(
    @PrimaryKey(autoGenerate = true)
    @ColumnInfo(name = "id")
    val id: Long = 0L,

    @ColumnInfo(name = "codigo")
    val codigo: String,

    @ColumnInfo(name = "nombre_supervisor")
    val nombreSupervisor: String,

    @ColumnInfo(name = "fundo_cuartel")
    val fundoCuartel: String
)
