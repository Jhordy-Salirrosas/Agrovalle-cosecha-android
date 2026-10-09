package com.dapm.android.agrovalle.data.local.entities

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

/**
 * Entidad que registra cada entrega de jabas pesadas por un jornalero (AGV-003, AGV-004).
 * Regla de inmutabilidad SENASA: Los registros NUNCA se borran ni editan.
 * Las correcciones se hacen mediante registros compensatorios (esRegistroCompensatorio = true).
 * Peso neto = Peso bruto - (cantidadJabas × taraCalculada).
 */
@Entity(
    tableName = "entregas_pesaje",
    foreignKeys = [
        ForeignKey(
            entity = JornaleroEntity::class,
            parentColumns = ["id"],
            childColumns = ["jornalero_id"],
            onDelete = ForeignKey.RESTRICT
        ),
        ForeignKey(
            entity = ParihuelaEntity::class,
            parentColumns = ["id"],
            childColumns = ["parihuela_id"],
            onDelete = ForeignKey.RESTRICT
        )
    ],
    indices = [
        Index(value = ["jornalero_id"]),
        Index(value = ["parihuela_id"])
    ]
)
data class EntregaPesajeEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,

    @ColumnInfo(name = "jornalero_id")
    val jornaleroId: Long,

    @ColumnInfo(name = "parihuela_id")
    val parihuelaId: Long,

    @ColumnInfo(name = "cantidad_jabas")
    val cantidadJabas: Int,

    @ColumnInfo(name = "peso_bruto")
    val pesoBruto: Double,

    @ColumnInfo(name = "tara_total")
    val taraTotal: Double,

    @ColumnInfo(name = "peso_neto")
    val pesoNeto: Double,

    @ColumnInfo(name = "marca_tiempo")
    val marcaTiempo: Long = System.currentTimeMillis(),

    @ColumnInfo(name = "esta_observado")
    val estaObservado: Boolean = false,

    @ColumnInfo(name = "motivo_observacion")
    val motivoObservacion: String? = null,

    @ColumnInfo(name = "es_registro_compensatorio")
    val esRegistroCompensatorio: Boolean = false,

    @ColumnInfo(name = "pesaje_original_id")
    val pesajeOriginalId: Long? = null,

    @ColumnInfo(name = "supervisor_autoriza")
    val supervisorAutoriza: String? = null,

    @ColumnInfo(name = "esta_sincronizado")
    val estaSincronizado: Boolean = false
)
