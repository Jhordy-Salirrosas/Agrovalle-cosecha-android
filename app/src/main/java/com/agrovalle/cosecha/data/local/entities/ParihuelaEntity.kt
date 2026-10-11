package com.agrovalle.cosecha.data.local.entities

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey

/**
 * Entidad Room: Parihuela (bandeja de cosecha).
 * Define el contexto agronómico: cuartel, CLP, variedad.
 * Una parihuela ABIERTA bloquea el cambio de contexto hasta que se cierre.
 */
@Entity(tableName = "parihuelas")
data class ParihuelaEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,

    @ColumnInfo(name = "codigo")
    val codigo: String, // Ej: "#PAR-001"

    @ColumnInfo(name = "cuartel")
    val cuartel: String,

    @ColumnInfo(name = "clp")
    val clp: String,

    @ColumnInfo(name = "variedad")
    val variedad: String, // "Biloxi" o "Ventura"

    @ColumnInfo(name = "estado")
    val estado: String, // "ABIERTA" o "CERRADA"

    @ColumnInfo(name = "fecha_apertura")
    val fechaApertura: String,

    @ColumnInfo(name = "fecha_cierre")
    val fechaCierre: String? = null
)
