package com.agrovalle.cosecha.data.local.entities

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey

/**
 * Entidad Room: Cuadrilla de trabajadores.
 * Cada cuadrilla tiene un capataz responsable.
 */
@Entity(tableName = "cuadrillas")
data class CuadrillaEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,

    @ColumnInfo(name = "nombre")
    val nombre: String,

    @ColumnInfo(name = "capataz_id")
    val capatazId: Long? = null,

    @ColumnInfo(name = "total_trabajadores")
    val totalTrabajadores: Int = 35
)
