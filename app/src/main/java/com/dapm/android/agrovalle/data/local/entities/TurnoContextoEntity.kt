package com.dapm.android.agrovalle.data.local.entities

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey

/**
 * Entidad que almacena el contexto del turno de trabajo activo.
 * Define el fundo, cuartel, variedad y capataz para la jornada.
 * Solo debe existir un turno con estado "ABIERTO" a la vez.
 */
@Entity(tableName = "turnos_contexto")
data class TurnoContextoEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,

    @ColumnInfo(name = "fundo")
    val fundo: String,

    @ColumnInfo(name = "cuartel")
    val cuartel: String,

    @ColumnInfo(name = "codigo_clp")
    val codigoClp: String,

    @ColumnInfo(name = "variedad")
    val variedad: String,

    @ColumnInfo(name = "nombre_capataz")
    val nombreCapataz: String,

    @ColumnInfo(name = "fecha_turno")
    val fechaTurno: String,

    @ColumnInfo(name = "parihuela_activa_id")
    val parihuelaActivaId: Long? = null,

    @ColumnInfo(name = "estado_turno")
    val estadoTurno: String = "ABIERTO"
)
