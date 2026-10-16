package com.agrovalle.cosecha.data.local.entity

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey

/**
 * Parihuela digital del turno (tabla `parihuelas`).
 *
 * [estado]: ABIERTA | SELLADA
 * [totalJabas]: estándar de estiba de 100 a 120 jabas (AGV-006, Sprint 2).
 */
@Entity(tableName = "parihuelas")
data class ParihuelaEntity(
    @PrimaryKey(autoGenerate = true)
    @ColumnInfo(name = "id")
    val id: Long = 0L,

    @ColumnInfo(name = "codigo_marbete")
    val codigoMarbete: String,

    @ColumnInfo(name = "cuartel")
    val cuartel: String,

    @ColumnInfo(name = "variedad")
    val variedad: String,

    @ColumnInfo(name = "clp")
    val clp: String,

    @ColumnInfo(name = "total_jabas")
    val totalJabas: Int = 0,

    /** ABIERTA | SELLADA */
    @ColumnInfo(name = "estado")
    val estado: String = ESTADO_ABIERTA
) {
    companion object {
        const val ESTADO_ABIERTA = "ABIERTA"
        const val ESTADO_SELLADA = "SELLADA"
    }
}
