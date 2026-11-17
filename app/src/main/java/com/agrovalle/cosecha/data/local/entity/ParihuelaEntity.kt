package com.agrovalle.cosecha.data.local.entity

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey
import java.util.Date

/**
 * Entidad Room que representa una parihuela de cosecha.
 */
@Entity(
    tableName = "parihuelas",
    indices = [Index(value = ["codigo"], unique = true)]
)
data class ParihuelaEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0L,
    val codigo: String,
    val estado: EstadoParihuela = EstadoParihuela.ABIERTA,
    val variedad: String,
    val cuartel: String,
    @ColumnInfo(name = "conteo_jabas")
    val conteoJabas: Int = 0,
    val pisos: Int = 0,
    @ColumnInfo(name = "hora_inicio")
    val horaInicio: Date,
    @ColumnInfo(name = "hora_sellado")
    val horaSellado: Date? = null,
    @ColumnInfo(name = "motivo_sellado_anticipado")
    val motivoSelladoAnticipado: String? = null
)
